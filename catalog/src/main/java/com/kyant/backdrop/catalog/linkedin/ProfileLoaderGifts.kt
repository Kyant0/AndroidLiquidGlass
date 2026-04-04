package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.R
import java.util.concurrent.ConcurrentHashMap

/** Known profile visit loader gift ids (match [R.raw] names, without extension). */
object ProfileLoaderGifts {
    const val BIG_BAD_WOLFIE = "big_bad_wolfie"

    fun rawResForGiftId(id: String?): Int? = when (id?.lowercase()) {
        BIG_BAD_WOLFIE, "wolfie" -> R.raw.big_bad_wolfie
        else -> null
    }
}

/**
 * Last resolved visit loader per profile user id so repeat visits can show the same loader while fetching.
 */
object ProfileLoaderGiftMemory {
    private val byUserId = ConcurrentHashMap<String, String?>()

    fun get(userId: String): String? = byUserId[userId]

    fun put(userId: String, giftId: String?) {
        if (giftId.isNullOrBlank()) byUserId.remove(userId) else byUserId[userId] = giftId
    }
}
