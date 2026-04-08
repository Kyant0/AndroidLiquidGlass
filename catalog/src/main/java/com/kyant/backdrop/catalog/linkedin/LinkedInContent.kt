package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Comment
import com.kyant.backdrop.catalog.network.models.FullProfileResponse
import com.kyant.backdrop.catalog.network.models.PendingConnectionRequest
import com.kyant.backdrop.catalog.network.models.ProfileUpdateRequest
import com.kyant.backdrop.catalog.network.models.CelebrationType
import com.kyant.backdrop.catalog.network.models.PollOption
import com.kyant.backdrop.catalog.network.models.Post
import com.kyant.backdrop.catalog.network.models.PremiumSubscriptionResponse
import com.kyant.backdrop.catalog.network.models.StoryGroup
import com.kyant.backdrop.catalog.payments.PremiumCheckoutManager
import com.kyant.backdrop.catalog.payments.findComponentActivity
import com.kyant.backdrop.catalog.chat.ChatTabContent
import com.kyant.backdrop.catalog.linkedin.posts.SharePostModal
import com.kyant.backdrop.catalog.linkedin.posts.FormattedContent
import com.kyant.backdrop.catalog.linkedin.posts.MentionProfilePreviewPopup
import com.kyant.backdrop.catalog.linkedin.groups.GroupsScreen
import com.kyant.backdrop.catalog.linkedin.groups.GroupDetailScreen
import com.kyant.backdrop.catalog.linkedin.groups.GroupChatScreen
import com.kyant.backdrop.catalog.linkedin.groups.CirclesScreen
import com.kyant.backdrop.catalog.linkedin.groups.CircleDetailScreen
import com.kyant.backdrop.catalog.linkedin.reels.ReelsPreviewSection
import com.kyant.backdrop.catalog.linkedin.reels.ReelsFeedScreen
import com.kyant.backdrop.catalog.linkedin.reels.ReelCommentsSheet
import com.kyant.backdrop.catalog.linkedin.reels.ReelsViewModel
import com.kyant.backdrop.catalog.network.models.Reel
import com.kyant.backdrop.catalog.onboarding.ProfileSetupWizard
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kyant.backdrop.catalog.chat.ChatViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Pacifico font family
private val PacificoFontFamily = FontFamily(
    Font(R.font.pacifico)
)

// Kaushan Script font family for vormeX branding
private val KaushanScriptFontFamily = FontFamily(
    Font(R.font.kaushan_script)
)

// Shimmer effect for skeleton loading
@Composable
private fun shimmerBrush(isLightTheme: Boolean): Brush {
    val shimmerColors = if (isLightTheme) {
        listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            Color.DarkGray.copy(alpha = 0.3f),
            Color.DarkGray.copy(alpha = 0.5f),
            Color.DarkGray.copy(alpha = 0.3f)
        )
    }
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 300f, translateAnimation.value - 300f),
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
}

