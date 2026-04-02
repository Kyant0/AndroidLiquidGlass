package com.kyant.backdrop.catalog.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.ChatMutePreferences

/**
 * Firebase Cloud Messaging Service for Vormex
 * 
 * Handles incoming push notifications and deep links for:
 * - Messages (new chat messages)
 * - Likes (post/reel likes)
 * - Comments (post/reel comments)
 * - Connections (requests, acceptances)
 * - Follows (new followers)
 * - Mentions (when mentioned in posts/comments)
 * - Streaks (reminders, achievements)
 * - Engagement (daily matches, weekly goals)
 */
class VormexMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "VormexMessaging"
        
        // Notification Channels
        const val CHANNEL_ID_MESSAGES = "messages"
        const val CHANNEL_ID_SOCIAL = "social"
        const val CHANNEL_ID_STREAKS = "streaks"
        const val CHANNEL_ID_CONNECTIONS = "connections"
        const val CHANNEL_ID_ENGAGEMENT = "engagement"
        
        // Deep link actions
        const val ACTION_CHAT = "chat"
        const val ACTION_POST = "post"
        const val ACTION_REEL = "reel"
        const val ACTION_PROFILE = "profile"
        const val ACTION_CONNECTIONS = "connections"
        const val ACTION_STREAK = "streak"
        const val ACTION_ENGAGEMENT = "engagement"
        const val ACTION_FIND_PEOPLE = "find_people"
        const val ACTION_STREAK_REMINDER = "streak_reminder"
        const val ACTION_WEEKLY_GOAL = "weekly_goal"
        const val ACTION_LEADERBOARD = "leaderboard"
        const val ACTION_CONNECTION_CELEBRATION = "connection_celebration"
        const val ACTION_SESSION_SUMMARY = "session_summary"
        
        // Intent extras
        const val EXTRA_ACTION = "notification_action"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_POST_ID = "post_id"
        const val EXTRA_REEL_ID = "reel_id"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_CONNECTION_ID = "connection_id"

        /**
         * Get current FCM token
         */
        fun getToken(onToken: (String?) -> Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onToken(task.result)
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.exception)
                    onToken(null)
                }
            }
        }

        /**
         * Subscribe to a topic (e.g., "announcements")
         */
        fun subscribeToTopic(topic: String) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Subscribed to topic: $topic")
                    } else {
                        Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
                    }
                }
        }

        /**
         * Get custom notification sound URI
         * Falls back to default notification sound if custom sound not found
         */
        private fun getCustomSoundUri(context: Context): Uri {
            return try {
                // Try to get custom sound from raw resources
                val resourceId = context.resources.getIdentifier(
                    "vormex_notification",
                    "raw",
                    context.packageName
                )
                if (resourceId != 0) {
                    Uri.parse("android.resource://${context.packageName}/$resourceId")
                } else {
                    Log.d(TAG, "Custom sound not found, using default")
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading custom sound", e)
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }

        /**
         * Create notification channels (call on app startup)
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // Custom notification sound URI - uses vormex_notification.mp3 from res/raw
                // Falls back to default notification sound if custom sound not found
                val customSoundUri: Uri = getCustomSoundUri(context)
                
                // Audio attributes for notification sound
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                
                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_ID_MESSAGES,
                        "Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "New messages and chat notifications"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 250, 250, 250)
                        enableLights(true)
                        setSound(customSoundUri, audioAttributes)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)
                    },
                    NotificationChannel(
                        CHANNEL_ID_SOCIAL,
                        "Social",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Likes, comments, mentions, and followers"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 100, 200)
                        setSound(customSoundUri, audioAttributes)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    },
                    NotificationChannel(
                        CHANNEL_ID_CONNECTIONS,
                        "Connections",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Connection requests and acceptances"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 300, 200, 300)
                        setSound(customSoundUri, audioAttributes)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)
                    },
                    NotificationChannel(
                        CHANNEL_ID_STREAKS,
                        "Streaks",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Streak reminders and achievements"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 200)
                        setSound(customSoundUri, audioAttributes)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    },
                    NotificationChannel(
                        CHANNEL_ID_ENGAGEMENT,
                        "Engagement",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Daily matches, weekly goals, and achievements"
                        enableVibration(true)
                        setSound(customSoundUri, audioAttributes)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    }
                )
                
                channels.forEach { notificationManager.createNotificationChannel(it) }
            }
        }

        /**
         * Show a local notification (for testing or manual triggers)
         */
        fun showLocalNotification(
            context: Context,
            title: String,
            body: String,
            channelId: String = CHANNEL_ID_ENGAGEMENT,
            data: Map<String, String> = emptyMap()
        ) {
            createNotificationChannels(context)
            
            val intent = createDeepLinkIntent(context, data)
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Use custom sound
            val soundUri = getCustomSoundUri(context)
            
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            
            if (body.length > 50) {
                notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        }

        private fun createDeepLinkIntent(context: Context, data: Map<String, String>): Intent {
            return Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                
                val type = data["type"] ?: data["screen"] ?: ""
                
                when {
                    type.contains("message", ignoreCase = true) || type == "chat" -> {
                        putExtra(EXTRA_ACTION, ACTION_CHAT)
                        data["conversationId"]?.let { putExtra(EXTRA_CONVERSATION_ID, it) }
                        data["user_id"]?.let { putExtra(EXTRA_USER_ID, it) }
                            ?: data["senderId"]?.let { putExtra(EXTRA_USER_ID, it) }
                    }
                    type.contains("like", ignoreCase = true) || 
                    type.contains("comment", ignoreCase = true) ||
                    type.contains("mention", ignoreCase = true) -> {
                        data["postId"]?.let {
                            putExtra(EXTRA_ACTION, ACTION_POST)
                            putExtra(EXTRA_POST_ID, it)
                        }
                        data["reelId"]?.let {
                            putExtra(EXTRA_ACTION, ACTION_REEL)
                            putExtra(EXTRA_REEL_ID, it)
                        }
                    }
                    type.equals("connection_accepted", ignoreCase = true) ||
                    type.equals("new_connection", ignoreCase = true) ||
                    type.equals("connection_celebration", ignoreCase = true) ||
                    data["screen"].equals("connection_celebration", ignoreCase = true) -> {
                        val connectionId = data["connectionId"].orEmpty()
                        if (connectionId.isNotBlank()) {
                            putExtra(EXTRA_ACTION, ACTION_CONNECTION_CELEBRATION)
                            putExtra(EXTRA_CONNECTION_ID, connectionId)
                        } else {
                            putExtra(EXTRA_ACTION, ACTION_CONNECTIONS)
                        }
                        data["actorId"]?.let { putExtra(EXTRA_USER_ID, it) }
                    }
                    type.contains("connection", ignoreCase = true) -> {
                        putExtra(EXTRA_ACTION, ACTION_CONNECTIONS)
                        data["connectionId"]?.let { putExtra(EXTRA_CONNECTION_ID, it) }
                    }
                    type.contains("follow", ignoreCase = true) -> {
                        putExtra(EXTRA_ACTION, ACTION_PROFILE)
                        data["actorId"]?.let { putExtra(EXTRA_USER_ID, it) }
                    }
                    type.contains("streak", ignoreCase = true) -> {
                        putExtra(EXTRA_ACTION, ACTION_STREAK)
                    }
                    type.contains("match", ignoreCase = true) || type == "find_people" -> {
                        putExtra(EXTRA_ACTION, ACTION_FIND_PEOPLE)
                    }
                    type.contains("profile", ignoreCase = true) -> {
                        putExtra(EXTRA_ACTION, ACTION_PROFILE)
                        data["viewerId"]?.let { putExtra(EXTRA_USER_ID, it) }
                    }
                    else -> {
                        putExtra(EXTRA_ACTION, ACTION_ENGAGEMENT)
                    }
                }
            }
        }

        /**
         * Map notification type to channel
         */
        private fun getChannelForType(type: String): String {
            return when {
                type.contains("message", ignoreCase = true) -> CHANNEL_ID_MESSAGES
                type.contains("connection", ignoreCase = true) -> CHANNEL_ID_CONNECTIONS
                type.contains("streak", ignoreCase = true) -> CHANNEL_ID_STREAKS
                type.contains("like", ignoreCase = true) ||
                type.contains("comment", ignoreCase = true) ||
                type.contains("mention", ignoreCase = true) ||
                type.contains("follow", ignoreCase = true) -> CHANNEL_ID_SOCIAL
                else -> CHANNEL_ID_ENGAGEMENT
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        PushTokenRegistrar.syncToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")
        
        // Acquire wake lock to ensure notification is processed when app is killed/background
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Vormex:NotificationWakeLock"
        )
        wakeLock.acquire(10000L) // 10 seconds max
        
        try {
            // Get notification data - data messages have title/body in data payload
            val data = remoteMessage.data
            val notification = remoteMessage.notification
            
            // For data-only messages (when app killed), data contains title/body
            // For notification+data messages (when app foreground), notification contains title/body
            val type = data["type"] ?: "general"
            val rawTitle = notification?.title ?: data["title"] ?: "Vormex"
            val title = normalizeNotificationTitle(rawTitle, type)
            val body = data["body"] ?: notification?.body ?: ""
            
            Log.d(TAG, "Processing notification - Title: $title, Body: $body, Type: $type")
            
            if (body.isNotEmpty()) {
                if (type.contains("message", ignoreCase = true)) {
                    showChatMessageNotification(title, body, data)
                    Log.d(TAG, "Chat notification displayed successfully")
                } else {
                    val channelId = getChannelForType(type)
                    showNotification(title, body, channelId, data)
                    Log.d(TAG, "Notification displayed successfully on channel: $channelId")
                }
            } else {
                Log.w(TAG, "Empty body in notification, skipping display")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing FCM message", e)
        } finally {
            // Always release wake lock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        data: Map<String, String>
    ) {
        createNotificationChannels(this)
        
        val intent = createDeepLinkIntent(this, data)
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Use custom sound (same as channel sound)
        val soundUri = getCustomSoundUri(this)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            // Full screen intent for heads-up notification on locked screen
            .setFullScreenIntent(pendingIntent, true)

        if (body.length > 50) {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // Add notification group for same type
        val type = data["type"] ?: "general"
        notificationBuilder.setGroup("vormex_$type")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        
        Log.d(TAG, "Notification posted with ID: ${System.currentTimeMillis().toInt()}")
    }

    private fun showChatMessageNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        if (MainActivity.isInForeground) {
            Log.d(TAG, "Skipping FCM chat notification because app is in foreground")
            return
        }

        val conversationId = data["conversationId"].orEmpty()
        if (conversationId.isBlank()) {
            showNotification(title, body, CHANNEL_ID_MESSAGES, data)
            return
        }

        if (ChatMutePreferences.isMuted(this, conversationId)) {
            Log.d(TAG, "Skipping FCM chat notification — conversation muted: $conversationId")
            return
        }

        val senderName = data["senderName"]?.takeIf { it.isNotBlank() }
            ?: title
        val senderId = data["user_id"] ?: data["senderId"].orEmpty()
        val senderImage = data["senderImage"]?.takeIf { it.isNotBlank() }

        MessageNotificationManager.showMessageNotification(
            context = this,
            senderName = senderName,
            messageContent = body,
            senderImageUrl = senderImage,
            conversationId = conversationId,
            senderId = senderId
        )
    }

    private fun normalizeNotificationTitle(title: String, type: String): String {
        if (!type.contains("follow", ignoreCase = true)) {
            return title
        }

        val normalized = title.replace(Regex("^[^\\p{L}\\p{N}]+\\s*"), "").trim()
        return normalized.ifBlank { "New Follower" }
    }
}
