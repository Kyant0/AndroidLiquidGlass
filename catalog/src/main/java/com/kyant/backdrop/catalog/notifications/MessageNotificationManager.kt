package com.kyant.backdrop.catalog.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.kyant.backdrop.catalog.MainActivity
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.ChatMutePreferences
import java.net.URL

/**
 * Handles rich Android system notifications for incoming chat messages.
 *
 * Features:
 * - Sender's profile picture as large icon (downloaded from URL, circle-cropped)
 * - App icon fallback when no profile picture is available
 * - MessagingStyle for proper chat notification UX
 * - Conversation-based notification stacking (new messages update same notification)
 * - Grouped notifications when messages arrive from multiple conversations
 * - Deep link to conversation on tap
 */
object MessageNotificationManager {
    private const val TAG = "MsgNotifMgr"
    private const val GROUP_KEY_MESSAGES = "com.vormex.android.MESSAGES"
    private const val SUMMARY_NOTIFICATION_ID = 0

    private val conversationNotificationIds = mutableMapOf<String, Int>()
    private val conversationMessages = mutableMapOf<String, MutableList<CachedMessage>>()

    private data class CachedMessage(
        val senderName: String,
        val content: String,
        val timestamp: Long,
        val senderBitmap: Bitmap? = null
    )

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageContent: String,
        senderImageUrl: String?,
        conversationId: String,
        senderId: String
    ) {
        if (conversationId.isNotBlank() && ChatMutePreferences.isMuted(context, conversationId)) {
            Log.d(TAG, "Skipping notification — conversation muted: $conversationId")
            return
        }

        VormexMessagingService.createNotificationChannels(context)

        val notificationId = getNotificationIdForConversation(conversationId)

        val senderBitmap = if (!senderImageUrl.isNullOrBlank()) {
            downloadAndCircleCrop(senderImageUrl)
        } else null

        val largeIcon = senderBitmap ?: NotificationBranding.getAppLogoBitmap(context)

        val cached = CachedMessage(
            senderName = senderName,
            content = messageContent,
            timestamp = System.currentTimeMillis(),
            senderBitmap = senderBitmap
        )
        val messages = conversationMessages.getOrPut(conversationId) { mutableListOf() }
        messages.add(cached)
        if (messages.size > 8) messages.removeAt(0)

        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )

        messages.forEach { msg ->
            val person = Person.Builder()
                .setName(msg.senderName)
                .apply { msg.senderBitmap?.let { setIcon(IconCompat.createWithBitmap(it)) } }
                .build()
            messagingStyle.addMessage(msg.content, msg.timestamp, person)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(VormexMessagingService.EXTRA_ACTION, VormexMessagingService.ACTION_CHAT)
            putExtra(VormexMessagingService.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(VormexMessagingService.EXTRA_USER_ID, senderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, VormexMessagingService.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setStyle(messagingStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY_MESSAGES)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notificationId, notification)

        if (conversationMessages.size > 1) {
            showSummaryNotification(context, nm)
        }

        Log.d(TAG, "Notification shown: $senderName - ${messageContent.take(40)}")
    }

    /**
     * Call when user opens a conversation to dismiss that conversation's notification.
     */
    fun clearConversationNotification(context: Context, conversationId: String) {
        conversationMessages.remove(conversationId)
        val id = conversationNotificationIds.remove(conversationId) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id)
        if (conversationMessages.isEmpty()) {
            nm.cancel(SUMMARY_NOTIFICATION_ID)
        }
    }

    fun clearAll(context: Context) {
        conversationMessages.clear()
        conversationNotificationIds.clear()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }

    private fun showSummaryNotification(context: Context, nm: NotificationManager) {
        val totalMessages = conversationMessages.values.sumOf { it.size }
        val totalChats = conversationMessages.size
        val summaryIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(VormexMessagingService.EXTRA_ACTION, VormexMessagingService.ACTION_CHAT)
        }
        val summaryPendingIntent = PendingIntent.getActivity(
            context,
            SUMMARY_NOTIFICATION_ID,
            summaryIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summary = NotificationCompat.Builder(context, VormexMessagingService.CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(NotificationBranding.getAppLogoBitmap(context))
            .setContentTitle("Vormex")
            .setContentText("$totalMessages messages from $totalChats chats")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("$totalMessages new messages")
                    .setSummaryText("Vormex")
            )
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setContentIntent(summaryPendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    private fun getNotificationIdForConversation(conversationId: String): Int {
        return conversationNotificationIds.getOrPut(conversationId) {
            1000 + (conversationId.hashCode() and 0x7FFFFFFF) % 100000
        }
    }

    private fun downloadAndCircleCrop(imageUrl: String): Bitmap? {
        return try {
            val connection = URL(imageUrl).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream = connection.getInputStream()
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            original?.let { circularCrop(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Profile picture download failed: ${e.message}")
            null
        }
    }

    private fun circularCrop(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)

        canvas.drawOval(RectF(rect), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val left = (source.width - size) / 2
        val top = (source.height - size) / 2
        canvas.drawBitmap(source, Rect(left, top, left + size, top + size), rect, paint)

        if (source != output) source.recycle()
        return output
    }
}