// Skeleton loading card for posts — avoid full glass blur on placeholders (much cheaper GPU work).
@Composable
private fun PostSkeletonCard(
    shimmerBrush: Brush,
    isLightTheme: Boolean
) {
    val surfaceColor =
        if (isLightTheme) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
    val borderColor =
        if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Author skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar skeleton
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    // Name skeleton
                    Box(
                        Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(6.dp))
                    // Headline skeleton
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Time skeleton
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            
            // Content skeleton - multiple lines
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            
            // Image skeleton
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )
            
            // Stats skeleton
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            
            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isLightTheme) Color.LightGray.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f))
            )
            
            // Action buttons skeleton
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionOverlayContainer(
    backdrop: LayerBackdrop,
    themeMode: String,
    glassBackgroundKey: String,
    accentColor: Color,
    glassMotionStyleKey: String,
    reduceAnimations: Boolean,
    content: @Composable () -> Unit
) {
    val appearance = currentVormexAppearance(themeMode)
    val isGlassTheme = appearance.isGlassTheme

    Box(Modifier.fillMaxSize()) {
        if (isGlassTheme) {
            GlassBackgroundLayer(
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                backgroundKey = glassBackgroundKey,
                accentColor = accentColor,
                motionStyleKey = glassMotionStyleKey,
                reduceAnimations = reduceAnimations
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(appearance.backgroundColor)
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            content()
        }
    }
}

enum class LinkedInTab {
    Home, Network, Post, Notifications, Jobs
}

// Helper to get Activity from Context
private fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInContent(
    deepLink: com.kyant.backdrop.catalog.NotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val appScope = rememberCoroutineScope()
    val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val glassBackgroundKey by SettingsPreferences.glassBackgroundPreset(context)
        .collectAsState(initial = DefaultGlassBackgroundPresetKey)
    val accentPaletteKey by SettingsPreferences.accentPalette(context)
        .collectAsState(initial = DefaultAccentPaletteKey)
    val glassMotionStyleKey by SettingsPreferences.glassMotionStyle(context)
        .collectAsState(initial = DefaultGlassMotionStyleKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val appearance = rememberVormexAppearance(themeMode)
    val isGlassTheme = appearance.isGlassTheme
    val isLightTheme = appearance.isLightTheme
    val isDarkTheme = appearance.isDarkTheme
    val contentColor = appearance.contentColor
    val accentColor = glassAccentPalette(accentPaletteKey).color

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showFullMoreScreen by rememberSaveable { mutableStateOf(false) }
    var moreHubAnimationKey by rememberSaveable { mutableIntStateOf(0) }
    var viewingProfileUserId by remember { mutableStateOf<String?>(null) }
    var openChatWithUserId by remember { mutableStateOf<String?>(null) }
    // Track if user is viewing a personal chat thread (for hiding bottom nav)
    var isInChatThread by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    
    // Messages screen state
    var showMessagesScreen by remember { mutableStateOf(false) }
    
    // Groups & Circles navigation state
    var showGroupsScreen by remember { mutableStateOf(false) }
    var showCirclesScreen by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedCircleId by remember { mutableStateOf<String?>(null) }
    var showGroupChat by remember { mutableStateOf(false) }
    
    // Retention features navigation state
    var showWeeklyGoalsScreen by remember { mutableStateOf(false) }
    var showStreakDetailsScreen by remember { mutableStateOf(false) }
    var showTopNetworkersScreen by remember { mutableStateOf(false) }
    var showOnboardingScreen by remember { mutableStateOf(false) }
    var showSessionSummary by remember { mutableStateOf(false) }
    var showConnectionCelebration by remember { mutableStateOf(false) }
    var celebrationConnectionId by remember { mutableStateOf<String?>(null) }
    
    // Deep link navigation state - for opening specific post/reel from notification
    var deepLinkPostId by remember { mutableStateOf<String?>(null) }
    var deepLinkReelId by remember { mutableStateOf<String?>(null) }
    var deepLinkConversationId by remember { mutableStateOf<String?>(null) }

    // Shared post detail/comments state (used by feed and profile screens)
    var showCommentsSheet by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<String?>(null) }
    
    // Settings & More screen navigation state
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSavedPostsScreen by remember { mutableStateOf(false) }
    var showConnectionRequestsScreen by remember { mutableStateOf(false) }
    var showProfileCustomizationsScreen by remember { mutableStateOf(false) }
    var showNotificationsInbox by remember { mutableStateOf(false) }
    var showNotificationSettingsScreen by remember { mutableStateOf(false) }
    var showPrivacySettingsScreen by remember { mutableStateOf(false) }
    var showAppearanceSettingsScreen by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var showInviteFriendsScreen by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showContactScreen by remember { mutableStateOf(false) }
    var showGrowthHubScreen by remember { mutableStateOf(false) }
    var showAgentSheet by remember { mutableStateOf(false) }
    var minimizeAgentSheetForVoice by remember { mutableStateOf(false) }
    var autoMinimizedAgentSheetForActiveVoice by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var notificationUnreadCount by remember { mutableIntStateOf(0) }
    
    val hasOverlayBackNavigation = viewingProfileUserId != null ||
            showMessagesScreen ||
            showGroupChat ||
            selectedGroupId != null ||
            selectedCircleId != null ||
            showGroupsScreen ||
            showCirclesScreen ||
            showWeeklyGoalsScreen ||
            showStreakDetailsScreen ||
            showTopNetworkersScreen ||
            showSessionSummary ||
            showConnectionCelebration ||
            showProfileScreen ||
            showSavedPostsScreen ||
            showConnectionRequestsScreen ||
            showProfileCustomizationsScreen ||
            showNotificationsInbox ||
            showNotificationSettingsScreen ||
            showPrivacySettingsScreen ||
            showAppearanceSettingsScreen ||
            showHelpScreen ||
            showInviteFriendsScreen ||
            showAboutScreen ||
            showContactScreen ||
            showGrowthHubScreen

    // Handle system back button for all overlay screens
    // Priority: innermost overlays first, then outer overlays
    BackHandler(enabled = hasOverlayBackNavigation) {
        when {
            // Profile viewing (highest priority - innermost overlay)
            viewingProfileUserId != null -> viewingProfileUserId = null
            
            // Messages screen
            showMessagesScreen -> showMessagesScreen = false
            
            // Group chat
            showGroupChat -> showGroupChat = false
            
            // Group/Circle detail screens
            selectedGroupId != null -> selectedGroupId = null
            selectedCircleId != null -> selectedCircleId = null
            
            // Groups/Circles list screens
            showGroupsScreen -> showGroupsScreen = false
            showCirclesScreen -> showCirclesScreen = false
            
            // Retention feature screens
            showWeeklyGoalsScreen -> showWeeklyGoalsScreen = false
            showStreakDetailsScreen -> showStreakDetailsScreen = false
            showTopNetworkersScreen -> showTopNetworkersScreen = false
            showSessionSummary -> showSessionSummary = false
            showConnectionCelebration -> showConnectionCelebration = false
            
            // Settings screens
            showProfileScreen -> showProfileScreen = false
            showSavedPostsScreen -> showSavedPostsScreen = false
            showConnectionRequestsScreen -> showConnectionRequestsScreen = false
            showProfileCustomizationsScreen -> showProfileCustomizationsScreen = false
            showNotificationsInbox -> showNotificationsInbox = false
            showNotificationSettingsScreen -> showNotificationSettingsScreen = false
            showPrivacySettingsScreen -> showPrivacySettingsScreen = false
            showAppearanceSettingsScreen -> showAppearanceSettingsScreen = false
            showHelpScreen -> showHelpScreen = false
            showInviteFriendsScreen -> showInviteFriendsScreen = false
            showAboutScreen -> showAboutScreen = false
            showContactScreen -> showContactScreen = false
            showGrowthHubScreen -> showGrowthHubScreen = false
        }
    }

    BackHandler(enabled = !hasOverlayBackNavigation && selectedTab == 3 && showFullMoreScreen) {
        showFullMoreScreen = false
    }

    // Android back gesture: from non-Home bottom tabs, go back to Home first.
    BackHandler(
        enabled = !hasOverlayBackNavigation &&
            selectedTab != 0 &&
            !(selectedTab == 3 && showFullMoreScreen)
    ) {
        selectedTab = 0
    }

    LaunchedEffect(uiState.isLoggedIn, uiState.currentUserId) {
        notificationUnreadCount = if (uiState.isLoggedIn) {
            ApiClient.getNotificationUnreadCount(context).getOrDefault(0)
        } else {
            0
        }
    }
    
    // Handle deep links from push notifications
    LaunchedEffect(deepLink) {
        deepLink?.let { link ->
            when (link.action) {
                "auth_flow" -> {
                    if (!uiState.isLoggedIn) {
                        viewModel.setPendingReferralCode(link.referralCode)
                        if (link.authMode.equals("signup", ignoreCase = true) || !link.referralCode.isNullOrBlank()) {
                            viewModel.showSignUp()
                        }
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_STREAK_REMINDER -> {
                    showStreakDetailsScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_WEEKLY_GOAL -> {
                    showWeeklyGoalsScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_LEADERBOARD -> {
                    showTopNetworkersScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_CONNECTION_CELEBRATION -> {
                    link.connectionId?.let { connectionId ->
                        celebrationConnectionId = connectionId
                        showConnectionCelebration = true
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_SESSION_SUMMARY -> {
                    showSessionSummary = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_PROFILE -> {
                    link.userId?.let { userId ->
                        viewingProfileUserId = userId
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_CHAT -> {
                    // Open the messages overlay instead of the Post tab.
                    showMessagesScreen = true
                    if (!link.conversationId.isNullOrBlank()) {
                        val convId = link.conversationId
                        deepLinkConversationId = convId
                        openChatWithUserId = null
                    } else if (!link.userId.isNullOrBlank()) {
                        deepLinkConversationId = null
                        openChatWithUserId = link.userId
                    } else {
                        deepLinkConversationId = null
                        openChatWithUserId = null
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_POST -> {
                    // Navigate to specific post (like, comment, mention notifications)
                    link.postId?.let { postId ->
                        selectedTab = 0 // Feed tab
                        deepLinkPostId = postId
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_REEL -> {
                    // Navigate to specific reel
                    link.reelId?.let { reelId ->
                        selectedTab = 0 // Feed tab
                        deepLinkReelId = reelId
                    }
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_CONNECTIONS -> {
                    // Navigate to connections screen
                    selectedTab = 1 // Find People tab (connections)
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_FIND_PEOPLE -> {
                    selectedTab = 1 // Find People tab
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_STREAK -> {
                    showStreakDetailsScreen = true
                }
                com.kyant.backdrop.catalog.notifications.VormexMessagingService.ACTION_ENGAGEMENT -> {
                    // Show engagement/rewards
                    selectedTab = 0 // Feed tab
                }
            }
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab != 2 && isInChatThread) {
            isInChatThread = false
        }
        if (selectedTab != 3 && showFullMoreScreen) {
            showFullMoreScreen = false
        }
    }

    // Find/Profile/Reels state shared across tab switches so those sections stay warm.
    val findPeopleViewModel: FindPeopleViewModel = viewModel(
        key = "find-people",
        factory = FindPeopleViewModel.Factory(context)
    )
    val rewardsState by findPeopleViewModel.uiState.collectAsState()
    val ownProfileViewModel: ProfileViewModel = viewModel(
        key = "profile:me",
        factory = ProfileViewModel.Factory(context)
    )
    val premiumRefreshSignal by PremiumCheckoutManager.refreshSignal.collectAsState()
    val rewardCardsViewModel: RewardCardsViewModel = viewModel(factory = RewardCardsViewModel.Factory(context))
    val rewardCardsState by rewardCardsViewModel.uiState.collectAsState()
    
    // Reels state
    val reelsViewModel: ReelsViewModel = viewModel(factory = ReelsViewModel.Factory(context))
    val reelsState by reelsViewModel.uiState.collectAsState()
    
    // Retention features state (Weekly Goals, Leaderboard, Session Summary)
    val retentionViewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val retentionState by retentionViewModel.uiState.collectAsState()
    
    // Chat state for unread message indicator
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(context))
    val chatState by chatViewModel.uiState.collectAsState()
    val agentViewModel: AgentViewModel = viewModel(factory = AgentViewModel.Factory(context))
    val agentState by agentViewModel.uiState.collectAsState()
    val canUseAgent = uiState.currentUser?.canUseAgent == true
    val isAgentVoiceLive =
        agentState.isVoiceSessionConnecting ||
            agentState.isRecordingVoice ||
            agentState.isVoiceListening ||
            agentState.isVoiceThinking ||
            agentState.isPlayingAudio
    val visibleInlinePeople =
        agentState.activeInlineResults?.visiblePeople(agentState.dismissedInlineResultIds).orEmpty()
    val visibleInlinePeopleIds = visibleInlinePeople.map { it.id }
    val agentSheetAnimationDuration = if (reduceAnimations) 0 else 220
    val agentSheetAlpha by animateFloatAsState(
        targetValue = if (minimizeAgentSheetForVoice) 0f else 1f,
        animationSpec = tween(durationMillis = agentSheetAnimationDuration, easing = FastOutSlowInEasing),
        label = "agentSheetAlpha"
    )
    val agentSheetScale by animateFloatAsState(
        targetValue = if (minimizeAgentSheetForVoice) 0.96f else 1f,
        animationSpec = tween(durationMillis = agentSheetAnimationDuration, easing = FastOutSlowInEasing),
        label = "agentSheetScale"
    )
    val agentSheetScrimAlpha by animateFloatAsState(
        targetValue = if (minimizeAgentSheetForVoice) 0f else 0.32f,
        animationSpec = tween(durationMillis = agentSheetAnimationDuration, easing = FastOutSlowInEasing),
        label = "agentSheetScrimAlpha"
    )

    val agentSurface = when {
        showGrowthHubScreen -> "growth_hub"
        showNotificationsInbox -> "notifications"
        showMessagesScreen || isInChatThread -> "chat"
        showGroupsScreen || selectedGroupId != null || showGroupChat -> "groups"
        viewingProfileUserId != null || selectedTab == 4 -> "profile"
        selectedTab == 1 -> "find_people"
        else -> "feed"
    }
    val agentSurfaceContext = remember(
        agentSurface,
        selectedTab,
        viewingProfileUserId,
        selectedGroupId,
        openChatWithUserId,
        chatState.selectedConversation?.id,
        uiState.currentUser?.id,
        visibleInlinePeopleIds,
        agentState.activeInlineResults?.source
    ) {
        buildMap<String, String> {
            put("surface", agentSurface)
            put("selectedTab", selectedTab.toString())
            uiState.currentUser?.id?.let { put("currentUserId", it) }
            viewingProfileUserId?.let { put("viewingProfileUserId", it) }
            selectedGroupId?.let { put("selectedGroupId", it) }
            openChatWithUserId?.let { put("openChatWithUserId", it) }
            chatState.selectedConversation?.id?.let { put("conversationId", it) }
            if (visibleInlinePeopleIds.isNotEmpty()) {
                put("inlineResultUserIds", visibleInlinePeopleIds.joinToString(","))
            }
            agentState.activeInlineResults?.source
                ?.takeIf { it.isNotBlank() }
                ?.let { put("inlineResultSource", it) }
        }
    }
    val shouldHideInlineResultsOverlay =
        viewingProfileUserId != null ||
            selectedTab == 4 ||
            showMessagesScreen ||
            isInChatThread ||
            showGroupsScreen ||
            selectedGroupId != null ||
            showGroupChat ||
            showNotificationsInbox ||
            showGrowthHubScreen
    val shouldShowInlineResultsOverlay =
        uiState.isLoggedIn &&
            !showAgentSheet &&
            !shouldHideInlineResultsOverlay &&
            visibleInlinePeople.isNotEmpty()

    LaunchedEffect(selectedTab, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && selectedTab == 3) {
            findPeopleViewModel.loadPendingConnectionRequests()
        }
    }

    LaunchedEffect(shouldHideInlineResultsOverlay, agentState.activeInlineResults) {
        if (shouldHideInlineResultsOverlay && agentState.activeInlineResults != null) {
            agentViewModel.dismissInlineResults()
        }
    }

    // Own profile caches for 5 minutes; refresh when opening the tab so new posts (e.g. celebrations) appear.
    LaunchedEffect(selectedTab, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && selectedTab == 4) {
            ownProfileViewModel.loadProfile(userId = null, forceRefresh = true)
        }
    }

    LaunchedEffect(showConnectionRequestsScreen, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && showConnectionRequestsScreen) {
            findPeopleViewModel.loadPendingConnectionRequests(forceRefresh = true)
        }
    }

    LaunchedEffect(premiumRefreshSignal, uiState.isLoggedIn) {
        if (uiState.isLoggedIn && premiumRefreshSignal != 0L) {
            viewModel.refreshCurrentUser()
            ownProfileViewModel.loadProfile(userId = null, forceRefresh = true)
        }
    }

    // Stagger background prefetches so cold start is not a thundering herd competing with
    // FeedViewModel (feed + user + socket). Reels prefetch (video bytes) runs last.
    LaunchedEffect(uiState.isLoggedIn) {
        if (!uiState.isLoggedIn) {
            rewardCardsViewModel.maybeLoadOnAppOpen(false)
            return@LaunchedEffect
        }
        delay(120)
        chatViewModel.preloadChats()
        delay(120)
        ownProfileViewModel.prefetchOwnProfile()
        delay(120)
        findPeopleViewModel.prefetchInitialData()
        delay(280)
        retentionViewModel.ensureRetentionLoaded()
        delay(220)
        reelsViewModel.prefetchAppStartData()
        rewardCardsViewModel.maybeLoadOnAppOpen(true)
    }

    LaunchedEffect(showAgentSheet, agentSurface, uiState.isLoggedIn, canUseAgent) {
        if (showAgentSheet && uiState.isLoggedIn && canUseAgent) {
            agentViewModel.ensureSession(surface = agentSurface)
        }
    }

    LaunchedEffect(uiState.isLoggedIn, agentSurface, agentSurfaceContext, canUseAgent) {
        if (uiState.isLoggedIn && canUseAgent) {
            agentViewModel.ensureSession(surface = agentSurface)
            agentViewModel.syncSurface(
                surface = agentSurface,
                surfaceContext = agentSurfaceContext
            )
        }
    }

    LaunchedEffect(canUseAgent) {
        if (!canUseAgent) {
            autoMinimizedAgentSheetForActiveVoice = false
            minimizeAgentSheetForVoice = false
            showAgentSheet = false
            agentViewModel.dismissInlineResults()
        }
    }

    val openAgentPanel: () -> Unit = {
        if (uiState.currentUser?.canUseAgent == true) {
            showFullMoreScreen = false
            minimizeAgentSheetForVoice = false
            showAgentSheet = true
        } else {
            Toast.makeText(
                context,
                "AI Agent access is not enabled for this account yet.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(isAgentVoiceLive) {
        if (!isAgentVoiceLive) {
            autoMinimizedAgentSheetForActiveVoice = false
            minimizeAgentSheetForVoice = false
        }
    }

    LaunchedEffect(showAgentSheet, isAgentVoiceLive, reduceAnimations) {
        if (showAgentSheet && isAgentVoiceLive && !autoMinimizedAgentSheetForActiveVoice) {
            autoMinimizedAgentSheetForActiveVoice = true
            minimizeAgentSheetForVoice = true
            if (!reduceAnimations) {
                delay(agentSheetAnimationDuration.toLong())
            }
            showAgentSheet = false
            minimizeAgentSheetForVoice = false
        }
    }

    LaunchedEffect(agentState.pendingUiIntents, reduceAnimations) {
        val pendingIntents = agentState.pendingUiIntents
        if (pendingIntents.isEmpty()) return@LaunchedEffect
        val previewDelayMs = if (reduceAnimations) 0L else 170L
        val transitionDelayMs = if (reduceAnimations) 0L else 140L

        suspend fun resetOverlayNavigation() {
            val hadOverlay =
                showGrowthHubScreen ||
                    showNotificationsInbox ||
                    showMessagesScreen ||
                    isInChatThread ||
                    openChatWithUserId != null ||
                    deepLinkConversationId != null ||
                    showGroupsScreen ||
                    selectedGroupId != null ||
                    showGroupChat ||
                    viewingProfileUserId != null
            showGrowthHubScreen = false
            showNotificationsInbox = false
            showMessagesScreen = false
            isInChatThread = false
            openChatWithUserId = null
            deepLinkConversationId = null
            showGroupsScreen = false
            selectedGroupId = null
            showGroupChat = false
            viewingProfileUserId = null
            if (hadOverlay && transitionDelayMs > 0) {
                delay(transitionDelayMs)
            }
        }

        pendingIntents.forEach { intent ->
            agentViewModel.previewUiIntent(intent)
            if (previewDelayMs > 0) {
                delay(previewDelayMs)
            }
            when (intent.type) {
                "switch_tab" -> {
                    resetOverlayNavigation()
                    when (intent.tab?.lowercase()) {
                        "feed", "home" -> selectedTab = 0
                        "find", "find_people", "network" -> selectedTab = 1
                        "post", "create_post" -> selectedTab = 2
                        "more" -> selectedTab = 3
                        "profile" -> selectedTab = 4
                        "groups" -> {
                            selectedTab = 3
                            showGroupsScreen = true
                        }
                    }
                }
                "open_profile" -> {
                    resetOverlayNavigation()
                    selectedTab = 4
                    intent.userId?.let { viewingProfileUserId = it }
                }
                "open_chat" -> {
                    resetOverlayNavigation()
                    showMessagesScreen = true
                    intent.conversationId?.let { deepLinkConversationId = it }
                    if (intent.conversationId.isNullOrBlank()) {
                        openChatWithUserId = intent.userId
                    }
                }
                "open_group" -> {
                    resetOverlayNavigation()
                    selectedTab = 3
                    showGroupsScreen = true
                    intent.groupId?.let { selectedGroupId = it }
                }
                "open_groups" -> {
                    resetOverlayNavigation()
                    selectedTab = 3
                    showGroupsScreen = true
                }
                "open_notifications" -> {
                    resetOverlayNavigation()
                    showNotificationsInbox = true
                }
                "open_growth_task" -> {
                    resetOverlayNavigation()
                    showGrowthHubScreen = true
                }
                "show_match_stack" -> {
                    resetOverlayNavigation()
                    selectedTab = 1
                }
            }
            if (transitionDelayMs > 0) {
                delay(transitionDelayMs / 2)
            }
        }

        agentViewModel.consumeUiIntents()
    }

    CompositionLocalProvider(LocalVormexAppearance provides appearance) {
    Box(Modifier.fillMaxSize()) {
        // Background based on theme
        if (isGlassTheme) {
            GlassBackgroundLayer(
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                backgroundKey = glassBackgroundKey,
                accentColor = accentColor,
                motionStyleKey = glassMotionStyleKey,
                reduceAnimations = reduceAnimations
            )
        } else {
            // Solid color background for White/Dark themes
            Box(
                Modifier
                    .fillMaxSize()
                    .background(appearance.backgroundColor)
            )
        }
        
        // Show auth screen if not logged in
        if (!uiState.isLoggedIn) {
            when (uiState.authScreen) {
                AuthScreen.LOGIN -> LoginScreen(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLoading = uiState.isLoading,
                    isGoogleLoading = uiState.isGoogleLoading,
                    error = uiState.error,
                    onLogin = { email, password -> viewModel.login(email, password) },
                    onGoogleSignIn = { activity?.let { viewModel.googleSignIn(it) } },
                    onForgotPassword = { email ->
                        appScope.launch {
                            ApiClient.forgotPassword(email)
                                .onSuccess { response ->
                                    Toast.makeText(
                                        context,
                                        response.message.ifBlank {
                                            "Password reset link sent. Check your email."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Could not send reset email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    },
                    onSignUpClick = { viewModel.showSignUp() },
                    onClearError = { viewModel.clearError() }
                )
                AuthScreen.SIGNUP -> SignUpScreen(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLoading = uiState.isLoading,
                    isGoogleLoading = uiState.isGoogleLoading,
                    error = uiState.error,
                    onSignUp = { email, password, name, username -> viewModel.register(email, password, name, username) },
                    onGoogleSignIn = { activity?.let { viewModel.googleSignIn(it) } },
                    onLoginClick = { viewModel.showLogin() },
                    onClearError = { viewModel.clearError() }
                )
            }
        } else if (uiState.showOnboarding) {
            // Show onboarding wizard for new users
            ProfileSetupWizard(
                onComplete = {
                    viewModel.completeOnboarding()
                },
                onSkip = {
                    viewModel.skipOnboarding()
                }
            )
        } else {
            // Content
            Column(
                Modifier
                    .fillMaxSize()
                    .then(
                        // Only add status bar padding when NOT on profile tab (to allow banner to extend to top)
                        if (selectedTab != 4) Modifier.statusBarsPadding() else Modifier
                    )
                    .displayCutoutPadding()
            ) {
                // Top bar (hidden when in chat thread or on profile tab)
                if (!isInChatThread && selectedTab != 4) {
                    LinkedInTopBar(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        userInitials = uiState.currentUser?.name?.firstOrNull()?.toString() ?: "U",
                        loginStreak = uiState.loginStreak,
                        isStreakAtRisk = uiState.isStreakAtRisk,
                        hasUnreadNotifications = notificationUnreadCount > 0,
                        unreadNotificationCount = notificationUnreadCount,
                        hasUnreadMessages = chatState.unreadCount > 0,
                        onStreakClick = { showStreakDetailsScreen = true },
                        onNotificationsClick = {
                            showNotificationsInbox = true
                        },
                        onMessagesClick = {
                            openChatWithUserId = null
                            showMessagesScreen = true
                        }
                    )
                }

                // Main content based on selected tab
                Box(
                    Modifier
                        .fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Handle deep link to specific post (from notification)
                            LaunchedEffect(deepLinkPostId) {
                                deepLinkPostId?.let { postId ->
                                    // Open the comments sheet for this post
                                    selectedPostForComments = postId
                                    viewModel.loadComments(postId)
                                    showCommentsSheet = true
                                    deepLinkPostId = null // Clear after handling
                                }
                            }
                            
                            // Handle deep link to specific reel (from notification)
                            LaunchedEffect(deepLinkReelId) {
                                deepLinkReelId?.let { reelId ->
                                    // Open the reel viewer
                                    reelsViewModel.loadReelById(reelId)
                                    deepLinkReelId = null // Clear after handling
                                }
                            }
                            
                            Box(Modifier.fillMaxSize()) {
                                FeedScreen(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    glassBackgroundKey = glassBackgroundKey,
                                    posts = uiState.posts,
                                    storyGroups = uiState.storyGroups,
                                    // Reels data
                                    reels = reelsState.previewReels,
                                    isLoadingReels = reelsState.isLoadingPreview,
                                    onReelClick = { index ->
                                        reelsViewModel.openReelsViewer(reelsState.previewReels, index)
                                    },
                                    onSeeAllReelsClick = {
                                        reelsViewModel.loadReelsFeed()
                                        reelsViewModel.openReelsViewer(reelsState.previewReels, 0)
                                    },
                                isLoading = uiState.isLoading,
                                error = uiState.error,
                                currentUserInitials = uiState.currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull()?.uppercase() }?.take(2)?.joinToString("") ?: "U",
                                currentUserProfileImage = uiState.currentUser?.profileImage,
                                currentUserName = uiState.currentUser?.name ?: "You",
                                isLightTheme = isLightTheme,
                                // Streak data (Duolingo Effect)
                                connectionStreak = uiState.connectionStreak,
                                loginStreak = uiState.loginStreak,
                                isStreakAtRisk = uiState.isStreakAtRisk,
                                showStreakReminder = uiState.showStreakReminder,
                                showLoginStreakBadge = uiState.showLoginStreakBadge,
                                onDismissStreakReminder = { viewModel.dismissStreakReminder() },
                                onDismissLoginStreakBadge = { viewModel.dismissLoginStreakBadge() },
                                onNavigateToFindPeople = { 
                                    viewModel.clearError() // Clear any error when navigating
                                    selectedTab = 1 
                                },
                                onRefresh = {
                                    viewModel.loadFeed(forceRefresh = true)
                                    viewModel.loadStories(forceRefresh = true)
                                    reelsViewModel.loadPreviewReels(forceRefresh = true)
                                    findPeopleViewModel.refreshAllVariableRewards()
                                    retentionViewModel.loadAllRetentionData(forceRefresh = true)
                                },
                                onLike = { postId -> viewModel.toggleLike(postId) },
                                onComment = { postId ->
                                    selectedPostForComments = postId
                                    viewModel.loadComments(postId)
                                    showCommentsSheet = true
                                },
                                onShare = { postId ->
                                    viewModel.showShareModal(postId)
                                },
                                onVotePoll = { postId, optionId ->
                                    viewModel.votePoll(postId, optionId)
                                },
                                onProfileClick = { userId ->
                                    viewingProfileUserId = userId
                                },
                                onMenuAction = { postId, action ->
                                    // Handle menu actions
                                    when (action) {
                                        "report" -> { /* Handle report */ }
                                        "save" -> { /* Handle save */ }
                                        "copy_link" -> { /* Handle copy link */ }
                                        "not_interested" -> { /* Handle not interested */ }
                                    }
                                },
                                onStoryClick = { groupIndex ->
                                    viewModel.openStoryViewer(groupIndex)
                                },
                                onAddStoryClick = {
                                    viewModel.openStoryCreator()
                                },
                                onMyStoryClick = {
                                    // Find own story group index and open viewer
                                    val myStoryIndex = uiState.storyGroups.indexOfFirst { it.isOwnStory }
                                    if (myStoryIndex >= 0) {
                                        viewModel.openStoryViewer(myStoryIndex)
                                    }
                                },
                                // Onboarding prompt - show for users who skipped but didn't complete
                                showOnboarding = !uiState.onboardingCompleted && !uiState.showOnboarding,
                                onNavigateToOnboarding = { viewModel.showOnboardingAgain() },
                                // Retention features data
                                retentionState = retentionState,
                                onWeeklyGoalsClick = { showWeeklyGoalsScreen = true },
                                onStreakDetailsClick = { showStreakDetailsScreen = true },
                                onTopNetworkersClick = { showTopNetworkersScreen = true },
                                reduceAnimations = reduceAnimations,
                                hasMore = uiState.hasMore,
                                isLoadingMore = uiState.isLoadingMore,
                                onLoadMore = { viewModel.loadMorePosts() }
                            )
                            
                            // Share modal
                            if (uiState.showShareModal && uiState.sharePostId != null) {
                                SharePostModal(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isLightTheme = isLightTheme,
                                    connections = uiState.mentionSearchResults,
                                    isLoading = uiState.isSearchingMentions,
                                    isSharing = uiState.isSharing,
                                    error = null,
                                    onDismiss = { viewModel.hideShareModal() },
                                    onShareToConnections = { connectionIds, message ->
                                        // For now, just share externally
                                        activity?.let { viewModel.sharePostExternal(uiState.sharePostId!!, it) }
                                    },
                                    onSearchConnections = { query ->
                                        viewModel.searchMentions(query)
                                    },
                                    onClearError = { /* No error state yet */ }
                                )
                            }
                            
                            // Story Viewer Dialog
                            if (uiState.isStoryViewerOpen && uiState.storyGroups.isNotEmpty()) {
                                StoryViewerDialog(
                                    storyGroups = uiState.storyGroups,
                                    accentColor = accentColor,
                                    initialGroupIndex = uiState.currentStoryGroupIndex,
                                    onDismiss = { viewModel.closeStoryViewer() },
                                    onStoryViewed = { storyId -> viewModel.viewStory(storyId) },
                                    onReact = { storyId, reaction -> viewModel.reactToStory(storyId, reaction) },
                                    onReply = { storyId, content -> viewModel.replyToStory(storyId, content) },
                                    onGetViewers = { storyId, callback -> viewModel.getStoryViewers(storyId, callback) }
                                )
                            }
                            
                            // Story Creator Dialog
                            if (uiState.isStoryCreatorOpen) {
                                StoryCreatorDialog(
                                    onDismiss = { viewModel.closeStoryCreator() },
                                    onCreateStory = { mediaType, mediaBytes, textContent, backgroundColor, category, visibility, linkUrl, linkTitle ->
                                        viewModel.createStory(
                                            mediaType = mediaType,
                                            mediaBytes = mediaBytes,
                                            textContent = textContent,
                                            backgroundColor = backgroundColor,
                                            category = category,
                                            visibility = visibility,
                                            linkUrl = linkUrl,
                                            linkTitle = linkTitle,
                                            onSuccess = { viewModel.closeStoryCreator() }
                                        )
                                    },
                                    isCreating = uiState.isCreatingStory
                                )
                            }
                            
                                // Upload Progress Bar (Instagram-style)
                                GlassUploadProgressBar(
                                    uploadProgress = uiState.uploadProgress,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onDismiss = { viewModel.dismissUploadError() },
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                        .statusBarsPadding()
                                )
                                
                                // Trending Banner (auto-hide after 2 seconds)
                                TrendingBannerAutoHide(
                                    isTrending = rewardsState.isTrending,
                                    rank = rewardsState.trendingRank,
                                    viewsToday = rewardsState.trendingViewsToday,
                                    message = rewardsState.trendingMessage,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp)
                                )
                            } // Close the Box wrapping FeedScreen
                        }
                        1 -> FindPeopleScreenNew(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            findPeopleViewModel = findPeopleViewModel,
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId },
                            reduceAnimations = reduceAnimations
                        )
                        2 -> com.kyant.backdrop.catalog.linkedin.posts.CreatePostScreen(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isCreating = uiState.isCreatingPost,
                            error = uiState.error,
                            userName = uiState.currentUser?.name ?: uiState.currentUser?.username ?: "User",
                            userAvatar = uiState.currentUser?.profileImage,
                            mentionSearchResults = uiState.mentionSearchResults,
                            isSearchingMentions = uiState.isSearchingMentions,
                            onCreateTextPost = { content, visibility, mentions ->
                                viewModel.createTextPost(content, visibility, mentions) { selectedTab = 0 }
                            },
                            onCreateImagePost = { content, visibility, images, mentions ->
                                viewModel.createImagePost(content, visibility, images, mentions) { selectedTab = 0 }
                            },
                            onCreateVideoPost = { content, visibility, videoBytes, videoFilename, mentions ->
                                viewModel.createVideoPost(content, visibility, videoBytes, videoFilename, mentions) { selectedTab = 0 }
                            },
                            onCreateLinkPost = { linkUrl, content, visibility, mentions ->
                                viewModel.createLinkPost(linkUrl, content, visibility, mentions) { selectedTab = 0 }
                            },
                            onCreatePollPost = { pollOptions, pollDurationHours, content, visibility, showResultsBeforeVote, mentions ->
                                viewModel.createPollPost(pollOptions, pollDurationHours, content, visibility, showResultsBeforeVote, mentions) { selectedTab = 0 }
                            },
                            onCreateArticlePost = { articleTitle, content, visibility, coverImage, articleTags, mentions ->
                                viewModel.createArticlePost(articleTitle, content, visibility, coverImage, articleTags, mentions) { selectedTab = 0 }
                            },
                            onCreateCelebrationPost = { celebrationType, content, visibility, mentions, celebrationGif ->
                                viewModel.createCelebrationPost(
                                    celebrationType,
                                    content,
                                    visibility,
                                    mentions,
                                    celebrationGif
                                ) { selectedTab = 0 }
                            },
                            onSearchMentions = { query -> viewModel.searchMentions(query) },
                            onClearMentionSearch = { viewModel.clearMentionSearch() },
                            onClearError = { viewModel.clearError() },
                            onPostCreated = { selectedTab = 0 }
                        )
                        3 -> MoreScreen(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            retentionState = retentionState,
                            currentUser = uiState.currentUser,
                            connectionRequests = rewardsState.pendingConnectionRequests,
                            isLoadingConnectionRequests = rewardsState.isLoadingPendingConnectionRequests,
                            connectionRequestsError = rewardsState.pendingConnectionRequestsError,
                            showFullMoreScreen = showFullMoreScreen,
                            quickHubAnimationKey = moreHubAnimationKey,
                            onOpenFullMoreScreen = { showFullMoreScreen = true },
                            onNavigateToProfile = { selectedTab = 4 },
                            onNavigateToConnectionRequests = { showConnectionRequestsScreen = true },
                            onNavigateToProfileCustomizations = {
                                if (uiState.currentUser?.canAccessProfileCustomization == true) {
                                    showProfileCustomizationsScreen = true
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Profile customizations are available for premium users.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onNavigateToGroups = { showGroupsScreen = true },
                            onNavigateToCircles = { showCirclesScreen = true },
                            onNavigateToReels = { 
                                reelsViewModel.loadAndOpenReels()
                            },
                            onNavigateToWeeklyGoals = { showWeeklyGoalsScreen = true },
                            onNavigateToStreakDetails = { showStreakDetailsScreen = true },
                            onNavigateToTopNetworkers = { showTopNetworkersScreen = true },
                            onNavigateToOnboarding = { showOnboardingScreen = true },
                            onNavigateToSavedPosts = { showSavedPostsScreen = true },
                            onNavigateToGrowthHub = { showGrowthHubScreen = true },
                            onOpenAgent = openAgentPanel,
                            onNavigateToNotificationSettings = { showNotificationSettingsScreen = true },
                            onNavigateToPrivacySettings = { showPrivacySettingsScreen = true },
                            onNavigateToAppearanceSettings = { showAppearanceSettingsScreen = true },
                            onNavigateToHelp = { showHelpScreen = true },
                            onNavigateToInviteFriends = { showInviteFriendsScreen = true },
                            onNavigateToAbout = { showAboutScreen = true },
                            onNavigateToContact = { showContactScreen = true },
                            onLogout = { showLogoutDialog = true }
                        )
                        4 -> {
                            // Use the new comprehensive ProfileScreen with its own ViewModel
                            ProfileScreen(
                                userId = null, // null means current user's profile
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                profileViewModel = ownProfileViewModel,
                                onNavigateBack = { selectedTab = 0 },
                                onEditProfile = { showOnboardingScreen = true },
                                onOpenProfile = { nextUserId -> viewingProfileUserId = nextUserId },
                                onOpenFeedItem = { item ->
                                    when (item.entityType?.lowercase()) {
                                        "reel" -> {
                                            selectedTab = 0
                                            reelsViewModel.loadReelById(item.id)
                                        }
                                        "post" -> {
                                            selectedTab = 0
                                            selectedPostForComments = item.id
                                            viewModel.loadComments(item.id)
                                            showCommentsSheet = true
                                        }
                                    }
                                }
                            )
                        }
                    }

                    selectedPostForComments?.let { selectedPostId ->
                        if (showCommentsSheet) {
                            com.kyant.backdrop.catalog.linkedin.posts.CommentsBottomSheet(
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                isLightTheme = isLightTheme,
                                postId = selectedPostId,
                                comments = uiState.comments,
                                isLoading = uiState.isLoadingComments,
                                isLoadingMore = uiState.isLoadingMoreComments,
                                isSendingComment = uiState.isSubmittingComment,
                                hasMoreComments = uiState.hasMoreComments,
                                currentUserAvatar = uiState.currentUser?.profileImage,
                                currentUserName = uiState.currentUser?.name ?: "You",
                                mentionSearchResults = uiState.mentionSearchResults,
                                isSearchingMentions = uiState.isSearchingMentions,
                                error = uiState.commentsError,
                                onDismiss = {
                                    showCommentsSheet = false
                                    selectedPostForComments = null
                                    viewModel.clearComments()
                                },
                                onLoadMore = { viewModel.loadMoreComments() },
                                onSendComment = { content, parentId ->
                                    selectedPostForComments?.let { postId ->
                                        viewModel.submitComment(postId, content, parentId)
                                    }
                                },
                                onLikeComment = { commentId ->
                                    viewModel.toggleCommentLike(commentId)
                                },
                                onDeleteComment = { commentId ->
                                    viewModel.deleteComment(commentId)
                                },
                                onSearchMentions = { query ->
                                    viewModel.searchMentions(query)
                                },
                                onClearMentionSearch = {
                                    viewModel.clearMentionSearch()
                                },
                                onClearError = {
                                    viewModel.clearCommentsError()
                                },
                                onProfileClick = { userId ->
                                    showCommentsSheet = false
                                    selectedPostForComments = null
                                    viewModel.clearComments()
                                    viewingProfileUserId = userId
                                }
                            )
                        }
                    }
                }
            }

            // Bottom navigation - floating over content, hidden when in chat thread
            AnimatedVisibility(
                visible = !isInChatThread,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab },
                    onTabSelected = {
                        selectedTab = it
                        if (it == 3) {
                            showFullMoreScreen = false
                        }
                    },
                    backdrop = backdrop,
                    tabsCount = 5,
                    modifier = Modifier
                        .padding(horizontal = 36.dp)
                        .navigationBarsPadding()
                        .fillMaxWidth()
                ) {
                LiquidBottomTab(onClick = { selectedTab = 0 }) {
                    FooterHomeIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    BasicText("Home", style = TextStyle(contentColor, 10.sp))
                }
                LiquidBottomTab(onClick = { 
                    viewModel.clearError() // Clear feed errors when switching tabs
                    selectedTab = 1 
                }) {
                    // Find tab - only show badge when streak is at risk
                    Box {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FooterFindIcon(
                                color = contentColor,
                                size = 22.dp
                            )
                            BasicText("Find", style = TextStyle(contentColor, 10.sp))
                        }
                        
                        // Only show badge when streak is AT RISK (not always)
                        if (uiState.isStreakAtRisk && uiState.connectionStreak > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6B6B)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "!",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                LiquidBottomTab(onClick = { selectedTab = 2 }) {
                    FooterCreateIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    BasicText("Post", style = TextStyle(contentColor, 10.sp))
                }
                LiquidBottomTab(onClick = {
                    moreHubAnimationKey += 1
                    showFullMoreScreen = false
                    selectedTab = 3
                }) {
                    FooterMoreIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    BasicText("More", style = TextStyle(contentColor, 10.sp))
                }
                LiquidBottomTab(onClick = { selectedTab = 4 }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FooterProfileIcon(
                            color = contentColor,
                            size = 22.dp
                        )
                        BasicText("Profile", style = TextStyle(contentColor, 10.sp))
                    }
                }
            } // End LiquidBottomTabs
            } // End AnimatedVisibility

            val openInlineProfile: (String) -> Unit = { userId ->
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
                showGrowthHubScreen = false
                showNotificationsInbox = false
                showMessagesScreen = false
                isInChatThread = false
                openChatWithUserId = null
                deepLinkConversationId = null
                showGroupsScreen = false
                selectedGroupId = null
                showGroupChat = false
                selectedTab = 4
                viewingProfileUserId = userId
            }
            val openInlineChat: (String) -> Unit = { userId ->
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
                showGrowthHubScreen = false
                showNotificationsInbox = false
                showGroupsScreen = false
                selectedGroupId = null
                showGroupChat = false
                viewingProfileUserId = null
                showMessagesScreen = true
                isInChatThread = false
                deepLinkConversationId = null
                openChatWithUserId = userId
            }
            val openInlineFind: () -> Unit = {
                agentViewModel.dismissInlineResults()
                minimizeAgentSheetForVoice = false
                showAgentSheet = false
                showGrowthHubScreen = false
                showNotificationsInbox = false
                showMessagesScreen = false
                isInChatThread = false
                openChatWithUserId = null
                deepLinkConversationId = null
                showGroupsScreen = false
                selectedGroupId = null
                showGroupChat = false
                viewingProfileUserId = null
                selectedTab = 1
            }

            if (shouldShowInlineResultsOverlay && canUseAgent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(
                            bottom = when {
                                isAgentVoiceLive -> 154.dp
                                isInChatThread -> 110.dp
                                else -> 154.dp
                            }
                        )
                ) {
                    agentState.activeInlineResults?.let { inlinePanel ->
                        AgentInlineResultsSurface(
                            panel = inlinePanel,
                            dismissedIds = agentState.dismissedInlineResultIds,
                            actionInProgress = agentState.inlineResultActionInProgress,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxWidth(),
                            onViewProfile = {
                                agentViewModel.dismissInlineResults()
                                openInlineProfile(it)
                            },
                            onMessage = {
                                agentViewModel.dismissInlineResults()
                                openInlineChat(it)
                            },
                            onConnect = agentViewModel::sendInlineConnectionRequest,
                            onCloseItem = agentViewModel::dismissInlineResultItem,
                            onClosePanel = agentViewModel::dismissInlineResults,
                            onSeeMore = openInlineFind
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.isLoggedIn && canUseAgent,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier.padding(end = 20.dp, bottom = if (isInChatThread) 20.dp else 84.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                minimizeAgentSheetForVoice = false
                                showAgentSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        RoboAgentLottie(
                            modifier = Modifier
                                .size(58.dp)
                                .graphicsLayer {
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                }
                        )
                    }

                    if (agentState.pendingApprovals.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5A5F))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            BasicText(
                                text = agentState.pendingApprovals.size.toString(),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            if (showAgentSheet && canUseAgent) {
                ModalBottomSheet(
                    onDismissRequest = {
                        minimizeAgentSheetForVoice = false
                        showAgentSheet = false
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = if (isDarkTheme) Color(0xFF131313) else Color(0xFFF9F7F2),
                    scrimColor = Color.Black.copy(alpha = agentSheetScrimAlpha)
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = agentSheetAlpha
                            scaleX = agentSheetScale
                            scaleY = agentSheetScale
                        }
                    ) {
                        AgentSheetContent(
                            viewModel = agentViewModel,
                            surface = agentSurface,
                            surfaceContext = agentSurfaceContext,
                            userDisplayName = uiState.currentUser?.name ?: uiState.currentUser?.username,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            backdrop = backdrop,
                            reduceAnimations = reduceAnimations,
                            isDarkTheme = isDarkTheme,
                            onOpenInlineProfile = openInlineProfile,
                            onOpenInlineChat = openInlineChat,
                            onSeeMoreInlineResults = openInlineFind,
                            onDismiss = {
                                minimizeAgentSheetForVoice = false
                                showAgentSheet = false
                            }
                        )
                    }
                }
            }
            
            // Reels Full-Screen Viewer Dialog (shown from any tab)
            if (reelsState.isViewerOpen) {
                val reelsToShow = if (reelsState.feedReels.isNotEmpty()) 
                    reelsState.feedReels 
                else 
                    reelsState.previewReels
                    
                if (reelsToShow.isNotEmpty()) {
                    ReelsFeedScreen(
                        reels = reelsToShow,
                        initialIndex = reelsState.currentReelIndex,
                        onDismiss = { reelsViewModel.closeReelsViewer() },
                        onLike = { reelId -> reelsViewModel.toggleLike(reelId) },
                        onSave = { reelId -> reelsViewModel.toggleSave(reelId) },
                        onComment = { reelId -> reelsViewModel.openComments(reelId) },
                        onShare = {},
                        onProfileClick = { userId ->
                            reelsViewModel.closeReelsViewer()
                            viewingProfileUserId = userId
                        },
                        onTrackView = { reelId, watchTime, completed ->
                            reelsViewModel.trackView(reelId, watchTime, completed)
                        },
                        onLoadMore = { reelsViewModel.loadMoreReels() }
                    )

                    if (reelsState.showCommentsSheet) {
                        ReelCommentsSheet(
                            backdrop = backdrop,
                            isGlassTheme = isGlassTheme,
                            isDarkTheme = isDarkTheme,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            comments = reelsState.reelComments,
                            repliesByParent = reelsState.replyCommentsByParent,
                            expandedParents = reelsState.expandedReplyParents,
                            replyTarget = reelsState.replyToComment,
                            isLoading = reelsState.isLoadingComments,
                            isLoadingMore = reelsState.isLoadingMoreComments,
                            hasMore = reelsState.hasMoreComments,
                            isSubmitting = reelsState.isSubmittingComment,
                            error = reelsState.commentsError,
                            onDismiss = { reelsViewModel.closeComments() },
                            onLoadMore = { reelsViewModel.loadReelComments(refresh = false) },
                            onToggleReplies = { parentId -> reelsViewModel.loadReplies(parentId) },
                            onReplyTo = { comment -> reelsViewModel.setReplyTarget(comment) },
                            onSubmitComment = { content -> reelsViewModel.submitComment(content) }
                        )
                    }
                } else {
                    // Loading or empty state dialog
                    Dialog(
                        onDismissRequest = { reelsViewModel.closeReelsViewer() },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            // Close button
                            Box(
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable { reelsViewModel.closeReelsViewer() }
                                    .align(Alignment.TopStart),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "✕",
                                    style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                                )
                            }
                            
                            if (reelsState.isLoadingFeed) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = Color.White)
                                    BasicText(
                                        "Loading Reels...",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                }
                            } else if (reelsState.feedError != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    BasicText(
                                        "🎬",
                                        style = TextStyle(fontSize = 48.sp)
                                    )
                                    BasicText(
                                        reelsState.feedError ?: "No reels available",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .clickable { reelsViewModel.loadAndOpenReels() }
                                            .padding(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        BasicText(
                                            "Try Again",
                                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    BasicText(
                                        "🎬",
                                        style = TextStyle(fontSize = 48.sp)
                                    )
                                    BasicText(
                                        "No reels yet",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Swipeable Reward Cards Overlay (shown when app opens)
            if (
                rewardCardsState.shouldShowOverlay &&
                rewardCardsState.cards.isNotEmpty() &&
                rewardCardsState.sessionId != null
            ) {
                SwipeableRewardCardsOverlay(
                    sessionId = rewardCardsState.sessionId!!,
                    cards = rewardCardsState.cards,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    currentTheme = themeMode,
                    onCardShown = {
                        rewardCardsViewModel.trackShownCardsOnce()
                    },
                    onSkip = { card ->
                        rewardCardsViewModel.trackSkipped(card)
                    },
                    onOpenProfile = { card ->
                        rewardCardsViewModel.openProfile(card)
                        viewingProfileUserId = card.id
                    },
                    onConnect = { card ->
                        rewardCardsViewModel.connect(card)
                    },
                    onDismissAll = {
                        rewardCardsViewModel.dismissAll()
                    }
                )
            }
            
            // Messages Screen Overlay
            AnimatedVisibility(
                visible = showMessagesScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                // Same shell as Groups / Appearance: glass uses GlassBackgroundLayer (wallpaper + motion),
                // light/dark use solid surfaces — avoids feed bleed-through and keeps chat aligned with theme.
                val messagesContentColor = contentColor
                val messagesAccentColor = accentColor
                
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // Header with back button - hidden when in chat thread
                        if (!isInChatThread) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable { showMessagesScreen = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        "←",
                                        style = TextStyle(
                                            color = messagesContentColor,
                                            fontSize = 22.sp
                                        )
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                BasicText(
                                    "Messages",
                                    style = TextStyle(
                                        color = messagesContentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                        
                        // Chat content
                        ChatTabContent(
                            backdrop = backdrop,
                            contentColor = messagesContentColor,
                            accentColor = messagesAccentColor,
                            viewModel = chatViewModel,
                            isGlassTheme = isGlassTheme,
                            openConversationId = deepLinkConversationId,
                            openChatWithUserId = openChatWithUserId,
                            onConsumedOpenConversation = { deepLinkConversationId = null },
                            onConsumedOpenChat = { openChatWithUserId = null },
                            onInChatThread = { inThread -> isInChatThread = inThread },
                            onNavigateToProfile = { userId ->
                                viewingProfileUserId = userId
                            }
                        )
                    }
                }
            }
            
            // Profile page when viewing another user's profile (above Messages so it opens on top of chat)
            AnimatedVisibility(
                visible = viewingProfileUserId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                viewingProfileUserId?.let { userId ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(
                                when {
                                    isGlassTheme -> Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(0f.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(24f.dp.toPx())
                                            lens(12f.dp.toPx(), 24f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.08f))
                                        }
                                    )
                                    isDarkTheme -> Modifier.background(Color(0xFF121212))
                                    else -> Modifier.background(Color(0xFFF5F5F5)) // Light theme
                                }
                            )
                            .statusBarsPadding()
                    ) {
                        ProfileScreen(
                            userId = userId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onNavigateBack = { viewingProfileUserId = null },
                            onOpenProfile = { nextUserId -> viewingProfileUserId = nextUserId },
                            onMessage = { otherUserId ->
                                viewingProfileUserId = null
                                openChatWithUserId = otherUserId
                                showMessagesScreen = true
                            },
                            onOpenFeedItem = { item ->
                                when (item.entityType?.lowercase()) {
                                    "reel" -> {
                                        viewingProfileUserId = null
                                        selectedTab = 0
                                        reelsViewModel.loadReelById(item.id)
                                    }
                                    "post" -> {
                                        viewingProfileUserId = null
                                        selectedTab = 0
                                        selectedPostForComments = item.id
                                        viewModel.loadComments(item.id)
                                        showCommentsSheet = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Groups Screen Overlay
            AnimatedVisibility(
                visible = showGroupsScreen && selectedGroupId == null && !showGroupChat,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    GroupsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showGroupsScreen = false },
                        onNavigateToGroupDetail = { groupId -> selectedGroupId = groupId },
                        onNavigateToGroupChat = { groupId -> 
                            selectedGroupId = groupId
                            // Could show group chat here
                        }
                    )
                }
            }
            
            // Group Detail Screen Overlay
            AnimatedVisibility(
                visible = selectedGroupId != null && !showGroupChat,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedGroupId?.let { groupId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        GroupDetailScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onNavigateBack = { selectedGroupId = null },
                            onNavigateToChat = { showGroupChat = true },
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId }
                        )
                    }
                }
            }
            
            // Group Chat Screen Overlay
            AnimatedVisibility(
                visible = showGroupChat && selectedGroupId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedGroupId?.let { groupId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        GroupChatScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            currentUserId = uiState.currentUser?.id,
                            onNavigateBack = { showGroupChat = false },
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId }
                        )
                    }
                }
            }
            
            // Circles Screen Overlay
            AnimatedVisibility(
                visible = showCirclesScreen && selectedCircleId == null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    CirclesScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showCirclesScreen = false },
                        onNavigateToCircle = { circleId -> selectedCircleId = circleId },
                        onNavigateToUpgrade = { /* TODO: Navigate to upgrade */ }
                    )
                }
            }
            
            // Circle Detail Screen Overlay
            AnimatedVisibility(
                visible = selectedCircleId != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                selectedCircleId?.let { circleId ->
                    SectionOverlayContainer(
                        backdrop = backdrop,
                        themeMode = themeMode,
                        glassBackgroundKey = glassBackgroundKey,
                        accentColor = accentColor,
                        glassMotionStyleKey = glassMotionStyleKey,
                        reduceAnimations = reduceAnimations
                    ) {
                        CircleDetailScreen(
                            circleId = circleId,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            currentUserId = uiState.currentUser?.id,
                            onNavigateBack = { selectedCircleId = null },
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId },
                            onInviteMember = { /* TODO: Show invite modal */ }
                        )
                    }
                }
            }
            
            // ==================== RETENTION FEATURE SCREENS ====================
            
            // Weekly Goals Screen Overlay
            AnimatedVisibility(
                visible = showWeeklyGoalsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    WeeklyGoalsDetailScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showWeeklyGoalsScreen = false },
                        onNavigateToFindPeople = {
                            showWeeklyGoalsScreen = false
                            selectedTab = 1
                        }
                    )
                }
            }
            
            // Streak Details Screen Overlay
            AnimatedVisibility(
                visible = showStreakDetailsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    StreakDetailsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showStreakDetailsScreen = false }
                    )
                }
            }
            
            // Top Networkers Leaderboard Screen Overlay
            AnimatedVisibility(
                visible = showTopNetworkersScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    TopNetworkersScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showTopNetworkersScreen = false },
                        onNavigateToProfile = { userId -> 
                            showTopNetworkersScreen = false
                            viewingProfileUserId = userId 
                        }
                    )
                }
            }
            
            // Onboarding / Profile Preferences Screen Overlay
            AnimatedVisibility(
                visible = showOnboardingScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    com.kyant.backdrop.catalog.onboarding.ProfileSetupWizard(
                        onComplete = { showOnboardingScreen = false },
                        onSkip = { showOnboardingScreen = false }
                    )
                }
            }
            
            // Saved Posts Screen Overlay
            AnimatedVisibility(
                visible = showSavedPostsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    SavedPostsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showSavedPostsScreen = false },
                        onNavigateToPost = { postId ->
                            showSavedPostsScreen = false
                            selectedTab = 0
                            selectedPostForComments = postId
                            viewModel.loadComments(postId)
                            showCommentsSheet = true
                        },
                        onNavigateToReel = { reelId ->
                            showSavedPostsScreen = false
                            selectedTab = 0
                            reelsViewModel.loadReelById(reelId)
                        }
                    )
                }
            }

            // Connection Requests Screen Overlay
            AnimatedVisibility(
                visible = showConnectionRequestsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ConnectionRequestsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        requests = rewardsState.pendingConnectionRequests,
                        isLoading = rewardsState.isLoadingPendingConnectionRequests,
                        error = rewardsState.pendingConnectionRequestsError,
                        actionInProgress = rewardsState.connectionActionInProgress,
                        onNavigateBack = { showConnectionRequestsScreen = false },
                        onOpenProfile = { userId ->
                            showConnectionRequestsScreen = false
                            viewingProfileUserId = userId
                        },
                        onAccept = { userId, connectionId ->
                            findPeopleViewModel.acceptConnection(userId, connectionId)
                        },
                        onReject = { userId, connectionId ->
                            findPeopleViewModel.rejectConnection(userId, connectionId)
                        }
                    )
                }
            }

            // Profile Customizations Screen Overlay
            AnimatedVisibility(
                visible = showProfileCustomizationsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ProfileCustomizationsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        currentUser = uiState.currentUser,
                        onNavigateBack = { showProfileCustomizationsScreen = false }
                    )
                }
            }

            // Notifications Inbox Overlay
            AnimatedVisibility(
                visible = showNotificationsInbox,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    NotificationsInboxScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showNotificationsInbox = false },
                        onUnreadCountChanged = { notificationUnreadCount = it },
                        onOpenProfile = { userId ->
                            showNotificationsInbox = false
                            viewingProfileUserId = userId
                        },
                        onOpenPost = { postId ->
                            showNotificationsInbox = false
                            selectedTab = 0
                            selectedPostForComments = postId
                            viewModel.loadComments(postId)
                            showCommentsSheet = true
                        },
                        onOpenReel = { reelId ->
                            showNotificationsInbox = false
                            selectedTab = 0
                            reelsViewModel.loadReelById(reelId)
                        },
                        onOpenConversation = { conversationId ->
                            showNotificationsInbox = false
                            openChatWithUserId = null
                            deepLinkConversationId = conversationId
                            showMessagesScreen = true
                        },
                        onOpenNetwork = {
                            showNotificationsInbox = false
                            selectedTab = 1
                        },
                        onOpenGrowthHub = {
                            showNotificationsInbox = false
                            showGrowthHubScreen = true
                        }
                    )
                }
            }

            // Growth Hub Overlay
            AnimatedVisibility(
                visible = showGrowthHubScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    GrowthHubScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showGrowthHubScreen = false },
                        onOpenHookAction = { hook ->
                            showGrowthHubScreen = false
                            when {
                                hook.action.label.contains("create post", ignoreCase = true) -> {
                                    selectedTab = 2
                                }
                                hook.action.href.equals("/find-people", ignoreCase = true) -> {
                                    selectedTab = 1
                                }
                                hook.action.href.equals("/onboarding", ignoreCase = true) ||
                                    hook.action.href.equals("/profile/edit", ignoreCase = true) -> {
                                    showOnboardingScreen = true
                                }
                                else -> {
                                    selectedTab = 0
                                }
                            }
                        },
                        onOpenProfile = { userId ->
                            showGrowthHubScreen = false
                            viewingProfileUserId = userId
                        }
                    )
                }
            }

            // Notification Settings Screen Overlay
            AnimatedVisibility(
                visible = showNotificationSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    NotificationSettingsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showNotificationSettingsScreen = false }
                    )
                }
            }
            
            // Privacy Settings Screen Overlay
            AnimatedVisibility(
                visible = showPrivacySettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    PrivacySettingsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showPrivacySettingsScreen = false }
                    )
                }
            }
            
            // Appearance Settings Screen Overlay
            AnimatedVisibility(
                visible = showAppearanceSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    AppearanceSettingsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showAppearanceSettingsScreen = false }
                    )
                }
            }
            
            // Help Screen Overlay
            AnimatedVisibility(
                visible = showHelpScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    HelpScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showHelpScreen = false }
                    )
                }
            }
            
            // About Screen Overlay
            AnimatedVisibility(
                visible = showAboutScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    AboutScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showAboutScreen = false }
                    )
                }
            }
            
            // Invite Friends Screen Overlay
            AnimatedVisibility(
                visible = showInviteFriendsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    InviteFriendsScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showInviteFriendsScreen = false }
                    )
                }
            }
            
            // Contact Screen Overlay
            AnimatedVisibility(
                visible = showContactScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SectionOverlayContainer(
                    backdrop = backdrop,
                    themeMode = themeMode,
                    glassBackgroundKey = glassBackgroundKey,
                    accentColor = accentColor,
                    glassMotionStyleKey = glassMotionStyleKey,
                    reduceAnimations = reduceAnimations
                ) {
                    ContactScreen(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onNavigateBack = { showContactScreen = false }
                    )
                }
            }
            
            // Logout Confirmation Dialog
            if (showLogoutDialog) {
                LogoutConfirmationDialog(
                    contentColor = Color.White,
                    accentColor = accentColor,
                    onConfirm = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    onDismiss = { showLogoutDialog = false }
                )
            }
            
            // Session Summary Overlay (Peak-End Rule)
            SessionSummaryOverlay(
                isVisible = showSessionSummary,
                sessionData = retentionState.sessionSummary,
                backdrop = backdrop,
                contentColor = Color.White,
                accentColor = accentColor,
                onDismiss = { showSessionSummary = false }
            )
        }
    }
    }
}

