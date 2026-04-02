package com.kyant.backdrop.catalog.data

import android.content.Context

/**
 * Per-conversation mute for chat message notifications (local + FCM).
 * When [getMuteUntilMillis] is in the future, [isMuted] is true.
 */
object ChatMutePreferences {
    private const val PREFS_NAME = "chat_notification_mute"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun key(conversationId: String) = "mute_until_$conversationId"

    fun getMuteUntilMillis(context: Context, conversationId: String): Long {
        if (conversationId.isBlank()) return 0L
        return prefs(context).getLong(key(conversationId), 0L)
    }

    fun isMuted(context: Context, conversationId: String): Boolean {
        return getMuteUntilMillis(context, conversationId) > System.currentTimeMillis()
    }

    fun setMuteUntilMillis(context: Context, conversationId: String, untilMillis: Long) {
        if (conversationId.isBlank()) return
        prefs(context).edit().putLong(key(conversationId), untilMillis).apply()
    }

    fun clearMute(context: Context, conversationId: String) {
        if (conversationId.isBlank()) return
        prefs(context).edit().remove(key(conversationId)).apply()
    }
}
