package com.kyant.backdrop.catalog.linkedin

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

class AgentRealtimeVoiceManager {
    companion object {
        private const val SampleRateHz = 24_000
        private const val BytesPerSample = 2
        private const val ChunkDurationMs = 60
        private const val ChunkSizeBytes = SampleRateHz * BytesPerSample * ChunkDurationMs / 1000
    }

    private data class PlaybackChunk(
        val generation: Int,
        val responseId: String?,
        val audioBase64: String
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playbackMutex = Mutex()
    private val playbackQueue = Channel<PlaybackChunk>(capacity = Channel.UNLIMITED)

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var audioTrack: AudioTrack? = null
    @Volatile
    private var suppressPlayback = false
    @Volatile
    private var currentPlaybackResponseId: String? = null
    @Volatile
    private var playbackGeneration = 0
    @Volatile
    private var isCapturingAudio = false

    init {
        scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            for (chunk in playbackQueue) {
                if (chunk.generation != playbackGeneration || suppressPlayback) {
                    continue
                }

                val bytes = runCatching {
                    Base64.decode(chunk.audioBase64, Base64.DEFAULT)
                }.getOrNull() ?: continue

                playbackMutex.withLock {
                    if (chunk.generation != playbackGeneration || suppressPlayback) {
                        return@withLock
                    }

                    val track = ensureAudioTrack()
                    if (chunk.responseId != null && currentPlaybackResponseId != chunk.responseId) {
                        currentPlaybackResponseId = chunk.responseId
                        runCatching {
                            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                track.pause()
                            }
                            track.flush()
                        }
                    }

                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        track.play()
                    }

                    var offset = 0
                    while (offset < bytes.size) {
                        if (chunk.generation != playbackGeneration || suppressPlayback) {
                            break
                        }

                        val written = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            track.write(
                                bytes,
                                offset,
                                bytes.size - offset,
                                AudioTrack.WRITE_BLOCKING
                            )
                        } else {
                            track.write(bytes, offset, bytes.size - offset)
                        }

                        if (written <= 0) {
                            break
                        }
                        offset += written
                    }
                }
            }
        }
    }

    fun startCapture(onAudioChunk: (String) -> Unit) {
        if (captureJob?.isActive == true) {
            return
        }

        suppressPlayback = false

        val minBufferSize = AudioRecord.getMinBufferSize(
            SampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize, ChunkSizeBytes * 4)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("Could not initialize realtime voice capture.")
        }

        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(recorder.audioSessionId)?.apply {
                enabled = true
            }
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(recorder.audioSessionId)?.apply {
                enabled = true
            }
        }

        val readBuffer = ByteArray(ChunkSizeBytes)
        recorder.startRecording()
        audioRecord = recorder
        isCapturingAudio = true

        captureJob = scope.launch {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (isActive) {
                val read = recorder.read(readBuffer, 0, readBuffer.size)
                if (read <= 0) {
                    continue
                }

                val chunk = if (read == readBuffer.size) {
                    readBuffer.copyOf()
                } else {
                    readBuffer.copyOf(read)
                }
                onAudioChunk(Base64.encodeToString(chunk, Base64.NO_WRAP))
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        isCapturingAudio = false

        runCatching {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        }
        audioRecord = null

        runCatching {
            echoCanceler?.release()
            noiseSuppressor?.release()
        }
        echoCanceler = null
        noiseSuppressor = null
    }

    fun preparePlayback() {
        scope.launch {
            playbackMutex.withLock {
                ensureAudioTrack()
            }
        }
    }

    fun playAssistantChunk(responseId: String?, audioBase64: String) {
        if (audioBase64.isBlank() || suppressPlayback) {
            return
        }

        playbackQueue.trySend(
            PlaybackChunk(
                generation = playbackGeneration,
                responseId = responseId?.takeIf { it.isNotBlank() },
                audioBase64 = audioBase64
            )
        )
    }

    fun stopAssistantPlayback(flush: Boolean = true) {
        if (flush) {
            invalidatePlaybackQueue()
        }
        scope.launch {
            playbackMutex.withLock {
                runCatching {
                    audioTrack?.pause()
                    if (flush) {
                        audioTrack?.flush()
                        currentPlaybackResponseId = null
                    }
                }
            }
        }
    }

    fun setAssistantPlaybackSuppressed(
        suppressed: Boolean,
        flush: Boolean = suppressed
    ) {
        suppressPlayback = suppressed
        if (flush) {
            stopAssistantPlayback(flush = true)
        }
    }

    fun release() {
        stopCapture()
        suppressPlayback = false
        invalidatePlaybackQueue()
        currentPlaybackResponseId = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        }
        audioTrack = null
        playbackQueue.close()
        scope.cancel()
    }

    fun isCapturing(): Boolean = isCapturingAudio

    private fun invalidatePlaybackQueue() {
        playbackGeneration += 1
        currentPlaybackResponseId = null
    }

    private fun ensureAudioTrack(): AudioTrack {
        audioTrack?.let { return it }

        val minBufferSize = AudioTrack.getMinBufferSize(
            SampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBufferSize, ChunkSizeBytes * 3)
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }
        val track = builder.build()

        audioTrack = track
        return track
    }
}