@Composable
private fun LinkedInTopBar(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    userInitials: String = "U",
    loginStreak: Int = 0,
    isStreakAtRisk: Boolean = false,
    hasUnreadNotifications: Boolean = false,
    unreadNotificationCount: Int = 0,
    hasUnreadMessages: Boolean = false,
    onStreakClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: login streak (tappable → streak details)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onStreakClick)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StreakFireLottie(modifier = Modifier.size(22.dp))
                BasicText(
                    loginStreak.coerceAtLeast(0).toString(),
                    style = TextStyle(
                        color = if (isStreakAtRisk) Color(0xFFFF9500) else contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // Center: vormeX
        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "vormeX",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 32.sp,
                    fontFamily = PacificoFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            )
        }

        // Right: notification + message (vector icons)
        Box(
            Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clickable { onNotificationsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    NotificationBellIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    if (hasUnreadNotifications) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFFF3B30))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            BasicText(
                                if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString(),
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    Modifier
                        .size(40.dp)
                        .clickable { onMessagesClick() },
                    contentAlignment = Alignment.Center
                ) {
                    HeaderMessageIcon(
                        color = contentColor,
                        size = 22.dp
                    )
                    if (hasUnreadMessages) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(10.dp)
                                .background(Color(0xFFFF3B30), CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    glassBackgroundKey: String = DefaultGlassBackgroundPresetKey,
    posts: List<Post> = emptyList(),
    storyGroups: List<StoryGroup> = emptyList(),
    // Reels data
    reels: List<Reel> = emptyList(),
    isLoadingReels: Boolean = false,
    onReelClick: (Int) -> Unit = {},
    onSeeAllReelsClick: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
    currentUserInitials: String = "U",
    currentUserProfileImage: String? = null,
    currentUserName: String = "You",
    isLightTheme: Boolean = true,
    // Streak data (Duolingo Effect)
    connectionStreak: Int = 0,
    loginStreak: Int = 0,
    isStreakAtRisk: Boolean = false,
    showStreakReminder: Boolean = false,
    showLoginStreakBadge: Boolean = false,
    onDismissStreakReminder: () -> Unit = {},
    onDismissLoginStreakBadge: () -> Unit = {},
    onNavigateToFindPeople: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLike: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    onProfileClick: (String) -> Unit = {},
    onMenuAction: (String, String) -> Unit = { _, _ -> },
    // Story callbacks
    onStoryClick: (Int) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onMyStoryClick: () -> Unit = {},
    // Onboarding prompt
    showOnboarding: Boolean = false,
    onNavigateToOnboarding: () -> Unit = {},
    // Retention features
    retentionState: RetentionUiState? = null,
    onWeeklyGoalsClick: () -> Unit = {},
    onStreakDetailsClick: () -> Unit = {},
    onTopNetworkersClick: () -> Unit = {},
    reduceAnimations: Boolean = false,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    // Single shimmer animation shared by all skeleton rows (avoids 3× infinite transitions).
    val skeletonShimmer = shimmerBrush(isLightTheme)
    
    // Widget positions for distributing engagement widgets in feed
    // Random positions within ranges: ensures varied feed experience on each app open
    val widgetPositions = remember {
        val pos1 = (3..8).random()  // PeopleLikeYou early in feed
        val pos2 = (pos1 + 5..pos1 + 12).random()  // TodaysMatches mid-feed, spaced from first
        val pos3 = (pos2 + 6..pos2 + 15).random()  // WeeklyGoals later, spaced from second
        mapOf(
            pos1 to "people_like_you",
            pos2 to "todays_matches",
            pos3 to "weekly_goals"
        )
    }

    val feedRows = remember(posts, retentionState, widgetPositions) {
        buildHomeFeedRows(posts, retentionState, widgetPositions)
    }

    val shouldPrefetchNextPage by remember {
        derivedStateOf {
            if (!hasMore || isLoadingMore) return@derivedStateOf false
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            if (total <= 0) return@derivedStateOf false
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            val buffer = 5
            if (total <= buffer + 1) {
                lastVisible >= total - 1
            } else {
                lastVisible >= total - buffer - 1
            }
        }
    }
    LaunchedEffect(shouldPrefetchNextPage, hasMore, isLoadingMore) {
        if (shouldPrefetchNextPage && hasMore && !isLoadingMore) {
            onLoadMore()
        }
    }
    
    // System default fling matches native RecyclerView-style physics and avoids custom decay work during scroll.
    val listFlingBehavior = ScrollableDefaults.flingBehavior()

    // Pull-to-refresh with haptic feedback and minimal line indicator
    // Twitter/X style - thin animated gradient line at top
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            isRefreshing = true
                onRefresh()
                // Reset after a short delay (ViewModel will update the data)
                scope.launch {
                    delay(1500)
                    isRefreshing = false
                }
        },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            // Minimal line loader - Twitter/X style
            MinimalLineRefreshIndicator(
                isRefreshing = isRefreshing,
                pullProgress = pullToRefreshState.distanceFraction,
                accentColor = accentColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = listFlingBehavior
        ) {
            item { Spacer(Modifier.height(4.dp)) }
        
        // Streak reminder banner at top (urgency driver) - ONLY when at risk
        if (showStreakReminder && connectionStreak > 0) {
            item {
                StreakReminderCard(
                    connectionStreak = connectionStreak,
                    isAtRisk = isStreakAtRisk,
                    backdrop = backdrop,
                    onDismiss = onDismissStreakReminder,
                    onAction = {
                        onDismissStreakReminder() // Also dismiss reminder when navigating
                        onNavigateToFindPeople()
                    }
                )
            }
        }
        
        // Login streak celebration - controlled by ViewModel (milestones only, 24hr cooldown)
        if (showLoginStreakBadge && !isLoading && posts.isNotEmpty()) {
            item {
                DismissableLoginStreakBadge(
                    loginStreak = loginStreak,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    onDismiss = onDismissLoginStreakBadge
                )
            }
        }
        
        // Onboarding prompt banner - show if user hasn't completed onboarding
        if (showOnboarding) {
            item {
                OnboardingPromptBanner(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onGetStarted = onNavigateToOnboarding
                )
            }
        }
        
        // Stories section - always show
        item {
            StoriesRow(
                storyGroups = storyGroups,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                currentUserProfileImage = currentUserProfileImage,
                currentUserInitials = currentUserInitials,
                onStoryClick = onStoryClick,
                onAddStoryClick = onAddStoryClick,
                onMyStoryClick = onMyStoryClick
            )
        }
        
        // Reels Preview Section - Instagram-like horizontal scrollable reels
        if (reels.isNotEmpty() || isLoadingReels) {
            item {
                ReelsPreviewSection(
                    reels = reels,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isLoading = isLoadingReels,
                    onReelClick = onReelClick,
                    onSeeAllClick = onSeeAllReelsClick
                )
            }
        }
        
        // ==================== RETENTION FEATURES SECTION ====================
        
        // Stay Active Banner (like web's "Stay active – check your feed and connect with someone today")
        retentionState?.let { state ->
            item {
                StayActiveBanner(
                    liveActivity = state.liveActivity,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onViewFeed = { /* Already on feed */ },
                    onConnect = onNavigateToFindPeople
                )
            }
            
        }

        // Loading state - Skeleton loading
        if (isLoading && posts.isEmpty()) {
            items(count = 3, key = { "feed_skeleton_$it" }) {
                PostSkeletonCard(
                    shimmerBrush = skeletonShimmer,
                    isLightTheme = isLightTheme
                )
            }
        }
        
        // Error state
        error?.let { errorMsg ->
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(16f.dp) },
                            effects = { blur(8f.dp.toPx()) },
                            onDrawSurface = { drawRect(Color.Red.copy(alpha = 0.1f)) }
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(errorMsg, style = TextStyle(contentColor, 14.sp))
                        Spacer(Modifier.height(8.dp))
                        LiquidButton(
                            onClick = onRefresh,
                            backdrop = backdrop
                        ) {
                            BasicText("Retry", style = TextStyle(contentColor, 14.sp))
                        }
                    }
                }
            }
        }
        
        // Empty state
        if (!isLoading && posts.isEmpty() && error == null) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "No posts yet. Be the first to share!",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                }
            }
        }

        // Posts + widgets: flattened list with stable keys and contentType for recycling
        items(
            items = feedRows,
            key = { it.itemKey },
            contentType = { it.contentType }
        ) { row ->
            when (row) {
                is FeedListRow.PostItem -> {
                    ApiPostCard(
                        post = row.post,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        glassBackgroundKey = glassBackgroundKey,
                        onLike = onLike,
                        onComment = onComment,
                        onShare = onShare,
                        onVotePoll = onVotePoll,
                        onProfileClick = { onProfileClick(row.post.author.id) },
                        onMentionClick = { username -> onProfileClick(username) },
                        onMenuAction = onMenuAction,
                        reduceAnimations = reduceAnimations
                    )
                }
                FeedListRow.WidgetPeopleLikeYou,
                FeedListRow.WidgetPeopleLikeYouFallback -> {
                    retentionState?.let { state ->
                        PeopleLikeYouSection(
                            people = state.peopleLikeYou,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onPersonClick = { userId -> onProfileClick(userId) },
                            onSeeAll = onNavigateToFindPeople
                        )
                    }
                }
                FeedListRow.WidgetTodaysMatches,
                FeedListRow.WidgetTodaysMatchesFallback -> {
                    retentionState?.let { state ->
                        TodaysMatchesSection(
                            matches = state.todaysMatches,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onMatchClick = { userId -> onProfileClick(userId) },
                            onConnect = { userId ->
                                scope.launch {
                                    ApiClient.sendConnectionRequest(context, userId)
                                        .onSuccess {
                                            Toast.makeText(
                                                context,
                                                "Connection request sent",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        .onFailure { error ->
                                            Toast.makeText(
                                                context,
                                                error.message ?: "Could not send request",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            },
                            onSeeAll = onNavigateToFindPeople
                        )
                    }
                }
                FeedListRow.WidgetWeeklyGoals,
                FeedListRow.WidgetWeeklyGoalsFallback -> {
                    retentionState?.let { state ->
                        EngagementDashboardCard(
                            weeklyGoals = state.weeklyGoals,
                            streakData = state.streakData,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onWeeklyGoalsClick = onWeeklyGoalsClick,
                            onStreakDetailsClick = onStreakDetailsClick
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
    } // Close PullToRefreshBox
}

// Minimal Line Refresh Indicator - Twitter/X style
@Composable
private fun MinimalLineRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "line_loader")
    
    // Animated gradient position for the loading state
    val animatedOffset = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )
    
    // Only show when pulling or refreshing
    val showIndicator = pullProgress > 0f || isRefreshing
    
    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .statusBarsPadding()
        ) {
            if (isRefreshing) {
                // Animated gradient line during refresh
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor,
                                    accentColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                startX = animatedOffset.value * 500f,
                                endX = (animatedOffset.value + 1f) * 500f
                            )
                        )
                )
            } else {
                // Static progress line based on pull distance
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pullProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.5f),
                                    accentColor
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun StoriesRow(
    storyGroups: List<StoryGroup>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    currentUserProfileImage: String? = null,
    currentUserInitials: String = "U",
    onStoryClick: (Int) -> Unit = {},
    onAddStoryClick: () -> Unit = {},
    onMyStoryClick: () -> Unit = {}
) {
    // Find user's own story group
    val myStoryGroup = storyGroups.find { it.isOwnStory }
    val hasMyStory = myStoryGroup != null && myStoryGroup.stories.isNotEmpty()
    
    // Filter out user's own story from the list (will be shown separately)
    val otherStoryGroups = storyGroups.filter { !it.isOwnStory }
    
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Your Story button - always shown first
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                if (hasMyStory) {
                    // Find the index of my story in the original list
                    val myStoryIndex = storyGroups.indexOfFirst { it.isOwnStory }
                    if (myStoryIndex >= 0) {
                        onMyStoryClick()
                    }
                } else {
                    onAddStoryClick()
                }
            }
        ) {
            Box(
                Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                // Profile image with story ring (if has story)
                Box(
                    Modifier
                        .size(if (hasMyStory) 76.dp else 72.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(38f.dp) },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                            },
                            onDrawSurface = {
                                if (hasMyStory && myStoryGroup?.hasUnviewed == true) {
                                    drawRect(accentColor.copy(alpha = 0.4f))
                                } else if (hasMyStory) {
                                    drawRect(Color.Gray.copy(alpha = 0.3f))
                                } else {
                                    drawRect(Color.White.copy(alpha = 0.2f))
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner profile image
                    Box(
                        Modifier
                            .size(if (hasMyStory) 68.dp else 64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUserProfileImage.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentUserProfileImage)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Your story",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(if (hasMyStory) 64.dp else 60.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(if (hasMyStory) 64.dp else 60.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    currentUserInitials,
                                    style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
                
                // "+" badge in bottom-right corner
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "+",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            BasicText(
                "Your story",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 11.sp)
            )
        }
        
        // Other story groups
        otherStoryGroups.forEachIndexed { index, storyGroup ->
            // Find the original index in the full list for proper callback
            val originalIndex = storyGroups.indexOf(storyGroup)
            StoryItem(
                storyGroup = storyGroup,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = { onStoryClick(originalIndex) }
            )
        }
    }
}

@Composable
private fun StoryItem(
    storyGroup: StoryGroup,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit = {}
) {
    val userName = storyGroup.user.name ?: storyGroup.user.username ?: "User"
    val firstName = userName.split(" ").firstOrNull() ?: userName
    val displayName = if (firstName.length > 8) firstName.take(7) + "…" else firstName
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        // Story ring with glass effect
        Box(
            Modifier
                .size(76.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(38f.dp) },
                    effects = {
                        vibrancy()
                        blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        if (storyGroup.hasUnviewed) {
                            drawRect(accentColor.copy(alpha = 0.4f))
                        } else {
                            drawRect(Color.Gray.copy(alpha = 0.3f))
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner profile image
            Box(
                Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                val profileImage = storyGroup.user.profileImage
                if (!profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = "$userName's story",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val initials = userName.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "U" }
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            initials,
                            style = TextStyle(Color.White, 20.sp, FontWeight.Bold)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        BasicText(
            displayName,
            style = TextStyle(contentColor.copy(alpha = 0.8f), 11.sp),
            maxLines = 1
        )
    }
}

@Composable
private fun MockPostCard(
    post: com.kyant.backdrop.catalog.linkedin.Post,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                    lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Author info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        post.author.avatarInitials,
                        style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    BasicText(
                        post.author.name,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        post.author.headline,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        post.timeAgo,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }
            }

            // Post content
            BasicText(
                post.content,
                style = TextStyle(contentColor, 14.sp, lineHeight = 20.sp)
            )

            // Image placeholder if has image
            if (post.hasImage) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("📷 Image", style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp))
                }
            }

            // Engagement stats
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicText(
                    "👍 ${post.likes}",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                )
                BasicText(
                    "${post.comments} comments",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                )
            }

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(contentColor.copy(alpha = 0.1f))
            )

            // Action buttons - removed Repost
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton("👍", "Like", contentColor)
                ActionButton("💬", "Comment", contentColor)
                ActionButton("📤", "Share", contentColor)
            }
        }
    }
}

