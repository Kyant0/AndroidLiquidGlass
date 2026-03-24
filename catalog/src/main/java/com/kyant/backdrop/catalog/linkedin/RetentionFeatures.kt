package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==================== RETENTION VIEWMODEL ====================

private const val STAY_ACTIVE_BANNER_DISMISS_WINDOW_MILLIS = 7L * 24L * 60L * 60L * 1000L

// Mock data for when backend is unavailable
private val mockWeeklyGoals = WeeklyGoalsData(
    goals = listOf(
        WeeklyGoal(id = "1", type = "connections", label = "Connections", current = 1, target = 10, isComplete = false),
        WeeklyGoal(id = "2", type = "posts", label = "Posts", current = 9, target = 3, isComplete = true)
    ),
    totalProgress = 0.5f,
    weekStartDate = "2026-03-02",
    weekEndDate = "2026-03-08",
    streakAtRisk = true,
    reminderMessage = "You're 1/10 on weekly connections. 9 more to go!"
)

private val mockLeaderboard = LeaderboardData(
    users = listOf(
        LeaderboardUser(rank = 1, userId = "1", name = "YASMANTH VEMALA", profileImage = null, score = 7, connectionsThisPeriod = 7, isCurrentUser = true),
        LeaderboardUser(rank = 2, userId = "2", name = "Medam Srinivas", headline = "IIT Kharagpur", profileImage = null, score = 3, connectionsThisPeriod = 3),
        LeaderboardUser(rank = 3, userId = "3", name = "Experimentingtech", profileImage = null, score = 3, connectionsThisPeriod = 3)
    ),
    period = "week",
    currentUserRank = 1,
    totalParticipants = 15
)

private val mockLiveActivity = LiveActivityData(
    activeNow = 12,
    location = "Worldwide",
    label = "people networking",
    recentJoins = listOf(
        RecentJoinUser(id = "1", name = "Emma", profileImage = null, joinedAt = "2026-03-07T10:30:00Z"),
        RecentJoinUser(id = "2", name = "John", profileImage = null, joinedAt = "2026-03-07T10:25:00Z")
    )
)

private val mockConnectionLimit = ConnectionLimitData(
    used = 3,
    limit = 10,
    remaining = 7,
    isPremium = false,
    resetsAt = "2026-03-08T00:00:00Z",
    unlimitedRequests = false
)

private val mockStreakData = StreakData(
    connectionStreak = 2,
    longestConnectionStreak = 14,
    loginStreak = 5,
    longestLoginStreak = 30,
    postingStreak = 1,
    longestPostingStreak = 7,
    messagingStreak = 0,
    longestMessagingStreak = 5,
    overallBestStreak = 30,
    weeklyConnectionsMade = 1,
    weeklyConnectionsGoal = 10,
    streakFreezes = 2,
    streakShieldActive = false,
    totalFreezesUsed = 1,
    isAtRisk = StreakIsAtRisk(
        connection = true,
        login = false,
        posting = false,
        messaging = true
    ),
    engagementScore = 85,
    showOnProfile = true
)

private val mockPeopleLikeYou = listOf(
    DailyMatchUser(id = "1", name = "yasmanth", profileImage = null, headline = null, college = null, isOnline = true, replyRate = 85),
    DailyMatchUser(id = "2", name = "Sainandu Gadila", profileImage = null, headline = null, college = null, isOnline = false, replyRate = 78),
    DailyMatchUser(id = "3", name = "nfx share", profileImage = null, headline = "IIT BHU", college = "IIT BHU", isOnline = true, replyRate = 92),
    DailyMatchUser(id = "4", name = "Manideep Kurap...", profileImage = null, headline = "NIT", college = "NIT", isOnline = false, replyRate = 75),
    DailyMatchUser(id = "5", name = "Unknown Gost", profileImage = null, headline = "IIT", college = "IIT", isOnline = false, replyRate = 80),
    DailyMatchUser(id = "6", name = "Deepak Puram", profileImage = null, headline = null, college = null, isOnline = true, replyRate = 88),
    DailyMatchUser(id = "7", name = "Bonamukkala C...", profileImage = null, headline = null, college = null, isOnline = false, replyRate = 72)
)

private val mockTodaysMatches = listOf(
    DailyMatchUser(id = "m1", name = "Divya Chandra", profileImage = null, headline = "Data Science Intern @ Big Corp", college = "BITS Pilani", isOnline = false, replyRate = 85),
    DailyMatchUser(id = "m2", name = "Ishaan Chauhan", profileImage = null, headline = "Product Builder | Hackathon Win...", college = "IIT Delhi", isOnline = true, replyRate = 88),
    DailyMatchUser(id = "m3", name = "Tejas Das", profileImage = null, headline = "Open to collaborations!", college = "BITS Pilani", isOnline = false, replyRate = 86),
    DailyMatchUser(id = "m4", name = "Nitin Tandon", profileImage = null, headline = "CS Student | Building side projects", college = "VIT Vellore", isOnline = false, replyRate = 76)
)

