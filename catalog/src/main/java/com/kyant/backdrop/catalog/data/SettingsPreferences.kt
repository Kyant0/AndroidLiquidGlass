package com.kyant.backdrop.catalog.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

/**
 * App Settings Preferences - Stores user preferences for notifications, privacy, and appearance
 */
object SettingsPreferences {
    
    // ==================== NOTIFICATION SETTINGS ====================
    private val PUSH_NOTIFICATIONS_ENABLED = booleanPreferencesKey("push_notifications_enabled")
    private val DAILY_DIGEST_ENABLED = booleanPreferencesKey("daily_digest_enabled")
    private val DAILY_DIGEST_TIME = stringPreferencesKey("daily_digest_time") // "09:00"
    private val MATCH_ALERTS_ENABLED = booleanPreferencesKey("match_alerts_enabled")
    private val MESSAGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("message_notifications_enabled")
    private val CONNECTION_NOTIFICATIONS_ENABLED = booleanPreferencesKey("connection_notifications_enabled")
    private val LIKE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("like_notifications_enabled")
    private val COMMENT_NOTIFICATIONS_ENABLED = booleanPreferencesKey("comment_notifications_enabled")
    private val STREAK_REMINDERS_ENABLED = booleanPreferencesKey("streak_reminders_enabled")
    private val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary_enabled")
    
    // ==================== PRIVACY SETTINGS ====================
    private val PROFILE_VISIBILITY = stringPreferencesKey("profile_visibility") // "public", "connections", "private"
    private val WHO_CAN_MESSAGE = stringPreferencesKey("who_can_message") // "everyone", "connections", "none"
    private val SHOW_ONLINE_STATUS = booleanPreferencesKey("show_online_status")
    private val SHOW_ACTIVITY_STATUS = booleanPreferencesKey("show_activity_status")
    private val SHOW_PROFILE_VIEWS = booleanPreferencesKey("show_profile_views")
    private val DISCOVERABLE_BY_EMAIL = booleanPreferencesKey("discoverable_by_email")
    private val DISCOVERABLE_BY_PHONE = booleanPreferencesKey("discoverable_by_phone")
    
    // ==================== APPEARANCE SETTINGS ====================
    private val THEME_MODE = stringPreferencesKey("theme_mode") // "glass", "light", "dark"
    private val GLASS_BACKGROUND_PRESET = stringPreferencesKey("glass_background_preset")
    private val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    private val GLASS_MOTION_STYLE = stringPreferencesKey("glass_motion_style")
    private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    private val FONT_SIZE = stringPreferencesKey("font_size") // "small", "medium", "large"
    private val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
    private val PROFILE_FRAME_ENABLED = booleanPreferencesKey("profile_frame_enabled")
    private val STAY_ACTIVE_BANNER_DISMISSED_AT = longPreferencesKey("stay_active_banner_dismissed_at")
    /** Equipped profile visit loader gift id (e.g. `big_bad_wolfie`). */
    private val EQUIPPED_PROFILE_LOADER_GIFT = stringPreferencesKey("equipped_profile_loader_gift")
    
    // ==================== NOTIFICATION GETTERS ====================
    
