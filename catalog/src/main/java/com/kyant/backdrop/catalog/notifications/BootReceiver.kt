package com.kyant.backdrop.catalog.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot receiver to initialize notification channels when device restarts
 * Ensures push notifications work even after device reboot
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "VormexBootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, initializing notification channels")
            
            // Create notification channels
            VormexMessagingService.createNotificationChannels(context)
            
            // Re-subscribe to important topics
            VormexMessagingService.subscribeToTopic("announcements")
            
            Log.d(TAG, "Notification channels initialized after boot")
        }
    }
}
