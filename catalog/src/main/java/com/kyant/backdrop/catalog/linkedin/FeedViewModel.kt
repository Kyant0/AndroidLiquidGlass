package com.kyant.backdrop.catalog.linkedin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.data.VormexPerformancePolicy
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.PostsApiService
import com.kyant.backdrop.catalog.network.GoogleAuthHelper
import com.kyant.backdrop.catalog.network.PostSocketManager
import com.kyant.backdrop.catalog.network.GrowthApiService
import com.kyant.backdrop.catalog.network.models.FullComment
import com.kyant.backdrop.catalog.network.models.FullPost
import com.kyant.backdrop.catalog.network.models.PollOption
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.backdrop.catalog.network.models.FullProfileResponse
import com.kyant.backdrop.catalog.network.models.Post
import com.kyant.backdrop.catalog.network.models.Story
import com.kyant.backdrop.catalog.network.models.StoryGroup
import com.kyant.backdrop.catalog.network.models.User
import com.kyant.backdrop.catalog.network.models.Connection
import com.kyant.backdrop.catalog.notifications.PushTokenRegistrar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Feed Diversity Algorithm
 * Ensures users see varied content each time they open the app by:
 * 1. Time-based shuffling (changes every 30 minutes)
 * 2. Author diversity (spreads posts from same author)
 * 3. Content type mixing (alternates text/image/video)
 */
private fun diversifyFeed(posts: List<Post>, userId: String?): List<Post> {
    if (posts.size <= 3) return posts
    
    // Time-based seed: changes every 30 minutes for variety
    val timeSeed = System.currentTimeMillis() / (30 * 60 * 1000)
    val userSeed = userId?.hashCode()?.toLong() ?: 0L
    val random = Random(timeSeed xor userSeed)
    
    // Group posts by author to ensure diversity
    val postsByAuthor = posts.groupBy { it.authorId }
    
    // If all posts are from different authors, just add some shuffle
    if (postsByAuthor.size == posts.size) {
        return posts.shuffled(random).take(5) + posts.drop(5).shuffled(random)
    }
    
    // Build diversified feed: avoid consecutive posts from same author
    val result = mutableListOf<Post>()
    val remaining = posts.toMutableList()
    var lastAuthorId: String? = null
    var lastType: String? = null
    
    while (remaining.isNotEmpty()) {
        // Find posts NOT from last author (prefer different content type too)
        val candidates = remaining.filter { it.authorId != lastAuthorId }
        
        val nextPost = when {
            candidates.isEmpty() -> {
                // All remaining posts are from same author, just take one
                remaining.removeAt(0)
            }
            candidates.size == 1 -> {
                val post = candidates.first()
                remaining.remove(post)
                post
            }
            else -> {
                // Prefer different content type for variety
                val typeVaried = candidates.filter { it.type != lastType }
                val pool = typeVaried.ifEmpty { candidates }
                
                // Add controlled randomness (not full random, maintain some relevance)
                val idx = if (pool.size > 3) random.nextInt(minOf(3, pool.size)) else 0
                val post = pool[idx]
                remaining.remove(post)
                post
            }
        }
        
        result.add(nextPost)
        lastAuthorId = nextPost.authorId
        lastType = nextPost.type
    }
    
    return result
}

/** Poll API may send non-finite doubles; [Double.toInt] throws on NaN and breaks Compose animations. */
private fun PollOption.withSafePercentage(): PollOption {
    val p = percentage
    val safe = when {
        p.isNaN() || p.isInfinite() -> 0.0
        else -> p.coerceIn(0.0, 100.0)
    }
    return copy(percentage = safe)
}

private fun Post.withSanitizedPoll(): Post =
    if (pollOptions.isEmpty()) this
    else copy(pollOptions = pollOptions.map { it.withSafePercentage() })

/**
 * Prepends a created post while removing any existing row with the same id.
 * Realtime [post:created] may arrive before the HTTP response; without this the feed
 * can contain duplicate ids and [LazyColumn] crashes (duplicate keys).
 */
private fun prependCreatedPost(post: Post, currentPosts: ImmutableList<Post>): ImmutableList<Post> =
    (listOf(post) + currentPosts.filterNot { it.id == post.id }).toImmutableList()

// Helper to convert FullPost to Post for FeedUiState compatibility
private fun FullPost.toPost(): Post = Post(
    id = id,
    kind = kind,
    type = type,
    authorId = authorId,
    author = author,
    content = content,
    contentType = contentType,
    mentions = mentions,
    mediaUrls = mediaUrls,
    mediaCount = mediaCount,
    videoUrl = videoUrl,
    videoThumbnail = videoThumbnail,
    videoDuration = videoDuration,
    videoSize = videoSize,
    videoFormat = videoFormat,
    documentUrl = documentUrl,
    documentName = documentName,
    documentType = documentType,
    documentSize = documentSize,
    documentPages = documentPages,
    documentThumbnail = documentThumbnail,
    linkUrl = linkUrl,
    linkTitle = linkTitle,
    linkDescription = linkDescription,
    linkImage = linkImage,
    linkDomain = linkDomain,
    articleTitle = articleTitle,
    articleCoverImage = articleCoverImage,
    articleReadTime = articleReadTime,
    articleTags = articleTags,
    pollDuration = pollDuration,
    pollEndsAt = pollEndsAt,
    pollOptions = pollOptions.map { it.withSafePercentage() },
    userVotedOptionId = userVotedOptionId,
    showResultsBeforeVote = showResultsBeforeVote,
    celebrationType = celebrationType,
    celebrationMeta = celebrationMeta,
    celebrationBadge = celebrationBadge,
    celebrationGifUrl = celebrationGifUrl,
    likesCount = likesCount,
    commentsCount = commentsCount,
    sharesCount = sharesCount,
    savesCount = savesCount,
    isLiked = isLiked,
    isSaved = isSaved,
    userReactionType = userReactionType,
    reactionSummary = reactionSummary,
    visibility = visibility,
    createdAt = createdAt,
    updatedAt = updatedAt
)

enum class AuthScreen {
    LOGIN,
    SIGNUP
}

// Upload status for Instagram-like progress bar
enum class UploadStatus {
    IDLE,
    UPLOADING,
    PROCESSING,
    SUCCESS,
    FAILED
}

data class UploadProgress(
    val status: UploadStatus = UploadStatus.IDLE,
    val progress: Float = 0f, // 0.0 to 1.0
    val message: String = "",
    val postType: String = "TEXT"
)