data class RetentionUiState(
    val isLoading: Boolean = false,
    // Weekly Goals (Zeigarnik Effect)
    val weeklyGoals: WeeklyGoalsData = WeeklyGoalsData(),
    // Leaderboard (Social Proof)
    val leaderboardData: LeaderboardData = LeaderboardData(),
    val leaderboardPeriod: String = "week",
    // Live Activity (FOMO)
    val liveActivity: LiveActivityData = LiveActivityData(),
    // Session Summary (Peak-End)
    val sessionSummary: SessionSummaryData = SessionSummaryData(),
    // Connection Limit (Scarcity)
    val connectionLimit: ConnectionLimitData = ConnectionLimitData(),
    // Streaks
    val streakData: StreakData = StreakData(),
    // Celebration
    val showCelebration: Boolean = false,
    val celebrationData: ConnectionCelebrationData = ConnectionCelebrationData(),
    // People recommendations (Social Proof + Variable Rewards)
    val peopleLikeYou: List<DailyMatchUser> = emptyList(),
    val todaysMatches: List<DailyMatchUser> = emptyList()
)

class RetentionViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RetentionUiState())
    val uiState: StateFlow<RetentionUiState> = _uiState.asStateFlow()

    private val cacheTtlMillis = 5 * 60 * 1000L
    private var lastLoadedAtMillis = 0L
    private var isRequestInFlight = false
    
    // 12-hour seed that changes twice a day for variety in recommendations
    private val twelveHourSeed: Long
        get() = System.currentTimeMillis() / (12 * 60 * 60 * 1000) // Changes every 12 hours
    
    // Shuffle people and matches based on 12-hour seed for variety
    private fun getShuffledPeopleLikeYou(): List<DailyMatchUser> {
        val random = java.util.Random(twelveHourSeed)
        return mockPeopleLikeYou.shuffled(random).take(5) // Show 5 different people each 12-hour period
    }
    
    private fun getShuffledTodaysMatches(): List<DailyMatchUser> {
        val random = java.util.Random(twelveHourSeed + 1) // Different seed for variety
        return mockTodaysMatches.shuffled(random)
    }

    fun ensureRetentionLoaded(forceRefresh: Boolean = false) {
        loadAllRetentionData(forceRefresh = forceRefresh)
    }

    fun loadAllRetentionData(forceRefresh: Boolean = false) {
        if (!forceRefresh && hasFreshCache()) return
        if (isRequestInFlight) return

        viewModelScope.launch {
            isRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Load all data in parallel
            val streaksResult = ApiClient.getStreaks(context)
            val goalsResult = ApiClient.getWeeklyGoals(context)
            val activityResult = ApiClient.getLiveActivity(context)
            val limitResult = ApiClient.getConnectionLimit(context)
            val leaderboardResult = ApiClient.getLeaderboard(context, "week")
            val peopleLikeYouResult = ApiClient.getPeopleLikeYou(context)
            val dailyMatchesResult = ApiClient.getDailyMatches(context)
            
            // Use API data with mock fallbacks
            lastLoadedAtMillis = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                streakData = streaksResult.getOrDefault(mockStreakData),
                weeklyGoals = goalsResult.getOrDefault(mockWeeklyGoals),
                liveActivity = activityResult.getOrDefault(mockLiveActivity),
                connectionLimit = limitResult.getOrDefault(mockConnectionLimit),
                leaderboardData = leaderboardResult.getOrDefault(mockLeaderboard),
                // Use real API data with shuffled mock fallback
                peopleLikeYou = peopleLikeYouResult.getOrNull()?.people ?: getShuffledPeopleLikeYou(),
                todaysMatches = dailyMatchesResult.getOrNull()?.matches ?: getShuffledTodaysMatches()
            )
            isRequestInFlight = false
        }
    }
    
    fun loadLeaderboard(period: String = "week") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(leaderboardPeriod = period)
            ApiClient.getLeaderboard(context, period)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(leaderboardData = data)
                }
        }
    }
    
    fun loadSessionSummary() {
        viewModelScope.launch {
            ApiClient.getSessionSummary(context)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(sessionSummary = data)
                }
        }
    }
    
    fun showConnectionCelebration(connectionId: String) {
        viewModelScope.launch {
            ApiClient.getConnectionCelebration(context, connectionId)
                .onSuccess { data ->
                    if (data.showCelebration) {
                        _uiState.value = _uiState.value.copy(
                            showCelebration = true,
                            celebrationData = data
                        )
                    }
                }
        }
    }
    
    fun dismissCelebration() {
        _uiState.value = _uiState.value.copy(showCelebration = false)
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RetentionViewModel(context) as T
        }
    }

    private fun hasFreshCache(): Boolean {
        return lastLoadedAtMillis != 0L && System.currentTimeMillis() - lastLoadedAtMillis < cacheTtlMillis
    }
}

// ==================== WEEKLY GOALS (ZEIGARNIK EFFECT) ====================