@Composable
private fun ActionButton(icon: String, label: String, contentColor: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(icon, style = TextStyle(fontSize = 16.sp))
        Spacer(Modifier.width(4.dp))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
    }
}

@Composable
private fun FindPeopleScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    retentionViewModel: RetentionViewModel? = null
) {
    val retentionState = retentionViewModel?.uiState?.collectAsState()?.value
    
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // Search Header
        Box(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .padding(16.dp)
        ) {
            Column {
                BasicText(
                    "Find People",
                    style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "Discover and connect with others",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
            }
        }
        
        // Connection limit indicator (Scarcity feature)
        retentionState?.connectionLimit?.let { limit ->
            ConnectionLimitIndicator(
                limitData = limit,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }

        // Suggested connections
        BasicText(
            "Suggested for you",
            Modifier.padding(start = 4.dp, top = 8.dp),
            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
        )

        MockData.users.filter { !it.isConnected }.forEach { user ->
            ConnectionCard(user, backdrop, contentColor, accentColor)
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ConnectionCard(
    user: User,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    user.avatarInitials,
                    style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    user.name,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                )
                BasicText(
                    user.headline,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    "${user.connections} connections",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                )
            }
            Spacer(Modifier.width(8.dp))
            LiquidButton(
                onClick = { },
                backdrop = backdrop,
                modifier = Modifier.height(36.dp),
                tint = accentColor
            ) {
                BasicText(
                    "Connect",
                    Modifier.padding(horizontal = 12.dp),
                    style = TextStyle(Color.White, 13.sp, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun PostScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    viewModel: FeedViewModel,
    isCreatingPost: Boolean,
    createError: String?,
    onPostCreated: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var postType by remember { mutableIntStateOf(0) }
    var imageBytes by remember { mutableStateOf<List<Pair<ByteArray, String>>>(emptyList()) }
    var videoBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var imagePreviewUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoPreviewUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val filename = it.lastPathSegment ?: "image.jpg"
                imageBytes = imageBytes + (bytes to filename)
                imagePreviewUris = imagePreviewUris + it
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                val filename = it.lastPathSegment ?: "video.mp4"
                videoBytes = bytes to filename
                videoPreviewUri = it
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp, bottom = 100.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BasicText(
                    "Create a post",
                    style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Text" to "TEXT", "Image" to "IMAGE", "Video" to "VIDEO").forEachIndexed { index, (label, _) ->
                        val selected = postType == index
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) accentColor.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable { postType = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (selected) accentColor else contentColor.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.08f))
                        .padding(16.dp),
                    textStyle = TextStyle(contentColor, 16.sp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (content.isEmpty()) {
                                BasicText(
                                    "What do you want to talk about?",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (imagePreviewUris.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imagePreviewUris.forEachIndexed { index, uri ->
                            Box(
                                Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(uri).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable {
                                            imageBytes = imageBytes.filterIndexed { i, _ -> i != index }
                                            imagePreviewUris = imagePreviewUris.filterIndexed { i, _ -> i != index }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("×", style = TextStyle(Color.White, 16.sp, FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                if (videoPreviewUri != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.1f))
                    ) {
                        BasicText(
                            "Video attached",
                            Modifier
                                .align(Alignment.Center)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = TextStyle(Color.White, 12.sp)
                        )
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable {
                                    videoBytes = null
                                    videoPreviewUri = null
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("×", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PostOption("📷", "Photo", contentColor) { imagePicker.launch("image/*") }
                    PostOption("🎥", "Video", contentColor) { videoPicker.launch("video/*") }
                }

                if (createError != null) {
                    BasicText(
                        createError,
                        style = TextStyle(Color(0xFFE53935), 14.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LiquidButton(
                    onClick = {
                        val type = when (postType) {
                            1 -> "IMAGE"
                            2 -> "VIDEO"
                            else -> "TEXT"
                        }
                        if (type == "IMAGE" && imageBytes.isEmpty()) return@LiquidButton
                        if (type == "VIDEO" && videoBytes == null) return@LiquidButton
                        if (type == "TEXT" && content.isBlank()) return@LiquidButton
                        viewModel.createPost(
                            type = type,
                            content = content.ifBlank { " " },
                            imageBytes = imageBytes,
                            videoBytes = videoBytes,
                            onSuccess = {
                                content = ""
                                imageBytes = emptyList()
                                imagePreviewUris = emptyList()
                                videoBytes = null
                                videoPreviewUri = null
                                onPostCreated()
                            }
                        )
                    },
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    tint = accentColor
                ) {
                    if (isCreatingPost) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            "Post",
                            style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostOption(
    icon: String,
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        BasicText(icon, style = TextStyle(fontSize = 24.sp))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
    }
}

@Composable
private fun MoreScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    retentionState: RetentionUiState,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    connectionRequests: List<PendingConnectionRequest> = emptyList(),
    isLoadingConnectionRequests: Boolean = false,
    connectionRequestsError: String? = null,
    showFullMoreScreen: Boolean = false,
    quickHubAnimationKey: Int = 0,
    onOpenFullMoreScreen: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToConnectionRequests: () -> Unit = {},
    onNavigateToProfileCustomizations: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToCircles: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToWeeklyGoals: () -> Unit = {},
    onNavigateToStreakDetails: () -> Unit = {},
    onNavigateToTopNetworkers: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSavedPosts: () -> Unit = {},
    onNavigateToGrowthHub: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToPrivacySettings: () -> Unit = {},
    onNavigateToAppearanceSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var showPremiumDetailsScreen by rememberSaveable { mutableStateOf(false) }
    val pendingRequestsCount = connectionRequests.size
    val canUseAgent = currentUser?.canUseAgent == true
    val pendingRequestsBadge = when {
        pendingRequestsCount <= 0 -> null
        pendingRequestsCount > 99 -> "99+"
        else -> pendingRequestsCount.toString()
    }
    val quickActions = buildList {
        add(
            MoreQuickAction(
                title = "Connection requests",
                label = "Requests",
                icon = Icons.Outlined.PersonAddAlt1,
                badgeLabel = pendingRequestsBadge,
                showIndicatorDot = pendingRequestsCount > 0,
                onClick = onNavigateToConnectionRequests
            )
        )
        add(
            MoreQuickAction(
                title = "Saved",
                label = "Saved",
                icon = Icons.Outlined.BookmarkBorder,
                onClick = onNavigateToSavedPosts
            )
        )
        add(
            MoreQuickAction(
                title = "Reels",
                label = "Reels",
                icon = Icons.Outlined.SmartDisplay,
                onClick = onNavigateToReels
            )
        )
        add(
            MoreQuickAction(
                title = "Groups",
                label = "Groups",
                icon = Icons.Outlined.Groups,
                onClick = onNavigateToGroups
            )
        )
        add(
            MoreQuickAction(
                title = "Growth hub",
                label = "Growth",
                icon = Icons.Outlined.School,
                onClick = onNavigateToGrowthHub
            )
        )
        if (canUseAgent) {
            add(
                MoreQuickAction(
                    title = "Agent",
                    label = "AI Agent",
                    icon = Icons.Outlined.AutoAwesome,
                    onClick = onOpenAgent
                )
            )
        }
        add(
            MoreQuickAction(
                title = "Notifications",
                label = "Alerts",
                icon = Icons.Outlined.NotificationsNone,
                onClick = onNavigateToNotificationSettings
            )
        )
    }

    BackHandler(enabled = showPremiumDetailsScreen) {
        showPremiumDetailsScreen = false
    }

    if (showPremiumDetailsScreen) {
        MorePremiumDetailsScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onNavigateBack = { showPremiumDetailsScreen = false },
            onOpenAgent = onOpenAgent
        )
    } else if (showFullMoreScreen) {
        MoreFullScreen(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            retentionState = retentionState,
            currentUser = currentUser,
            connectionRequests = connectionRequests,
            isLoadingConnectionRequests = isLoadingConnectionRequests,
            connectionRequestsError = connectionRequestsError,
            hiddenQuickActionTitles = quickActions.mapTo(linkedSetOf()) { it.title },
            onOpenPremiumDetails = { showPremiumDetailsScreen = true },
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToConnectionRequests = onNavigateToConnectionRequests,
            onNavigateToProfileCustomizations = onNavigateToProfileCustomizations,
            onNavigateToGroups = onNavigateToGroups,
            onNavigateToCircles = onNavigateToCircles,
            onNavigateToReels = onNavigateToReels,
            onNavigateToWeeklyGoals = onNavigateToWeeklyGoals,
            onNavigateToStreakDetails = onNavigateToStreakDetails,
            onNavigateToTopNetworkers = onNavigateToTopNetworkers,
            onNavigateToOnboarding = onNavigateToOnboarding,
            onNavigateToSavedPosts = onNavigateToSavedPosts,
            onNavigateToGrowthHub = onNavigateToGrowthHub,
            onOpenAgent = onOpenAgent,
            onNavigateToNotificationSettings = onNavigateToNotificationSettings,
            onNavigateToPrivacySettings = onNavigateToPrivacySettings,
            onNavigateToAppearanceSettings = onNavigateToAppearanceSettings,
            onNavigateToHelp = onNavigateToHelp,
            onNavigateToInviteFriends = onNavigateToInviteFriends,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToContact = onNavigateToContact,
            onLogout = onLogout
        )
    } else {
        MoreQuickAccessHub(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            quickActions = quickActions,
            animationKey = quickHubAnimationKey,
            onOpenFullMoreScreen = onOpenFullMoreScreen
        )
    }
}

@Composable
private fun MoreFullScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    retentionState: RetentionUiState,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    connectionRequests: List<PendingConnectionRequest> = emptyList(),
    isLoadingConnectionRequests: Boolean = false,
    connectionRequestsError: String? = null,
    hiddenQuickActionTitles: Set<String> = emptySet(),
    onOpenPremiumDetails: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToConnectionRequests: () -> Unit = {},
    onNavigateToProfileCustomizations: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToCircles: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToWeeklyGoals: () -> Unit = {},
    onNavigateToStreakDetails: () -> Unit = {},
    onNavigateToTopNetworkers: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSavedPosts: () -> Unit = {},
    onNavigateToGrowthHub: () -> Unit = {},
    onOpenAgent: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToPrivacySettings: () -> Unit = {},
    onNavigateToAppearanceSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val goalsData = retentionState.weeklyGoals
    val goalsProgressText = if (goalsData.goals.isNotEmpty()) {
        "${(goalsData.totalProgress * 100).toInt()}% complete"
    } else {
        "Start tracking"
    }
    val isDarkSurface = contentColor == Color.White
    val pageBackground = Color.Transparent
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.09f else 0.06f)
    } else {
        Color.White.copy(alpha = if (isGlassTheme) 0.22f else 0.68f)
    }
    val searchSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.08f)
    } else {
        Color.White.copy(alpha = if (isGlassTheme) 0.26f else 0.58f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.14f else 0.08f)
    } else {
        Color.Black.copy(alpha = if (isGlassTheme) 0.10f else 0.07f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val sectionHeaderColor = contentColor.copy(alpha = if (isDarkSurface) 0.72f else 0.48f)
    val destructiveColor = if (isDarkSurface) Color(0xFFFF7A7A) else Color(0xFFD33A3A)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val pendingRequestsCount = connectionRequests.size
    val canUseAgent = currentUser?.canUseAgent == true
    val canAccessProfileCustomization = currentUser?.canAccessProfileCustomization == true
    val connectionRequestsSubtitle = when {
        isLoadingConnectionRequests && pendingRequestsCount == 0 -> "Checking for incoming requests"
        pendingRequestsCount == 0 && connectionRequestsError != null -> "Open to retry loading requests"
        pendingRequestsCount == 0 -> "People who send requests will show up here"
        pendingRequestsCount == 1 -> "1 person is waiting for your reply"
        else -> "$pendingRequestsCount people are waiting for your reply"
    }

    val accountItems = mutableListOf<MoreSettingsItem>().apply {
        if (currentUser == null) {
            add(
                MoreSettingsItem(
                    title = "Profile",
                    subtitle = "View and edit your profile",
                    icon = Icons.Outlined.PersonOutline,
                    onClick = onNavigateToProfile
                )
            )
        }
        add(
            MoreSettingsItem(
                title = "Saved",
                subtitle = "Posts you've bookmarked",
                icon = Icons.Outlined.BookmarkBorder,
                onClick = onNavigateToSavedPosts
            )
        )
        add(
            MoreSettingsItem(
                title = "Profile preferences",
                subtitle = "Goals, interests and matching settings",
                icon = Icons.Outlined.Description,
                onClick = onNavigateToOnboarding
            )
        )
    }

    val sections = listOf(
        MoreSettingsSection(
            title = "Your account",
            items = accountItems
        ),
        MoreSettingsSection(
            title = "Profile customizations",
            items = if (canAccessProfileCustomization) {
                listOf(
                    MoreSettingsItem(
                        title = "Customize your profile",
                        subtitle = "Frames, avatar preview, Big Bad Wolfie and Morty Dance loaders",
                        icon = Icons.Outlined.Palette,
                        onClick = onNavigateToProfileCustomizations,
                        searchTerms = listOf(
                            "profile customization",
                            "custom",
                            "customizations",
                            "loader",
                            "loaders",
                            "visitor loader",
                            "visitor",
                            "frame",
                            "avatar",
                            "gift",
                            "wolf",
                            "wolfie",
                            "morty",
                            "dance"
                        )
                    )
                )
            } else {
                emptyList()
            }
        ),
        MoreSettingsSection(
            title = "Connections",
            items = listOf(
                MoreSettingsItem(
                    title = "Connection requests",
                    subtitle = connectionRequestsSubtitle,
                    icon = Icons.Outlined.PersonAddAlt1,
                    onClick = onNavigateToConnectionRequests,
                    trailingLabel = pendingRequestsCount.takeIf { it > 0 }?.toString(),
                    showIndicatorDot = pendingRequestsCount > 0
                )
            )
        ),
        MoreSettingsSection(
            title = "How you use Vormex",
            items = listOf(
                MoreSettingsItem(
                    title = "Weekly goals",
                    subtitle = goalsProgressText,
                    icon = Icons.Outlined.TrackChanges,
                    onClick = onNavigateToWeeklyGoals,
                    trailingLabel = if (goalsData.streakAtRisk) "At risk" else null,
                    showIndicatorDot = goalsData.streakAtRisk
                ),
                MoreSettingsItem(
                    title = "Streaks & activity",
                    subtitle = "Networking, login, posting, messaging",
                    icon = Icons.Outlined.Schedule,
                    onClick = onNavigateToStreakDetails
                ),
                MoreSettingsItem(
                    title = "Top networkers",
                    subtitle = "Weekly and monthly leaderboard",
                    icon = Icons.Outlined.EmojiEvents,
                    onClick = onNavigateToTopNetworkers
                ),
                MoreSettingsItem(
                    title = "Notifications",
                    subtitle = "Push, digest and alerts",
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onNavigateToNotificationSettings
                )
            )
        ),
        MoreSettingsSection(
            title = "For professionals",
            items = buildList {
                if (canUseAgent) {
                    add(
                        MoreSettingsItem(
                            title = "AI Agent",
                            subtitle = "Ask Vormex to help across the app",
                            icon = Icons.Outlined.AutoAwesome,
                            onClick = onOpenAgent
                        )
                    )
                }
                add(
                    MoreSettingsItem(
                        title = "Growth hub",
                        subtitle = "Jobs, learning, AI coach, rewards",
                        icon = Icons.Outlined.School,
                        onClick = onNavigateToGrowthHub
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Groups",
                        subtitle = "Connect with communities",
                        icon = Icons.Outlined.Groups,
                        onClick = onNavigateToGroups
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Circles",
                        subtitle = "Share with close friends",
                        icon = Icons.Default.FavoriteBorder,
                        onClick = onNavigateToCircles
                    )
                )
                add(
                    MoreSettingsItem(
                        title = "Reels",
                        subtitle = "Watch short videos",
                        icon = Icons.Outlined.SmartDisplay,
                        onClick = onNavigateToReels
                    )
                )
            }
        ),
        MoreSettingsSection(
            title = "Who can see your content",
            items = listOf(
                MoreSettingsItem(
                    title = "Privacy",
                    subtitle = "Profile visibility and messaging",
                    icon = Icons.Outlined.Lock,
                    onClick = onNavigateToPrivacySettings
                ),
                MoreSettingsItem(
                    title = "Appearance",
                    subtitle = "Theme, font and accessibility",
                    icon = Icons.Outlined.Palette,
                    onClick = onNavigateToAppearanceSettings
                )
            )
        ),
        MoreSettingsSection(
            title = "Support and legal",
            items = listOf(
                MoreSettingsItem(
                    title = "Help & FAQ",
                    subtitle = "Getting started and troubleshooting",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = onNavigateToHelp
                ),
                MoreSettingsItem(
                    title = "Invite friends",
                    subtitle = "Share Vormex with others",
                    icon = Icons.Outlined.CardGiftcard,
                    onClick = onNavigateToInviteFriends
                ),
                MoreSettingsItem(
                    title = "About",
                    subtitle = "Version, terms and privacy policy",
                    icon = Icons.Outlined.Info,
                    onClick = onNavigateToAbout
                ),
                MoreSettingsItem(
                    title = "Contact us",
                    subtitle = "Support and feedback",
                    icon = Icons.Outlined.AlternateEmail,
                    onClick = onNavigateToContact
                )
            )
        ),
        MoreSettingsSection(
            title = "Account actions",
            items = listOf(
                MoreSettingsItem(
                    title = "Log out",
                    subtitle = "Sign out of your account on this device",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = onLogout,
                    isDestructive = true
                )
            )
        )
    )

    val visibleSections = sections.mapNotNull { section ->
        val visibleItems = section.items.filterNot { it.title in hiddenQuickActionTitles }
        if (visibleItems.isNotEmpty()) {
            section.copy(items = visibleItems)
        } else {
            null
        }
    }

    val normalizedQuery = searchQuery.trim()
    val showPremiumSection = normalizedQuery.isBlank() ||
        normalizedQuery.contains("premium", ignoreCase = true) ||
        normalizedQuery.contains("pro", ignoreCase = true) ||
        normalizedQuery.contains("upgrade", ignoreCase = true) ||
        normalizedQuery.contains("subscription", ignoreCase = true) ||
        normalizedQuery.contains("member", ignoreCase = true) ||
        normalizedQuery.contains("gift", ignoreCase = true)
    val filteredSections = visibleSections.mapNotNull { section ->
        val filteredItems = if (normalizedQuery.isBlank()) {
            section.items
        } else {
            section.items.filter { item ->
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                    item.subtitle.contains(normalizedQuery, ignoreCase = true) ||
                    item.searchTerms.any { term ->
                        term.contains(normalizedQuery, ignoreCase = true)
                    }
            }
        }
        if (filteredItems.isNotEmpty()) {
            section.copy(items = filteredItems)
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                BasicText(
                    "Settings and activity",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            MoreSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                surfaceColor = searchSurfaceColor,
                contentColor = contentColor,
                placeholderColor = secondaryTextColor,
                cursorColor = accentColor
            )

            if (showPremiumSection) {
                MorePremiumSection(
                    backdrop = backdrop,
                    isGlassTheme = isGlassTheme,
                    sectionSurfaceColor = sectionSurfaceColor,
                    dividerColor = dividerColor,
                    accentColor = accentColor,
                    sectionHeaderColor = sectionHeaderColor,
                    onOpenPremiumDetails = onOpenPremiumDetails
                )
            }

            filteredSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoreSectionHeader(section.title, sectionHeaderColor)

                    if (section.title == "Your account" && currentUser != null && normalizedQuery.isBlank()) {
                        MoreCurrentUserCard(
                            user = currentUser,
                            backdrop = backdrop,
                            isGlassTheme = isGlassTheme,
                            surfaceColor = sectionSurfaceColor,
                            borderColor = dividerColor,
                            contentColor = contentColor,
                            secondaryTextColor = secondaryTextColor,
                            onClick = onNavigateToProfile
                        )
                    }

                    MoreSettingsSectionCard(
                        items = section.items,
                        backdrop = backdrop,
                        isGlassTheme = isGlassTheme,
                        surfaceColor = sectionSurfaceColor,
                        borderColor = dividerColor,
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        destructiveColor = destructiveColor
                    )
                }
            }

            if (filteredSections.isEmpty() && !showPremiumSection) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .then(
                            if (isGlassTheme) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(18.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(14f.dp.toPx())
                                        lens(6f.dp.toPx(), 12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(sectionSurfaceColor)
                                    }
                                )
                            } else {
                                Modifier.background(sectionSurfaceColor)
                            }
                        )
                        .border(1.dp, dividerColor, RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(
                            "No settings found",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        BasicText(
                            "Try a different search term.",
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private data class MoreQuickAction(
    val title: String,
    val label: String,
    val icon: ImageVector,
    val badgeLabel: String? = null,
    val showIndicatorDot: Boolean = false,
    val onClick: () -> Unit
)

private data class PremiumHighlight(
    val title: String,
    val detail: String,
    val icon: ImageVector
)

@Composable
private fun MorePremiumSection(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    accentColor: Color,
    sectionHeaderColor: Color,
    onOpenPremiumDetails: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoreSectionHeader("Premium", sectionHeaderColor)
        MorePremiumEntryCard(
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            accentColor = accentColor,
            onOpenPremiumDetails = onOpenPremiumDetails
        )
    }
}

@Composable
private fun MorePremiumDetailsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    onNavigateBack: () -> Unit,
    onOpenAgent: () -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    val pageBackground = Color.Transparent
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.09f else 0.06f)
    } else {
        Color.White.copy(alpha = if (isGlassTheme) 0.22f else 0.68f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.14f else 0.08f)
    } else {
        Color.Black.copy(alpha = if (isGlassTheme) 0.10f else 0.07f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val sectionHeaderColor = contentColor.copy(alpha = if (isDarkSurface) 0.72f else 0.48f)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val premiumRefreshSignal by PremiumCheckoutManager.refreshSignal.collectAsState()
    val celebrationSignal by PremiumCheckoutManager.celebrationSignal.collectAsState()
    var premiumState by remember { mutableStateOf<PremiumSubscriptionResponse?>(null) }
    var isLoadingPremiumState by remember { mutableStateOf(true) }
    var isLaunchingCheckout by remember { mutableStateOf(false) }
    var isCancellingPremium by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    fun launchPremiumCheckout() {
        val activity = context.findComponentActivity()
        if (activity == null) {
            Toast.makeText(
                context,
                "Premium checkout needs an activity context.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isLoadingPremiumState && premiumState?.checkoutEnabled == false) {
            Toast.makeText(
                context,
                "Premium checkout is not configured on the server yet.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        scope.launch {
            isLaunchingCheckout = true
            val checkoutResult = ApiClient.createPremiumCheckout(context)
            isLaunchingCheckout = false

            checkoutResult
                .onSuccess { checkoutSession ->
                    PremiumCheckoutManager.startCheckout(activity, checkoutSession)
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to start premium checkout.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun cancelPremiumAccess() {
        scope.launch {
            isCancellingPremium = true
            val cancelResult = ApiClient.cancelPremiumSubscription(context)
            isCancellingPremium = false

            cancelResult
                .onSuccess { response ->
                    premiumState = response.subscription ?: premiumState
                    PremiumCheckoutManager.notifyPremiumStateChanged()
                    Toast.makeText(
                        context,
                        response.message.ifBlank { "Premium cancelled. Buy again anytime." },
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Unable to cancel premium right now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    LaunchedEffect(premiumRefreshSignal) {
        isLoadingPremiumState = true
        ApiClient.getPremiumSubscription(context)
            .onSuccess { premiumState = it }
            .onFailure { premiumState = null }
        isLoadingPremiumState = false
    }

    LaunchedEffect(celebrationSignal) {
        if (celebrationSignal == 0L) return@LaunchedEffect
        showCelebration = true
        delay(2800)
        showCelebration = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onNavigateBack)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                BasicText(
                    "Back",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            MoreSectionHeader("Premium", sectionHeaderColor)
            MorePremiumPromoCard(
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                sectionSurfaceColor = sectionSurfaceColor,
                dividerColor = dividerColor,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                premiumState = premiumState,
                isLoadingPremiumState = isLoadingPremiumState,
                isLaunchingCheckout = isLaunchingCheckout,
                isCancellingPremium = isCancellingPremium,
                showCelebration = showCelebration,
                onStartCheckout = ::launchPremiumCheckout,
                onCancelPremium = ::cancelPremiumAccess,
                onOpenAgent = onOpenAgent
            )
        }
    }
}

@Composable
private fun MorePremiumEntryCard(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    accentColor: Color,
    onOpenPremiumDetails: () -> Unit
) {
    val context = LocalContext.current
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val ctaComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.go_premium))
    val ctaProgress by animateLottieCompositionAsState(
        composition = ctaComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceAnimations
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(22.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenPremiumDetails)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isGlassTheme) 0.22f else 0.14f),
                            Color(0xFFFFC857).copy(alpha = if (isGlassTheme) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(900f, 460f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(520f, 92f),
                        radius = 240f
                    )
                )
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (ctaComposition != null) {
                LottieAnimation(
                    composition = ctaComposition,
                    progress = { ctaProgress },
                    modifier = Modifier.size(156.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun MorePremiumPromoCard(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    premiumState: PremiumSubscriptionResponse?,
    isLoadingPremiumState: Boolean,
    isLaunchingCheckout: Boolean,
    isCancellingPremium: Boolean,
    showCelebration: Boolean,
    onStartCheckout: () -> Unit,
    onCancelPremium: () -> Unit,
    onOpenAgent: () -> Unit
) {
    val context = LocalContext.current
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val ctaComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.go_premium))
    val ctaProgress by animateLottieCompositionAsState(
        composition = ctaComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceAnimations
    )
    val confettiComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.confetti_animation_01)
    )
    val confettiProgress by animateLottieCompositionAsState(
        composition = confettiComposition,
        iterations = if (reduceAnimations) 1 else LottieConstants.IterateForever,
        isPlaying = showCelebration
    )
    val featureHighlights = listOf(
        PremiumHighlight(
            title = "AI Agent",
            detail = "Unlock the agent button and get premium AI help inside Vormex.",
            icon = Icons.Outlined.AutoAwesome
        ),
        PremiumHighlight(
            title = "Premium looks",
            detail = "Use premium themes, stronger post styling, and a custom profile visitor look.",
            icon = Icons.Outlined.Palette
        ),
        PremiumHighlight(
            title = "Featured surfaces",
            detail = "Open premium-only sections, featured cards, and standout profile surfaces.",
            icon = Icons.Outlined.Widgets
        ),
        PremiumHighlight(
            title = "Fast support",
            detail = premiumState?.supportLabel ?: "24/7 fast support when you need help.",
            icon = Icons.Outlined.SupportAgent
        )
    )
    val checkoutEnabled = premiumState?.checkoutEnabled != false
    val isPremiumActive = premiumState?.isPremium == true
    val customPriceApplied = premiumState?.customPriceApplied == true
    val ctaEnabled =
        !isLoadingPremiumState && !isLaunchingCheckout && !isCancellingPremium && checkoutEnabled && !isPremiumActive
    val badgeLabel = when {
        isPremiumActive -> "Premium active"
        customPriceApplied -> "Custom offer"
        else -> "31-day premium"
    }
    val secondaryBadgeLabel = when {
        premiumState?.canUseAgent == true -> "AI Agent included"
        premiumState?.canAccessProfileCustomization == true && !isPremiumActive -> "Customization ready"
        else -> null
    }
    val description = premiumState?.description?.takeIf { it.isNotBlank() }
        ?: "Unlock a sharper Vormex presence with AI Agent access, stronger styling, featured cards, themes, support, and premium-only upgrades."
    val priceLabel = premiumState?.displayAmount?.takeIf { it.isNotBlank() } ?: "Premium access"
    val durationLabel = premiumState?.premiumDurationDays?.takeIf { it > 0 }?.let { "$it days" } ?: "31 days"
    val renewalLabel = premiumState?.renewalModeLabel ?: "Manual renewal"
    val supportLabel = premiumState?.supportLabel ?: "24/7 fast support"
    val creditsUsedLabel = "${premiumState?.creditsUsed ?: 0}"
    val remainingDays = premiumState?.premiumDaysRemaining ?: 0
    val statusSummary = when {
        isLoadingPremiumState -> "Checking premium access..."
        isLaunchingCheckout -> "Preparing secure checkout..."
        isCancellingPremium -> "Cancelling premium access..."
        isPremiumActive && remainingDays > 0 ->
            "$remainingDays days left in your premium plan."
        isPremiumActive -> "Premium is active on this account."
        customPriceApplied -> "This account has a special premium price."
        !checkoutEnabled -> "Premium checkout is not configured yet."
        else -> "Upgrade once and keep premium access live for 31 days."
    }
    val statusDetail = when {
        isPremiumActive && premiumState.canUseAgent ->
            "AI Agent is unlocked. Open it any time from here or from the More menu."
        isPremiumActive ->
            "Your premium look, featured design, and customization access stay active during this cycle."
        premiumState?.canAccessProfileCustomization == true ->
            "Profile customization is already enabled for this account."
        else ->
            "Premium also gives you featured designs, premium themes, better post styling, and support."
    }
    val premiumFontFamily = FontFamily.SansSerif

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(22.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(22.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isGlassTheme) 0.22f else 0.14f),
                            Color(0xFFFFC857).copy(alpha = if (isGlassTheme) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(960f, 520f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp)
                .size(132.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        badgeLabel,
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = premiumFontFamily
                        )
                    )
                }
                secondaryBadgeLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = if (isGlassTheme) 0.14f else 0.36f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = premiumFontFamily
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicText(
                    premiumState?.title ?: "Vormex Premium",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = premiumFontFamily,
                        letterSpacing = (-0.3).sp
                    )
                )
                BasicText(
                    description,
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontFamily = premiumFontFamily
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.38f))
                    .border(1.dp, dividerColor.copy(alpha = 0.72f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BasicText(
                    statusSummary,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = premiumFontFamily
                    )
                )
                BasicText(
                    statusDetail,
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontFamily = premiumFontFamily
                    )
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MorePremiumFeaturePill(
                        label = renewalLabel,
                        accentColor = accentColor,
                        contentColor = contentColor,
                        fontFamily = premiumFontFamily
                    )
                    MorePremiumFeaturePill(
                        label = supportLabel,
                        accentColor = accentColor,
                        contentColor = contentColor,
                        fontFamily = premiumFontFamily
                    )
                    if (isPremiumActive && remainingDays > 0) {
                        MorePremiumFeaturePill(
                            label = "$remainingDays days remaining",
                            accentColor = accentColor,
                            contentColor = contentColor,
                            fontFamily = premiumFontFamily
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MorePremiumMetricCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPremiumActive) "Current access" else "Price",
                    value = priceLabel,
                    supporting = if (customPriceApplied) "Custom offer active" else premiumBillingLabel(
                        premiumState?.billingCycle,
                        premiumState?.premiumDurationDays ?: 31
                    ),
                    accentColor = accentColor,
                    dividerColor = dividerColor,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    isGlassTheme = isGlassTheme,
                    fontFamily = premiumFontFamily
                )
                MorePremiumMetricCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPremiumActive && remainingDays > 0) "Days left" else "Plan length",
                    value = if (isPremiumActive && remainingDays > 0) "$remainingDays days" else durationLabel,
                    supporting = if (isPremiumActive && remainingDays > 0) {
                        "Remaining in the current premium cycle."
                    } else {
                        "One purchase covers the full premium window."
                    },
                    accentColor = accentColor,
                    dividerColor = dividerColor,
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    isGlassTheme = isGlassTheme,
                    fontFamily = premiumFontFamily
                )
            }

            MorePremiumMetricCard(
                modifier = Modifier.fillMaxWidth(),
                label = if (isPremiumActive) "Credits used" else "AI Agent access",
                value = if (isPremiumActive) creditsUsedLabel else "Included",
                supporting = if (isPremiumActive) {
                    "Agent prompts used in the current premium window."
                } else {
                    "Premium unlocks the in-app AI Agent for this account."
                },
                accentColor = accentColor,
                dividerColor = dividerColor,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                isGlassTheme = isGlassTheme,
                fontFamily = premiumFontFamily
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BasicText(
                    "Included with premium",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = premiumFontFamily
                    )
                )
                featureHighlights.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            MorePremiumFeatureCard(
                                modifier = Modifier.weight(1f),
                                item = item,
                                accentColor = accentColor,
                                dividerColor = dividerColor,
                                contentColor = contentColor,
                                secondaryTextColor = secondaryTextColor,
                                isGlassTheme = isGlassTheme,
                                fontFamily = premiumFontFamily
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (isPremiumActive) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MorePremiumAnimatedButton(
                        label = if (premiumState.canUseAgent) "Open AI Agent" else "Premium active",
                        supporting = if (premiumState.canUseAgent) {
                            "Your premium account has agent access right now."
                        } else {
                            "Premium is active. Agent access depends on rollout controls."
                        },
                        accentColor = accentColor,
                        contentColor = contentColor,
                        enabled = premiumState.canUseAgent,
                        onClick = onOpenAgent,
                        composition = ctaComposition,
                        progress = ctaProgress,
                        isGlassTheme = isGlassTheme,
                        fontFamily = premiumFontFamily
                    )
                    if (premiumState.canCancel) {
                        MorePremiumSecondaryButton(
                            label = if (isCancellingPremium) "Cancelling..." else "Cancel premium now",
                            contentColor = contentColor,
                            dividerColor = dividerColor,
                            fontFamily = premiumFontFamily,
                            enabled = !isCancellingPremium,
                            onClick = onCancelPremium
                        )
                    }
                }
            } else {
                MorePremiumAnimationCta(
                    accentColor = accentColor,
                    enabled = ctaEnabled,
                    onClick = onStartCheckout,
                    composition = ctaComposition,
                    progress = ctaProgress
                )
            }
        }
    }

    if (showCelebration) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0E1527),
                                    Color(0xFF1A2238)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (confettiComposition != null) {
                            LottieAnimation(
                                composition = confettiComposition,
                                progress = { confettiProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        BasicText(
                            "Premium unlocked",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = premiumFontFamily
                            )
                        )
                        BasicText(
                            "Your AI Agent access and premium styling are now active for this account.",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.80f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = premiumFontFamily
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun premiumBillingLabel(billingCycle: String?, durationDays: Int = 31): String {
    return when (billingCycle?.lowercase()) {
        "monthly" -> "monthly access"
        "yearly" -> "yearly access"
        "weekly" -> "weekly access"
        else -> "$durationDays-day access"
    }
}

@Composable
private fun MorePremiumMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    supporting: String,
    accentColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    isGlassTheme: Boolean,
    fontFamily: FontFamily
) {
    Column(
        modifier = modifier
            .widthIn(min = 152.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (isGlassTheme) 0.14f else 0.48f))
            .border(1.dp, dividerColor.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily
            )
        )
        BasicText(
            value,
            style = TextStyle(
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily
            )
        )
        BasicText(
            supporting,
            style = TextStyle(
                color = secondaryTextColor.copy(alpha = 0.92f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumFeatureCard(
    modifier: Modifier = Modifier,
    item: PremiumHighlight,
    accentColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    isGlassTheme: Boolean,
    fontFamily: FontFamily
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (isGlassTheme) 0.11f else 0.34f))
            .border(1.dp, dividerColor.copy(alpha = 0.66f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        BasicText(
            item.title,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            )
        )
        BasicText(
            item.detail,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumAnimatedButton(
    label: String,
    supporting: String,
    accentColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    composition: com.airbnb.lottie.LottieComposition?,
    progress: Float,
    isGlassTheme: Boolean,
    fontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor,
                            Color(0xFFEAA22D)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.40f),
                            accentColor.copy(alpha = 0.28f)
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    label,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily
                    )
                )
                BasicText(
                    supporting,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = fontFamily
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (isGlassTheme) 0.16f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (composition != null) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = label,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MorePremiumAnimationCta(
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    composition: com.airbnb.lottie.LottieComposition?,
    progress: Float
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.46f }
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(136.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "Go Premium",
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun MorePremiumSecondaryButton(
    label: String,
    contentColor: Color,
    dividerColor: Color,
    fontFamily: FontFamily,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Transparent)
            .border(1.dp, dividerColor.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MorePremiumFeaturePill(
    label: String,
    accentColor: Color,
    contentColor: Color,
    fontFamily: FontFamily
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = fontFamily
            )
        )
    }
}

@Composable
private fun MoreQuickAccessHub(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    quickActions: List<MoreQuickAction>,
    animationKey: Int,
    onOpenFullMoreScreen: () -> Unit
) {
    val pinnedAgentAction = quickActions.firstOrNull { it.label == "AI Agent" || it.title == "AI Agent" || it.title == "Agent" }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.free_interactive_radial_menu)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    val scope = rememberCoroutineScope()
    var expanded by remember(animationKey) { mutableStateOf(false) }
    var isTransitioning by remember(animationKey) { mutableStateOf(false) }
    val isDarkSurface = contentColor == Color.White
    val actionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.08f)
    } else {
        Color.White.copy(alpha = if (isGlassTheme) 0.22f else 0.72f)
    }
    val actionBorderColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val radius = 122f
    val angleStep = 360f / quickActions.size.coerceAtLeast(1)
    val lottieScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.82f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "more_hub_lottie_scale"
    )
    val lottieAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.55f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "more_hub_lottie_alpha"
    )
    val centerScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.82f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "more_hub_center_scale"
    )
    val centerRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -135f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "more_hub_center_rotation"
    )

    LaunchedEffect(animationKey) {
        isTransitioning = false
        expanded = false
        delay(90)
        expanded = true
    }

    fun launchHubAction(openFullMore: Boolean, action: () -> Unit) {
        if (isTransitioning) return
        isTransitioning = true
        expanded = false
        scope.launch {
            delay(180)
            action()
            if (!openFullMore) {
                delay(240)
                expanded = true
                isTransitioning = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        pinnedAgentAction?.let { action ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        if (isGlassTheme) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(18.dp) },
                                effects = {
                                    vibrancy()
                                    blur(12f.dp.toPx())
                                    lens(6f.dp.toPx(), 10f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(actionSurfaceColor)
                                }
                            )
                        } else {
                            Modifier.background(actionSurfaceColor)
                        }
                    )
                    .border(1.dp, actionBorderColor, RoundedCornerShape(18.dp))
                    .clickable {
                        launchHubAction(openFullMore = false) {
                            action.onClick()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            "AI Agent",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        BasicText(
                            "Open the agent instantly",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.62f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(360.dp)
                .padding(top = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .size(282.dp)
                        .graphicsLayer {
                            alpha = lottieAlpha
                            scaleX = lottieScale
                            scaleY = lottieScale
                        },
                    contentScale = ContentScale.Fit,
                    clipToCompositionBounds = true
                )
            }

            quickActions.forEachIndexed { index, action ->
                val itemProgress by animateFloatAsState(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 420,
                        delayMillis = 70 + (index * 45),
                        easing = FastOutSlowInEasing
                    ),
                    label = "more_hub_item_$index"
                )
                val angleDegrees = -90f + (index * angleStep)
                val angleRadians = angleDegrees * (PI / 180.0)
                val targetX = (radius * cos(angleRadians)).toFloat()
                val targetY = (radius * sin(angleRadians)).toFloat()

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(
                            x = (targetX * itemProgress).dp,
                            y = (targetY * itemProgress).dp
                        )
                        .graphicsLayer {
                            alpha = itemProgress
                            val scale = 0.52f + (itemProgress * 0.48f)
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    MoreRadialShortcutBubble(
                        action = action,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isGlassTheme = isGlassTheme,
                        surfaceColor = actionSurfaceColor,
                        borderColor = actionBorderColor,
                        onClick = {
                            launchHubAction(openFullMore = false) {
                                action.onClick()
                            }
                        }
                    )
                }
            }

            MoreRadialCenterButton(
                modifier = Modifier.graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                    rotationZ = centerRotation
                },
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                onClick = {
                    launchHubAction(openFullMore = true) {
                        onOpenFullMoreScreen()
                    }
                }
            )
        }
    }
}

@Composable
private fun MoreRadialShortcutBubble(
    action: MoreQuickAction,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(width = 88.dp, height = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .then(
                        if (isGlassTheme) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(16f.dp.toPx())
                                    lens(8f.dp.toPx(), 14f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(surfaceColor)
                                }
                            )
                        } else {
                            Modifier.background(surfaceColor)
                        }
                    )
                    .border(1.dp, borderColor, CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            BasicText(
                action.label,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                ),
                maxLines = 2
            )
        }

        action.badgeLabel?.let { badge ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    badge,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (action.showIndicatorDot && action.badgeLabel == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun MoreRadialCenterButton(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    onClick: () -> Unit
) {
    val centerSurfaceColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.28f else 0.18f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = modifier
                .size(92.dp)
                .clip(CircleShape)
                .then(
                    if (isGlassTheme) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(18f.dp.toPx())
                                lens(10f.dp.toPx(), 18f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(centerSurfaceColor)
                            }
                        )
                    } else {
                        Modifier.background(centerSurfaceColor)
                    }
                )
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "More",
                tint = contentColor,
                modifier = Modifier.size(34.dp)
            )
        }
        BasicText(
            "More",
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private data class MoreSettingsItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val trailingLabel: String? = null,
    val showIndicatorDot: Boolean = false,
    val searchTerms: List<String> = emptyList(),
    val isDestructive: Boolean = false
)

private data class MoreSettingsSection(
    val title: String,
    val items: List<MoreSettingsItem>
)

@Composable
private fun MoreProfileCustomizationsSection(
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    sectionHeaderColor: Color,
    showSectionHeader: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val equipped by SettingsPreferences.equippedProfileLoaderGiftId(context).collectAsState(initial = null)
    val profileFrameEnabled by SettingsPreferences.profileFrameEnabled(context).collectAsState(initial = false)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val activeLoaderGiftId = equipped?.takeIf { ProfileLoaderGifts.rawResForGiftId(it) != null }
    val isWolfieEquipped = equipped == ProfileLoaderGifts.BIG_BAD_WOLFIE
    val isMortyEquipped = equipped == ProfileLoaderGifts.MORTY_DANCE

    fun syncLoaderGift(nextGiftId: String?) {
        scope.launch {
            val previousGiftId = equipped
            SettingsPreferences.setEquippedProfileLoaderGiftId(context, nextGiftId)
            ApiClient.getCurrentUserId(context)?.let { userId ->
                ProfileLoaderGiftMemory.put(userId, nextGiftId)
            }
            ApiClient.updateProfile(
                context,
                ProfileUpdateRequest(visitLoaderGiftId = nextGiftId),
                explicitNullFields = if (nextGiftId == null) setOf("visitLoaderGiftId") else emptySet()
            ).onFailure { e ->
                SettingsPreferences.setEquippedProfileLoaderGiftId(context, previousGiftId)
                ApiClient.getCurrentUserId(context)?.let { userId ->
                    ProfileLoaderGiftMemory.put(userId, previousGiftId)
                }
                Toast.makeText(
                    context,
                    e.message ?: "Couldn't sync gift",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showSectionHeader) {
            MoreSectionHeader("Profile customizations", sectionHeaderColor)
        }

        MoreProfileCustomizationPreviewCard(
            user = currentUser,
            activeLoaderGiftId = activeLoaderGiftId,
            profileFrameEnabled = profileFrameEnabled,
            reduceAnimations = reduceAnimations,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor
        )

        MoreProfileCustomizationCard(
            title = "Profile Frame",
            subtitle = if (reduceAnimations && profileFrameEnabled) {
                "Shows around your avatar on your profile. Equipped, but paused while Reduce Animations is on."
            } else {
                "Shows an animated frame around your avatar on your profile."
            },
            isEquipped = profileFrameEnabled,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            preview = {
                MoreProfileAvatarPreview(
                    user = currentUser,
                    showFrame = true,
                    reduceAnimations = reduceAnimations,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    modifier = Modifier.size(82.dp)
                )
            },
            onToggle = {
                scope.launch {
                    val nextEnabled = !profileFrameEnabled
                    SettingsPreferences.setProfileFrameEnabled(context, nextEnabled)
                    ApiClient.updateProfile(
                        context,
                        ProfileUpdateRequest(
                            profileRing = if (nextEnabled) PROFILE_FRAME_RING_ID else null
                        ),
                        explicitNullFields = if (nextEnabled) emptySet() else setOf("profileRing")
                    ).onFailure { e ->
                        SettingsPreferences.setProfileFrameEnabled(context, profileFrameEnabled)
                        Toast.makeText(
                            context,
                            e.message ?: "Couldn't sync profile frame",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        MoreProfileCustomizationCard(
            title = "Big Bad Wolfie",
            subtitle = "Visitors see this animation while your profile loads. Equip to use it.",
            isEquipped = isWolfieEquipped,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            preview = {
                MoreProfileLoaderAnimationPreview(
                    giftId = ProfileLoaderGifts.BIG_BAD_WOLFIE,
                    reduceAnimations = reduceAnimations,
                    accentColor = accentColor,
                    modifier = Modifier.size(74.dp)
                )
            },
            onToggle = {
                syncLoaderGift(
                    if (isWolfieEquipped) null else ProfileLoaderGifts.BIG_BAD_WOLFIE
                )
            }
        )

        MoreProfileCustomizationCard(
            title = "Morty Dance Loader",
            subtitle = "Visitors see this dance animation while your profile opens. Equip to use it.",
            isEquipped = isMortyEquipped,
            backdrop = backdrop,
            isGlassTheme = isGlassTheme,
            sectionSurfaceColor = sectionSurfaceColor,
            dividerColor = dividerColor,
            contentColor = contentColor,
            secondaryTextColor = secondaryTextColor,
            accentColor = accentColor,
            preview = {
                MoreProfileLoaderAnimationPreview(
                    giftId = ProfileLoaderGifts.MORTY_DANCE,
                    reduceAnimations = reduceAnimations,
                    accentColor = accentColor,
                    modifier = Modifier.size(74.dp)
                )
            },
            onToggle = {
                syncLoaderGift(
                    if (isMortyEquipped) null else ProfileLoaderGifts.MORTY_DANCE
                )
            }
        )
    }
}

@Composable
private fun MoreProfileCustomizationPreviewCard(
    user: com.kyant.backdrop.catalog.network.models.User?,
    activeLoaderGiftId: String?,
    profileFrameEnabled: Boolean,
    reduceAnimations: Boolean,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color
) {
    val activeLoaderLabel = ProfileLoaderGifts.resolvedVisitorLabel(activeLoaderGiftId)
    val summaryText = when {
        profileFrameEnabled && !activeLoaderGiftId.isNullOrBlank() && reduceAnimations ->
            "Your frame is active, and $activeLoaderLabel is selected for visitors. Reduce Animations pauses the motion preview."
        profileFrameEnabled && !activeLoaderGiftId.isNullOrBlank() ->
            "Your frame is active, and visitors will see $activeLoaderLabel while your profile opens."
        profileFrameEnabled ->
            "Your profile frame is active. Visitors still see $activeLoaderLabel while your profile opens."
        !activeLoaderGiftId.isNullOrBlank() && reduceAnimations ->
            "$activeLoaderLabel is selected for profile visits. Reduce Animations pauses the live motion."
        !activeLoaderGiftId.isNullOrBlank() ->
            "$activeLoaderLabel is ready for profile visits."
        else ->
            "$activeLoaderLabel is your default visitor loader until you equip something else."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(20.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "Live preview",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        summaryText,
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        "3 options",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MoreCustomizationPreviewTile(
                    title = "Profile look",
                    subtitle = if (profileFrameEnabled) "Frame on" else "Frame off",
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor
                ) {
                    MoreProfileAvatarPreview(
                        user = user,
                        showFrame = profileFrameEnabled,
                        reduceAnimations = reduceAnimations,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        modifier = Modifier.size(96.dp)
                    )
                }

                MoreCustomizationPreviewTile(
                    title = "Visitor loader",
                    subtitle = if (reduceAnimations && !activeLoaderGiftId.isNullOrBlank()) {
                        "$activeLoaderLabel selected"
                    } else {
                        activeLoaderLabel
                    },
                    contentColor = contentColor,
                    secondaryTextColor = secondaryTextColor,
                    borderColor = dividerColor
                ) {
                    MoreProfileLoaderAnimationPreview(
                        giftId = activeLoaderGiftId,
                        reduceAnimations = reduceAnimations,
                        accentColor = accentColor,
                        modifier = Modifier.size(82.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MoreCustomizationPreviewTile(
    title: String,
    subtitle: String,
    contentColor: Color,
    secondaryTextColor: Color,
    borderColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .border(1.dp, borderColor.copy(alpha = 0.78f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        BasicText(
            subtitle,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 11.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MoreProfileAvatarPreview(
    user: com.kyant.backdrop.catalog.network.models.User?,
    showFrame: Boolean,
    reduceAnimations: Boolean,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: user?.username ?: "You"
    val profileImage = user?.profileImage
    val initials = displayName
        .split(" ")
        .mapNotNull { token -> token.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "Y" }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            if (!profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                BasicText(
                    initials,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (showFrame) {
            ProfileFrameLottie(
                modifier = Modifier.fillMaxSize(),
                isPlaying = !reduceAnimations
            )
        }
    }
}

@Composable
private fun MoreProfileLoaderAnimationPreview(
    giftId: String?,
    reduceAnimations: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val rawRes = ProfileLoaderGifts.resolvedVisitorRawRes(giftId)

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = !reduceAnimations
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            clipToCompositionBounds = true
        )
        return
    }

    CircularProgressIndicator(
        modifier = modifier.size(38.dp),
        color = accentColor,
        strokeWidth = 3.dp
    )
}

@Composable
private fun MoreProfileCustomizationCard(
    title: String,
    subtitle: String,
    isEquipped: Boolean,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    sectionSurfaceColor: Color,
    dividerColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    preview: (@Composable BoxScope.() -> Unit)? = null,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(sectionSurfaceColor)
                        }
                    )
                } else {
                    Modifier.background(sectionSurfaceColor)
                }
            )
            .border(1.dp, dividerColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (preview != null) 0.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        subtitle,
                        style = TextStyle(secondaryTextColor, 12.sp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (preview != null) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(contentColor.copy(alpha = 0.04f))
                            .border(1.dp, dividerColor.copy(alpha = 0.78f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                        content = preview
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isEquipped) contentColor.copy(alpha = 0.12f) else accentColor
                        )
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (isEquipped) "Unequip" else "Equip",
                        style = TextStyle(
                            color = if (isEquipped) contentColor else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    contentColor: Color,
    placeholderColor: Color,
    cursorColor: Color
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(14.dp) },
                        effects = {
                            vibrancy()
                            blur(12f.dp.toPx())
                            lens(5f.dp.toPx(), 10f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(surfaceColor)
                        }
                    )
                } else {
                    Modifier.background(surfaceColor)
                }
            )
            .padding(horizontal = 14.dp),
        textStyle = TextStyle(
            color = contentColor,
            fontSize = 13.sp
        ),
        cursorBrush = SolidColor(cursorColor),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = placeholderColor,
                    modifier = Modifier.size(18.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BasicText(
                            "Search",
                            style = TextStyle(
                                color = placeholderColor,
                                fontSize = 13.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun MoreCurrentUserCard(
    user: com.kyant.backdrop.catalog.network.models.User,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(surfaceColor)
                        }
                    )
                } else {
                    Modifier.background(surfaceColor)
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.profileImage,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BasicText(
                        user.name?.firstOrNull()?.uppercase() ?: "U",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BasicText(
                    user.name ?: "User",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user.username.isNullOrEmpty()) {
                    BasicText(
                        "@${user.username}",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicText(
                    user.headline?.takeIf { it.isNotBlank() } ?: "Profile and account details",
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BasicText(
                "›",
                style = TextStyle(
                    color = secondaryTextColor,
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Composable
private fun MoreSettingsSectionCard(
    items: List<MoreSettingsItem>,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    destructiveColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(surfaceColor)
                        }
                    )
                } else {
                    Modifier.background(surfaceColor)
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        items.forEachIndexed { index, item ->
            MoreSettingsRow(
                item = item,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                destructiveColor = destructiveColor
            )

            if (index != items.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 58.dp)
                        .height(1.dp)
                        .background(borderColor)
                )
            }
        }
    }
}

@Composable
private fun MoreConnectionRequestsCard(
    requests: List<PendingConnectionRequest>,
    isLoading: Boolean,
    error: String?,
    actionInProgress: Set<String>,
    backdrop: LayerBackdrop,
    isGlassTheme: Boolean,
    surfaceColor: Color,
    borderColor: Color,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onOpenProfile: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(18.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(6f.dp.toPx(), 12f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(surfaceColor)
                        }
                    )
                } else {
                    Modifier.background(surfaceColor)
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        when {
            isLoading && requests.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            "Checking for new requests",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        BasicText(
                            "People who want to connect will show up here.",
                            style = TextStyle(
                                color = secondaryTextColor,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            error != null && requests.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "Could not load connection requests",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicText(
                        error,
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            requests.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "No connection requests right now",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicText(
                        "When someone sends you a request, you’ll see them here.",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            else -> {
                requests.forEachIndexed { index, request ->
                    MoreConnectionRequestRow(
                        request = request,
                        isActionInProgress = actionInProgress.contains(request.user.id),
                        contentColor = contentColor,
                        secondaryTextColor = secondaryTextColor,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        onOpenProfile = onOpenProfile,
                        onAccept = onAccept,
                        onReject = onReject
                    )

                    if (index != requests.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 74.dp)
                                .height(1.dp)
                                .background(borderColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreConnectionRequestRow(
    request: PendingConnectionRequest,
    isActionInProgress: Boolean,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    borderColor: Color,
    onOpenProfile: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    val user = request.user
    val subtitle = listOfNotNull(
        user.headline?.takeIf { it.isNotBlank() },
        user.college?.takeIf { it.isNotBlank() }
    ).joinToString(" • ").ifBlank { "Sent you a connection request" }
    val avatarLetter = (
        user.name?.firstOrNull()?.toString()
            ?: user.username?.firstOrNull()?.toString()
            ?: "U"
        ).uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (!user.profileImage.isNullOrEmpty()) {
                AsyncImage(
                    model = user.profileImage,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                BasicText(
                    avatarLetter,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.clickable { onOpenProfile(user.id) },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BasicText(
                    user.name?.takeIf { it.isNotBlank() } ?: user.username ?: "Vormex user",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!user.username.isNullOrBlank()) {
                    BasicText(
                        "@${user.username}",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isActionInProgress) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                    BasicText(
                        "Updating request",
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoreConnectionRequestActionButton(
                        label = "Ignore",
                        filled = false,
                        accentColor = accentColor,
                        contentColor = contentColor,
                        borderColor = borderColor,
                        onClick = { onReject(user.id, request.id) }
                    )
                    MoreConnectionRequestActionButton(
                        label = "Accept",
                        filled = true,
                        accentColor = accentColor,
                        contentColor = contentColor,
                        borderColor = borderColor,
                        onClick = { onAccept(user.id, request.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreConnectionRequestActionButton(
    label: String,
    filled: Boolean,
    accentColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) accentColor else accentColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = if (filled) accentColor else borderColor,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (filled) Color.White else contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun MoreSettingsRow(
    item: MoreSettingsItem,
    contentColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    destructiveColor: Color
) {
    val rowColor = if (item.isDestructive) destructiveColor else contentColor
    val rowSecondaryColor = if (item.isDestructive) {
        destructiveColor.copy(alpha = 0.7f)
    } else {
        secondaryTextColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = rowColor.copy(alpha = if (item.isDestructive) 1f else 0.9f),
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    item.title,
                    style = TextStyle(
                        color = rowColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                BasicText(
                    item.subtitle,
                    style = TextStyle(
                        color = rowSecondaryColor,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.trailingLabel?.let { label ->
                BasicText(
                    label,
                    style = TextStyle(
                        color = if (item.isDestructive) destructiveColor else secondaryTextColor,
                        fontSize = 11.sp
                    )
                )
            }

            if (item.showIndicatorDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            BasicText(
                "›",
                style = TextStyle(
                    color = rowSecondaryColor,
                    fontSize = 18.sp
                )
            )
        }
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    contentColor: Color
) {
    BasicText(
        title,
        style = TextStyle(
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.padding(start = 2.dp)
    )
}

@Composable
private fun ConnectionRequestsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    requests: List<PendingConnectionRequest>,
    isLoading: Boolean,
    error: String?,
    actionInProgress: Set<String>,
    onNavigateBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onAccept: (String, String) -> Unit,
    onReject: (String, String) -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    val summarySurfaceColor = if (isDarkSurface) {
        accentColor.copy(alpha = 0.10f)
    } else {
        accentColor.copy(alpha = 0.12f)
    }
    val summaryBorderColor = if (isDarkSurface) {
        accentColor.copy(alpha = 0.18f)
    } else {
        accentColor.copy(alpha = 0.12f)
    }
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.09f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val requestCountText = when (requests.size) {
        0 -> "No pending requests"
        1 -> "1 pending request"
        else -> "${requests.size} pending requests"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Connection requests",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(summarySurfaceColor)
                    .border(1.dp, summaryBorderColor, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        requestCountText,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "Review the people who sent you connection requests and respond from here.",
                        style = TextStyle(secondaryTextColor, 12.sp)
                    )
                }
            }

            MoreConnectionRequestsCard(
                requests = requests,
                isLoading = isLoading,
                error = error,
                actionInProgress = actionInProgress,
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                surfaceColor = sectionSurfaceColor,
                borderColor = dividerColor,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                onOpenProfile = onOpenProfile,
                onAccept = onAccept,
                onReject = onReject
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfileCustomizationsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    onNavigateBack: () -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    val sectionSurfaceColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.09f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }
    val dividerColor = if (isDarkSurface) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }
    val secondaryTextColor = contentColor.copy(alpha = if (isDarkSurface) 0.66f else 0.58f)
    val sectionHeaderColor = contentColor.copy(alpha = if (isDarkSurface) 0.72f else 0.48f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        SettingsHeader(
            title = "Profile customizations",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoreProfileCustomizationsSection(
                backdrop = backdrop,
                isGlassTheme = isGlassTheme,
                currentUser = currentUser,
                sectionSurfaceColor = sectionSurfaceColor,
                dividerColor = dividerColor,
                contentColor = contentColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                sectionHeaderColor = sectionHeaderColor,
                showSectionHeader = false
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MoreWorkspaceCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    goalsProgressText: String,
    connectionStreak: Int,
    liveNow: Int,
    remainingConnections: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (isGlassTheme) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(22.dp) },
                        effects = {
                            vibrancy()
                            blur(14f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(accentColor.copy(alpha = 0.14f))
                        }
                    )
                } else {
                    Modifier.background(contentColor.copy(alpha = 0.08f))
                }
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    "Your workspace",
                    style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                )
                BasicText(
                    "Everything you revisit often should feel instant and easy to scan.",
                    style = TextStyle(contentColor.copy(alpha = 0.65f), 12.sp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoreWorkspaceMetric(
                    modifier = Modifier.weight(1f),
                    label = "Goals",
                    value = goalsProgressText,
                    accentColor = accentColor,
                    contentColor = contentColor
                )
                MoreWorkspaceMetric(
                    modifier = Modifier.weight(1f),
                    label = "Streak",
                    value = "${connectionStreak.coerceAtLeast(0)} days",
                    accentColor = accentColor,
                    contentColor = contentColor
                )
                MoreWorkspaceMetric(
                    modifier = Modifier.weight(1f),
                    label = "Room",
                    value = "${remainingConnections.coerceAtLeast(0)} left",
                    accentColor = accentColor,
                    contentColor = contentColor
                )
            }

            if (liveNow > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = if (isGlassTheme) 0.12f else 0.06f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicText(
                        "$liveNow people are networking right now. Good moment to reply, connect, or post.",
                        style = TextStyle(contentColor.copy(alpha = 0.72f), 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreWorkspaceMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    contentColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                label,
                style = TextStyle(contentColor.copy(alpha = 0.58f), 11.sp, FontWeight.Medium)
            )
            BasicText(
                value,
                style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MoreQuickActionChip(
    label: String,
    icon: String,
    accentColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BasicText(icon, style = TextStyle(fontSize = 14.sp))
            BasicText(
                label,
                style = TextStyle(contentColor, 12.sp, FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun MoreMenuItemWithSubtitle(
    title: String,
    subtitle: String,
    iconResId: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    trailingIconResId: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isGlassTheme) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(10f.dp.toPx())
                        lens(4f.dp.toPx(), 8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.08f))
                    }
                ) else Modifier.background(contentColor.copy(alpha = 0.08f))
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    BasicText(
                        subtitle,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingIconResId?.let { resId ->
                    Image(
                        painter = painterResource(resId),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.7f))
                    )
                    Spacer(Modifier.width(8.dp))
                }
                BasicText(
                    "›",
                    style = TextStyle(contentColor.copy(alpha = 0.3f), 24.sp)
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    title: String,
    iconResId: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    isGlassTheme: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isGlassTheme) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(16.dp) },
                    effects = {
                        vibrancy()
                        blur(10f.dp.toPx())
                        lens(4f.dp.toPx(), 8f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.08f))
                    }
                ) else Modifier.background(contentColor.copy(alpha = 0.08f))
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
            Spacer(Modifier.width(16.dp))
            BasicText(
                title,
                style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
            )
        }
    }
}

/**
 * Onboarding Prompt Banner - Shows when user hasn't completed profile setup
 * Encourages users to complete their profile to unlock collaborations
 */
@Composable
private fun OnboardingPromptBanner(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(14f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    // Gradient background for attention
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.25f),
                                Color(0xFF9C27B0).copy(alpha = 0.2f)
                            )
                        )
                    )
                }
            )
            .clickable(onClick = onGetStarted)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon/Emoji section
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(accentColor, Color(0xFF9C27B0))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "✨",
                    style = TextStyle(fontSize = 28.sp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Text content
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    "Complete Your Profile",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(Modifier.height(4.dp))
                
                BasicText(
                    "Fill in your details to unlock collaborations and connect with like-minded people",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Get Started button
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(accentColor, Color(0xFF9C27B0))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    BasicText(
                        "Get Started →",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItemWithAction(
    title: String,
    icon: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(accentColor.copy(alpha = 0.15f))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(icon, style = TextStyle(fontSize = 24.sp))
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    BasicText(
                        when (title) {
                            "Groups" -> "Connect with communities"
                            "Circles" -> "Share with close friends"
                            "Reels" -> "Watch short videos"
                            "Edit Profile Preferences" -> "Update goals, interests & more"
                            else -> ""
                        },
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
            }
            
            // Arrow indicator
            BasicText(
                "→",
                style = TextStyle(contentColor.copy(alpha = 0.5f), 20.sp)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    val icon = when (notification.type) {
        NotificationType.LIKE -> "👍"
        NotificationType.COMMENT -> "💬"
        NotificationType.CONNECTION -> "👤"
        NotificationType.JOB -> "💼"
        NotificationType.MENTION -> "📢"
        NotificationType.VIEW -> "👁️"
    }

    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(icon, style = TextStyle(fontSize = 20.sp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText(
                    notification.title,
                    style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                )
                BasicText(
                    notification.description,
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            BasicText(
                notification.timeAgo,
                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
            )
        }
    }
}

// Renamed to avoid conflict with new ProfileScreen from ProfileScreen.kt
@Composable
private fun OldProfileScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    profile: FullProfileResponse? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Loading state
        if (isLoading && profile == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
            return@Column
        }
        
        // Error state
        error?.let { errorMsg ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(errorMsg, style = TextStyle(Color.Red, 14.sp))
                    Spacer(Modifier.height(8.dp))
                    LiquidButton(onClick = onRefresh, backdrop = backdrop) {
                        BasicText("Retry", style = TextStyle(contentColor, 14.sp))
                    }
                }
            }
            return@Column
        }
        
        val user = profile?.user ?: return@Column
        val stats = profile.stats
        
        // Banner Image
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (!user.bannerImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.bannerImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Default gradient banner
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1e3a5f),
                                    Color(0xFF2d5a87),
                                    Color(0xFF1e3a5f)
                                )
                            )
                        )
                )
            }
        }
        
        // Profile Card overlapping banner
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset(y = (-50).dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.15f))
                        }
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Profile Avatar and Name
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        // Avatar with ring
                        Box(
                            Modifier
                                .size(100.dp)
                                .offset(y = (-30).dp)
                        ) {
                            val hasRing = !user.profileRing.isNullOrEmpty()
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (hasRing) Modifier.background(
                                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                colors = listOf(
                                                    Color(0xFFdd8448),
                                                    Color(0xFFf59e0b),
                                                    Color(0xFFdd8448),
                                                    Color(0xFFb45309),
                                                    Color(0xFFdd8448)
                                                )
                                            )
                                        )
                                        else Modifier.background(Color.White)
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.avatar.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(user.avatar)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = user.name.split(" ")
                                            .mapNotNull { it.firstOrNull()?.uppercase() }
                                            .take(2)
                                            .joinToString("")
                                        BasicText(
                                            initials.ifEmpty { "?" },
                                            style = TextStyle(Color.White, 32.sp, FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                            
                            // Verified badge
                            if (user.verified) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1d9bf0))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("✓", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        // Name, headline, location
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText(
                                    user.name,
                                    style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
                                )
                                if (user.isOpenToOpportunities) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF10b981).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        BasicText(
                                            "Open to work",
                                            style = TextStyle(Color(0xFF10b981), 10.sp, FontWeight.Medium)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            
                            user.headline?.let { headline ->
                                BasicText(
                                    headline,
                                    style = TextStyle(contentColor.copy(alpha = 0.8f), 14.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            // Location + College
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                user.location?.let { loc ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText("📍 ", style = TextStyle(fontSize = 12.sp))
                                        BasicText(loc, style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                                    }
                                }
                                user.college?.let { college ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText("🎓 ", style = TextStyle(fontSize = 12.sp))
                                        BasicText(college, style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // XP Level Bar
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Level ${stats.level}",
                                style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                "${stats.xp} / ${stats.xp + stats.xpToNextLevel} XP",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Gray.copy(alpha = 0.2f))
                        ) {
                            val progress = stats.xp.toFloat() / (stats.xp + stats.xpToNextLevel)
                            Box(
                                Modifier
                                    .fillMaxWidth(progress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(accentColor, Color(0xFF60a5fa))
                                        )
                                    )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Stats Row with Public Streak Badge
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        ProfileStat("Connections", stats.connectionsCount, contentColor)
                        
                        // Prominent Public Streak Badge (Duolingo Effect)
                        PublicStreakBadge(
                            currentStreak = stats.currentStreak,
                            longestStreak = stats.longestStreak,
                            contentColor = contentColor
                        )
                        
                        ProfileStat("Followers", stats.followersCount, contentColor)
                    }
                }
            }
        }
        
        // Content sections
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .offset(y = (-40).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // About section
            user.bio?.let { bio ->
                ProfileSection(
                    title = "About",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    BasicText(
                        bio,
                        style = TextStyle(contentColor.copy(alpha = 0.9f), 14.sp, lineHeight = 20.sp)
                    )
                }
            }
            
            // Skills section
            if (profile.skills.isNotEmpty()) {
                ProfileSection(
                    title = "Skills (${profile.skills.size})",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.skills.forEach { skill ->
                            SkillChip(skill.skill.name, skill.proficiency, contentColor)
                        }
                    }
                }
            }
            
            // Experience section
            if (profile.experiences.isNotEmpty()) {
                ProfileSection(
                    title = "Experience",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        profile.experiences.forEach { exp ->
                            ExperienceItem(exp, contentColor)
                        }
                    }
                }
            }
            
            // Education section
            if (profile.education.isNotEmpty()) {
                ProfileSection(
                    title = "Education",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        profile.education.forEach { edu ->
                            EducationItem(edu, contentColor)
                        }
                    }
                }
            }
            
            // Projects section
            if (profile.projects.isNotEmpty()) {
                ProfileSection(
                    title = "Projects (${profile.projects.size})",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.projects.take(3).forEach { project ->
                            ProjectItem(project, contentColor, accentColor)
                        }
                    }
                }
            }
            
            // Achievements section
            if (profile.achievements.isNotEmpty()) {
                ProfileSection(
                    title = "Achievements",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.achievements.forEach { achievement ->
                            AchievementItem(achievement, contentColor)
                        }
                    }
                }
            }
            
            // Certificates section  
            if (profile.certificates.isNotEmpty()) {
                ProfileSection(
                    title = "Certificates",
                    backdrop = backdrop,
                    contentColor = contentColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.certificates.forEach { cert ->
                            CertificateItem(cert, contentColor)
                        }
                    }
                }
            }
            
            // Logout button
            LiquidButton(
                onClick = onLogout,
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                tint = Color(0xFFe53935)
            ) {
                BasicText(
                    "Logout",
                    style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                )
            }
            
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: Int, contentColor: Color, emoji: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            emoji?.let { 
                BasicText(it, style = TextStyle(fontSize = 14.sp))
                Spacer(Modifier.width(4.dp))
            }
            BasicText(
                formatNumber(value),
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
            )
        }
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .padding(16.dp)
    ) {
        Column {
            BasicText(
                title,
                style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SkillChip(name: String, proficiency: String?, contentColor: Color) {
    val bgColor = when (proficiency) {
        "Expert" -> Color(0xFF10b981).copy(alpha = 0.15f)
        "Advanced" -> Color(0xFF3b82f6).copy(alpha = 0.15f)
        "Intermediate" -> Color(0xFFf59e0b).copy(alpha = 0.15f)
        else -> Color.Gray.copy(alpha = 0.15f)
    }
    val textColor = when (proficiency) {
        "Expert" -> Color(0xFF10b981)
        "Advanced" -> Color(0xFF3b82f6)
        "Intermediate" -> Color(0xFFf59e0b)
        else -> contentColor.copy(alpha = 0.8f)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BasicText(name, style = TextStyle(textColor, 13.sp, FontWeight.Medium))
    }
}

@Composable
private fun ExperienceItem(exp: com.kyant.backdrop.catalog.network.models.Experience, contentColor: Color) {
    Row {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText("💼", style = TextStyle(fontSize = 24.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(exp.title, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
            BasicText(exp.company, style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp))
            Row {
                exp.type?.let {
                    BasicText(it, style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    BasicText(" • ", style = TextStyle(contentColor.copy(alpha = 0.4f), 12.sp))
                }
                BasicText(
                    if (exp.isCurrent) "Present" else exp.endDate?.take(7) ?: "",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                )
            }
        }
    }
}

@Composable
private fun EducationItem(edu: com.kyant.backdrop.catalog.network.models.Education, contentColor: Color) {
    Row {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText("🎓", style = TextStyle(fontSize = 24.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(edu.school, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
            BasicText(
                "${edu.degree}${edu.fieldOfStudy?.let { " in $it" } ?: ""}",
                style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp)
            )
            edu.grade?.let {
                BasicText("Grade: $it", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
            }
        }
    }
}

@Composable
private fun ProjectItem(project: com.kyant.backdrop.catalog.network.models.Project, contentColor: Color, accentColor: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (project.featured) {
                BasicText("⭐ ", style = TextStyle(fontSize = 14.sp))
            }
            BasicText(project.name, style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold))
        }
        project.description?.let { desc ->
            Spacer(Modifier.height(4.dp))
            BasicText(
                desc,
                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (project.techStack.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                project.techStack.take(4).forEach { tech ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(tech, style = TextStyle(accentColor, 11.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(achievement: com.kyant.backdrop.catalog.network.models.Achievement, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val emoji = when (achievement.type) {
            "Hackathon" -> "🏆"
            "Competition" -> "🥇"
            "Award" -> "🏅"
            "Scholarship" -> "📚"
            else -> "✨"
        }
        BasicText(emoji, style = TextStyle(fontSize = 24.sp))
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(achievement.title, style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold))
            BasicText(
                "${achievement.organization} • ${achievement.date.take(4)}",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
            )
        }
    }
}

@Composable
private fun CertificateItem(cert: com.kyant.backdrop.catalog.network.models.Certificate, contentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText("📜", style = TextStyle(fontSize = 24.sp))
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(cert.name, style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold))
            BasicText(
                "${cert.issuingOrg} • ${cert.issueDate.take(7)}",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
            )
        }
    }
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1000000 -> "${(num / 1000000.0).let { if (it == it.toLong().toDouble()) it.toLong() else String.format("%.1f", it) }}M"
        num >= 1000 -> "${(num / 1000.0).let { if (it == it.toLong().toDouble()) it.toLong() else String.format("%.1f", it) }}K"
        else -> num.toString()
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                value,
                style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
            )
            BasicText(
                label,
                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
            )
        }
    }
}

@Composable
private fun JobCard(
    job: Job,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(14f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .clickable { }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("🏢", style = TextStyle(fontSize = 24.sp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    BasicText(
                        job.title,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        job.company,
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp)
                    )
                    BasicText(
                        job.location,
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    job.salary?.let {
                        BasicText(
                            it,
                            style = TextStyle(Color(0xFF00A86B), 13.sp, FontWeight.Medium)
                        )
                    }
                    BasicText(
                        "Posted ${job.postedAgo}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }

                if (job.isEasyApply) {
                    LiquidButton(
                        onClick = { },
                        backdrop = backdrop,
                        modifier = Modifier.height(36.dp),
                        tint = accentColor
                    ) {
                        BasicText(
                            "Easy Apply",
                            Modifier.padding(horizontal = 12.dp),
                            style = TextStyle(Color.White, 12.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onClearError: () -> Unit
) {
    LiquidGlassLoginScreen(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isLoading = isLoading,
        isGoogleLoading = isGoogleLoading,
        error = error,
        onLogin = onLogin,
        onGoogleSignIn = onGoogleSignIn,
        onForgotPassword = onForgotPassword,
        onSignUpClick = onSignUpClick,
        onClearError = onClearError
    )
}

@Composable
private fun SignUpScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    isGoogleLoading: Boolean,
    error: String?,
    onSignUp: (email: String, password: String, name: String, username: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    LiquidGlassSignUpScreen(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isLoading = isLoading,
        isGoogleLoading = isGoogleLoading,
        error = error,
        onSignUp = onSignUp,
        onGoogleSignIn = onGoogleSignIn,
        onLoginClick = onLoginClick,
        onClearError = onClearError
    )
}

@Composable
private fun ApiPostCard(
    post: Post,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    glassBackgroundKey: String = DefaultGlassBackgroundPresetKey,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onVotePoll: (String, String) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onMenuAction: (String, String) -> Unit = { _, _ -> },
    reduceAnimations: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showFullScreenVideo by remember { mutableStateOf(false) }
    var displayIsLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    var displayLikesCount by remember(post.id) { mutableIntStateOf(post.likesCount) }
    var isLikePending by remember(post.id) { mutableStateOf(false) }
    
    // Mention preview state
    var showMentionPreview by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }
    val context = LocalContext.current
    val relativeTimeLabel by rememberRelativeTimeLabel(post.createdAt)

    LaunchedEffect(post.id, post.isLiked, post.likesCount) {
        displayIsLiked = post.isLiked
        displayLikesCount = post.likesCount
        isLikePending = false
    }
    
    // Red color for active likes
    val likeActiveColor = Color(0xFFE53935)
    val useCrystalPureGlass = glassBackgroundKey == "crystal"
    val containerShape = RoundedCornerShape(0.dp)
    val innerSectionShape = RoundedCornerShape(0.dp)
    val subtleTextColor = contentColor.copy(alpha = 0.62f)
    val hasMedia = !post.videoUrl.isNullOrEmpty() || post.mediaUrls.isNotEmpty()
    val hasLinkPreview = !post.linkUrl.isNullOrEmpty()
    val showContentBlock = !post.content.isNullOrBlank()
    val cardBorderColor = if (useCrystalPureGlass) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.16f)
    }
    val mediaContainerColor = if (useCrystalPureGlass) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(containerShape)
            .then(
                if (reduceAnimations) {
                    if (useCrystalPureGlass) {
                        Modifier.background(Color.White.copy(alpha = 0.08f), containerShape)
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            shape = containerShape
                        )
                    }
                } else {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(0f.dp) },
                        effects = {
                            vibrancy()
                            if (useCrystalPureGlass) {
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            } else {
                                blur(18f.dp.toPx())
                                lens(8f.dp.toPx(), 16f.dp.toPx())
                            }
                        },
                        onDrawSurface = {
                            if (useCrystalPureGlass) {
                                drawRect(Color.White.copy(alpha = 0.06f))
                            } else {
                                drawRect(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.16f),
                                            Color.White.copy(alpha = 0.10f),
                                            Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                            }
                        }
                    )
                }
            )
            // Only left/right strokes — a full .border() also draws top+bottom, so when cards
            // stack flush the adjacent top+bottom borders read as a gray seam between posts.
            .drawWithContent {
                drawContent()
                val strokeWidth = 1.dp.toPx()
                val c = cardBorderColor
                drawLine(
                    color = c,
                    start = Offset(strokeWidth / 2f, 0f),
                    end = Offset(strokeWidth / 2f, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = c,
                    start = Offset(size.width - strokeWidth / 2f, 0f),
                    end = Offset(size.width - strokeWidth / 2f, size.height),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // Author info with menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, end = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile image or initials fallback
                val profileImageUrl = post.author.profileImage
                val authorName = post.author.name ?: post.author.username ?: "U"
                val initials = authorName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                    .ifEmpty { "U" }
                
                // Clickable author section (avatar + name)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onProfileClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.95f),
                                        accentColor.copy(alpha = 0.55f)
                                    )
                                )
                            )
                            .padding(1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profileImageUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(profileImageUrl)
                                        .crossfade(imageCrossfadeMs)
                                        .build(),
                                    contentDescription = "Profile picture of $authorName",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                BasicText(
                                    initials,
                                    style = TextStyle(Color.White, 13.sp, FontWeight.Bold)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BasicText(
                            post.author.name ?: post.author.username ?: "Unknown",
                            style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                        )
                        post.author.headline?.let { headline ->
                            BasicText(
                                headline,
                                style = TextStyle(subtleTextColor, 11.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ApiMetricChip(
                                label = relativeTimeLabel,
                                contentColor = subtleTextColor,
                                containerColor = Color.White.copy(alpha = 0.08f)
                            )
                            when (post.visibility) {
                                "CONNECTIONS" -> ApiMetricChip(
                                    label = "Connections",
                                    contentColor = accentColor,
                                    containerColor = accentColor.copy(alpha = 0.12f),
                                    borderColor = accentColor.copy(alpha = 0.2f)
                                )
                                "PRIVATE" -> ApiMetricChip(
                                    label = "Only me",
                                    contentColor = subtleTextColor,
                                    containerColor = Color.White.copy(alpha = 0.06f)
                                )
                            }
                        }
                    }
                }
                
                // Menu button (three dots) with SVG icon
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showMenu = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MenuDotsIcon(
                        color = contentColor,
                        size = 18.dp
                    )
                }
            }
            
            // Glass-themed dropdown menu
            if (showMenu) {
                GlassDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    backdrop = backdrop,
                    contentColor = contentColor,
                    useGlassBackdropEffects = false
                ) {
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "save")
                        },
                        contentColor = contentColor,
                        leadingIcon = { BookmarkIcon(contentColor, size = 18.dp) },
                        text = "Save"
                    )
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "copy_link")
                        },
                        contentColor = contentColor,
                        leadingIcon = { LinkIcon(contentColor, size = 18.dp) },
                        text = "Copy Link"
                    )
                    GlassMenuDivider(contentColor)
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "not_interested")
                        },
                        contentColor = contentColor,
                        leadingIcon = { BlockIcon(contentColor, size = 18.dp) },
                        text = "Not Interested"
                    )
                    GlassMenuItem(
                        onClick = {
                            showMenu = false
                            onMenuAction(post.id, "report")
                        },
                        contentColor = contentColor,
                        leadingIcon = { WarningIcon(Color.Red.copy(alpha = 0.8f), size = 18.dp) },
                        text = "Report",
                        textColor = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }

            val normalizedPostType = post.type.uppercase()
            if (normalizedPostType == "ARTICLE" || !post.articleTitle.isNullOrBlank()) {
                ApiArticleCard(
                    articleTitle = post.articleTitle.orEmpty(),
                    articleCoverImage = post.articleCoverImage,
                    articleTags = post.articleTags,
                    articleReadTime = post.articleReadTime,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    reduceAnimations = reduceAnimations
                )
            }

            val celebrationGifShown =
                normalizedPostType == "CELEBRATION" &&
                    (!post.celebrationGifUrl.isNullOrBlank() || post.mediaUrls.isNotEmpty())
            if (normalizedPostType == "CELEBRATION" || !post.celebrationType.isNullOrBlank()) {
                ApiCelebrationHero(
                    celebrationType = post.celebrationType,
                    celebrationGifUrl = post.celebrationGifUrl ?: post.mediaUrls.firstOrNull(),
                    celebrationBadge = post.celebrationBadge,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    backdrop = backdrop,
                    reduceAnimations = reduceAnimations
                )
            }

            // Post content with mention support
            if (showContentBlock) {
                Column(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormattedContent(
                        content = post.content.orEmpty(),
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onMentionClick = { username -> onMentionClick(username) },
                        onMentionLongPress = { username ->
                            mentionUsername = username
                            showMentionPreview = true
                        }
                    )
                }
            }

            // Media: Video or Image
            val isVideoPost = normalizedPostType == "VIDEO" || !post.videoUrl.isNullOrEmpty()
            
            if (isVideoPost && !post.videoUrl.isNullOrEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(innerSectionShape)
                        .background(mediaContainerColor)
                ) {
                    VideoPlayer(
                        videoUrl = post.videoUrl,
                        modifier = Modifier.fillMaxWidth(),
                        autoPlay = false,
                        showControls = true,
                        contentColor = contentColor,
                        onFullScreenClick = { showFullScreenVideo = true }
                    )
                }
            } else if (post.mediaUrls.isNotEmpty() && normalizedPostType != "ARTICLE" && !celebrationGifShown) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(innerSectionShape)
                        .background(mediaContainerColor)
                ) {
                    ApiImagePostGrid(
                        images = post.mediaUrls,
                        reduceAnimations = reduceAnimations,
                        onImageClick = { index ->
                            selectedImageIndex = index
                            showImageViewer = true
                        }
                    )
                }
            }

            if (!post.linkUrl.isNullOrEmpty()) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = if (hasMedia) 14.dp else 0.dp)) {
                    ApiLinkPreview(
                        url = post.linkUrl,
                        title = post.linkTitle,
                        description = post.linkDescription,
                        domain = post.linkDomain,
                        linkImage = post.linkImage,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        reduceAnimations = reduceAnimations,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(post.linkUrl))
                                )
                            }
                        }
                    )
                }
            } else if (hasMedia) {
                Spacer(Modifier.height(14.dp))
            }

            if ((normalizedPostType == "POLL" || post.pollOptions.isNotEmpty()) && post.pollOptions.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = if (!hasLinkPreview) 0.dp else 2.dp)) {
                    ApiPollContent(
                        options = post.pollOptions,
                        endsAt = post.pollEndsAt,
                        userVotedOptionId = post.userVotedOptionId,
                        showResultsBeforeVote = post.showResultsBeforeVote,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onVote = { optionId -> onVotePoll(post.id, optionId) }
                    )
                }
            }

            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayLikesCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ApiMetricChip(
                            icon = {
                                LikeIcon(
                                    color = if (displayIsLiked) likeActiveColor else contentColor.copy(alpha = 0.72f),
                                    size = 13.dp,
                                    filled = displayIsLiked
                                )
                            },
                            label = "${displayLikesCount} like${if (displayLikesCount == 1) "" else "s"}",
                            contentColor = contentColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ApiActionButton(
                        modifier = Modifier.weight(1f),
                        icon = {
                            LikeIcon(
                                color = if (displayIsLiked) likeActiveColor else contentColor.copy(alpha = 0.7f),
                                size = 18.dp,
                                filled = displayIsLiked
                            )
                        },
                        label = "Like",
                        onClick = {
                            if (!isLikePending) {
                                val nextLiked = !displayIsLiked
                                displayIsLiked = nextLiked
                                displayLikesCount = if (nextLiked) displayLikesCount + 1 else (displayLikesCount - 1).coerceAtLeast(0)
                                isLikePending = true
                                onLike(post.id)
                            }
                        }
                    )
                    ApiActionButton(
                        modifier = Modifier.weight(1f),
                        icon = { CommentIcon(contentColor.copy(alpha = 0.72f), size = 18.dp) },
                        label = "Comment",
                        onClick = { onComment(post.id) }
                    )
                    ApiActionButton(
                        modifier = Modifier.weight(1f),
                        icon = { ShareIcon(contentColor.copy(alpha = 0.72f), size = 18.dp) },
                        label = "Share",
                        onClick = { onShare(post.id) }
                    )
                }
            }
        }
    }
    
    // Full screen image viewer dialog
    if (showImageViewer && post.mediaUrls.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            com.kyant.backdrop.catalog.linkedin.posts.FullScreenImageViewer(
                images = post.mediaUrls,
                initialIndex = selectedImageIndex,
                onDismiss = { showImageViewer = false }
            )
        }
    }
    
    // Full screen video player dialog
    if (showFullScreenVideo && !post.videoUrl.isNullOrEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullScreenVideo = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            FullScreenVideoPlayer(
                videoUrl = post.videoUrl,
                onDismiss = { showFullScreenVideo = false }
            )
        }
    }
    
    // Mention profile preview popup (glass theme with animation)
    if (showMentionPreview && mentionUsername.isNotEmpty()) {
        MentionProfilePreviewPopup(
            username = mentionUsername,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onDismiss = { showMentionPreview = false },
            onViewProfile = { 
                showMentionPreview = false
                onMentionClick(mentionUsername)
            }
        )
    }
}

@Composable
private fun ApiActionButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun ApiMetricChip(
    icon: (@Composable () -> Unit)? = null,
    label: String,
    contentColor: Color,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.12f)
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.invoke()
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.78f), 11.sp, FontWeight.Medium)
        )
    }
}

