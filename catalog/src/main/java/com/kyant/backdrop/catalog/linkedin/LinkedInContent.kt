package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.network.models.Comment
import com.kyant.backdrop.catalog.network.models.FullProfileResponse
import com.kyant.backdrop.catalog.network.models.Post
import com.kyant.backdrop.catalog.network.models.StoryGroup
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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.kyant.backdrop.catalog.chat.ChatViewModel

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

// Skeleton loading card for posts
@Composable
private fun PostSkeletonCard(
    backdrop: LayerBackdrop,
    isLightTheme: Boolean
) {
    val shimmer = shimmerBrush(isLightTheme)
    
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
            // Author skeleton
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar skeleton
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(shimmer)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    // Name skeleton
                    Box(
                        Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Spacer(Modifier.height(6.dp))
                    // Headline skeleton
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Time skeleton
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmer)
                    )
                }
            }
            
            // Content skeleton - multiple lines
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
            
            // Image skeleton
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmer)
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
                        .background(shimmer)
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
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
                            .background(shimmer)
                    )
                }
            }
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

@Composable
fun LinkedInContent(
    deepLink: com.kyant.backdrop.catalog.NotificationDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = "glass")
    val isGlassTheme = themeMode == "glass"
    val isLightTheme = themeMode == "light" || themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    // Glass and Bright themes use black text, Dark theme uses white text
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val accentColor = Color(0xFF0A66C2) // LinkedIn blue

    var selectedTab by remember { mutableIntStateOf(0) }
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
    
    // Settings & More screen navigation state
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSavedPostsScreen by remember { mutableStateOf(false) }
    var showNotificationSettingsScreen by remember { mutableStateOf(false) }
    var showPrivacySettingsScreen by remember { mutableStateOf(false) }
    var showAppearanceSettingsScreen by remember { mutableStateOf(false) }
    var showHelpScreen by remember { mutableStateOf(false) }
    var showInviteFriendsScreen by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showContactScreen by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Handle system back button for all overlay screens
    // Priority: innermost overlays first, then outer overlays
    BackHandler(
        enabled = viewingProfileUserId != null ||
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
                showNotificationSettingsScreen ||
                showPrivacySettingsScreen ||
                showAppearanceSettingsScreen ||
                showHelpScreen ||
                showInviteFriendsScreen ||
                showAboutScreen ||
                showContactScreen
    ) {
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
            showNotificationSettingsScreen -> showNotificationSettingsScreen = false
            showPrivacySettingsScreen -> showPrivacySettingsScreen = false
            showAppearanceSettingsScreen -> showAppearanceSettingsScreen = false
            showHelpScreen -> showHelpScreen = false
            showInviteFriendsScreen -> showInviteFriendsScreen = false
            showAboutScreen -> showAboutScreen = false
            showContactScreen -> showContactScreen = false
        }
    }
    
    // Handle deep links from push notifications
    LaunchedEffect(deepLink) {
        deepLink?.let { link ->
            when (link.action) {
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
                    // Navigate to chat - use conversationId or userId
                    selectedTab = 2 // Chat tab
                    link.conversationId?.let { convId ->
                        deepLinkConversationId = convId
                    }
                    link.userId?.let { userId ->
                        openChatWithUserId = userId
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
    }
    
    // Variable Rewards state (Hook Model)
    val rewardsViewModel: FindPeopleViewModel = viewModel(factory = FindPeopleViewModel.Factory(context))
    val rewardsState by rewardsViewModel.uiState.collectAsState()
    var showRewardCardsOverlay by remember { mutableStateOf(true) } // Show on app open
    var hasShownRewards by remember { mutableStateOf(false) }
    
    // Reels state
    val reelsViewModel: ReelsViewModel = viewModel(factory = ReelsViewModel.Factory(context))
    val reelsState by reelsViewModel.uiState.collectAsState()
    
    // Retention features state (Weekly Goals, Leaderboard, Session Summary)
    val retentionViewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val retentionState by retentionViewModel.uiState.collectAsState()
    
    // Chat state for unread message indicator
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(context))
    val chatState by chatViewModel.uiState.collectAsState()
    
    // Load retention data when user logs in
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            retentionViewModel.loadAllRetentionData()
        }
    }
    
    // Load rewards when user logs in
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn && !hasShownRewards) {
            hasShownRewards = true
            showRewardCardsOverlay = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Background based on theme
        if (isGlassTheme) {
            // Glass theme: show wallpaper background
            Image(
                painterResource(R.drawable.wallpaper_light),
                null,
                Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Solid color background for Bright/Dark themes
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color.Black else Color.White)
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
                        hasUnreadMessages = chatState.unreadCount > 0,
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
                            var showCommentsSheet by remember { mutableStateOf(false) }
                            var selectedPostForComments by remember { mutableStateOf<String?>(null) }
                            
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
                                onRefresh = { viewModel.loadFeed(); viewModel.loadStories(); reelsViewModel.loadPreviewReels(); retentionViewModel.loadAllRetentionData() },
                                onLike = { postId -> viewModel.toggleLike(postId) },
                                onComment = { postId ->
                                    selectedPostForComments = postId
                                    viewModel.loadComments(postId)
                                    showCommentsSheet = true
                                },
                                onShare = { postId ->
                                    viewModel.showShareModal(postId)
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
                                onTopNetworkersClick = { showTopNetworkersScreen = true }
                            )
                            
                            // Comments bottom sheet
                            if (showCommentsSheet && selectedPostForComments != null) {
                                com.kyant.backdrop.catalog.linkedin.posts.CommentsBottomSheet(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isLightTheme = isLightTheme,
                                    postId = selectedPostForComments!!,
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
                                        viewModel.clearComments()
                                        viewingProfileUserId = userId
                                    }
                                )
                            }
                            
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
                            onNavigateToProfile = { userId -> viewingProfileUserId = userId }
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
                            onCreateCelebrationPost = { celebrationType, content, visibility, mentions ->
                                viewModel.createCelebrationPost(celebrationType, content, visibility, mentions) { selectedTab = 0 }
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
                            currentUser = uiState.currentUser,
                            onNavigateToProfile = { selectedTab = 4 },
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
                                accentColor = accentColor
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
                    onTabSelected = { selectedTab = it },
                    backdrop = backdrop,
                    tabsCount = 5,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                LiquidBottomTab(onClick = { selectedTab = 0 }) {
                    Image(
                        painterResource(R.drawable.ic_home),
                        contentDescription = "Home",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(contentColor)
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
                            Image(
                                painterResource(R.drawable.ic_find_people),
                                contentDescription = "Find People",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(contentColor)
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
                    Image(
                        painterResource(R.drawable.ic_post),
                        contentDescription = "Post",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(contentColor)
                    )
                    BasicText("Post", style = TextStyle(contentColor, 10.sp))
                }
                LiquidBottomTab(onClick = { selectedTab = 3 }) {
                    Image(
                        painterResource(R.drawable.ic_more),
                        contentDescription = "More",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(contentColor)
                    )
                    BasicText("More", style = TextStyle(contentColor, 10.sp))
                }
                LiquidBottomTab(onClick = { selectedTab = 4 }) {
                    // Profile tab with streak indicator
                    Box {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painterResource(R.drawable.ic_profile),
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(contentColor)
                            )
                            BasicText("Profile", style = TextStyle(contentColor, 10.sp))
                        }
                        
                        // Show streak badge on profile if user has an active streak
                        if (uiState.connectionStreak > 2) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-4).dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFF9800))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText("🔥", style = TextStyle(fontSize = 8.sp))
                                    BasicText(
                                        "${uiState.connectionStreak}",
                                        style = TextStyle(Color.White, 9.sp, FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            } // End LiquidBottomTabs
            } // End AnimatedVisibility
            
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
                        onComment = { reelId -> 
                            // TODO: Open comments sheet for reel
                        },
                        onShare = { reelId ->
                            // TODO: Share reel
                        },
                        onProfileClick = { userId ->
                            reelsViewModel.closeReelsViewer()
                            viewingProfileUserId = userId
                        },
                        onTrackView = { reelId, watchTime, completed ->
                            reelsViewModel.trackView(reelId, watchTime, completed)
                        },
                        onLoadMore = { reelsViewModel.loadMoreReels() }
                    )
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
            if (showRewardCardsOverlay && (rewardsState.dailyMatches.isNotEmpty() || rewardsState.hiddenGem != null)) {
                SwipeableRewardCardsOverlay(
                    dailyMatches = rewardsState.dailyMatches,
                    hiddenGem = rewardsState.hiddenGem,
                    hiddenGemMessage = rewardsState.hiddenGemMessage,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    currentTheme = themeMode,
                    onMatchClick = { userId ->
                        showRewardCardsOverlay = false
                        viewingProfileUserId = userId
                    },
                    onHiddenGemConnect = {
                        rewardsState.hiddenGem?.id?.let { 
                            rewardsViewModel.sendConnectionRequest(it)
                        }
                        showRewardCardsOverlay = false
                    },
                    onDismissAll = {
                        showRewardCardsOverlay = false
                    }
                )
            }
            
            // Profile page when viewing another user's profile
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
                            onMessage = { otherUserId ->
                                viewingProfileUserId = null
                                openChatWithUserId = otherUserId
                                showMessagesScreen = true
                            }
                        )
                    }
                }
            }
            
            // Messages Screen Overlay
            AnimatedVisibility(
                visible = showMessagesScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                // Theme-aware colors
                val messagesContentColor = contentColor
                val messagesAccentColor = accentColor
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (isGlassTheme) {
                                Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(0f.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(12f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.15f))
                                    }
                                )
                            } else if (isDarkTheme) {
                                Modifier.background(Color(0xFF1a1a2e))
                            } else {
                                Modifier.background(Color.White)
                            }
                        )
                        .statusBarsPadding()
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
                            isGlassTheme = isGlassTheme,
                            openChatWithUserId = openChatWithUserId,
                            onConsumedOpenChat = { openChatWithUserId = null },
                            onInChatThread = { inThread -> isInChatThread = inThread },
                            onNavigateToProfile = { userId ->
                                viewingProfileUserId = userId
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    GroupsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                    val darkContentColor = Color.White
                    val darkAccentColor = Color(0xFF0A66C2)
                    
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(0f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(24f.dp.toPx())
                                    lens(12f.dp.toPx(), 24f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                                }
                            )
                            .statusBarsPadding()
                    ) {
                        GroupDetailScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = darkContentColor,
                            accentColor = darkAccentColor,
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
                    val darkContentColor = Color.White
                    val darkAccentColor = Color(0xFF0A66C2)
                    
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(0f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(24f.dp.toPx())
                                    lens(12f.dp.toPx(), 24f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                                }
                            )
                            .statusBarsPadding()
                    ) {
                        GroupChatScreen(
                            groupId = groupId,
                            backdrop = backdrop,
                            contentColor = darkContentColor,
                            accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF6C5CE7) // Purple accent for Circles
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    CirclesScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                    val darkContentColor = Color.White
                    val darkAccentColor = Color(0xFF6C5CE7)
                    
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(0f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(24f.dp.toPx())
                                    lens(12f.dp.toPx(), 24f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                                }
                            )
                            .statusBarsPadding()
                    ) {
                        CircleDetailScreen(
                            circleId = circleId,
                            backdrop = backdrop,
                            contentColor = darkContentColor,
                            accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    WeeklyGoalsDetailScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFFFF9800)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    StreakDetailsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFFFFD700)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    TopNetworkersScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    SavedPostsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
                        onNavigateBack = { showSavedPostsScreen = false }
                    )
                }
            }
            
            // Notification Settings Screen Overlay
            AnimatedVisibility(
                visible = showNotificationSettingsScreen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    NotificationSettingsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    PrivacySettingsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    AppearanceSettingsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    HelpScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    AboutScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    InviteFriendsScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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
                val darkContentColor = Color.White
                val darkAccentColor = Color(0xFF0A66C2)
                
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(0f.dp) },
                            effects = {
                                vibrancy()
                                blur(24f.dp.toPx())
                                lens(12f.dp.toPx(), 24f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFF1a1a2e).copy(alpha = 0.98f))
                            }
                        )
                        .statusBarsPadding()
                ) {
                    ContactScreen(
                        backdrop = backdrop,
                        contentColor = darkContentColor,
                        accentColor = darkAccentColor,
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

@Composable
private fun LinkedInTopBar(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    userInitials: String = "U",
    hasUnreadMessages: Boolean = false,
    onMessagesClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left spacer for centering
        Spacer(Modifier.width(92.dp))
        
        // Centered app name with solid color
        BasicText(
            "vormeX",
            style = TextStyle(
                color = Color.Black,
                fontSize = 32.sp,
                fontFamily = PacificoFontFamily,
                fontWeight = FontWeight.Normal
            )
        )

        // Right side icons
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification icon
            Box(
                Modifier
                    .size(40.dp)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_notifications),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
            }
            
            Spacer(Modifier.width(8.dp))

            // Messages icon with unread dot
            Box(
                Modifier
                    .size(40.dp)
                    .clickable { onMessagesClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.ic_message),
                    contentDescription = "Messages",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
                // Unread indicator dot
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
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
    onTopNetworkersClick: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
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
    
    // Custom smooth fling behavior for buttery scrolling
    val smoothFlingBehavior = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                // Use exponential decay with lower friction for smoother, longer-lasting scrolls
                val decay = exponentialDecay<Float>(
                    frictionMultiplier = 0.8f, // Lower = smoother/longer scroll
                    absVelocityThreshold = 0.5f // Lower threshold = scroll continues longer
                )
                var remainingVelocity = initialVelocity
                var lastValue = 0f
                
                androidx.compose.animation.core.AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity
                ).animateDecay(decay) {
                    val delta = value - lastValue
                    lastValue = value
                    val consumed = scrollBy(delta)
                    // If we hit a boundary, stop the animation
                    if (kotlin.math.abs(delta - consumed) > 0.5f) {
                        cancelAnimation()
                    }
                    remainingVelocity = velocity
                }
                return remainingVelocity
            }
        }
    }
    
    // Pull-to-refresh with haptic feedback and minimal line indicator
    // Twitter/X style - thin animated gradient line at top
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            isRefreshing = true
                onRefresh()
                // Reset after a short delay (ViewModel will update the data)
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(1500)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            flingBehavior = smoothFlingBehavior
        ) {
            item { Spacer(Modifier.height(8.dp)) }
        
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
            
            // Top Networkers Preview (like web's leaderboard sidebar) - Keep at top
            if (state.leaderboardData.users.isNotEmpty()) {
                item {
                    TopNetworkersPreview(
                        leaderboard = state.leaderboardData,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onSeeAll = onTopNetworkersClick
                    )
                }
            }
        }

        // Loading state - Skeleton loading
        if (isLoading && posts.isEmpty()) {
            items(3) {
                PostSkeletonCard(
                    backdrop = backdrop,
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

        // Posts from API with engagement widgets interspersed at positions 4, 10, 18
        posts.forEachIndexed { index, post ->
            // Check if we should show a widget before this post
            retentionState?.let { state ->
                val widgetType = widgetPositions[index]
                when (widgetType) {
                    "people_like_you" -> {
                        if (state.peopleLikeYou.isNotEmpty()) {
                            item(key = "widget_people_like_you") {
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
                    }
                    "todays_matches" -> {
                        if (state.todaysMatches.isNotEmpty()) {
                            item(key = "widget_todays_matches") {
                                TodaysMatchesSection(
                                    matches = state.todaysMatches,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onMatchClick = { userId -> onProfileClick(userId) },
                                    onConnect = { /* TODO: Send connection request */ },
                                    onSeeAll = onNavigateToFindPeople
                                )
                            }
                        }
                    }
                    "weekly_goals" -> {
                        item(key = "widget_weekly_goals") {
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
            
            // Render the post
            item(key = post.id) {
                ApiPostCard(
                    post = post,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onLike = onLike,
                    onComment = onComment,
                    onShare = onShare,
                    onProfileClick = { onProfileClick(post.author.id) },
                    onMentionClick = { username -> onProfileClick(username) },
                    onMenuAction = onMenuAction
                )
            }
        }
        
        // Show widgets at the end if there aren't enough posts
        if (posts.size < 25) {
            retentionState?.let { state ->
                if (state.peopleLikeYou.isNotEmpty() && posts.size < 5) {
                    item(key = "widget_people_like_you_fallback") {
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
                if (state.todaysMatches.isNotEmpty() && posts.size < 12) {
                    item(key = "widget_todays_matches_fallback") {
                        TodaysMatchesSection(
                            matches = state.todaysMatches,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onMatchClick = { userId -> onProfileClick(userId) },
                            onConnect = { /* TODO: Send connection request */ },
                            onSeeAll = onNavigateToFindPeople
                        )
                    }
                }
                if (posts.size < 20) {
                    item(key = "widget_weekly_goals_fallback") {
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
    currentUser: com.kyant.backdrop.catalog.network.models.User? = null,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToCircles: () -> Unit = {},
    onNavigateToReels: () -> Unit = {},
    onNavigateToWeeklyGoals: () -> Unit = {},
    onNavigateToStreakDetails: () -> Unit = {},
    onNavigateToTopNetworkers: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToSavedPosts: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToPrivacySettings: () -> Unit = {},
    onNavigateToAppearanceSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToContact: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: FindPeopleViewModel = viewModel(factory = FindPeopleViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    // Retention ViewModel for weekly goals and streaks
    val retentionViewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val retentionState by retentionViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        retentionViewModel.loadAllRetentionData()
    }
    
    // Calculate weekly goals summary
    val goalsData = retentionState.weeklyGoals
    val goalsProgressText = if (goalsData.goals.isNotEmpty()) {
        val completed = goalsData.goals.count { it.isComplete }
        val total = goalsData.goals.size
        "${(goalsData.totalProgress * 100).toInt()}% complete"
    } else {
        "Start tracking"
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        
        // ==================== User Header (Optional) ====================
        currentUser?.let { user ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isGlassTheme) Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(20.dp) },
                            effects = {
                                vibrancy()
                                blur(12f.dp.toPx())
                                lens(6f.dp.toPx(), 12f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(accentColor.copy(alpha = 0.15f))
                            }
                        ) else Modifier.background(contentColor.copy(alpha = 0.08f))
                    )
                    .clickable(onClick = onNavigateToProfile)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profileImage != null) {
                            coil.compose.AsyncImage(
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
                                style = TextStyle(Color.White, 24.sp, FontWeight.Bold)
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            user.name ?: "User",
                            style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                        )
                        if (!user.username.isNullOrEmpty()) {
                            BasicText(
                                "@${user.username}",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                            )
                        }
                        if (!user.headline.isNullOrEmpty()) {
                            BasicText(
                                user.headline!!,
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp),
                                maxLines = 1
                            )
                        }
                    }
                    
                    BasicText(
                        "›",
                        style = TextStyle(contentColor.copy(alpha = 0.4f), 24.sp)
                    )
                }
            }
        } ?: run {
            // Fallback: Just "More" title
            BasicText(
                "More",
                Modifier.padding(start = 4.dp),
                style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        // ==================== GROUP 1: Goals & Activity ====================
        MoreSectionHeader("Goals & Activity", contentColor)
        
        // Weekly Goals with live progress
        MoreMenuItemWithSubtitle(
            title = "Weekly Goals",
            subtitle = goalsProgressText,
            iconResId = R.drawable.ic_target,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            trailingIconResId = if (goalsData.streakAtRisk) R.drawable.ic_warning else null,
            onClick = onNavigateToWeeklyGoals
        )
        
        // Streaks & Activity
        MoreMenuItemWithSubtitle(
            title = "Streaks & Activity",
            subtitle = "Networking, Login, Posting, Messaging",
            iconResId = R.drawable.ic_flame,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToStreakDetails
        )
        
        // Top Networkers
        MoreMenuItemWithSubtitle(
            title = "Top Networkers",
            subtitle = "Weekly & monthly leaderboard",
            iconResId = R.drawable.ic_trophy,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToTopNetworkers
        )
        
        // Live Activity Banner (if active)
        if (retentionState.liveActivity.activeNow > 0) {
            LiveActivityBanner(
                activityData = retentionState.liveActivity,
                backdrop = backdrop,
                contentColor = contentColor,
                isGlassTheme = isGlassTheme
            )
        }
        
        // ==================== GROUP 2: Account & Content ====================
        MoreSectionHeader("Account & Content", contentColor)
        
        // Profile
        MoreMenuItemWithSubtitle(
            title = "Profile",
            subtitle = "View and edit your profile",
            iconResId = R.drawable.ic_profile,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToProfile
        )
        
        // Saved Posts
        MoreMenuItemWithSubtitle(
            title = "Saved Posts",
            subtitle = "Posts you've bookmarked",
            iconResId = R.drawable.ic_bookmark,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToSavedPosts
        )
        
        // Edit Profile Preferences / Onboarding
        MoreMenuItemWithSubtitle(
            title = "Profile Preferences",
            subtitle = "Goals, interests & matching settings",
            iconResId = R.drawable.ic_edit,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToOnboarding
        )
        
        // ==================== GROUP 3: Communities ====================
        MoreSectionHeader("Communities", contentColor)
        
        // Groups
        MoreMenuItemWithSubtitle(
            title = "Groups",
            subtitle = "Connect with communities",
            iconResId = R.drawable.ic_users,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToGroups
        )
        
        // Circles
        MoreMenuItemWithSubtitle(
            title = "Circles",
            subtitle = "Share with close friends",
            iconResId = R.drawable.ic_circle,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToCircles
        )
        
        // Reels
        MoreMenuItemWithSubtitle(
            title = "Reels",
            subtitle = "Watch short videos",
            iconResId = R.drawable.ic_video,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToReels
        )
        
        // ==================== GROUP 4: Preferences ====================
        MoreSectionHeader("Preferences", contentColor)
        
        // Notifications
        MoreMenuItemWithSubtitle(
            title = "Notifications",
            subtitle = "Push, digest & alerts",
            iconResId = R.drawable.ic_notifications,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToNotificationSettings
        )
        
        // Privacy
        MoreMenuItemWithSubtitle(
            title = "Privacy",
            subtitle = "Profile visibility & messaging",
            iconResId = R.drawable.ic_lock,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToPrivacySettings
        )
        
        // Appearance
        MoreMenuItemWithSubtitle(
            title = "Appearance",
            subtitle = "Theme, font & accessibility",
            iconResId = R.drawable.ic_palette,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToAppearanceSettings
        )
        
        // ==================== GROUP 5: Support & Legal ====================
        MoreSectionHeader("Support & Legal", contentColor)
        
        // Help & FAQ
        MoreMenuItemWithSubtitle(
            title = "Help & FAQ",
            subtitle = "Getting started & troubleshooting",
            iconResId = R.drawable.ic_help,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToHelp
        )
        
        // Invite Friends
        MoreMenuItemWithSubtitle(
            title = "Invite Friends",
            subtitle = "Share Vormex with others",
            iconResId = R.drawable.ic_gift,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToInviteFriends
        )
        
        // About
        MoreMenuItemWithSubtitle(
            title = "About",
            subtitle = "Version, terms & privacy policy",
            iconResId = R.drawable.ic_info,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToAbout
        )
        
        // Contact Us
        MoreMenuItemWithSubtitle(
            title = "Contact Us",
            subtitle = "Support & feedback",
            iconResId = R.drawable.ic_email,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isGlassTheme = isGlassTheme,
            onClick = onNavigateToContact
        )
        
        // ==================== GROUP 6: Account Actions ====================
        MoreSectionHeader("Account Actions", contentColor)
        
        // Log Out - special styling (danger action)
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
                            drawRect(Color.Red.copy(alpha = 0.1f))
                        }
                    ) else Modifier.background(Color.Red.copy(alpha = 0.08f))
                )
                .clickable(onClick = onLogout)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.Red.copy(alpha = 0.9f))
                )
                Spacer(Modifier.width(16.dp))
                BasicText(
                    "Log Out",
                    style = TextStyle(Color.Red.copy(alpha = 0.9f), 16.sp, FontWeight.Medium)
                )
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun MoreSectionHeader(
    title: String,
    contentColor: Color
) {
    BasicText(
        title.uppercase(),
        style = TextStyle(
            color = contentColor.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp)
    )
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
    onSignUpClick: () -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(32f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
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
                    "Vormex",
                    style = TextStyle(accentColor, 32.sp, FontWeight.Bold)
                )
                
                BasicText(
                    "Sign in to continue",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Email field
                BasicTextField(
                    value = email,
                    onValueChange = { email = it; onClearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (email.isEmpty()) {
                                BasicText(
                                    "Email",
                                    style = TextStyle(Color.Gray, 16.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Password field
                BasicTextField(
                    value = password,
                    onValueChange = { password = it; onClearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    visualTransformation = PasswordVisualTransformation(),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (password.isEmpty()) {
                                BasicText(
                                    "Password",
                                    style = TextStyle(Color.Gray, 16.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Error message
                error?.let { errorMsg ->
                    BasicText(
                        errorMsg,
                        style = TextStyle(Color.Red, 12.sp)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Login button
                LiquidButton(
                    onClick = { 
                        if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                            onLogin(email, password)
                        }
                    },
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    tint = accentColor,
                    isInteractive = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            "Sign In",
                            style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                        )
                    }
                }
                
                // OR divider
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(contentColor.copy(alpha = 0.3f))
                    )
                    BasicText(
                        "OR",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(contentColor.copy(alpha = 0.3f))
                    )
                }
                
                // Google Sign-In button
                LiquidButton(
                    onClick = { 
                        if (!isGoogleLoading && !isLoading) {
                            onGoogleSignIn()
                        }
                    },
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    surfaceColor = Color.White.copy(alpha = 0.2f),
                    isInteractive = !isGoogleLoading && !isLoading
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painterResource(R.drawable.ic_google),
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp)
                            )
                            BasicText(
                                "Continue with Google",
                                style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Forgot password
                BasicText(
                    "Forgot password?",
                    modifier = Modifier
                        .clickable { /* TODO: Navigate to forgot password */ }
                        .padding(vertical = 8.dp),
                    style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Don't have an account
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Don't have an account? ",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                    )
                    BasicText(
                        "Sign Up",
                        modifier = Modifier
                            .clickable { onSignUpClick() }
                            .padding(4.dp),
                        style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                    )
                }
            }
        }
    }
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
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(32f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(8f.dp.toPx(), 16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BasicText(
                    "Create Account",
                    style = TextStyle(accentColor, 28.sp, FontWeight.Bold)
                )
                
                BasicText(
                    "Join Vormex today",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Name field
                BasicTextField(
                    value = name,
                    onValueChange = { name = it; onClearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (name.isEmpty()) {
                                BasicText("Full Name", style = TextStyle(Color.Gray, 16.sp))
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Username field
                BasicTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }; onClearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (username.isEmpty()) {
                                BasicText("Username", style = TextStyle(Color.Gray, 16.sp))
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Email field
                BasicTextField(
                    value = email,
                    onValueChange = { email = it; onClearError() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (email.isEmpty()) {
                                BasicText("Email", style = TextStyle(Color.Gray, 16.sp))
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Password field
                BasicTextField(
                    value = password,
                    onValueChange = { password = it; onClearError(); passwordError = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    visualTransformation = PasswordVisualTransformation(),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (password.isEmpty()) {
                                BasicText("Password (min 8 chars)", style = TextStyle(Color.Gray, 16.sp))
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Confirm Password field
                BasicTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp),
                    textStyle = TextStyle(Color.Black, 16.sp),
                    cursorBrush = SolidColor(accentColor),
                    visualTransformation = PasswordVisualTransformation(),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (confirmPassword.isEmpty()) {
                                BasicText("Confirm Password", style = TextStyle(Color.Gray, 16.sp))
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Error messages
                passwordError?.let { err ->
                    BasicText(err, style = TextStyle(Color.Red, 12.sp))
                }
                error?.let { errorMsg ->
                    BasicText(errorMsg, style = TextStyle(Color.Red, 12.sp))
                }
                
                Spacer(Modifier.height(4.dp))
                
                // Sign Up button
                LiquidButton(
                    onClick = { 
                        when {
                            name.isBlank() -> passwordError = "Name is required"
                            username.isBlank() -> passwordError = "Username is required"
                            username.length < 3 -> passwordError = "Username must be at least 3 characters"
                            email.isBlank() -> passwordError = "Email is required"
                            password.length < 8 -> passwordError = "Password must be at least 8 characters"
                            password != confirmPassword -> passwordError = "Passwords do not match"
                            !isLoading -> onSignUp(email, password, name, username)
                        }
                    },
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    tint = accentColor,
                    isInteractive = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        BasicText(
                            "Create Account",
                            style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                        )
                    }
                }
                
                // OR divider
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(contentColor.copy(alpha = 0.3f))
                    )
                    BasicText("OR", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(contentColor.copy(alpha = 0.3f))
                    )
                }
                
                // Google Sign-In button
                LiquidButton(
                    onClick = { 
                        if (!isGoogleLoading && !isLoading) {
                            onGoogleSignIn()
                        }
                    },
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    surfaceColor = Color.White.copy(alpha = 0.2f),
                    isInteractive = !isGoogleLoading && !isLoading
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = contentColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painterResource(R.drawable.ic_google),
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp)
                            )
                            BasicText(
                                "Continue with Google",
                                style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Already have an account
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Already have an account? ",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                    )
                    BasicText(
                        "Sign In",
                        modifier = Modifier
                            .clickable { onLoginClick() }
                            .padding(4.dp),
                        style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiPostCard(
    post: Post,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    onMenuAction: (String, String) -> Unit = { _, _ -> }
) {
    var showMenu by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    var showFullScreenVideo by remember { mutableStateOf(false) }
    
    // Mention preview state
    var showMentionPreview by remember { mutableStateOf(false) }
    var mentionUsername by remember { mutableStateOf("") }
    
    // Red color for active likes
    val likeActiveColor = Color(0xFFE53935)
    
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(0f.dp) },
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
            // Author info with menu
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onProfileClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profileImageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profileImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile picture of $authorName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            BasicText(
                                initials,
                                style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        BasicText(
                            post.author.name ?: post.author.username ?: "Unknown",
                            style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                        )
                        post.author.headline?.let { headline ->
                            BasicText(
                                headline,
                                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicText(
                            formatTimeAgo(post.createdAt),
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                        )
                    }
                }
                
                // Menu button (three dots) with SVG icon
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { showMenu = true }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MenuDotsIcon(
                        color = contentColor,
                        size = 20.dp
                    )
                }
            }
            
            // Glass-themed dropdown menu
            if (showMenu) {
                GlassDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    backdrop = backdrop,
                    contentColor = contentColor
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

            // Post content with mention support
            post.content?.let { content ->
                FormattedContent(
                    content = content,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onMentionClick = { username -> onMentionClick(username) },
                    onMentionLongPress = { username ->
                        mentionUsername = username
                        showMentionPreview = true
                    }
                )
            }

            // Media: Video or Image
            val isVideoPost = post.type == "VIDEO" || !post.videoUrl.isNullOrEmpty()
            
            if (isVideoPost && !post.videoUrl.isNullOrEmpty()) {
                // Video player for video posts - full width with original aspect ratio
                VideoPlayer(
                    videoUrl = post.videoUrl,
                    modifier = Modifier.fillMaxWidth(),
                    autoPlay = false,
                    showControls = true,
                    contentColor = contentColor,
                    onFullScreenClick = { showFullScreenVideo = true }
                )
            } else if (post.mediaUrls.isNotEmpty()) {
                // Image grid for image posts
                ApiImagePostGrid(
                    images = post.mediaUrls,
                    onImageClick = { index ->
                        selectedImageIndex = index
                        showImageViewer = true
                    }
                )
            }

            // Engagement stats with SVG icon
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LikeIcon(
                        color = if (post.isLiked) likeActiveColor else contentColor.copy(alpha = 0.6f),
                        size = 14.dp,
                        filled = post.isLiked
                    )
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        "${post.likesCount}",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                }
                Row {
                    BasicText(
                        "${post.commentsCount} comments",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                    if (post.sharesCount > 0) {
                        BasicText(
                            " • ${post.sharesCount} shares",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                        )
                    }
                }
            }

            // Divider
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(contentColor.copy(alpha = 0.1f))
            )

            // Action buttons with SVG icons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ApiActionButton(
                    icon = { 
                        LikeIcon(
                            color = if (post.isLiked) likeActiveColor else contentColor.copy(alpha = 0.7f),
                            size = 18.dp,
                            filled = post.isLiked
                        )
                    },
                    label = "Like",
                    contentColor = if (post.isLiked) likeActiveColor else contentColor,
                    onClick = { onLike(post.id) }
                )
                ApiActionButton(
                    icon = { CommentIcon(contentColor.copy(alpha = 0.7f), size = 18.dp) },
                    label = "Comment",
                    contentColor = contentColor,
                    onClick = { onComment(post.id) }
                )
                ApiActionButton(
                    icon = { ShareIcon(contentColor.copy(alpha = 0.7f), size = 18.dp) },
                    label = "Share",
                    contentColor = contentColor,
                    onClick = { onShare(post.id) }
                )
            }
        }
    }
    
    // Full screen image viewer dialog
    if (showImageViewer && post.mediaUrls.isNotEmpty()) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
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
    icon: @Composable () -> Unit,
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp))
    }
}

private fun formatTimeAgo(dateString: String): String {
    // Simple time ago formatter - in production use a proper date library
    return try {
        "Just now" // Simplified - you can add proper date parsing
    } catch (e: Exception) {
        dateString
    }
}

/**
 * Image grid for API posts - adapts layout based on image count
 */
@Composable
private fun ApiImagePostGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit
) {
    val spacing = 2.dp
    val displayImages = images.take(9)
    val extraCount = (images.size - 9).coerceAtLeast(0)
    
    Box(modifier = Modifier.fillMaxWidth()) {
        when (images.size) {
            1 -> {
                // Single image - full width with actual aspect ratio
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(images[0])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post image 1",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
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
                                .crossfade(true)
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
                            .crossfade(true)
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
                                .crossfade(true)
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
                                .crossfade(true)
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
                                    .crossfade(true)
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
                                    .crossfade(true)
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
                                .crossfade(true)
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
                                        .crossfade(true)
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
                                        .crossfade(true)
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
                                                .crossfade(true)
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
