package com.kyant.backdrop.catalog.linkedin

import com.kyant.backdrop.catalog.R
import java.util.concurrent.ConcurrentHashMap

/** Known profile visit loader gift ids (match [R.raw] names, without extension). */
object ProfileLoaderGifts {
    const val BIG_BAD_WOLFIE = "big_bad_wolfie"
    const val MORTY_DANCE = "the_morty_dance_loader"
    private const val DAVSAN = "davsan"

    fun rawResForGiftId(id: String?): Int? = when (id?.lowercase()) {
        BIG_BAD_WOLFIE, "wolfie" -> R.raw.big_bad_wolfie
        MORTY_DANCE, "morty", "morty_dance", "morty_dance_loader" -> R.raw.the_morty_dance_loader
        DAVSAN -> R.raw.davsan
        else -> null
    }

    fun labelForGiftId(id: String?): String? = when (id?.lowercase()) {
        BIG_BAD_WOLFIE, "wolfie" -> "Big Bad Wolfie"
        MORTY_DANCE, "morty", "morty_dance", "morty_dance_loader" -> "Morty Dance Loader"
        DAVSAN -> "Davsan"
        else -> null
    }

    fun defaultVisitorRawRes(): Int = R.raw.davsan

    fun defaultVisitorLabel(): String = "Davsan"

    fun resolvedVisitorRawRes(id: String?): Int = rawResForGiftId(id) ?: defaultVisitorRawRes()

    fun resolvedVisitorLabel(id: String?): String = labelForGiftId(id) ?: defaultVisitorLabel()
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