private fun celebrationTypeLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return "Celebration"
    val match = CelebrationType.entries.find { it.name == raw }
    return match?.label
        ?: raw.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
}

@Composable
private fun ApiArticleCard(
    articleTitle: String,
    articleCoverImage: String?,
    articleTags: List<String>,
    articleReadTime: Int?,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val cardModifier = Modifier
        .padding(horizontal = 12.dp, vertical = 8.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .then(
            if (reduceAnimations) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            } else {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(14f.dp) },
                    effects = {
                        vibrancy()
                        blur(18f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.07f)
                                )
                            )
                        )
                    }
                )
            }
        )
        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))

    Column(modifier = cardModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
        ) {
            if (!articleCoverImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(articleCoverImage)
                        .crossfade(if (reduceAnimations) 0 else 400)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.55f),
                                    accentColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                        )
                    )
            )
            BasicText(
                text = articleTitle,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                style = TextStyle(Color.White, 18.sp, FontWeight.Bold, lineHeight = 22.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val readLabel = articleReadTime?.takeIf { it > 0 }?.let { "$it min read" }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApiMetricChip(
                    label = "Long-form article",
                    contentColor = accentColor,
                    containerColor = accentColor.copy(alpha = 0.14f),
                    borderColor = accentColor.copy(alpha = 0.22f)
                )
                readLabel?.let {
                    ApiMetricChip(
                        label = it,
                        contentColor = contentColor.copy(alpha = 0.78f),
                        containerColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }
            if (articleTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    articleTags.take(6).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                text = "#$tag",
                                style = TextStyle(accentColor.copy(alpha = 0.95f), 11.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiCelebrationHero(
    celebrationType: String?,
    celebrationGifUrl: String?,
    celebrationBadge: String?,
    contentColor: Color,
    accentColor: Color,
    backdrop: LayerBackdrop,
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val pulse = rememberInfiniteTransition(label = "celebratePulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.985f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrateScale"
    )
    val scale = if (reduceAnimations) 1f else pulseScale
    val heroSurface = if (reduceAnimations) {
        Modifier.background(
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.18f),
                    Color(0xFFFFD54F).copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedRectangle(16f.dp) },
            effects = {
                vibrancy()
                blur(20f.dp.toPx())
            },
            onDrawSurface = {
                drawRect(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.22f),
                            Color(0xFFFFD54F).copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
            }
        )
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .then(heroSurface)
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "CELEBRATION",
                    style = TextStyle(accentColor, 10.sp, FontWeight.Bold)
                )
                BasicText(
                    text = celebrationTypeLabel(celebrationType),
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!celebrationBadge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        text = celebrationBadge,
                        style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!celebrationGifUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(celebrationGifUrl)
                    .crossfade(if (reduceAnimations) 0 else 320)
                    .build(),
                contentDescription = "Celebration animation",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 240.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ApiLinkPreview(
    url: String?,
    title: String?,
    description: String?,
    domain: String?,
    linkImage: String?,
    contentColor: Color,
    accentColor: Color,
    reduceAnimations: Boolean = false,
    onClick: () -> Unit
) {
    if (url.isNullOrBlank()) return

    val shimmer = rememberInfiniteTransition(label = "linkShimmer")
    val shimmerShift by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val linkBg = if (reduceAnimations) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.1f),
                accentColor.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.08f + 0.04f * shimmerShift),
                accentColor.copy(alpha = 0.06f + 0.05f * (1f - shimmerShift))
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(linkBg)
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        if (!linkImage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(linkImage)
                        .crossfade(if (reduceAnimations) 0 else 400)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                            )
                        )
                )
            }
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicText(
                text = domain ?: "Open link",
                style = TextStyle(accentColor.copy(alpha = 0.9f), 10.sp, FontWeight.SemiBold)
            )
            BasicText(
                text = title ?: url,
                style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!description.isNullOrBlank()) {
                BasicText(
                    text = description,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicText(
                text = "Tap to open in browser",
                style = TextStyle(contentColor.copy(alpha = 0.52f), 11.sp)
            )
        }
    }
}

