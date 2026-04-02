package com.kyant.backdrop.catalog.chat

import com.kyant.backdrop.catalog.network.models.Message
import com.kyant.backdrop.catalog.network.models.GroupMessage
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Show time (and delivery) only at the end of a run: same sender, next message within [gapMinutes].
 * [Instant.parse] alone often fails on API timestamps (missing Z, millis, offsets) — that used to
 * return MAX_VALUE gap and incorrectly showed a time on every bubble.
 */
private const val CLUSTER_GAP_MINUTES = 5L

private val chatTimestampPatterns = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
    "yyyy-MM-dd HH:mm:ss"
)

internal fun shouldShowClusterMetaForDm(current: Message, next: Message?): Boolean {
    if (next == null) return true
    if (next.senderId != current.senderId) return true
    return minutesBetweenChronological(current.createdAt, next.createdAt) >= CLUSTER_GAP_MINUTES
}

internal fun shouldShowClusterMetaForGroup(current: GroupMessage, next: GroupMessage?): Boolean {
    if (next == null) return true
    if (next.senderId != current.senderId) return true
    return minutesBetweenChronological(current.createdAt, next.createdAt) >= CLUSTER_GAP_MINUTES
}

private fun minutesBetweenChronological(earlier: String, later: String): Long {
    val ma = parseChatTimestampMillis(earlier)
    val mb = parseChatTimestampMillis(later)
    if (ma == null || mb == null) return 0L
    return max(0L, (mb - ma) / 60_000L)
}

/** Parses common backend formats; returns null only when string is blank. */
private fun parseChatTimestampMillis(raw: String): Long? {
    if (raw.isBlank()) return null
    runCatching { Instant.parse(raw) }.getOrNull()?.toEpochMilli()?.let { return it }
    runCatching { OffsetDateTime.parse(raw) }.getOrNull()?.toInstant()?.toEpochMilli()?.let { return it }
    for (pattern in chatTimestampPatterns) {
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = true
            }.parse(raw)?.time
        }.getOrNull()?.let { return it }
    }
    runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(raw)?.time }
        .getOrNull()?.let { return it }
    return null
}