data class FeedUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val isCreatingPost: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val authScreen: AuthScreen = AuthScreen.LOGIN,
    val pendingReferralCode: String? = null,
    val posts: ImmutableList<Post> = persistentListOf(),
    val storyGroups: List<StoryGroup> = emptyList(),
    val myStories: List<Story> = emptyList(),
    val error: String? = null,
    val currentUser: User? = null,
    val currentUserId: String? = null,
    // Onboarding state
    val showOnboarding: Boolean = false,
    val onboardingCompleted: Boolean = true, // Default true to not show until we check
    // Story viewer state
    val isStoryViewerOpen: Boolean = false,
    val currentStoryGroupIndex: Int = 0,
    val currentStoryIndex: Int = 0,
    val isCreatingStory: Boolean = false,
    val isStoryCreatorOpen: Boolean = false,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    // Upload progress state (Instagram-like)
    val uploadProgress: UploadProgress = UploadProgress(),
    // Streak state (Duolingo Effect)
    val connectionStreak: Int = 0,
    val loginStreak: Int = 0,
    val postingStreak: Int = 0,
    val isStreakAtRisk: Boolean = false,
    val lastConnectionDate: String? = null,
    val showStreakReminder: Boolean = false,
    val showLoginStreakBadge: Boolean = false, // Added: separate control for login badge
    // Profile state
    val profile: FullProfileResponse? = null,
    val isProfileLoading: Boolean = false,
    val profileError: String? = null,
    // Comments state
    val selectedPostId: String? = null,
    val comments: List<FullComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val commentsError: String? = null,
    val isSubmittingComment: Boolean = false,
    val hasMoreComments: Boolean = false,
    val commentsPage: Int = 1,
    // Mention search state
    val mentionSearchResults: List<MentionUser> = emptyList(),
    val isSearchingMentions: Boolean = false,
    // Share modal state
    val showShareModal: Boolean = false,
    val sharePostId: String? = null,
    val shareConnections: List<Connection> = emptyList(),
    val isLoadingConnections: Boolean = false,
    val isSharing: Boolean = false
)

class FeedViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val feedCacheTtlMillis = VormexPerformancePolicy.FeedCacheTtlMillis
    private val supportingDataTtlMillis = VormexPerformancePolicy.SupportingDataTtlMillis

    private var lastFeedLoadedAt = 0L
    private var lastStoriesLoadedAt = 0L
    private var lastCurrentUserLoadedAt = 0L
    private var lastStreakLoadedAt = 0L

    private var isFeedRequestInFlight = false
    private var isStoriesRequestInFlight = false
    private var isCurrentUserRequestInFlight = false
    private var isStreakRequestInFlight = false
    
    init {
        observePostRealtime()
        checkLoginStatus()
    }

    private fun observePostRealtime() {
        viewModelScope.launch {
            PostSocketManager.postCreatedFlow.collectLatest { post ->
                val safe = post.withSanitizedPoll()
                _uiState.value = _uiState.value.copy(
                    posts = (listOf(safe) + _uiState.value.posts.filterNot { it.id == safe.id }).toImmutableList()
                )
            }
        }

        viewModelScope.launch {
            PostSocketManager.postLikedFlow.collectLatest { event ->
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.map { post ->
                        if (post.id == event.postId) {
                            post.copy(
                                likesCount = event.likesCount,
                                isLiked = if (_uiState.value.currentUserId == event.userId) event.liked else post.isLiked
                            )
                        } else {
                            post
                        }
                    }.toImmutableList()
                )
            }
        }

        viewModelScope.launch {
            PostSocketManager.postSharedFlow.collectLatest { event ->
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.map { post ->
                        if (post.id == event.postId) post.copy(sharesCount = event.sharesCount) else post
                    }.toImmutableList()
                )
            }
        }

        viewModelScope.launch {
            PostSocketManager.commentCreatedFlow.collectLatest { event ->
                val updatedPosts = _uiState.value.posts.map { post ->
                    if (post.id == event.postId) post.copy(commentsCount = event.commentsCount) else post
                }.toImmutableList()
                val selectedPostId = _uiState.value.selectedPostId
                val updatedComments =
                    if (selectedPostId == event.postId && event.comment != null) {
                        insertComment(_uiState.value.comments, event.comment)
                    } else {
                        _uiState.value.comments
                    }

                _uiState.value = _uiState.value.copy(
                    posts = updatedPosts,
                    comments = updatedComments
                )
            }
        }

        viewModelScope.launch {
            PostSocketManager.commentLikedFlow.collectLatest { event ->
                if (_uiState.value.selectedPostId == event.postId) {
                    _uiState.value = _uiState.value.copy(
                        comments = updateCommentLike(
                            _uiState.value.comments,
                            event.commentId,
                            event.liked,
                            event.likesCount
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            PostSocketManager.commentDeletedFlow.collectLatest { event ->
                val updatedPosts = _uiState.value.posts.map { post ->
                    if (post.id == event.postId) post.copy(commentsCount = event.commentsCount) else post
                }.toImmutableList()
                val updatedComments =
                    if (_uiState.value.selectedPostId == event.postId) {
                        removeComment(_uiState.value.comments, event.commentId)
                    } else {
                        _uiState.value.comments
                    }

                _uiState.value = _uiState.value.copy(
                    posts = updatedPosts,
                    comments = updatedComments
                )
            }
        }

        viewModelScope.launch {
            PostSocketManager.pollUpdatedFlow.collectLatest { event ->
                _uiState.value = _uiState.value.copy(
                    posts = _uiState.value.posts.map { post ->
                        if (post.id == event.postId) {
                            post.copy(
                                pollOptions = event.pollOptions.map { it.withSafePercentage() },
                                userVotedOptionId =
                                    if (_uiState.value.currentUserId == event.voterId) {
                                        event.votedOptionId ?: post.userVotedOptionId
                                    } else {
                                        post.userVotedOptionId
                                    }
                            )
                        } else {
                            post
                        }
                    }.toImmutableList()
                )
            }
        }
    }

    private suspend fun ensurePostRealtimeConnected() {
        val token = ApiClient.getToken(context) ?: return
        PostSocketManager.connect(token)
    }
    
    fun showLogin() {
        _uiState.value = _uiState.value.copy(authScreen = AuthScreen.LOGIN, error = null)
    }
    
    fun showSignUp() {
        _uiState.value = _uiState.value.copy(authScreen = AuthScreen.SIGNUP, error = null)
    }

    fun setPendingReferralCode(code: String?) {
        _uiState.value = _uiState.value.copy(
            pendingReferralCode = code?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            val token = ApiClient.getToken(context)
            if (token != null) {
                ensurePostRealtimeConnected()
                _uiState.value = _uiState.value.copy(isLoggedIn = true)
                // Critical path first: user + feed (home scroll). Defer stories/streaks so the first
                // paint and feed request are not competing with as many parallel HTTP calls.
                loadCurrentUser()
                loadFeed()
                delay(280)
                loadStories()
                loadStreakData()
            } else {
                _uiState.value = _uiState.value.copy(isLoggedIn = false)
            }
        }
    }
    
    // ==================== Streak Data (Duolingo Effect) ====================
    
    private fun loadStreakData(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(lastStreakLoadedAt, supportingDataTtlMillis)) {
            return
        }
        if (isStreakRequestInFlight) return

        viewModelScope.launch {
            isStreakRequestInFlight = true
            // Check 24-hour cooldown from Find People screen (shared cooldown)
            val findPeoplePrefs = context.getSharedPreferences("vormex_find_people", Context.MODE_PRIVATE)
            val lastDismissTime = findPeoplePrefs.getLong("last_error_dismiss_time", 0)
            val cooldownHours = 24
            val isInCooldown = System.currentTimeMillis() - lastDismissTime < cooldownHours * 60 * 60 * 1000
            
            // Fetch streaks from backend API (same source as web)
            ApiClient.getStreaks(context)
                .onSuccess { streakData ->
                    // Calculate if connection streak is at risk - respect 24h cooldown
                    val isAtRisk = streakData.isAtRisk.connection && !isInCooldown
                    
                    val prefs = context.getSharedPreferences("vormex_streaks", Context.MODE_PRIVATE)
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    
                    // Streak reminder: ONLY show if at risk AND not in cooldown
                    val lastReminderDismissed = prefs.getString("last_reminder_dismissed", null)
                    val showReminder = isAtRisk && lastReminderDismissed != today && !isInCooldown
                    
                    // Login streak badge: ONLY show at milestones (1, 3, 7, 14, 30, etc.) 
                    // AND not dismissed today AND NOT at risk (don't overwhelm with badges)
                    val lastBadgeDismissed = prefs.getString("last_badge_dismissed", null)
                    val milestones = setOf(1, 3, 7, 14, 21, 30, 60, 90, 100, 365)
                    val isMilestone = streakData.loginStreak in milestones
                    val showBadge = !isAtRisk && 
                                    streakData.loginStreak > 0 && 
                                    (isMilestone || lastBadgeDismissed != today)
                    lastStreakLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        connectionStreak = streakData.connectionStreak,
                        loginStreak = streakData.loginStreak,
                        postingStreak = streakData.postingStreak,
                        isStreakAtRisk = isAtRisk,
                        lastConnectionDate = null, // Backend handles this
                        showStreakReminder = showReminder,
                        showLoginStreakBadge = showBadge
                    )
                }
                .onFailure {
                    // Silently fail - streaks are not critical
                    println("Failed to load streaks: ${it.message}")
                }
            
            // Record login to backend (updates login streak on backend)
            recordLoginToBackend()
            isStreakRequestInFlight = false
        }
    }
    
    private fun recordLoginToBackend() {
        viewModelScope.launch {
            // Check 24-hour cooldown
            val findPeoplePrefs = context.getSharedPreferences("vormex_find_people", Context.MODE_PRIVATE)
            val lastDismissTime = findPeoplePrefs.getLong("last_error_dismiss_time", 0)
            val isInCooldown = System.currentTimeMillis() - lastDismissTime < 24 * 60 * 60 * 1000
            
            ApiClient.recordLogin(context)
                .onSuccess { streakData ->
                    // Respect cooldown when updating at-risk state
                    val isAtRisk = streakData.isAtRisk.connection && !isInCooldown
                    _uiState.value = _uiState.value.copy(
                        loginStreak = streakData.loginStreak,
                        connectionStreak = streakData.connectionStreak,
                        postingStreak = streakData.postingStreak,
                        isStreakAtRisk = isAtRisk
                    )
                }
                .onFailure {
                    // Silently fail
                    println("Failed to record login: ${it.message}")
                }
        }
    }
    
    fun dismissStreakReminder() {
        // Also triggers 24-hour cooldown
        val findPeoplePrefs = context.getSharedPreferences("vormex_find_people", Context.MODE_PRIVATE)
        findPeoplePrefs.edit().putLong("last_error_dismiss_time", System.currentTimeMillis()).apply()
        
        val prefs = context.getSharedPreferences("vormex_streaks", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        prefs.edit().putString("last_reminder_dismissed", today).apply()
        _uiState.value = _uiState.value.copy(showStreakReminder = false, isStreakAtRisk = false)
    }
    
    fun dismissLoginStreakBadge() {
        val prefs = context.getSharedPreferences("vormex_streaks", Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        prefs.edit().putString("last_badge_dismissed", today).apply()
        _uiState.value = _uiState.value.copy(showLoginStreakBadge = false)
    }
    
    fun refreshStreaks() {
        loadStreakData(forceRefresh = true)
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            ApiClient.login(email, password)
                .onSuccess { response ->
                    ApiClient.saveToken(context, response.token, response.user.id)
                    PushTokenRegistrar.syncCurrentToken(context)
                    ensurePostRealtimeConnected()
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        currentUser = response.user,
                        currentUserId = response.user.id,
                        isLoading = false,
                        showOnboarding = !response.user.onboardingCompleted,
                        onboardingCompleted = response.user.onboardingCompleted
                    )
                    loadCurrentUser(forceRefresh = true)
                    loadFeed(forceRefresh = true)
                    loadStories(forceRefresh = true)
                    loadStreakData(forceRefresh = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Login failed"
                    )
                }
        }
    }
    
    fun register(email: String, password: String, name: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            ApiClient.register(email, password, name, username)
                .onSuccess { response ->
                    ApiClient.saveToken(context, response.token, response.user.id)
                    applyPendingReferralCodeIfNeeded(shouldApply = true)
                    PushTokenRegistrar.syncCurrentToken(context)
                    ensurePostRealtimeConnected()
                    _uiState.value = _uiState.value.copy(
                        isLoggedIn = true,
                        currentUser = response.user,
                        currentUserId = response.user.id,
                        isLoading = false,
                        showOnboarding = true, // New users always need onboarding
                        onboardingCompleted = false
                    )
                    loadCurrentUser(forceRefresh = true)
                    loadFeed(forceRefresh = true)
                    loadStories(forceRefresh = true)
                    loadStreakData(forceRefresh = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Registration failed"
                    )
                }
        }
    }
    
    fun googleSignIn(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGoogleLoading = true, error = null)
            val shouldApplyReferral = _uiState.value.authScreen == AuthScreen.SIGNUP
            
            when (val result = GoogleAuthHelper.signIn(activity)) {
                is GoogleAuthHelper.GoogleSignInResult.Success -> {
                    // Send ID token to backend
                    ApiClient.googleSignIn(result.idToken)
                        .onSuccess { response ->
                            ApiClient.saveToken(context, response.token, response.user.id)
                            applyPendingReferralCodeIfNeeded(shouldApply = shouldApplyReferral)
                            PushTokenRegistrar.syncCurrentToken(context)
                            ensurePostRealtimeConnected()
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                currentUser = response.user,
                                currentUserId = response.user.id,
                                isGoogleLoading = false,
                                showOnboarding = !response.user.onboardingCompleted,
                                onboardingCompleted = response.user.onboardingCompleted
                            )
                            loadCurrentUser(forceRefresh = true)
                            loadFeed(forceRefresh = true)
                            loadStories(forceRefresh = true)
                            loadStreakData(forceRefresh = true)
                        }
                        .onFailure { e ->
                            _uiState.value = _uiState.value.copy(
                                isGoogleLoading = false,
                                error = e.message ?: "Google Sign-In failed"
                            )
                        }
                }
                is GoogleAuthHelper.GoogleSignInResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGoogleLoading = false,
                        error = result.message
                    )
                }
                GoogleAuthHelper.GoogleSignInResult.Cancelled -> {
                    _uiState.value = _uiState.value.copy(isGoogleLoading = false)
                }
            }
        }
    }

    private suspend fun applyPendingReferralCodeIfNeeded(shouldApply: Boolean) {
        val code = _uiState.value.pendingReferralCode ?: return
        if (!shouldApply) return

        GrowthApiService.applyReferralCode(context, code)
        _uiState.value = _uiState.value.copy(pendingReferralCode = null)
    }
    
    fun logout() {
        viewModelScope.launch {
            _uiState.value.selectedPostId?.let { PostSocketManager.leavePost(it) }
            PostSocketManager.disconnect()
            ApiClient.clearToken(context)
            _uiState.value = FeedUiState()
        }
    }
    
    private fun loadCurrentUser(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(lastCurrentUserLoadedAt, supportingDataTtlMillis)) {
            return
        }
        if (isCurrentUserRequestInFlight) return

        viewModelScope.launch {
            isCurrentUserRequestInFlight = true
            ApiClient.getCurrentUser(context)
                .onSuccess { user ->
                    lastCurrentUserLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        currentUser = user,
                        currentUserId = user.id,
                        onboardingCompleted = user.onboardingCompleted,
                        showOnboarding = !user.onboardingCompleted
                    )
                }
            isCurrentUserRequestInFlight = false
        }
    }

    fun refreshCurrentUser() {
        loadCurrentUser(forceRefresh = true)
    }
    
    fun completeOnboarding() {
        _uiState.value = _uiState.value.copy(
            showOnboarding = false,
            onboardingCompleted = true
        )
        // Reload user data after onboarding
        loadCurrentUser(forceRefresh = true)
        loadFeed(forceRefresh = true)
        loadStories(forceRefresh = true)
    }
    
    fun skipOnboarding() {
        _uiState.value = _uiState.value.copy(showOnboarding = false)
    }
    
    fun showOnboardingAgain() {
        _uiState.value = _uiState.value.copy(showOnboarding = true)
    }
    
    fun loadFeed(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(lastFeedLoadedAt, feedCacheTtlMillis)) {
            return
        }
        if (isFeedRequestInFlight) return

        viewModelScope.launch {
            isFeedRequestInFlight = true
            val hasCachedFeed = _uiState.value.posts.isNotEmpty()
            // Stale-while-revalidate: keep showing previous posts while refreshing (no full-list loading flash).
            _uiState.value = _uiState.value.copy(
                isLoading = !hasCachedFeed,
                error = null
            )
            
            ApiClient.getFeed(context)
                .onSuccess { response ->
                    // Apply diversity algorithm to prevent same-feed-every-time
                    val diversifiedPosts = diversifyFeed(response.posts, _uiState.value.currentUserId)
                        .map { it.withSanitizedPoll() }
                    lastFeedLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        posts = diversifiedPosts.toImmutableList(),
                        nextCursor = response.nextCursor,
                        hasMore = response.hasMore,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        // Only block the feed with an error row when nothing is cached to show
                        error = if (_uiState.value.posts.isEmpty()) {
                            e.message ?: "Failed to load feed"
                        } else {
                            null
                        }
                    )
                }
            isFeedRequestInFlight = false
        }
    }
    
    fun loadStories(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(lastStoriesLoadedAt, supportingDataTtlMillis)) {
            return
        }
        if (isStoriesRequestInFlight) return

        viewModelScope.launch {
            isStoriesRequestInFlight = true
            ApiClient.getStories(context)
                .onSuccess { response ->
                    lastStoriesLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        storyGroups = response.storyGroups
                    )
                }
            isStoriesRequestInFlight = false
        }
    }
    
    // ==================== Story Functions ====================
    
    fun loadMyStories() {
        viewModelScope.launch {
            ApiClient.getMyStories(context)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        myStories = response.stories
                    )
                }
        }
    }
    
    fun createStory(
        mediaType: String = "IMAGE",
        mediaBytes: Pair<ByteArray, String>? = null,
        textContent: String? = null,
        backgroundColor: String? = null,
        category: String = "GENERAL",
        visibility: String = "PUBLIC",
        linkUrl: String? = null,
        linkTitle: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingStory = true, error = null)
            
            ApiClient.createStory(
                context = context,
                mediaType = mediaType,
                mediaBytes = mediaBytes,
                textContent = textContent,
                backgroundColor = backgroundColor,
                category = category,
                visibility = visibility,
                linkUrl = linkUrl,
                linkTitle = linkTitle
            )
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        myStories = listOf(response.story) + _uiState.value.myStories,
                        isCreatingStory = false
                    )
                    loadStories(forceRefresh = true) // Refresh all stories
                    onSuccess?.invoke()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingStory = false,
                        error = e.message ?: "Failed to create story"
                    )
                }
        }
    }
    
    fun viewStory(storyId: String) {
        viewModelScope.launch {
            ApiClient.viewStory(context, storyId)
                .onSuccess { response ->
                    // Update views count in the story
                    val updatedGroups = _uiState.value.storyGroups.map { group ->
                        group.copy(
                            stories = group.stories.map { story ->
                                if (story.id == storyId) {
                                    story.copy(viewsCount = response.viewsCount)
                                } else {
                                    story
                                }
                            }
                        )
                    }
                    _uiState.value = _uiState.value.copy(storyGroups = updatedGroups)
                }
        }
    }
    
    fun reactToStory(storyId: String, reaction: String = "LIKE") {
        viewModelScope.launch {
            ApiClient.reactToStory(context, storyId, reaction)
                .onSuccess { response ->
                    // Update reactions count in the story
                    val updatedGroups = _uiState.value.storyGroups.map { group ->
                        group.copy(
                            stories = group.stories.map { story ->
                                if (story.id == storyId) {
                                    story.copy(reactionsCount = response.reactionsCount)
                                } else {
                                    story
                                }
                            }
                        )
                    }
                    _uiState.value = _uiState.value.copy(storyGroups = updatedGroups)
                }
        }
    }
    
    fun getStoryViewers(storyId: String, callback: (List<StoryViewer>) -> Unit) {
        viewModelScope.launch {
            println("DEBUG: Fetching viewers for story: $storyId")
            ApiClient.getStoryViewers(context, storyId)
                .onSuccess { response ->
                    println("DEBUG: Got ${response.viewers.size} viewers, totalCount: ${response.totalCount}")
                    val viewers = response.viewers.map { viewerData ->
                        println("DEBUG: Viewer data - id: ${viewerData.id}, user: ${viewerData.user?.name}")
                        StoryViewer(
                            id = viewerData.id,
                            viewedAt = viewerData.viewedAt,
                            user = viewerData.user?.let { user ->
                                StoryViewerUser(
                                    id = user.id,
                                    name = user.name,
                                    username = user.username,
                                    profileImage = user.profileImage,
                                    headline = user.headline
                                )
                            }
                        )
                    }
                    callback(viewers)
                }
                .onFailure { e ->
                    println("DEBUG: Failed to get viewers: ${e.message}")
                    callback(emptyList())
                }
        }
    }
    
    fun replyToStory(storyId: String, content: String) {
        viewModelScope.launch {
            ApiClient.replyToStory(context, storyId, content)
                .onSuccess {
                    // Show toast or feedback
                    android.widget.Toast.makeText(context, "Reply sent!", android.widget.Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    android.widget.Toast.makeText(context, "Failed to send reply", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            ApiClient.deleteStory(context, storyId)
                .onSuccess {
                    // Remove from my stories
                    _uiState.value = _uiState.value.copy(
                        myStories = _uiState.value.myStories.filter { it.id != storyId }
                    )
                    loadStories(forceRefresh = true) // Refresh all stories
                }
        }
    }

    private fun isFresh(loadedAt: Long, ttlMillis: Long): Boolean {
        return loadedAt != 0L && System.currentTimeMillis() - loadedAt < ttlMillis
    }
    
    fun openStoryViewer(groupIndex: Int, storyIndex: Int = 0) {
        _uiState.value = _uiState.value.copy(
            isStoryViewerOpen = true,
            currentStoryGroupIndex = groupIndex,
            currentStoryIndex = storyIndex
        )
    }
    
    fun closeStoryViewer() {
        _uiState.value = _uiState.value.copy(
            isStoryViewerOpen = false,
            currentStoryGroupIndex = 0,
            currentStoryIndex = 0
        )
    }
    
    fun nextStory() {
        val groups = _uiState.value.storyGroups
        val currentGroupIndex = _uiState.value.currentStoryGroupIndex
        val currentStoryIndex = _uiState.value.currentStoryIndex
        
        if (currentGroupIndex < groups.size) {
            val currentGroup = groups[currentGroupIndex]
            if (currentStoryIndex < currentGroup.stories.size - 1) {
                // Next story in same group
                _uiState.value = _uiState.value.copy(currentStoryIndex = currentStoryIndex + 1)
            } else if (currentGroupIndex < groups.size - 1) {
                // Next group
                _uiState.value = _uiState.value.copy(
                    currentStoryGroupIndex = currentGroupIndex + 1,
                    currentStoryIndex = 0
                )
            } else {
                // End of all stories
                closeStoryViewer()
            }
        }
    }
    
    fun previousStory() {
        val currentGroupIndex = _uiState.value.currentStoryGroupIndex
        val currentStoryIndex = _uiState.value.currentStoryIndex
        
        if (currentStoryIndex > 0) {
            // Previous story in same group
            _uiState.value = _uiState.value.copy(currentStoryIndex = currentStoryIndex - 1)
        } else if (currentGroupIndex > 0) {
            // Previous group (go to last story)
            val prevGroup = _uiState.value.storyGroups[currentGroupIndex - 1]
            _uiState.value = _uiState.value.copy(
                currentStoryGroupIndex = currentGroupIndex - 1,
                currentStoryIndex = prevGroup.stories.size - 1
            )
        }
    }
    
    fun openStoryCreator() {
        _uiState.value = _uiState.value.copy(isStoryCreatorOpen = true)
    }
    
    fun closeStoryCreator() {
        _uiState.value = _uiState.value.copy(isStoryCreatorOpen = false)
    }

    fun createPost(
        type: String = "TEXT",
        content: String,
        visibility: String = "PUBLIC",
        imageBytes: List<Pair<ByteArray, String>> = emptyList(),
        videoBytes: Pair<ByteArray, String>? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        if (content.isBlank() && imageBytes.isEmpty() && videoBytes == null) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, error = null)
            
            ApiClient.createPost(context, type, content, visibility, imageBytes, videoBytes)
                .onSuccess { post ->
                    // Refresh streaks from backend (backend automatically tracks posting streak)
                    refreshStreaks()
                    
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(post, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    onSuccess?.invoke()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        error = it.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    // ==================== Upload Progress Helpers ====================
    
    private fun startUpload(postType: String, message: String = "Uploading...") {
        _uiState.value = _uiState.value.copy(
            uploadProgress = UploadProgress(
                status = UploadStatus.UPLOADING,
                progress = 0f,
                message = message,
                postType = postType
            )
        )
    }
    
    private fun updateUploadProgress(progress: Float, message: String? = null) {
        val current = _uiState.value.uploadProgress
        _uiState.value = _uiState.value.copy(
            uploadProgress = current.copy(
                progress = progress,
                message = message ?: current.message
            )
        )
    }
    
    private fun setProcessing() {
        val current = _uiState.value.uploadProgress
        _uiState.value = _uiState.value.copy(
            uploadProgress = current.copy(
                status = UploadStatus.PROCESSING,
                progress = 0.9f,
                message = "Processing..."
            )
        )
    }
    
    private fun uploadSuccess() {
        val current = _uiState.value.uploadProgress
        _uiState.value = _uiState.value.copy(
            uploadProgress = current.copy(
                status = UploadStatus.SUCCESS,
                progress = 1f,
                message = "Posted!"
            )
        )
        // Auto-hide after 2 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            clearUploadProgress()
        }
    }
    
    private fun uploadFailed(error: String) {
        val current = _uiState.value.uploadProgress
        _uiState.value = _uiState.value.copy(
            uploadProgress = current.copy(
                status = UploadStatus.FAILED,
                message = error
            ),
            error = error
        )
        // Auto-hide after 3 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            clearUploadProgress()
        }
    }
    
    fun clearUploadProgress() {
        _uiState.value = _uiState.value.copy(
            uploadProgress = UploadProgress()
        )
    }
    
    fun dismissUploadError() {
        clearUploadProgress()
        clearError()
    }
    
    // ==================== Full Post Type Create Methods ====================
    
    fun createTextPost(
        content: String,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Content is required")
            return
        }
        
        // Navigate immediately - Instagram style
        onSuccess()
        
        // Start upload in background with progress
        viewModelScope.launch {
            startUpload("TEXT", "Posting...")
            
            // Simulate initial progress
            updateUploadProgress(0.3f)
            kotlinx.coroutines.delay(200)
            updateUploadProgress(0.5f)
            
            PostsApiService.createTextPost(context, content, visibility, mentions)
                .onSuccess { fullPost ->
                    setProcessing()
                    kotlinx.coroutines.delay(300)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to create post")
                }
        }
    }
    
    fun createImagePost(
        content: String?,
        visibility: String = "PUBLIC",
        images: List<Pair<ByteArray, String>>,
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (images.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "At least one image is required")
            return
        }
        
        // Navigate immediately - Instagram style
        onSuccess()
        
        // Start upload in background with progress
        viewModelScope.launch {
            val imageCount = images.size
            val totalSize = images.sumOf { it.first.size }
            startUpload("IMAGE", "Uploading ${imageCount} image${if (imageCount > 1) "s" else ""}...")
            
            // Start actual API call immediately
            val uploadJob = async {
                PostsApiService.createImagePost(context, content, visibility, images, mentions)
            }
            
            // Show progress animation while actual upload happens
            var progress = 0f
            while (!uploadJob.isCompleted && progress < 0.85f) {
                kotlinx.coroutines.delay(150)
                // Increment progress based on total size
                val increment = when {
                    totalSize > 10 * 1024 * 1024 -> 0.02f // Large: slow
                    totalSize > 3 * 1024 * 1024 -> 0.03f // Medium
                    else -> 0.05f // Small: faster
                }
                progress = (progress + increment).coerceAtMost(0.85f)
                updateUploadProgress(progress, "Uploading image${if (imageCount > 1) "s" else ""}... ${(progress * 100).toInt()}%")
            }
            
            // Wait for actual upload to complete
            val result = uploadJob.await()
            
            result
                .onSuccess { fullPost ->
                    setProcessing()
                    kotlinx.coroutines.delay(300)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to upload image${if (imageCount > 1) "s" else ""}")
                }
        }
    }
    
    fun createVideoPost(
        content: String?,
        visibility: String = "PUBLIC",
        videoBytes: ByteArray,
        videoFilename: String,
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        // Navigate immediately - Instagram style
        onSuccess()
        
        // Start upload in background with progress
        viewModelScope.launch {
            startUpload("VIDEO", "Uploading video...")
            
            // Start actual API call immediately
            val uploadJob = async {
                PostsApiService.createVideoPost(context, content, visibility, videoBytes, videoFilename, mentions)
            }
            
            // Show progress animation while actual upload happens
            var progress = 0f
            while (!uploadJob.isCompleted && progress < 0.85f) {
                kotlinx.coroutines.delay(200)
                // Increment progress slowly based on file size
                val increment = when {
                    videoBytes.size > 50 * 1024 * 1024 -> 0.01f // Large video: slow progress
                    videoBytes.size > 10 * 1024 * 1024 -> 0.02f // Medium video
                    else -> 0.03f // Small video: faster progress
                }
                progress = (progress + increment).coerceAtMost(0.85f)
                updateUploadProgress(progress, "Uploading video... ${(progress * 100).toInt()}%")
            }
            
            // Wait for actual upload to complete
            val result = uploadJob.await()
            
            result
                .onSuccess { fullPost ->
                    setProcessing()
                    updateUploadProgress(0.95f, "Processing video...")
                    kotlinx.coroutines.delay(300)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to upload video")
                }
        }
    }
    
    fun createLinkPost(
        linkUrl: String,
        content: String?,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (linkUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "URL is required")
            return
        }
        
        // Navigate immediately
        onSuccess()
        
        viewModelScope.launch {
            startUpload("LINK", "Creating link post...")
            updateUploadProgress(0.4f)
            
            PostsApiService.createLinkPost(context, linkUrl, content, visibility, mentions)
                .onSuccess { fullPost ->
                    setProcessing()
                    kotlinx.coroutines.delay(300)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to create link post")
                }
        }
    }
    
    fun createPollPost(
        pollOptions: List<String>,
        pollDurationHours: Int,
        content: String?,
        visibility: String = "PUBLIC",
        showResultsBeforeVote: Boolean = false,
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        val validOptions = pollOptions.filter { it.isNotBlank() }
        if (validOptions.size < 2) {
            _uiState.value = _uiState.value.copy(error = "At least 2 poll options required")
            return
        }
        
        // Navigate immediately
        onSuccess()
        
        viewModelScope.launch {
            startUpload("POLL", "Creating poll...")
            updateUploadProgress(0.4f)
            
            PostsApiService.createPollPost(context, validOptions, pollDurationHours, content, visibility, showResultsBeforeVote, mentions)
                .onSuccess { fullPost ->
                    setProcessing()
                    kotlinx.coroutines.delay(300)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to create poll")
                }
        }
    }
    
    fun createArticlePost(
        articleTitle: String,
        content: String?,
        visibility: String = "PUBLIC",
        coverImage: Pair<ByteArray, String>? = null,
        articleTags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (articleTitle.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Article title is required")
            return
        }
        
        // Navigate immediately
        onSuccess()
        
        viewModelScope.launch {
            startUpload("ARTICLE", "Publishing article...")
            
            // Simulate progress
            for (i in 1..4) {
                kotlinx.coroutines.delay(300)
                updateUploadProgress(i * 0.2f)
            }
            
            PostsApiService.createArticlePost(context, articleTitle, content, visibility, coverImage, articleTags, mentions)
                .onSuccess { fullPost ->
                    setProcessing()
                    kotlinx.coroutines.delay(400)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to publish article")
                }
        }
    }
    
    fun createCelebrationPost(
        celebrationType: String,
        content: String?,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        celebrationGif: Pair<ByteArray, String>? = null,
        onSuccess: () -> Unit = {}
    ) {
        // Navigate immediately
        onSuccess()
        
        viewModelScope.launch {
            startUpload("CELEBRATION", "Posting celebration...")
            updateUploadProgress(0.4f)
            
            PostsApiService.createCelebrationPost(
                context,
                celebrationType,
                content,
                visibility,
                mentions,
                celebrationGif
            )
                .onSuccess { fullPost ->
                    setProcessing()
                    kotlinx.coroutines.delay(300)
                    refreshStreaks()
                    val newPost = fullPost.toPost()
                    _uiState.value = _uiState.value.copy(
                        posts = prependCreatedPost(newPost, _uiState.value.posts),
                        isCreatingPost = false
                    )
                    uploadSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isCreatingPost = false)
                    uploadFailed(e.message ?: "Failed to post celebration")
                }
        }
    }
    
    fun loadMorePosts() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            ApiClient.getFeed(context, cursor)
                .onSuccess { response ->
                    // Apply light diversity to new posts before appending
                    val diversifiedNewPosts = diversifyFeed(response.posts, _uiState.value.currentUserId)
                        .map { it.withSanitizedPoll() }
                    _uiState.value = _uiState.value.copy(
                        posts = (_uiState.value.posts + diversifiedNewPosts).toImmutableList(),
                        nextCursor = response.nextCursor,
                        hasMore = response.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
        }
    }
    
    fun toggleLike(postId: String) {
        // Optimistic UI update - update immediately before API call
        val currentPost = _uiState.value.posts.find { it.id == postId }
        val currentlyLiked = currentPost?.isLiked ?: false
        val currentLikesCount = currentPost?.likesCount ?: 0
        
        // Update UI immediately with optimistic state
        val optimisticPosts = _uiState.value.posts.map { post ->
            if (post.id == postId) {
                post.copy(
                    isLiked = !currentlyLiked,
                    likesCount = if (currentlyLiked) (currentLikesCount - 1).coerceAtLeast(0) else currentLikesCount + 1
                )
            } else {
                post
            }
        }.toImmutableList()
        _uiState.value = _uiState.value.copy(posts = optimisticPosts)
        
        // Then make API call and sync with server response
        viewModelScope.launch {
            ApiClient.toggleLike(context, postId)
                .onSuccess { response ->
                    // Update with actual server response
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                isLiked = response.liked,
                                likesCount = response.likesCount
                            )
                        } else {
                            post
                        }
                    }.toImmutableList()
                    _uiState.value = _uiState.value.copy(posts = updatedPosts)
                }
                .onFailure {
                    // Revert optimistic update on failure
                    val revertedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                isLiked = currentlyLiked,
                                likesCount = currentLikesCount
                            )
                        } else {
                            post
                        }
                    }.toImmutableList()
                    _uiState.value = _uiState.value.copy(posts = revertedPosts)
                }
        }
    }

    fun votePoll(postId: String, optionId: String) {
        val currentPost = _uiState.value.posts.find { it.id == postId } ?: return
        if (currentPost.userVotedOptionId != null) return

        val originalOptions = currentPost.pollOptions
        val optimisticOptions = originalOptions.map { option ->
            if (option.id == optionId) option.copy(votes = option.votes + 1, hasVoted = true) else option
        }
        val totalVotes = optimisticOptions.sumOf { it.votes }
        val optionsWithPercentages = optimisticOptions.map { option ->
            option.copy(
                percentage = if (totalVotes > 0) (option.votes.toDouble() / totalVotes.toDouble()) * 100.0 else 0.0,
                hasVoted = option.id == optionId
            )
        }

        _uiState.value = _uiState.value.copy(
            posts = _uiState.value.posts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        pollOptions = optionsWithPercentages,
                        userVotedOptionId = optionId
                    )
                } else {
                    post
                }
            }.toImmutableList()
        )

        viewModelScope.launch {
            PostsApiService.votePoll(context, postId, optionId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        posts = _uiState.value.posts.map { post ->
                            if (post.id == postId) {
                                post.copy(
                                    pollOptions = response.pollOptions,
                                    userVotedOptionId = response.userVotedOptionId ?: optionId
                                )
                            } else {
                                post
                            }
                        }.toImmutableList()
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        posts = _uiState.value.posts.map { post ->
                            if (post.id == postId) {
                                post.copy(
                                    pollOptions = originalOptions,
                                    userVotedOptionId = currentPost.userVotedOptionId
                                )
                            } else {
                                post
                            }
                        }.toImmutableList()
                    )
                }
        }
    }
    
    // Comments functionality
    fun loadComments(postId: String, page: Int = 1) {
        viewModelScope.launch {
            if (page == 1 && _uiState.value.selectedPostId != postId) {
                _uiState.value.selectedPostId?.let { PostSocketManager.leavePost(it) }
                PostSocketManager.joinPost(postId)
            }
            _uiState.value = _uiState.value.copy(
                selectedPostId = postId,
                isLoadingComments = page == 1,
                isLoadingMoreComments = page > 1,
                commentsError = null,
                commentsPage = page
            )
            
            PostsApiService.getComments(context, postId, page = page)
                .onSuccess { response ->
                    val newComments = if (page == 1) {
                        response.comments
                    } else {
                        _uiState.value.comments + response.comments
                    }
                    _uiState.value = _uiState.value.copy(
                        comments = newComments,
                        hasMoreComments = response.hasMore,
                        isLoadingComments = false,
                        isLoadingMoreComments = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = e.message ?: "Failed to load comments"
                    )
                }
        }
    }
    
    fun loadMoreComments() {
        val postId = _uiState.value.selectedPostId ?: return
        if (_uiState.value.isLoadingMoreComments || !_uiState.value.hasMoreComments) return
        loadComments(postId, _uiState.value.commentsPage + 1)
    }
    
    fun submitComment(postId: String, content: String, parentId: String? = null, mentions: List<String>? = null) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            
            PostsApiService.createComment(context, postId, content, parentId, mentions)
                .onSuccess { newComment ->
                    val updatedComments = insertComment(_uiState.value.comments, newComment)
                    
                    // Update the post's comment count
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(commentsCount = post.commentsCount + 1)
                        } else {
                            post
                        }
                    }.toImmutableList()
                    
                    _uiState.value = _uiState.value.copy(
                        comments = updatedComments,
                        posts = updatedPosts,
                        isSubmittingComment = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingComment = false,
                        commentsError = "Failed to post comment"
                    )
                }
        }
    }
    
    fun toggleCommentLike(commentId: String) {
        val postId = _uiState.value.selectedPostId ?: return
        
        viewModelScope.launch {
            PostsApiService.toggleCommentLike(context, postId, commentId)
                .onSuccess { response ->
                    val updatedComments = updateCommentLike(_uiState.value.comments, commentId, response.liked, response.likesCount)
                    _uiState.value = _uiState.value.copy(comments = updatedComments)
                }
        }
    }
    
    private fun updateCommentLike(
        comments: List<FullComment>,
        commentId: String,
        liked: Boolean,
        likesCount: Int
    ): List<FullComment> {
        return comments.map { comment ->
            if (comment.id == commentId) {
                comment.copy(isLiked = liked, likesCount = likesCount)
            } else {
                comment.copy(
                    replies = updateCommentLike(comment.replies, commentId, liked, likesCount)
                )
            }
        }
    }

    private fun insertComment(comments: List<FullComment>, newComment: FullComment): List<FullComment> {
        if (comments.any { it.id == newComment.id }) return comments

        if (newComment.parentId == null) {
            return listOf(newComment) + comments
        }

        return comments.map { comment ->
            if (comment.id == newComment.parentId) {
                val updatedReplies =
                    if (comment.replies.any { it.id == newComment.id }) comment.replies else comment.replies + newComment
                comment.copy(
                    replies = updatedReplies,
                    replyCount = maxOf(comment.replyCount, updatedReplies.size)
                )
            } else {
                val updatedReplies = insertComment(comment.replies, newComment)
                if (updatedReplies != comment.replies) {
                    comment.copy(replies = updatedReplies)
                } else {
                    comment
                }
            }
        }
    }
    
    fun deleteComment(commentId: String) {
        val postId = _uiState.value.selectedPostId ?: return
        
        viewModelScope.launch {
            PostsApiService.deleteComment(context, postId, commentId)
                .onSuccess {
                    val updatedComments = removeComment(_uiState.value.comments, commentId)
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(commentsCount = maxOf(0, post.commentsCount - 1))
                        } else {
                            post
                        }
                    }.toImmutableList()
                    _uiState.value = _uiState.value.copy(
                        comments = updatedComments,
                        posts = updatedPosts
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        commentsError = "Failed to delete comment"
                    )
                }
        }
    }
    
    private fun removeComment(comments: List<FullComment>, commentId: String): List<FullComment> {
        return comments.mapNotNull { comment ->
            if (comment.id == commentId) {
                null
            } else {
                val updatedReplies = removeComment(comment.replies, commentId)
                val replyRemoved = updatedReplies.size != comment.replies.size
                comment.copy(
                    replies = updatedReplies,
                    replyCount = if (replyRemoved) maxOf(0, comment.replyCount - 1) else comment.replyCount
                )
            }
        }
    }
    
    // Mention search
    fun searchMentions(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(
                mentionSearchResults = emptyList(),
                isSearchingMentions = false
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingMentions = true)
            
            PostsApiService.searchMentions(context, query)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        mentionSearchResults = response.users,
                        isSearchingMentions = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        mentionSearchResults = emptyList(),
                        isSearchingMentions = false
                    )
                }
        }
    }
    
    fun clearMentionSearch() {
        _uiState.value = _uiState.value.copy(
            mentionSearchResults = emptyList(),
            isSearchingMentions = false
        )
    }
    
    fun clearComments() {
        _uiState.value.selectedPostId?.let { PostSocketManager.leavePost(it) }
        _uiState.value = _uiState.value.copy(
            selectedPostId = null,
            comments = emptyList(),
            commentsError = null,
            hasMoreComments = false,
            commentsPage = 1
        )
    }
    
    fun clearCommentsError() {
        _uiState.value = _uiState.value.copy(commentsError = null)
    }
    
    // Share functionality
    fun showShareModal(postId: String) {
        _uiState.value = _uiState.value.copy(
            showShareModal = true,
            sharePostId = postId
        )
    }
    
    fun hideShareModal() {
        _uiState.value = _uiState.value.copy(
            showShareModal = false,
            sharePostId = null,
            shareConnections = emptyList()
        )
    }
    
    fun copyPostLink(postId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val postUrl = "https://vormex.com/post/$postId"
        val clip = ClipData.newPlainText("Post URL", postUrl)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        
        // Also record the share on the backend
        viewModelScope.launch {
            PostsApiService.sharePost(context, postId)
                .onSuccess { response ->
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(sharesCount = response.sharesCount)
                        } else {
                            post
                        }
                    }.toImmutableList()
                    _uiState.value = _uiState.value.copy(posts = updatedPosts)
                }
        }
    }
    
    fun sharePostExternal(postId: String, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSharing = true)
            
            PostsApiService.sharePost(context, postId)
                .onSuccess { response ->
                    // Update the post's share count
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(sharesCount = response.sharesCount)
                        } else {
                            post
                        }
                    }.toImmutableList()
                    _uiState.value = _uiState.value.copy(
                        posts = updatedPosts,
                        isSharing = false,
                        showShareModal = false
                    )
                    
                    // Launch native share dialog
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this post on Vormex: ${response.shareUrl ?: "https://vormex.com/post/$postId"}")
                        type = "text/plain"
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSharing = false)
                    // If share URL fails, just share a generic link
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this post on Vormex: https://vormex.com/post/$postId")
                        type = "text/plain"
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                }
        }
    }
    
    // Legacy sharePost for backward compatibility
    fun sharePost(postId: String, activity: Activity) {
        sharePostExternal(postId, activity)
    }
    
    fun loadProfile(userId: String = "me") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProfileLoading = true, profileError = null)
            
            ApiClient.getProfile(context, userId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        profile = response,
                        isProfileLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isProfileLoading = false,
                        profileError = e.message ?: "Failed to load profile"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        _uiState.value.selectedPostId?.let { PostSocketManager.leavePost(it) }
        super.onCleared()
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedViewModel(context.applicationContext) as T
        }
    }
}
