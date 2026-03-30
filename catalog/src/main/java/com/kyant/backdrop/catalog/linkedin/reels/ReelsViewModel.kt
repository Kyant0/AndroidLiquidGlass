package com.kyant.backdrop.catalog.linkedin.reels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.network.models.ReelComment
import com.kyant.backdrop.catalog.network.models.ReelsFeedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Preload status for a reel
 */
enum class PreloadStatus {
    NOT_STARTED,
    PRELOADING,
    PRELOADED,
    FAILED
}

/**
 * UI State for Reels feature
 */
data class ReelsUiState(
    // Preview section state (for home feed)
    val previewReels: List<Reel> = emptyList(),
    val isLoadingPreview: Boolean = false,
    val previewError: String? = null,
    
    // Full feed state
    val feedReels: List<Reel> = emptyList(),
    val isLoadingFeed: Boolean = false,
    val feedError: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    
    // Current reel viewer state
    val isViewerOpen: Boolean = false,
    val currentReelIndex: Int = 0,

    // Comments state (supports nested replies)
    val showCommentsSheet: Boolean = false,
    val commentsReelId: String? = null,
    val reelComments: List<ReelComment> = emptyList(),
    val replyCommentsByParent: Map<String, List<ReelComment>> = emptyMap(),
    val expandedReplyParents: Set<String> = emptySet(),
    val commentsCursor: String? = null,
    val hasMoreComments: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val isSubmittingComment: Boolean = false,
    val commentsError: String? = null,
    val replyToComment: ReelComment? = null,
    
    // Preload status map (reelId -> status)
    val preloadStatus: Map<String, PreloadStatus> = emptyMap()
)

/**
 * ViewModel for Reels with aggressive preloading for Instagram-like speed
 */
class ReelsViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()
    
    // HTTP client for preloading
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    // Cache for preloaded video data (URL -> cached bytes range)
    private val preloadCache = ConcurrentHashMap<String, ByteArray>()
    
    // Track ongoing preload jobs
    private val preloadJobs = ConcurrentHashMap<String, Job>()
    
    // Number of reels to preload ahead while user watches current reel.
    private val PRELOAD_AHEAD_COUNT = 8

    private val previewCacheTtlMillis = 5 * 60 * 1000L
    private val feedCacheTtlMillis = 5 * 60 * 1000L
    private var lastPreviewLoadedAt = 0L
    private var lastFeedLoadedAt = 0L
    private var isPreviewRequestInFlight = false
    private var isFeedRequestInFlight = false
    
    // Min and max preload bytes per MP4 reel. We aim for ~30% when size is known.
    private val MIN_PRELOAD_BYTES = 2 * 1024 * 1024L
    private val MAX_PRELOAD_BYTES = 12 * 1024 * 1024L
    
    init {
        loadPreviewReels()
    }

    private fun prefetchFeedSilently(mode: String = "foryou") {
        val now = System.currentTimeMillis()
        val isFeedFresh =
            _uiState.value.feedReels.isNotEmpty() &&
                (now - lastFeedLoadedAt) < feedCacheTtlMillis

        if (isFeedFresh || isFeedRequestInFlight) return

        isFeedRequestInFlight = true
        viewModelScope.launch {
            val result = ApiClient.getReelsFeed(context, limit = 20, mode = mode)
            result.onSuccess { response ->
                lastFeedLoadedAt = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    feedReels = response.reels,
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore
                )
                preloadReelsAhead(0)
            }
            isFeedRequestInFlight = false
        }
    }

    fun prefetchAppStartData() {
        loadPreviewReels()
        prefetchFeedSilently()
    }
    
    /**
     * Load trending reels for the home feed preview section
     */
    fun loadPreviewReels(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val isPreviewFresh =
            !forceRefresh &&
                _uiState.value.previewReels.isNotEmpty() &&
                (now - lastPreviewLoadedAt) < previewCacheTtlMillis

        if (isPreviewFresh || isPreviewRequestInFlight) return

        isPreviewRequestInFlight = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPreview = _uiState.value.previewReels.isEmpty() || forceRefresh,
                previewError = null
            )
            
            val result = ApiClient.getTrendingReels(context, hours = 48, limit = 15)
            
            result.onSuccess { response ->
                lastPreviewLoadedAt = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    previewReels = response.reels,
                    isLoadingPreview = false
                )
                
                // Preload thumbnails for preview
                preloadThumbnails(response.reels.take(8))
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingPreview = false,
                    previewError = error.message
                )
            }
            isPreviewRequestInFlight = false
        }
    }
    
    /**
     * Load the main reels feed
     */
    fun loadReelsFeed(mode: String = "foryou", forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        val isFeedFresh =
            !forceRefresh &&
                _uiState.value.feedReels.isNotEmpty() &&
                (now - lastFeedLoadedAt) < feedCacheTtlMillis

        if (isFeedFresh || isFeedRequestInFlight) return

        isFeedRequestInFlight = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingFeed = true,
                feedError = null,
                nextCursor = null,
                hasMore = true
            )
            
            val result = ApiClient.getReelsFeed(context, limit = 20, mode = mode)
            
            result.onSuccess { response ->
                lastFeedLoadedAt = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    feedReels = response.reels,
                    isLoadingFeed = false,
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore
                )
                
                // Start preloading first few reels
                preloadReelsAhead(0)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingFeed = false,
                    feedError = error.message
                )
            }
            isFeedRequestInFlight = false
        }
    }
    
    /**
     * Load more reels (pagination)
     */
    fun loadMoreReels(mode: String = "foryou") {
        val currentState = _uiState.value
        
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.nextCursor == null) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoadingMore = true)
            
            val result = ApiClient.getReelsFeed(
                context,
                cursor = currentState.nextCursor,
                limit = 15,
                mode = mode
            )
            
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    feedReels = currentState.feedReels + response.reels,
                    isLoadingMore = false,
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
    
    /**
     * Open the full-screen reels viewer at a specific index
     */
    fun openReelsViewer(reels: List<Reel>, startIndex: Int = 0) {
        _uiState.value = _uiState.value.copy(
            isViewerOpen = true,
            currentReelIndex = startIndex,
            feedReels = if (_uiState.value.feedReels.isEmpty()) reels else _uiState.value.feedReels
        )
        
        // Preload reels around the current index
        preloadReelsAhead(startIndex)
    }
    
    /**
     * Close the reels viewer
     */
    fun closeReelsViewer() {
        _uiState.value = _uiState.value.copy(isViewerOpen = false)
    }
    
    /**
     * Load a specific reel by ID and open it in the viewer (for deep links from notifications)
     */
    fun loadReelById(reelId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isViewerOpen = true,
                isLoadingFeed = true,
                feedError = null
            )
            
            val result = ApiClient.getReel(context, reelId)
            
            result.onSuccess { reel ->
                // Add the reel to feedReels and open at index 0
                _uiState.value = _uiState.value.copy(
                    feedReels = listOf(reel) + _uiState.value.feedReels.filter { it.id != reelId },
                    isLoadingFeed = false,
                    currentReelIndex = 0
                )
                preloadReelsAhead(0)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingFeed = false,
                    feedError = error.message ?: "Failed to load reel",
                    isViewerOpen = false
                )
            }
        }
    }

    /**
     * Load reels and immediately open the viewer
     */
    fun loadAndOpenReels() {
        viewModelScope.launch {
            // If we already have reels, just open the viewer
            if (_uiState.value.previewReels.isNotEmpty()) {
                openReelsViewer(_uiState.value.previewReels, 0)
                if (_uiState.value.feedReels.isEmpty()) {
                    loadReelsFeed()
                }
                return@launch
            }
            
            // If we have feed reels, use those
            if (_uiState.value.feedReels.isNotEmpty()) {
                openReelsViewer(_uiState.value.feedReels, 0)
                return@launch
            }
            
            // Set viewer open immediately to trigger loading state
            _uiState.value = _uiState.value.copy(
                isViewerOpen = true,
                isLoadingFeed = true, 
                feedError = null
            )
            
            // Try trending reels first
            var result = ApiClient.getTrendingReels(context, hours = 48, limit = 20)
            
            result.onSuccess { response ->
                if (response.reels.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        previewReels = response.reels,
                        feedReels = response.reels,
                        isLoadingFeed = false,
                        currentReelIndex = 0
                    )
                    return@launch
                }
            }
            
            // Fall back to regular feed if trending is empty
            result = ApiClient.getReelsFeed(context, limit = 20, mode = "foryou")
            
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    feedReels = response.reels,
                    isLoadingFeed = false,
                    nextCursor = response.nextCursor,
                    hasMore = response.hasMore,
                    currentReelIndex = 0
                )
                
                if (response.reels.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        feedError = "No reels available yet"
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingFeed = false,
                    feedError = error.message ?: "Failed to load reels"
                )
            }
        }
    }
    
    /**
     * Update current reel index (called when user scrolls)
     */
    fun onReelChanged(newIndex: Int) {
        _uiState.value = _uiState.value.copy(currentReelIndex = newIndex)
        
        // Preload reels ahead of new position
        preloadReelsAhead(newIndex)
        
        // Load more if near the end
        val reels = _uiState.value.feedReels
        if (newIndex >= reels.size - 8) {
            loadMoreReels()
        }
    }
    
    /**
     * Toggle like on a reel
     */
    fun toggleLike(reelId: String) {
        viewModelScope.launch {
            val result = ApiClient.toggleReelLike(context, reelId)
            
            result.onSuccess { response ->
                updateReelInLists(reelId) { reel ->
                    reel.copy(
                        isLiked = response.liked,
                        likesCount = response.likesCount
                    )
                }
            }
        }
    }
    
    /**
     * Toggle save on a reel
     */
    fun toggleSave(reelId: String) {
        viewModelScope.launch {
            val result = ApiClient.toggleReelSave(context, reelId)
            
            result.onSuccess { response ->
                updateReelInLists(reelId) { reel ->
                    reel.copy(
                        isSaved = response.saved,
                        savesCount = response.savesCount
                    )
                }
            }
        }
    }
    
    /**
     * Track a reel view
     */
    fun trackView(reelId: String, watchTimeMs: Long, completed: Boolean) {
        viewModelScope.launch {
            ApiClient.trackReelView(context, reelId, watchTimeMs, completed)
        }
    }

    // ==================== Comments (Nested) ====================

    fun openComments(reelId: String) {
        _uiState.value = _uiState.value.copy(
            showCommentsSheet = true,
            commentsReelId = reelId,
            reelComments = emptyList(),
            replyCommentsByParent = emptyMap(),
            expandedReplyParents = emptySet(),
            commentsCursor = null,
            hasMoreComments = false,
            commentsError = null,
            replyToComment = null
        )
        loadReelComments(reelId, refresh = true)
    }

    fun closeComments() {
        _uiState.value = _uiState.value.copy(
            showCommentsSheet = false,
            commentsReelId = null,
            reelComments = emptyList(),
            replyCommentsByParent = emptyMap(),
            expandedReplyParents = emptySet(),
            commentsCursor = null,
            hasMoreComments = false,
            isLoadingComments = false,
            isLoadingMoreComments = false,
            isSubmittingComment = false,
            commentsError = null,
            replyToComment = null
        )
    }

    fun loadReelComments(reelId: String? = _uiState.value.commentsReelId, refresh: Boolean = false) {
        val targetReelId = reelId ?: return

        if (refresh) {
            _uiState.value = _uiState.value.copy(
                isLoadingComments = true,
                commentsError = null,
                commentsCursor = null,
                hasMoreComments = false
            )
        } else {
            if (_uiState.value.isLoadingMoreComments || !_uiState.value.hasMoreComments) return
            _uiState.value = _uiState.value.copy(isLoadingMoreComments = true)
        }

        viewModelScope.launch {
            val cursor = if (refresh) null else _uiState.value.commentsCursor
            ApiClient.getReelComments(context, reelId = targetReelId, cursor = cursor, limit = 20)
                .onSuccess { response ->
                    val merged = if (refresh) {
                        response.comments
                    } else {
                        _uiState.value.reelComments + response.comments
                    }

                    _uiState.value = _uiState.value.copy(
                        reelComments = merged,
                        commentsCursor = response.nextCursor,
                        hasMoreComments = response.hasMore,
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = null
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = e.message ?: "Failed to load comments"
                    )
                }
        }
    }

    fun loadReplies(parentCommentId: String) {
        val reelId = _uiState.value.commentsReelId ?: return
        if (_uiState.value.replyCommentsByParent[parentCommentId] != null) {
            _uiState.value = _uiState.value.copy(
                expandedReplyParents = if (_uiState.value.expandedReplyParents.contains(parentCommentId)) {
                    _uiState.value.expandedReplyParents - parentCommentId
                } else {
                    _uiState.value.expandedReplyParents + parentCommentId
                }
            )
            return
        }

        viewModelScope.launch {
            ApiClient.getReelComments(context, reelId = reelId, limit = 50, parentId = parentCommentId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        replyCommentsByParent = _uiState.value.replyCommentsByParent + (parentCommentId to response.comments),
                        expandedReplyParents = _uiState.value.expandedReplyParents + parentCommentId
                    )
                }
        }
    }

    fun setReplyTarget(comment: ReelComment?) {
        _uiState.value = _uiState.value.copy(replyToComment = comment)
    }

    fun submitComment(content: String) {
        val reelId = _uiState.value.commentsReelId ?: return
        val trimmed = content.trim()
        if (trimmed.isEmpty() || _uiState.value.isSubmittingComment) return

        val parent = _uiState.value.replyToComment
        _uiState.value = _uiState.value.copy(isSubmittingComment = true, commentsError = null)

        viewModelScope.launch {
            ApiClient.createReelComment(context, reelId = reelId, content = trimmed, parentId = parent?.id)
                .onSuccess { comment ->
                    if (parent == null) {
                        _uiState.value = _uiState.value.copy(
                            reelComments = listOf(comment) + _uiState.value.reelComments,
                            isSubmittingComment = false,
                            replyToComment = null
                        )
                        updateReelInLists(reelId) { reel -> reel.copy(commentsCount = reel.commentsCount + 1) }
                    } else {
                        val existingReplies = _uiState.value.replyCommentsByParent[parent.id] ?: emptyList()
                        val updatedParents = _uiState.value.reelComments.map {
                            if (it.id == parent.id) it.copy(repliesCount = it.repliesCount + 1) else it
                        }
                        _uiState.value = _uiState.value.copy(
                            reelComments = updatedParents,
                            replyCommentsByParent = _uiState.value.replyCommentsByParent + (parent.id to (listOf(comment) + existingReplies)),
                            expandedReplyParents = _uiState.value.expandedReplyParents + parent.id,
                            isSubmittingComment = false,
                            replyToComment = null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingComment = false,
                        commentsError = e.message ?: "Failed to post comment"
                    )
                }
        }
    }
    
    // ==================== Preloading Logic ====================
    
    /**
     * Preload thumbnails for faster rendering
     */
    private fun preloadThumbnails(reels: List<Reel>) {
        viewModelScope.launch(Dispatchers.IO) {
            reels.forEach { reel ->
                reel.thumbnailUrl?.let { url ->
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .head() // Just warm up the connection and cache
                            .build()
                        httpClient.newCall(request).execute().close()
                    } catch (e: Exception) {
                        // Ignore errors in preloading
                    }
                }
            }
        }
    }
    
    /**
     * Preload video data for reels ahead of current position
     * This downloads the first few MB of each video for instant playback
     */
    private fun preloadReelsAhead(currentIndex: Int) {
        val reels = _uiState.value.feedReels
        val previewReels = _uiState.value.previewReels
        val allReels = if (reels.isNotEmpty()) reels else previewReels
        
        if (allReels.isEmpty()) return
        
        // Preload current and next N reels
        val indicesToPreload = (currentIndex..(currentIndex + PRELOAD_AHEAD_COUNT))
            .filter { it in allReels.indices }
        
        indicesToPreload.forEach { index ->
            val reel = allReels[index]
            preloadReel(reel)
        }
    }
    
    /**
     * Preload a single reel's video data
     */
    private fun preloadReel(reel: Reel) {
        val videoUrl = reel.hlsUrl ?: reel.videoUrl
        
        // Skip if already preloading or preloaded
        val currentStatus = _uiState.value.preloadStatus[reel.id]
        if (currentStatus == PreloadStatus.PRELOADING || currentStatus == PreloadStatus.PRELOADED) {
            return
        }
        
        // Skip if cache already has data
        if (preloadCache.containsKey(videoUrl)) {
            // Use non-suspend version to update state
            updatePreloadStatusSync(reel.id, PreloadStatus.PRELOADED)
            return
        }
        
        // Cancel any existing job for this reel
        preloadJobs[reel.id]?.cancel()
        
        val job = viewModelScope.launch(Dispatchers.IO) {
            updatePreloadStatus(reel.id, PreloadStatus.PRELOADING)
            
            try {
                // For HLS, just warm up the playlist
                if (reel.hlsUrl != null) {
                    val request = Request.Builder()
                        .url(reel.hlsUrl)
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val playlist = response.body?.string().orEmpty()
                    response.close()

                    // Warm first few segments for faster startup.
                    val segmentUrls = playlist
                        .lineSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .take(3)
                        .map { segment ->
                            if (segment.startsWith("http://") || segment.startsWith("https://")) {
                                segment
                            } else {
                                URL(URL(reel.hlsUrl), segment).toString()
                            }
                        }
                        .toList()

                    segmentUrls.forEach { segmentUrl ->
                        runCatching {
                            httpClient.newCall(
                                Request.Builder().url(segmentUrl).header("Range", "bytes=0-524287").build()
                            ).execute().use { segResp -> segResp.body?.bytes() }
                        }
                    }
                    
                    updatePreloadStatus(reel.id, PreloadStatus.PRELOADED)
                } else {
                    // For MP4, preload around 30% (bounded) for faster uninterrupted play.
                    val contentLength = runCatching {
                        httpClient.newCall(Request.Builder().url(videoUrl).head().build())
                            .execute()
                            .use { headResp ->
                                headResp.header("Content-Length")?.toLongOrNull()
                            }
                    }.getOrNull()

                    val targetPreloadBytes = contentLength
                        ?.let { (it * 0.30).toLong() }
                        ?.coerceIn(MIN_PRELOAD_BYTES, MAX_PRELOAD_BYTES)
                        ?: MIN_PRELOAD_BYTES

                    val request = Request.Builder()
                        .url(videoUrl)
                        .header("Range", "bytes=0-${targetPreloadBytes - 1}")
                        .build()
                    
                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            preloadCache[videoUrl] = bytes
                        }
                        updatePreloadStatus(reel.id, PreloadStatus.PRELOADED)
                    } else {
                        updatePreloadStatus(reel.id, PreloadStatus.FAILED)
                    }
                    response.close()
                }
            } catch (e: Exception) {
                updatePreloadStatus(reel.id, PreloadStatus.FAILED)
            }
        }
        
        preloadJobs[reel.id] = job
    }
    
    /**
     * Update preload status for a reel (suspend version for coroutines)
     */
    private suspend fun updatePreloadStatus(reelId: String, status: PreloadStatus) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                preloadStatus = _uiState.value.preloadStatus + (reelId to status)
            )
        }
    }
    
    /**
     * Update preload status synchronously (for non-coroutine contexts)
     */
    private fun updatePreloadStatusSync(reelId: String, status: PreloadStatus) {
        _uiState.value = _uiState.value.copy(
            preloadStatus = _uiState.value.preloadStatus + (reelId to status)
        )
    }
    
    /**
     * Update a reel in both preview and feed lists
     */
    private fun updateReelInLists(reelId: String, update: (Reel) -> Reel) {
        _uiState.value = _uiState.value.copy(
            previewReels = _uiState.value.previewReels.map { 
                if (it.id == reelId) update(it) else it 
            },
            feedReels = _uiState.value.feedReels.map { 
                if (it.id == reelId) update(it) else it 
            }
        )
    }
    
    /**
     * Clean up preload cache when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        preloadCache.clear()
    }
    
    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReelsViewModel(context) as T
                }
            }
        }
    }
}

/**
 * Extension to copy Reel with updated fields
 */