    fun pushNotificationsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[PUSH_NOTIFICATIONS_ENABLED] ?: true }
    
    fun dailyDigestEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[DAILY_DIGEST_ENABLED] ?: true }
    
    fun dailyDigestTime(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[DAILY_DIGEST_TIME] ?: "09:00" }
    
    fun matchAlertsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[MATCH_ALERTS_ENABLED] ?: true }
    
    fun messageNotificationsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[MESSAGE_NOTIFICATIONS_ENABLED] ?: true }
    
    fun connectionNotificationsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[CONNECTION_NOTIFICATIONS_ENABLED] ?: true }
    
    fun likeNotificationsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[LIKE_NOTIFICATIONS_ENABLED] ?: true }
    
    fun commentNotificationsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[COMMENT_NOTIFICATIONS_ENABLED] ?: true }
    
    fun streakRemindersEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[STREAK_REMINDERS_ENABLED] ?: true }
    
    fun weeklySummaryEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[WEEKLY_SUMMARY_ENABLED] ?: true }
    
    // ==================== PRIVACY GETTERS ====================
    
    fun profileVisibility(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[PROFILE_VISIBILITY] ?: "public" }
    
    fun whoCanMessage(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[WHO_CAN_MESSAGE] ?: "everyone" }
    
    fun showOnlineStatus(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[SHOW_ONLINE_STATUS] ?: true }
    
    fun showActivityStatus(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[SHOW_ACTIVITY_STATUS] ?: true }
    
    fun showProfileViews(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[SHOW_PROFILE_VIEWS] ?: true }
    
    fun discoverableByEmail(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[DISCOVERABLE_BY_EMAIL] ?: true }
    
    fun discoverableByPhone(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[DISCOVERABLE_BY_PHONE] ?: false }
    
    // ==================== APPEARANCE GETTERS ====================
    
    fun themeMode(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[THEME_MODE] ?: com.kyant.backdrop.catalog.linkedin.DefaultThemeModeKey }

    fun glassBackgroundPreset(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[GLASS_BACKGROUND_PRESET] ?: "wallpaper" }

    fun accentPalette(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[ACCENT_PALETTE] ?: "linkedin" }

    fun glassMotionStyle(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[GLASS_MOTION_STYLE] ?: "float" }
    
    fun dynamicColors(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[DYNAMIC_COLORS] ?: true }
    
    fun fontSize(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[FONT_SIZE] ?: "medium" }
    
    fun reduceAnimations(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[REDUCE_ANIMATIONS] ?: false }

    fun profileFrameEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[PROFILE_FRAME_ENABLED] ?: false }

    fun stayActiveBannerDismissedAt(context: Context): Flow<Long> =
        context.settingsDataStore.data.map { it[STAY_ACTIVE_BANNER_DISMISSED_AT] ?: 0L }

    fun equippedProfileLoaderGiftId(context: Context): Flow<String?> =
        context.settingsDataStore.data.map { it[EQUIPPED_PROFILE_LOADER_GIFT] }
    
    // ==================== NOTIFICATION SETTERS ====================
    
    suspend fun setPushNotificationsEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[PUSH_NOTIFICATIONS_ENABLED] = value }
    }
    
    suspend fun setDailyDigestEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[DAILY_DIGEST_ENABLED] = value }
    }
    
    suspend fun setDailyDigestTime(context: Context, value: String) {
        context.settingsDataStore.edit { it[DAILY_DIGEST_TIME] = value }
    }
    
    suspend fun setMatchAlertsEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[MATCH_ALERTS_ENABLED] = value }
    }
    
    suspend fun setMessageNotificationsEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[MESSAGE_NOTIFICATIONS_ENABLED] = value }
    }
    
    suspend fun setConnectionNotificationsEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[CONNECTION_NOTIFICATIONS_ENABLED] = value }
    }
    
    suspend fun setLikeNotificationsEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[LIKE_NOTIFICATIONS_ENABLED] = value }
    }
    
    suspend fun setCommentNotificationsEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[COMMENT_NOTIFICATIONS_ENABLED] = value }
    }
    
    suspend fun setStreakRemindersEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[STREAK_REMINDERS_ENABLED] = value }
    }
    
    suspend fun setWeeklySummaryEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[WEEKLY_SUMMARY_ENABLED] = value }
    }
    
    // ==================== PRIVACY SETTERS ====================
    
    suspend fun setProfileVisibility(context: Context, value: String) {
        context.settingsDataStore.edit { it[PROFILE_VISIBILITY] = value }
    }
    
    suspend fun setWhoCanMessage(context: Context, value: String) {
        context.settingsDataStore.edit { it[WHO_CAN_MESSAGE] = value }
    }
    
    suspend fun setShowOnlineStatus(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_ONLINE_STATUS] = value }
    }
    
    suspend fun setShowActivityStatus(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_ACTIVITY_STATUS] = value }
    }
    
    suspend fun setShowProfileViews(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_PROFILE_VIEWS] = value }
    }
    
    suspend fun setDiscoverableByEmail(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[DISCOVERABLE_BY_EMAIL] = value }
    }
    
    suspend fun setDiscoverableByPhone(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[DISCOVERABLE_BY_PHONE] = value }
    }
    
    // ==================== APPEARANCE SETTERS ====================
    
    suspend fun setThemeMode(context: Context, value: String) {
        context.settingsDataStore.edit { it[THEME_MODE] = value }
    }

    suspend fun setGlassBackgroundPreset(context: Context, value: String) {
        context.settingsDataStore.edit { it[GLASS_BACKGROUND_PRESET] = value }
    }

    suspend fun setAccentPalette(context: Context, value: String) {
        context.settingsDataStore.edit { it[ACCENT_PALETTE] = value }
    }

    suspend fun setGlassMotionStyle(context: Context, value: String) {
        context.settingsDataStore.edit { it[GLASS_MOTION_STYLE] = value }
    }
    
    suspend fun setDynamicColors(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[DYNAMIC_COLORS] = value }
    }
    
    suspend fun setFontSize(context: Context, value: String) {
        context.settingsDataStore.edit { it[FONT_SIZE] = value }
    }
    
    suspend fun setReduceAnimations(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[REDUCE_ANIMATIONS] = value }
    }

    suspend fun setProfileFrameEnabled(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[PROFILE_FRAME_ENABLED] = value }
    }

    suspend fun setStayActiveBannerDismissedAt(context: Context, value: Long) {
        context.settingsDataStore.edit { it[STAY_ACTIVE_BANNER_DISMISSED_AT] = value }
    }

    suspend fun setEquippedProfileLoaderGiftId(context: Context, giftId: String?) {
        context.settingsDataStore.edit {
            if (giftId.isNullOrBlank()) it.remove(EQUIPPED_PROFILE_LOADER_GIFT)
            else it[EQUIPPED_PROFILE_LOADER_GIFT] = giftId
        }
    }
    
    // ==================== CLEAR ALL ====================
    
    suspend fun clearAll(context: Context) {
        context.settingsDataStore.edit { it.clear() }
    }
}