/** Non-finite poll % crashes [Double.toInt] and Compose [animateFloatAsState] / [fillMaxWidth]. */
private fun sanitizePollPercentForUi(raw: Double): Double =
    when {
        raw.isNaN() || raw.isInfinite() -> 0.0
        else -> raw.coerceIn(0.0, 100.0)
    }

@Composable
private fun ApiPollOptionRow(
    option: PollOption,
    showResults: Boolean,
    percentage: Double,
    hasVoted: Boolean,
    isPollEnded: Boolean,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onVote: () -> Unit
) {
    val safePct = sanitizePollPercentForUi(percentage)
    val targetFrac = (safePct / 100.0).toFloat().let { f ->
        if (f.isFinite()) f.coerceIn(0f, 1f) else 0f
    }
    val animatedFrac by animateFloatAsState(
        targetValue = if (showResults) targetFrac else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
        label = "pollBar"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(enabled = !hasVoted && !isPollEnded) { onVote() }
    ) {
        if (showResults) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFrac)
                    .height(50.dp)
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.24f)
                        else Color.White.copy(alpha = 0.14f)
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = option.text,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            if (showResults) {
                BasicText(
                    text = "${safePct.roundToInt()}%",
                    style = TextStyle(contentColor.copy(alpha = 0.65f), 12.sp, FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun ApiPollContent(
    options: List<PollOption>,
    endsAt: String?,
    userVotedOptionId: String?,
    showResultsBeforeVote: Boolean,
    contentColor: Color,
    accentColor: Color,
    onVote: (String) -> Unit
) {
    val hasVoted = userVotedOptionId != null
    val showResults = hasVoted || showResultsBeforeVote
    val isPollEnded = isPollExpired(endsAt)
    val totalVotes = options.sumOf { it.votes }
    val livePulse = rememberInfiniteTransition(label = "pollLive")
    val liveAlpha by livePulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveA"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "Poll",
                style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
            )
            if (!isPollEnded && !endsAt.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = liveAlpha))
                    )
                    BasicText(
                        text = "Live",
                        style = TextStyle(accentColor.copy(alpha = 0.9f), 11.sp, FontWeight.Medium)
                    )
                }
            }
        }

        options.forEach { option ->
            val rawPercentage = option.percentage.takeIf { showResults } ?: if (totalVotes > 0) {
                (option.votes.toDouble() / totalVotes.toDouble()) * 100.0
            } else {
                0.0
            }
            val percentage = sanitizePollPercentForUi(rawPercentage)
            val isSelected = option.id == userVotedOptionId
            ApiPollOptionRow(
                option = option,
                showResults = showResults,
                percentage = percentage,
                hasVoted = hasVoted,
                isPollEnded = isPollEnded,
                isSelected = isSelected,
                contentColor = contentColor,
                accentColor = accentColor,
                onVote = { onVote(option.id) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                text = "$totalVotes vote${if (totalVotes == 1) "" else "s"}",
                style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
            )
            BasicText(
                text = when {
                    isPollEnded -> "Poll ended"
                    !endsAt.isNullOrBlank() -> "Ends soon"
                    else -> ""
                },
                style = TextStyle(contentColor.copy(alpha = 0.55f), 11.sp)
            )
        }
    }
}