@Composable
fun WeeklyGoalsCard(
    goalsData: WeeklyGoalsData,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateToDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalProgress = goalsData.totalProgress
    
    Box(
        modifier
            .fillMaxWidth()
            .retentionCard(contentColor, 20.dp)
            .clickable { onNavigateToDetails() }
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetentionIconBadge(
                        icon = Icons.Outlined.TrackChanges,
                        tint = accentColor,
                        modifier = Modifier.size(34.dp)
                    )
                    BasicText(
                        "Weekly Goals",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                // Progress percentage badge
                Box(
                    Modifier
                        .clip(Capsule())
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    BasicText(
                        "${(totalProgress * 100).toInt()}% complete",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            
            // Progress bars for each goal
            goalsData.goals.forEach { goal ->
                GoalProgressRow(
                    goal = goal,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
            
            // Reminder message if streak at risk
            if (goalsData.streakAtRisk && goalsData.reminderMessage.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B6B).copy(alpha = 0.15f))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                        BasicText(
                            goalsData.reminderMessage,
                            style = TextStyle(
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
            
            // Details button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier.clickable { onNavigateToDetails() },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "View details",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressRow(
    goal: WeeklyGoal,
    contentColor: Color,
    accentColor: Color
) {
    val progress = if (goal.target > 0) goal.current.toFloat() / goal.target else 0f
    val progressColor = when {
        goal.isComplete -> Color(0xFF4CAF50) // Green
        progress >= 0.7f -> accentColor
        progress >= 0.3f -> Color(0xFFFFBB33) // Orange
        else -> Color(0xFFFF6B6B) // Red
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(
                goal.label,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            )
            BasicText(
                "${goal.current}/${goal.target}",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        
        // Progress bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(Capsule())
                .background(contentColor.copy(alpha = 0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(Capsule())
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                progressColor.copy(alpha = 0.7f),
                                progressColor
                            )
                        )
                    )
            )
        }
    }
}

// ==================== WEEKLY GOALS DETAIL SCREEN ====================

private fun Modifier.retentionCard(
    contentColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp
): Modifier {
    val isDarkSurface = contentColor == Color.White
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = if (isDarkSurface) {
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.07f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.18f)
                    )
                }
            )
        )
        .border(
            1.dp,
            if (isDarkSurface) Color.White.copy(alpha = 0.14f)
            else Color.White.copy(alpha = 0.42f),
            shape
        )
}

@Composable
private fun RetentionHeader(
    title: String,
    contentColor: Color,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.10f))
                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun RetentionIconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun WeeklyGoalsDetailScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onNavigateToFindPeople: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val state by viewModel.uiState.collectAsState()
    val goalsData = state.weeklyGoals
    
    LaunchedEffect(Unit) {
        viewModel.ensureRetentionLoaded()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RetentionHeader(
            title = "Weekly Goals",
            contentColor = contentColor,
            onNavigateBack = onNavigateBack
        )
        
        // Overall progress circle
        Box(
            Modifier
                .fillMaxWidth()
                .retentionCard(contentColor, 24.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular progress
                CircularProgressIndicatorWithText(
                    progress = goalsData.totalProgress,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    size = 140.dp
                )
                
                BasicText(
                    "This Week's Progress",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                )
                
                if (goalsData.weekStartDate.isNotEmpty()) {
                    BasicText(
                        "${goalsData.weekStartDate} - ${goalsData.weekEndDate}",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
        
        // Individual goals
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(goalsData.goals) { goal ->
                GoalDetailCard(
                    goal = goal,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onActionClick = {
                        if (goal.type == "connections") {
                            onNavigateToFindPeople()
                        }
                    }
                )
            }
            
            // Motivation card
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .retentionCard(contentColor, 16.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RetentionIconBadge(
                                icon = Icons.Outlined.CheckCircle,
                                tint = accentColor,
                                modifier = Modifier.size(44.dp)
                            )
                            BasicText(
                                "Keep going! You're building great networking habits.",
                                style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularProgressIndicatorWithText(
    progress: Float,
    contentColor: Color,
    accentColor: Color,
    size: androidx.compose.ui.unit.Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Box(
        Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2
            val center = Offset(this.size.width / 2, this.size.height / 2)
            
            // Background circle
            drawCircle(
                color = contentColor.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(accentColor.copy(alpha = 0.5f), accentColor)
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "${(animatedProgress * 100).toInt()}%",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun GoalDetailCard(
    goal: WeeklyGoal,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onActionClick: () -> Unit
) {
    val progress = if (goal.target > 0) goal.current.toFloat() / goal.target else 0f
    val remaining = goal.target - goal.current
    val icon = when (goal.type) {
        "connections" -> Icons.Outlined.PersonAddAlt1
        "posts" -> Icons.Outlined.Create
        "messages" -> Icons.Outlined.ChatBubbleOutline
        else -> Icons.Outlined.CheckCircle
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .retentionCard(contentColor, 16.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetentionIconBadge(icon = icon, tint = accentColor, modifier = Modifier.size(36.dp))
                    BasicText(
                        goal.label,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    
                    if (goal.isComplete) {
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "✓ Done",
                                style = TextStyle(
                                    color = Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                
                // Progress bar
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(Capsule())
                        .background(contentColor.copy(alpha = 0.1f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(Capsule())
                            .background(if (goal.isComplete) Color(0xFF4CAF50) else accentColor)
                    )
                }
                
                BasicText(
                    if (goal.isComplete) "Goal complete." else "$remaining more to go",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Progress circle + action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = contentColor.copy(alpha = 0.1f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = if (goal.isComplete) Color(0xFF4CAF50) else accentColor,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    BasicText(
                        "${goal.current}",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                if (!goal.isComplete && goal.type == "connections") {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(accentColor)
                            .clickable { onActionClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicText(
                            "Connect",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== STREAK DETAILS SCREEN ====================

@Composable
fun StreakDetailsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val state by viewModel.uiState.collectAsState()
    val streaks = state.streakData
    
    LaunchedEffect(Unit) {
        viewModel.ensureRetentionLoaded()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RetentionHeader(
            title = "Your Streaks",
            contentColor = contentColor,
            onNavigateBack = onNavigateBack
        )
        
        // Main streak display
        Box(
            Modifier
                .fillMaxWidth()
                .retentionCard(contentColor, 24.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetentionIconBadge(
                    icon = Icons.Outlined.LocalFireDepartment,
                    tint = accentColor,
                    modifier = Modifier.size(52.dp)
                )
                BasicText(
                    "${streaks.connectionStreak}-day",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                BasicText(
                    "Networking Streak",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                )
                
                if (streaks.longestConnectionStreak > streaks.connectionStreak) {
                    BasicText(
                        "Best: ${streaks.longestConnectionStreak} days",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
        
        // 4 Streak bars (like Duolingo)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StreakDetailBar(
                    label = "Networking",
                    icon = Icons.Outlined.PersonAddAlt1,
                    currentStreak = streaks.connectionStreak,
                    longestStreak = streaks.longestConnectionStreak,
                    isAtRisk = streaks.isAtRisk.connection,
                    color = Color(0xFFFF9800),
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            item {
                StreakDetailBar(
                    label = "Login",
                    icon = Icons.Outlined.Schedule,
                    currentStreak = streaks.loginStreak,
                    longestStreak = streaks.longestLoginStreak,
                    isAtRisk = streaks.isAtRisk.login,
                    color = Color(0xFF2196F3),
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            item {
                StreakDetailBar(
                    label = "Posting",
                    icon = Icons.Outlined.Create,
                    currentStreak = streaks.postingStreak,
                    longestStreak = streaks.longestPostingStreak,
                    isAtRisk = streaks.isAtRisk.posting,
                    color = Color(0xFF9C27B0),
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            item {
                StreakDetailBar(
                    label = "Messaging",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    currentStreak = streaks.messagingStreak,
                    longestStreak = streaks.longestMessagingStreak,
                    isAtRisk = streaks.isAtRisk.messaging,
                    color = Color(0xFF4CAF50),
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            
            // Streak freeze info
            if (streaks.streakFreezes > 0) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .retentionCard(contentColor, 12.dp)
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            BasicText(
                                "${streaks.streakFreezes} Streak Freeze${if (streaks.streakFreezes > 1) "s" else ""} available",
                                style = TextStyle(
                                    color = contentColor,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakDetailBar(
    label: String,
    icon: ImageVector,
    currentStreak: Int,
    longestStreak: Int,
    isAtRisk: Boolean,
    color: Color,
    backdrop: LayerBackdrop,
    contentColor: Color
) {
    val progress = if (longestStreak > 0) currentStreak.toFloat() / longestStreak else 0f
    
    Box(
        Modifier
            .fillMaxWidth()
            .retentionCard(contentColor, 16.dp)
            .then(
                if (isAtRisk) Modifier.border(
                    width = 1.dp,
                    color = Color(0xFFFF8A80),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetentionIconBadge(icon = icon, tint = color, modifier = Modifier.size(36.dp))
                    BasicText(
                        label,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    if (isAtRisk) {
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(Color(0xFFFF8A80).copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "AT RISK",
                                style = TextStyle(
                                    color = Color(0xFFFF8A80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    BasicText(
                        "${currentStreak}d",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    BasicText(
                        "Best: ${longestStreak}d",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
            
            // Progress bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(Capsule())
                    .background(contentColor.copy(alpha = 0.1f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(Capsule())
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.7f),
                                    color
                                )
                            )
                        )
                )
            }
        }
    }
}

// ==================== TOP NETWORKERS LEADERBOARD ====================

@Composable
fun TopNetworkersScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val state by viewModel.uiState.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf("week") }
    
    LaunchedEffect(selectedPeriod) {
        viewModel.loadLeaderboard(selectedPeriod)
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.padding(horizontal = 16.dp)) {
            RetentionHeader(
                title = "Top Networkers",
                contentColor = contentColor,
                onNavigateBack = onNavigateBack
            )
        }
        
        // Period toggle (Week/Month)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .retentionCard(contentColor, 18.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("week" to "This Week", "month" to "This Month").forEach { (period, label) ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(Capsule())
                        .background(if (selectedPeriod == period) accentColor else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        label,
                        style = TextStyle(
                            color = if (selectedPeriod == period) Color.White else contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        // Leaderboard list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top 3 podium
            if (state.leaderboardData.users.size >= 3) {
                item {
                    TopThreePodium(
                        users = state.leaderboardData.users.take(3),
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onProfileClick = onNavigateToProfile
                    )
                }
            }
            
            // Rest of the list
            itemsIndexed(
                state.leaderboardData.users.drop(3)
            ) { index, user ->
                LeaderboardUserRow(
                    user = user,
                    rank = index + 4,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onProfileClick = { onNavigateToProfile(user.userId) }
                )
            }
            
            // Current user position (if not in top list)
            state.leaderboardData.currentUserRank?.let { rank ->
                if (rank > 10) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .retentionCard(contentColor, 12.dp)
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RetentionIconBadge(
                                    icon = Icons.Outlined.EmojiEvents,
                                    tint = accentColor,
                                    modifier = Modifier.size(34.dp)
                                )
                                BasicText(
                                    "You're ranked #$rank",
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopThreePodium(
    users: List<LeaderboardUser>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onProfileClick: (String) -> Unit
) {
    if (users.size < 3) return
    
    val first = users[0]
    val second = users[1]
    val third = users[2]
    
    Box(
        Modifier
            .fillMaxWidth()
            .retentionCard(contentColor, 20.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Second place (left)
            PodiumUser(
                user = second,
                rank = 2,
                color = Color(0xFFC0C0C0),
                height = 80.dp,
                contentColor = contentColor,
                onProfileClick = { onProfileClick(second.userId) }
            )
            
            // First place (center, tallest)
            PodiumUser(
                user = first,
                rank = 1,
                color = Color(0xFFFFD700),
                height = 100.dp,
                contentColor = contentColor,
                onProfileClick = { onProfileClick(first.userId) }
            )
            
            // Third place (right)
            PodiumUser(
                user = third,
                rank = 3,
                color = Color(0xFFCD7F32),
                height = 60.dp,
                contentColor = contentColor,
                onProfileClick = { onProfileClick(third.userId) }
            )
        }
    }
}

@Composable
private fun PodiumUser(
    user: LeaderboardUser,
    rank: Int,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    contentColor: Color,
    onProfileClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onProfileClick() }
    ) {
        RetentionIconBadge(
            icon = Icons.Outlined.EmojiEvents,
            tint = color,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(4.dp))
        BasicText(
            "#$rank",
            style = TextStyle(
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
        
        // Avatar
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(3.dp, color, CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(user.profileImage ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = user.name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(Modifier.height(4.dp))
        
        // Name
        BasicText(
            user.name ?: user.username ?: "User",
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
        
        // Score
        BasicText(
            "${user.score} pts",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        )
        
        // Stand
        Box(
            Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun LeaderboardUserRow(
    user: LeaderboardUser,
    rank: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onProfileClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .retentionCard(
                contentColor = contentColor,
                cornerRadius = 12.dp
            )
            .clickable { onProfileClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank
                BasicText(
                    "#$rank",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.width(32.dp)
                )
                
                // Avatar
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .then(
                            if (user.isCurrentUser) Modifier.border(2.dp, accentColor, CircleShape)
                            else Modifier
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.profileImage ?: "")
                            .crossfade(true)
                            .build(),
                        contentDescription = user.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Info
                Column {
                    BasicText(
                        user.name ?: user.username ?: "User",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    if (user.headline != null) {
                        BasicText(
                            user.headline,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Score
            Column(horizontalAlignment = Alignment.End) {
                BasicText(
                    "${user.score}",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                BasicText(
                    "points",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// ==================== LIVE ACTIVITY BANNER ====================

@Composable
fun LiveActivityBanner(
    activityData: LiveActivityData,
    backdrop: LayerBackdrop,
    contentColor: Color,
    isGlassTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (activityData.activeNow <= 0) return
    
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (isGlassTheme) Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(12f.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color(0xFF4CAF50).copy(alpha = 0.15f))
                    }
                ) else Modifier.background(Color(0xFF4CAF50).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing dot
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50).copy(alpha = pulseAlpha))
            )
            
            BasicText(
                "${activityData.activeNow} ${activityData.label}",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            
            activityData.location?.let { location ->
                BasicText(
                    location,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

// ==================== SESSION SUMMARY (ON EXIT) ====================

@Composable
fun SessionSummaryOverlay(
    isVisible: Boolean,
    sessionData: SessionSummaryData,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .padding(32.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            vibrancy()
                            blur(24f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.95f))
                        }
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BasicText("🚀", style = TextStyle(fontSize = 48.sp))
                    
                    BasicText(
                        "See you tomorrow!",
                        style = TextStyle(
                            color = Color(0xFF1a1a2e),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    if (sessionData.connectionsMadeToday > 0) {
                        BasicText(
                            "You made ${sessionData.connectionsMadeToday} new connection${if (sessionData.connectionsMadeToday > 1) "s" else ""} today! 🎉",
                            style = TextStyle(
                                color = Color(0xFF1a1a2e).copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    
                    if (sessionData.streakPreserved) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText("🔥", style = TextStyle(fontSize = 20.sp))
                            BasicText(
                                "${sessionData.currentStreak}-day streak preserved!",
                                style = TextStyle(
                                    color = Color(0xFFFF9800),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    
                    if (sessionData.motivationalMessage.isNotEmpty()) {
                        BasicText(
                            sessionData.motivationalMessage,
                            style = TextStyle(
                                color = Color(0xFF1a1a2e).copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(accentColor)
                            .clickable { onDismiss() }
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            "Got it!",
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
}

// ==================== CONNECTION ACCEPTED CELEBRATION ====================

@Composable
fun ConnectionAcceptedCelebration(
    isVisible: Boolean,
    celebrationData: ConnectionCelebrationData,
    backdrop: LayerBackdrop,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onSendMessage: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Confetti
            ConfettiOverlay(
                isVisible = isVisible,
                particleCount = 100
            )
            
            // Celebration card
            Box(
                Modifier
                    .padding(32.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(28f.dp) },
                        effects = {
                            vibrancy()
                            blur(24f.dp.toPx())
                            lens(12f.dp.toPx(), 24f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.95f))
                        }
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success animation
                    val infiniteTransition = rememberInfiniteTransition(label = "celebrate")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    BasicText(
                        "🎉",
                        style = TextStyle(fontSize = 56.sp),
                        modifier = Modifier.scale(scale)
                    )
                    
                    BasicText(
                        "You're Connected!",
                        style = TextStyle(
                            color = Color(0xFF1a1a2e),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    // Connected user
                    celebrationData.connectedUser?.let { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(
                                        3.dp,
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                                        ),
                                        CircleShape
                                    )
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(user.profileImage ?: "")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = user.name,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            BasicText(
                                user.name ?: "New connection",
                                style = TextStyle(
                                    color = Color(0xFF1a1a2e),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            
                            user.headline?.let {
                                BasicText(
                                    it,
                                    style = TextStyle(
                                        color = Color(0xFF1a1a2e).copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    // Milestone badge
                    if (celebrationData.milestoneReached) {
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "🏆 ${celebrationData.milestoneType} Connection!",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    // XP earned
                    if (celebrationData.xpEarned > 0) {
                        BasicText(
                            "+${celebrationData.xpEarned} XP",
                            style = TextStyle(
                                color = Color(0xFF6C5CE7),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(Color(0xFF1a1a2e).copy(alpha = 0.1f))
                                .clickable { onViewProfile() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            BasicText(
                                "View Profile",
                                style = TextStyle(
                                    color = Color(0xFF1a1a2e),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0A66C2), Color(0xFF1976D2))
                                    )
                                )
                                .clickable { onSendMessage() }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            BasicText(
                                "Send Message",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== CONNECTION LIMIT INDICATOR ====================

@Composable
fun ConnectionLimitIndicator(
    limitData: ConnectionLimitData,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (limitData.unlimitedRequests) return
    
    val progress = if (limitData.limit > 0) limitData.used.toFloat() / limitData.limit else 0f
    val isLow = limitData.remaining <= 3
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Progress bar
        Box(
            Modifier
                .width(60.dp)
                .height(6.dp)
                .clip(Capsule())
                .background(contentColor.copy(alpha = 0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(Capsule())
                    .background(if (isLow) Color(0xFFFF6B6B) else accentColor)
            )
        }
        
        BasicText(
            "${limitData.remaining}/${limitData.limit} left",
            style = TextStyle(
                color = if (isLow) Color(0xFFFF6B6B) else contentColor.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        )
    }
}

// ==================== MATCH EXPIRY COUNTDOWN ====================

@Composable
fun MatchExpiryCountdown(
    expiresAt: String,
    hoursRemaining: Int,
    minutesRemaining: Int,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val isUrgent = hoursRemaining < 6
    val color = if (isUrgent) Color(0xFFFF6B6B) else contentColor.copy(alpha = 0.6f)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        BasicText(
            if (hoursRemaining > 0) "Expires in ${hoursRemaining}h" else "Expires in ${minutesRemaining}m",
            style = TextStyle(
                color = color,
                fontSize = 11.sp,
                fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

// ==================== HOME FEED COMPONENTS ====================

/**
 * Stay Active Banner - Like web's "Stay active – check your feed and connect with someone today"
 * Shows live activity count and encourages engagement
 */
@Composable
fun StayActiveBanner(
    liveActivity: LiveActivityData,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onViewFeed: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dismissedAt by SettingsPreferences.stayActiveBannerDismissedAt(context).collectAsState(initial = 0L)
    val isVisible = dismissedAt == 0L ||
        System.currentTimeMillis() - dismissedAt >= STAY_ACTIVE_BANNER_DISMISS_WINDOW_MILLIS

    if (!isVisible) return

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .retentionCard(
                contentColor = contentColor,
                cornerRadius = 16.dp
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RetentionIconBadge(
                    icon = Icons.Outlined.LocalFireDepartment,
                    tint = accentColor,
                    modifier = Modifier.size(34.dp)
                )

                Column {
                    BasicText(
                        "Stay active – connect with someone today",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (liveActivity.activeNow > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            BasicText(
                                "${liveActivity.activeNow} people active now",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor)
                        .clickable { onConnect() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    BasicText(
                        "Connect",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable {
                            scope.launch {
                                SettingsPreferences.setStayActiveBannerDismissedAt(
                                    context,
                                    System.currentTimeMillis()
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "×",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Engagement Dashboard Card - Combined Weekly Goals + Streak progress
 * Like web's sidebar with goals and streak bars
 */
@Composable
fun EngagementDashboardCard(
    weeklyGoals: WeeklyGoalsData,
    streakData: StreakData,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onWeeklyGoalsClick: () -> Unit,
    onStreakDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .retentionCard(contentColor, 20.dp)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Goals Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWeeklyGoalsClick() },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RetentionIconBadge(
                            icon = Icons.Outlined.TrackChanges,
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                        BasicText(
                            "Weekly Goals",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    BasicText(
                        "›",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 18.sp
                        )
                    )
                }
                
                // Goals progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    weeklyGoals.goals.take(3).forEach { goal ->
                        GoalProgressMini(
                            goal = goal,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Progress message
                val connectionsGoal = weeklyGoals.goals.find { it.type == "connections" }
                connectionsGoal?.let { goal ->
                    if (goal.current < goal.target) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAddAlt1,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            BasicText(
                                "You're ${goal.current}/${goal.target} on weekly connections. ${goal.target - goal.current} more to go!",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            )
                        }
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
            
            // Streak Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStreakDetailsClick() },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compute current best streak and streaks at risk
                val currentStreak = maxOf(
                    streakData.connectionStreak,
                    streakData.loginStreak,
                    streakData.postingStreak,
                    streakData.messagingStreak
                )
                val streaksAtRisk = listOf(
                    streakData.isAtRisk.connection,
                    streakData.isAtRisk.login,
                    streakData.isAtRisk.posting,
                    streakData.isAtRisk.messaging
                ).count { it }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "streak_fire")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "fire"
                        )
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size((18 * scale).dp)
                        )
                        BasicText(
                            "${currentStreak}-day streak!",
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (streaksAtRisk > 0) {
                            BasicText(
                                "$streaksAtRisk at risk",
                                style = TextStyle(
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        BasicText(
                            "Details ›",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                // Streak bars
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StreakBarMini(
                        label = "Networking",
                        current = streakData.connectionStreak,
                        isAtRisk = streakData.isAtRisk.connection,
                        contentColor = contentColor,
                        accentColor = Color(0xFF3B82F6)
                    )
                    StreakBarMini(
                        label = "Login",
                        current = streakData.loginStreak,
                        isAtRisk = streakData.isAtRisk.login,
                        contentColor = contentColor,
                        accentColor = Color(0xFF22C55E)
                    )
                    StreakBarMini(
                        label = "Posting",
                        current = streakData.postingStreak,
                        isAtRisk = streakData.isAtRisk.posting,
                        contentColor = contentColor,
                        accentColor = Color(0xFF8B5CF6)
                    )
                    StreakBarMini(
                        label = "Messaging",
                        current = streakData.messagingStreak,
                        isAtRisk = streakData.isAtRisk.messaging,
                        contentColor = contentColor,
                        accentColor = Color(0xFFFF6B35)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressMini(
    goal: WeeklyGoal,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (goal.current.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f)
    val isComplete = goal.current >= goal.target
    val color = if (isComplete) Color(0xFF22C55E) else accentColor
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val icon = when (goal.type) {
            "connections" -> Icons.Outlined.PersonAddAlt1
            "posts" -> Icons.Outlined.Create
            "messages" -> Icons.Outlined.ChatBubbleOutline
            "comments" -> Icons.Outlined.ModeComment
            else -> Icons.Outlined.CheckCircle
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        // Progress text
        BasicText(
            "${goal.current}/${goal.target}",
            style = TextStyle(
                color = if (isComplete) Color(0xFF22C55E) else contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        
        // Progress bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(contentColor.copy(alpha = 0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(color)
            )
        }
    }
}

@Composable
private fun StreakBarMini(
    label: String,
    current: Int,
    isAtRisk: Boolean,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val maxDays = 7 // Show progress towards weekly
    val progress = (current.toFloat() / maxDays.toFloat()).coerceIn(0f, 1f)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp
            ),
            modifier = Modifier.width(70.dp)
        )
        
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(contentColor.copy(alpha = 0.1f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        if (isAtRisk) Color(0xFFFF6B6B) else accentColor
                    )
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAtRisk) {
                BasicText(
                    "At risk",
                    style = TextStyle(
                        color = Color(0xFFFF6B6B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            BasicText(
                "${current}d",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

/**
 * Top Networkers Preview - Compact leaderboard for home feed
 * Like web's "Top Networkers" sidebar section
 */
@Composable
fun TopNetworkersPreview(
    leaderboard: LeaderboardData,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .retentionCard(contentColor, 20.dp)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetentionIconBadge(
                        icon = Icons.Outlined.EmojiEvents,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    BasicText(
                        "Top Networkers",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeeAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    BasicText(
                        "See all ›",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            // Top 3 users
            leaderboard.users.take(3).forEachIndexed { index, user ->
                LeaderboardRowMini(
                    rank = index + 1,
                    user = user,
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRowMini(
    rank: Int,
    user: LeaderboardUser,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank"
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        BasicText(
            medalEmoji,
            style = TextStyle(
                fontSize = if (rank <= 3) 18.sp else 14.sp
            )
        )
        
        // Avatar
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            if (user.profileImage?.isNotEmpty() == true) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                BasicText(
                    (user.name ?: user.username ?: "?").firstOrNull()?.uppercase() ?: "?",
                    style = TextStyle(Color.White, 12.sp, FontWeight.Bold)
                )
            }
        }
        
        // Name
        Column(
            modifier = Modifier.weight(1f)
        ) {
            BasicText(
                user.name ?: user.username ?: "User",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            user.headline?.let { headline ->
                BasicText(
                    headline,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Score
        BasicText(
            "${user.connectionsThisPeriod}",
            style = TextStyle(
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// ==================== PEOPLE LIKE YOU SECTION (Social Proof) ====================

/**
 * Horizontal scrollable row of people similar to the user
 * Shows shared interests, same goals, mutual connections
 */
@Composable
fun PeopleLikeYouSection(
    people: List<DailyMatchUser>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onPersonClick: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("✨", style = TextStyle(fontSize = 16.sp))
                BasicText(
                    "People like you",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            BasicText(
                "See all ›",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
        
        // Horizontal scroll
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(people.take(8)) { person ->
                PeopleLikeYouCard(
                    person = person,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onPersonClick(person.id) }
                )
            }
        }
    }
}

@Composable
private fun PeopleLikeYouCard(
    person: DailyMatchUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use real matchReason from API, fallback to default
    val badge = person.matchReason ?: ""
    
    Box(
        modifier = modifier
            .width(100.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16f.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar with online indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.4f),
                                    accentColor.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!person.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(person.profileImage)
                                .crossfade(true)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_gallery)
                                .build(),
                            contentDescription = person.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initials = (person.name ?: "U")
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                        BasicText(
                            initials.ifEmpty { "U" },
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                // Online indicator
                if (person.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                    }
                }
            }
            
            // Name
            BasicText(
                person.name ?: "User",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            // College/headline
            val subtitle = person.college ?: person.headline
            if (!subtitle.isNullOrEmpty()) {
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Badge (Same goal, shared interest, etc.)
            if (badge.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        badge,
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

// ==================== TODAY'S MATCHES SECTION ====================

/**
 * Today's Matches - horizontal card showing daily algorithmic matches
 * Shows reply rate to encourage action
 */
@Composable
fun TodaysMatchesSection(
    matches: List<DailyMatchUser>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onMatchClick: (String) -> Unit,
    onConnect: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("✨", style = TextStyle(fontSize = 16.sp))
                BasicText(
                    "Today's Matches",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                // Match count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        "${matches.size} people",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            BasicText(
                "See all ›",
                style = TextStyle(
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
        
        // Match cards - horizontal scroll
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(matches.take(4)) { match ->
                TodaysMatchCard(
                    match = match,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { onMatchClick(match.id) },
                    onConnect = { onConnect(match.id) }
                )
            }
        }
    }
}

@Composable
private fun TodaysMatchCard(
    match: DailyMatchUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(160.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(18f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.12f))
                }
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar with online indicator
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.4f),
                                    accentColor.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!match.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(match.profileImage)
                                .crossfade(true)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_gallery)
                                .build(),
                            contentDescription = match.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initials = (match.name ?: "U")
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                        BasicText(
                            initials.ifEmpty { "U" },
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                // Online indicator
                if (match.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                    }
                }
            }
            
            // Name
            BasicText(
                match.name ?: "User",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            // College or Headline
            val subtitle = match.college ?: match.headline
            if (!subtitle.isNullOrEmpty()) {
                BasicText(
                    subtitle,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Headline (if college shown above)
            if (match.college != null && !match.headline.isNullOrEmpty()) {
                BasicText(
                    match.headline,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Reply rate badge
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(12.dp)
                )
                BasicText(
                    "${match.replyRate}% reply rate",
                    style = TextStyle(
                        color = Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Connect button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor)
                    .clickable { onConnect() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    BasicText(
                        "Connect",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
