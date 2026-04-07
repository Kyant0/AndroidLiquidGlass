package com.kyant.backdrop.catalog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.network.ChatSocketManager
import com.kyant.backdrop.catalog.data.ChatMutePreferences
import com.kyant.backdrop.catalog.notifications.MessageNotificationManager
import com.kyant.backdrop.catalog.notifications.PushTokenRegistrar
import com.kyant.backdrop.catalog.notifications.VormexMessagingService
import com.kyant.backdrop.catalog.onboarding.AppRoot
import com.kyant.backdrop.catalog.payments.PremiumCheckoutManager
import com.google.firebase.messaging.FirebaseMessaging
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

/**
 * Deep link navigation state from push notifications
 */
data class NotificationDeepLink(
    val action: String,
    val userId: String? = null,
    val connectionId: String? = null,
    val postId: String? = null,
    val reelId: String? = null,
    val conversationId: String? = null,
    val referralCode: String? = null,
    val authMode: String? = null
)

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    
    companion object {
        private const val TAG = "MainActivity"
        @Volatile
        var isInForeground: Boolean = false
    }
    
    // Deep link state that can be consumed by composables
    var pendingDeepLink by mutableStateOf<NotificationDeepLink?>(null)
        private set
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            initializeFirebaseMessaging()
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen with animated exit
        val splashScreen = installSplashScreen()
        
        // Add exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Fade out animation
            splashScreenView.view.animate()
                .alpha(0f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    splashScreenView.remove()
                }
                .start()
        }
        
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request notification permission (Android 13+)
        requestNotificationPermission()
        
        // Set up local notification callback for real-time messages
        setupLocalNotifications()
        
        // Handle initial intent (e.g., app launched from notification)
        handleIntent(intent)

        PremiumCheckoutManager.preload(applicationContext)

        setContent {
            val isLightTheme = !isSystemInDarkTheme()

            CompositionLocalProvider(
                LocalIndication provides ripple(color = if (isLightTheme) Color.Black else Color.White)
            ) {
                AppRoot(
                    initialDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        isInForeground = true
    }

    override fun onStop() {
        isInForeground = false
        super.onStop()
    }

    override fun onPaymentSuccess(
        razorpayPaymentId: String?,
        paymentData: PaymentData?
    ) {
        PremiumCheckoutManager.handlePaymentSuccess(
            activity = this,
            razorpayPaymentId = razorpayPaymentId,
            paymentData = paymentData
        )
    }

    override fun onPaymentError(
        code: Int,
        response: String?,
        paymentData: PaymentData?
    ) {
        PremiumCheckoutManager.handlePaymentError(
            activity = this,
            code = code,
            response = response
        )
    }
    
    private fun handleIntent(intent: Intent?) {
        intent ?: return

        intent.data?.let { uri ->
            if (uri.path?.contains("agent", ignoreCase = true) == true) {
                pendingDeepLink = NotificationDeepLink(action = "ai_agent")
                return
            }
            val mode = uri.getQueryParameter("mode")
            val ref = uri.getQueryParameter("ref")
            if (!mode.isNullOrBlank() || !ref.isNullOrBlank()) {
                pendingDeepLink = NotificationDeepLink(
                    action = "auth_flow",
                    referralCode = ref,
                    authMode = mode
                )
                return
            }
        }
        
        val action = intent.getStringExtra(VormexMessagingService.EXTRA_ACTION)
        if (action != null) {
            val userId = intent.getStringExtra(VormexMessagingService.EXTRA_USER_ID)
            val connectionId = intent.getStringExtra(VormexMessagingService.EXTRA_CONNECTION_ID)
            val postId = intent.getStringExtra(VormexMessagingService.EXTRA_POST_ID)
            val reelId = intent.getStringExtra(VormexMessagingService.EXTRA_REEL_ID)
            val conversationId = intent.getStringExtra(VormexMessagingService.EXTRA_CONVERSATION_ID)
            
            Log.d(TAG, "Handling deep link: action=$action, userId=$userId, postId=$postId, reelId=$reelId, conversationId=$conversationId")
            
            pendingDeepLink = NotificationDeepLink(
                action = action,
                userId = userId,
                connectionId = connectionId,
                postId = postId,
                reelId = reelId,
                conversationId = conversationId
            )
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    initializeFirebaseMessaging()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // TODO: Show UI explaining why notifications are important
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, no runtime permission needed
            initializeFirebaseMessaging()
        }
    }

    private fun initializeFirebaseMessaging() {
        try {
            PushTokenRegistrar.syncCurrentToken(this)
            
            // Subscribe to general announcements topic
            FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Subscribed to announcements topic")
                    }
                }
                
            Log.d(TAG, "Firebase Messaging initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Messaging initialization failed - check google-services.json", e)
        }
    }
    
    private fun setupLocalNotifications() {
        VormexMessagingService.createNotificationChannels(this)

        ChatSocketManager.setNotificationCallback { senderName, messageContent, data ->
            if (!isInForeground) {
                Log.d(TAG, "🔕 Skipping socket notification while app is backgrounded")
                return@setNotificationCallback
            }

            val convId = data["conversationId"].orEmpty()
            if (convId.isNotBlank() && ChatMutePreferences.isMuted(this, convId)) {
                Log.d(TAG, "🔕 Skipping socket notification — conversation muted: $convId")
                return@setNotificationCallback
            }

            Log.d(TAG, "🔔 Local notification: $senderName - $messageContent")
            MessageNotificationManager.showMessageNotification(
                context = this,
                senderName = senderName,
                messageContent = messageContent,
                senderImageUrl = data["senderImage"],
                conversationId = data["conversationId"] ?: "",
                senderId = data["user_id"] ?: ""
            )
        }
    }
}