private fun isPollExpired(endsAt: String?): Boolean {
    if (endsAt.isNullOrBlank()) return false

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )

    for (pattern in formats) {
        val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val parsed = runCatching { parser.parse(endsAt) }.getOrNull()
        if (parsed != null) {
            return parsed.time < System.currentTimeMillis()
        }
    }

    return false
}

private fun formatTimeAgo(dateString: String): String {
    return try {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        var parsedDate: java.util.Date? = null
        for (pattern in patterns) {
            val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            parsedDate = runCatching { parser.parse(dateString) }.getOrNull()
            if (parsedDate != null) break
        }

        val date = parsedDate ?: return dateString
        val diffMillis = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
        val seconds = diffMillis / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        val days = hours / 24L
        val weeks = days / 7L

        when {
            seconds < 60L -> "Just now"
            minutes < 60L -> "${minutes}m"
            hours < 24L -> "${hours}h"
            days < 7L -> "${days}d"
            weeks < 4L -> "${weeks}w"
            else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.US).format(date)
        }
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun rememberRelativeTimeLabel(dateString: String) = produceState(
    initialValue = formatTimeAgo(dateString),
    key1 = dateString
) {
    while (true) {
        value = formatTimeAgo(dateString)
        delay(
            when (value) {
                "Just now" -> 15_000L
                else -> 60_000L
            }
        )
    }
}

/**
 * Image grid for API posts - adapts layout based on image count
 */
@Composable
private fun ApiImagePostGrid(
    images: List<String>,
    reduceAnimations: Boolean = false,
    onImageClick: (Int) -> Unit
) {
    val crossfadeMs = if (reduceAnimations) 0 else 300
    val spacing = 3.dp
    val displayImages = images.take(9)
    val extraCount = (images.size - 9).coerceAtLeast(0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
    ) {
        when (images.size) {
            1 -> {
                // Single image: full card width, height = natural aspect ratio (original proportions, not a fixed 4:3).
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[0])
                        .crossfade(crossfadeMs)
                        .build(),
                    contentDescription = "Post image 1",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top)
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable { onImageClick(0) }
                )
            }
            2 -> {
                // 2 images side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    images.forEachIndexed { index, url ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(url)
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable { onImageClick(index) }
                        )
                    }
                }
            }
            3 -> {
                // 3 images: large left, 2 stacked right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(images[0])
                            .crossfade(crossfadeMs)
                            .build(),
                        contentDescription = "Post image 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onImageClick(0) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[1])
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image 2",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onImageClick(1) }
                        )
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(images[2])
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onImageClick(2) }
                        )
                    }
                }
            }
            4 -> {
                // 2x2 grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (i in 0..1) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[i])
                                    .crossfade(crossfadeMs)
                                    .build(),
                                contentDescription = "Post image ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onImageClick(i) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        for (i in 2..3) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(images[i])
                                    .crossfade(crossfadeMs)
                                    .build(),
                                contentDescription = "Post image ${i + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onImageClick(i) }
                            )
                        }
                    }
                }
            }
            else -> {
                // 5+ images: Big first image (2x2), rest in grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(displayImages[0])
                                .crossfade(crossfadeMs)
                                .build(),
                            contentDescription = "Post image 1",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(2f)
                                .aspectRatio(1f)
                                .clickable { onImageClick(0) }
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            if (displayImages.size > 1) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(displayImages[1])
                                        .crossfade(crossfadeMs)
                                        .build(),
                                    contentDescription = "Post image 2",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { onImageClick(1) }
                                )
                            }
                            if (displayImages.size > 2) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(displayImages[2])
                                        .crossfade(crossfadeMs)
                                        .build(),
                                    contentDescription = "Post image 3",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { onImageClick(2) }
                                )
                            }
                        }
                    }
                    
                    if (displayImages.size > 3) {
                        val remainingImages = displayImages.drop(3)
                        remainingImages.chunked(3).forEachIndexed { rowIndex, rowImages ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing)
                            ) {
                                rowImages.forEachIndexed { colIndex, url ->
                                    val imageIndex = 3 + rowIndex * 3 + colIndex
                                    val isLastVisibleImage = imageIndex == 8 && extraCount > 0
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(url)
                                                .crossfade(crossfadeMs)
                                                .build(),
                                            contentDescription = "Post image ${imageIndex + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable { onImageClick(imageIndex) }
                                        )
                                        
                                        if (isLastVisibleImage) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BasicText(
                                                    text = "+$extraCount",
                                                    style = TextStyle(
                                                        color = Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(3 - rowImages.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