private fun Reel.copy(
    isLiked: Boolean = this.isLiked,
    likesCount: Int = this.likesCount,
    isSaved: Boolean = this.isSaved,
    savesCount: Int = this.savesCount,
    commentsCount: Int = this.commentsCount
): Reel {
    return Reel(
        id = this.id,
        author = this.author,
        videoId = this.videoId,
        videoUrl = this.videoUrl,
        hlsUrl = this.hlsUrl,
        thumbnailUrl = this.thumbnailUrl,
        previewGifUrl = this.previewGifUrl,
        title = this.title,
        caption = this.caption,
        durationSeconds = this.durationSeconds,
        width = this.width,
        height = this.height,
        aspectRatio = this.aspectRatio,
        audio = this.audio,
        hashtags = this.hashtags,
        mentions = this.mentions,
        skills = this.skills,
        topics = this.topics,
        category = this.category,
        locationName = this.locationName,
        pollQuestion = this.pollQuestion,
        pollOptions = this.pollOptions,
        pollEndsAt = this.pollEndsAt,
        userVotedOption = this.userVotedOption,
        quizQuestion = this.quizQuestion,
        quizOptions = this.quizOptions,
        codeSnippet = this.codeSnippet,
        codeLanguage = this.codeLanguage,
        visibility = this.visibility,
        allowComments = this.allowComments,
        allowDownload = this.allowDownload,
        allowSharing = this.allowSharing,
        status = this.status,
        viewsCount = this.viewsCount,
        likesCount = likesCount,
        commentsCount = commentsCount,
        sharesCount = this.sharesCount,
        savesCount = savesCount,
        isLiked = isLiked,
        isSaved = isSaved,
        publishedAt = this.publishedAt,
        createdAt = this.createdAt
    )
}
