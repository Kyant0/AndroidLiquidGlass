package com.kyant.backdrop.catalog.linkedin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.AccountabilityPartnerSummary
import com.kyant.backdrop.catalog.network.models.BadgeSummary
import com.kyant.backdrop.catalog.network.models.DailyChallengeSummary
import com.kyant.backdrop.catalog.network.models.DailyHookSummary
import com.kyant.backdrop.catalog.network.models.GrowthJob
import com.kyant.backdrop.catalog.network.models.InterviewCategorySummary
import com.kyant.backdrop.catalog.network.models.InterviewStatsSummary
import com.kyant.backdrop.catalog.network.models.LearningPathSummary
import com.kyant.backdrop.catalog.network.models.MentorshipSummary
import com.kyant.backdrop.catalog.network.models.ReferralShareLinks
import com.kyant.backdrop.catalog.network.models.ReferralStatsSummary
import com.kyant.backdrop.catalog.network.models.StoreItemSummary
import com.kyant.backdrop.catalog.network.models.ChallengeStatsSummary
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthHubScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onOpenHookAction: (DailyHookSummary) -> Unit = {},
    onOpenProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GrowthHubViewModel = viewModel(factory = GrowthHubViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val refreshState = rememberPullToRefreshState()
    var aiInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.checkInMessage) {
        uiState.checkInMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearCheckInMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SettingsHeader(
                title = "Growth Hub",
                contentColor = contentColor,
                onBack = onNavigateBack
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshAll() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && !hasGrowthContent(uiState) -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                GrowthHeroCard(
                                    uiState = uiState,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            }

                            item {
                                if (uiState.error != null) {
                                    GrowthInlineMessage(
                                        message = uiState.error ?: "Something went wrong",
                                        backdrop = backdrop,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onRetry = { viewModel.refreshAll(showLoader = true) }
                                    )
                                }
                            }

                            item {
                                DailyHooksCard(
                                    hooks = uiState.dailyHooks,
                                    date = uiState.hooksDate,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onOpenHookAction = onOpenHookAction
                                )
                            }

                            item {
                                AiCoachCard(
                                    messages = uiState.aiMessages.takeLast(4),
                                    isSending = uiState.isSendingAiMessage,
                                    aiError = uiState.aiError,
                                    input = aiInput,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onInputChange = { aiInput = it.take(280) },
                                    onSend = {
                                        val message = aiInput.trim()
                                        if (message.isNotEmpty()) {
                                            viewModel.sendCareerMessage(message)
                                            aiInput = ""
                                        }
                                    },
                                    onClearError = viewModel::clearAiError
                                )
                            }

                            item {
                                JobsCard(
                                    jobs = uiState.featuredJobs.take(3),
                                    jobTypes = uiState.jobTypes.take(6),
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            }

                            item {
                                LearningCard(
                                    paths = uiState.featuredPaths.take(3),
                                    categories = uiState.learningCategories.take(6),
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            }

                            item {
                                PracticeCard(
                                    challenge = uiState.dailyChallenge,
                                    categories = uiState.challengeCategories.take(6),
                                    stats = uiState.challengeStats,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            }

                            item {
                                InterviewsCard(
                                    categories = uiState.interviewCategories.take(4),
                                    stats = uiState.interviewStats,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            }

                            item {
                                RewardsCard(
                                    xpBalance = uiState.xpBalance,
                                    badges = uiState.badges.take(3),
                                    badgeCategories = uiState.badgeCategories.take(6),
                                    storeItems = uiState.storeItems.take(3),
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor
                                )
                            }

                            item {
                                ReferralCard(
                                    referralCode = uiState.referralCode,
                                    stats = uiState.referralStats,
                                    shareLinks = uiState.referralShareLinks,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onCopyCode = {
                                        val code = uiState.referralCode ?: uiState.referralShareLinks?.code
                                        if (!code.isNullOrBlank()) {
                                            copyText(context, "Referral code", code)
                                            Toast.makeText(context, "Referral code copied", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onShare = {
                                        val shareLink = uiState.referralShareLinks
                                        val message = buildShareMessage(shareLink, uiState.referralCode)
                                        if (message != null) {
                                            shareText(context, message)
                                        }
                                    }
                                )
                            }

                            item {
                                AccountabilityCard(
                                    partners = uiState.partners.take(3),
                                    mentorships = uiState.mentorships.take(3),
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onOpenProfile = onOpenProfile,
                                    onCheckIn = viewModel::checkIn
                                )
                            }

                            item {
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthHeroCard(
    uiState: GrowthHubUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor,
        tint = accentColor.copy(alpha = 0.08f)
    ) {
        BasicText(
            "Build your next move",
            style = TextStyle(
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(6.dp))
        BasicText(
            "Jobs, learning, interviews, rewards, and AI coaching in one calm place.",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GrowthStatChip(
                value = uiState.dailyHooks.size.toString(),
                label = "Today",
                contentColor = contentColor,
                accentColor = accentColor
            )
            GrowthStatChip(
                value = uiState.xpBalance.toString(),
                label = "XP",
                contentColor = contentColor,
                accentColor = accentColor
            )
            GrowthStatChip(
                value = (uiState.referralStats?.successfulReferrals ?: 0).toString(),
                label = "Referrals",
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun DailyHooksCard(
    hooks: List<DailyHookSummary>,
    date: String?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onOpenHookAction: (DailyHookSummary) -> Unit
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Daily hooks",
            subtitle = date?.let { "Personal nudges for $it" } ?: "Personal nudges to keep momentum.",
            contentColor = contentColor
        )
        Spacer(Modifier.height(12.dp))
        if (hooks.isEmpty()) {
            GrowthEmptyState(
                text = "You’re all set for now. New prompts will appear as your profile grows.",
                contentColor = contentColor
            )
        } else {
            hooks.take(4).forEach { hook ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            hook.emoji.ifBlank { "✨" },
                            style = TextStyle(fontSize = 20.sp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            hook.title,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(Modifier.height(2.dp))
                        BasicText(
                            hook.type.replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.48f),
                                fontSize = 11.sp
                            )
                        )
                    }
                    GrowthActionPill(
                        label = hook.action.label.ifBlank { "Open" },
                        accentColor = accentColor,
                        contentColor = contentColor,
                        onClick = { onOpenHookAction(hook) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiCoachCard(
    messages: List<GrowthHubChatMessage>,
    isSending: Boolean,
    aiError: String?,
    input: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearError: () -> Unit
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor,
        tint = Color.White.copy(alpha = 0.03f)
    ) {
        GrowthSectionTitle(
            title = "AI career coach",
            subtitle = "Ask for interview prep, a study plan, or networking advice.",
            contentColor = contentColor
        )
        Spacer(Modifier.height(12.dp))
        messages.forEach { message ->
            GrowthChatBubble(
                message = message,
                contentColor = contentColor,
                accentColor = accentColor
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        if (input.isBlank()) {
                            BasicText(
                                "What should I focus on this week?",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.45f),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )
                Spacer(Modifier.width(10.dp))
                GrowthActionPill(
                    label = if (isSending) "Sending" else "Send",
                    accentColor = accentColor,
                    contentColor = contentColor,
                    enabled = input.isNotBlank() && !isSending,
                    onClick = onSend
                )
            }
        }
        if (aiError != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = aiError,
                modifier = Modifier.clickable { onClearError() },
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.62f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun JobsCard(
    jobs: List<GrowthJob>,
    jobTypes: List<String>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Jobs radar",
            subtitle = "A quick read on the hiring side of the platform.",
            contentColor = contentColor
        )
        if (jobTypes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            GrowthTagRow(
                tags = jobTypes,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
        Spacer(Modifier.height(12.dp))
        if (jobs.isEmpty()) {
            GrowthEmptyState(
                text = "Featured roles are not populated yet, but the backend is ready for them.",
                contentColor = contentColor
            )
        } else {
            jobs.forEach { job ->
                GrowthListItemCard(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    emoji = "💼",
                    title = job.title,
                    subtitle = listOfNotNull(
                        job.company?.name,
                        job.location.takeIf { it.isNotBlank() },
                        job.type.takeIf { it.isNotBlank() }
                    ).joinToString(" • ").ifBlank { "Role preview" },
                    supporting = if (job.skills.isNotEmpty()) job.skills.take(3).joinToString(" • ") else job.experienceLevel
                )
            }
        }
    }
}

@Composable
private fun LearningCard(
    paths: List<LearningPathSummary>,
    categories: List<String>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Learning paths",
            subtitle = "Keep a shortlist of tracks worth adding when content fills out.",
            contentColor = contentColor
        )
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            GrowthTagRow(
                tags = categories,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
        Spacer(Modifier.height(12.dp))
        if (paths.isEmpty()) {
            GrowthEmptyState(
                text = "Featured learning paths are still quiet on the backend.",
                contentColor = contentColor
            )
        } else {
            paths.forEach { path ->
                GrowthListItemCard(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    emoji = "📚",
                    title = path.title,
                    subtitle = listOf(path.category, path.difficulty).filter { it.isNotBlank() }.joinToString(" • "),
                    supporting = "${path.estimatedHours} hrs • ${path.xpReward} XP"
                )
            }
        }
    }
}

@Composable
private fun PracticeCard(
    challenge: DailyChallengeSummary?,
    categories: List<String>,
    stats: ChallengeStatsSummary?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Practice and challenges",
            subtitle = "Daily reps, streaks, and challenge categories.",
            contentColor = contentColor
        )
        Spacer(Modifier.height(10.dp))
        challenge?.let {
            GrowthListItemCard(
                accentColor = accentColor,
                contentColor = contentColor,
                emoji = "⚡",
                title = it.title,
                subtitle = it.description,
                supporting = "${it.difficulty} • ${it.xpReward} XP"
            )
            Spacer(Modifier.height(10.dp))
        }
        if (stats != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrowthStatChip(
                    value = stats.totalSolved.toString(),
                    label = "Solved",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                GrowthStatChip(
                    value = stats.currentStreak.toString(),
                    label = "Streak",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                GrowthStatChip(
                    value = stats.rank?.toString() ?: "-",
                    label = "Rank",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            GrowthTagRow(
                tags = categories,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
        if (challenge == null && stats == null && categories.isEmpty()) {
            GrowthEmptyState(
                text = "This area is scaffolded, but there is not much live data yet.",
                contentColor = contentColor
            )
        }
    }
}

@Composable
private fun InterviewsCard(
    categories: List<InterviewCategorySummary>,
    stats: InterviewStatsSummary?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Interview prep",
            subtitle = "Categories and personal performance snapshots.",
            contentColor = contentColor
        )
        if (stats != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrowthStatChip(
                    value = stats.completedSessions.toString(),
                    label = "Completed",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                GrowthStatChip(
                    value = stats.averageScore.toString(),
                    label = "Avg score",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                GrowthStatChip(
                    value = stats.totalTimeSpent.toString(),
                    label = "Minutes",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (categories.isEmpty()) {
            GrowthEmptyState(
                text = "Interview categories exist on the backend, but practice content is still thin.",
                contentColor = contentColor
            )
        } else {
            categories.forEach { category ->
                GrowthListItemCard(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    emoji = "🎯",
                    title = category.name,
                    subtitle = category.description ?: "Interview set",
                    supporting = "${category.questionCount} questions"
                )
            }
        }
    }
}

@Composable
private fun RewardsCard(
    xpBalance: Int,
    badges: List<BadgeSummary>,
    badgeCategories: List<String>,
    storeItems: List<StoreItemSummary>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Rewards and unlocks",
            subtitle = "XP balance, early badges, and store previews.",
            contentColor = contentColor
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GrowthStatChip(
                value = xpBalance.toString(),
                label = "XP balance",
                contentColor = contentColor,
                accentColor = accentColor
            )
            GrowthStatChip(
                value = badges.size.toString(),
                label = "Badges",
                contentColor = contentColor,
                accentColor = accentColor
            )
            GrowthStatChip(
                value = storeItems.size.toString(),
                label = "Store",
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
        if (badgeCategories.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            GrowthTagRow(
                tags = badgeCategories,
                contentColor = contentColor,
                accentColor = accentColor
            )
        }
        Spacer(Modifier.height(12.dp))
        if (badges.isEmpty() && storeItems.isEmpty() && xpBalance == 0) {
            GrowthEmptyState(
                text = "Rewards are wired up, but this account has not accumulated much here yet.",
                contentColor = contentColor
            )
        } else {
            badges.forEach { badge ->
                GrowthListItemCard(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    emoji = badgeEmoji(badge.rarity),
                    title = badge.name,
                    subtitle = badge.description,
                    supporting = "${badge.category} • ${badge.xpReward} XP"
                )
            }
            storeItems.forEach { item ->
                GrowthListItemCard(
                    accentColor = accentColor,
                    contentColor = contentColor,
                    emoji = "🛍️",
                    title = item.name,
                    subtitle = item.description ?: item.category,
                    supporting = item.price?.let { "$it coins" } ?: item.xpCost?.let { "$it XP" } ?: "Reward item"
                )
            }
        }
    }
}

@Composable
private fun ReferralCard(
    referralCode: String?,
    stats: ReferralStatsSummary?,
    shareLinks: ReferralShareLinks?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onCopyCode: () -> Unit,
    onShare: () -> Unit
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor,
        tint = accentColor.copy(alpha = 0.06f)
    ) {
        GrowthSectionTitle(
            title = "Referral loop",
            subtitle = "Invite others without making the screen feel salesy.",
            contentColor = contentColor
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                BasicText(
                    "Your code",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.56f),
                        fontSize = 11.sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    referralCode ?: shareLinks?.code ?: "Generating...",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GrowthActionPill(
                label = "Copy code",
                accentColor = accentColor,
                contentColor = contentColor,
                enabled = !referralCode.isNullOrBlank() || !shareLinks?.code.isNullOrBlank(),
                onClick = onCopyCode
            )
            GrowthActionPill(
                label = "Share",
                accentColor = accentColor,
                contentColor = contentColor,
                enabled = shareLinks != null || !referralCode.isNullOrBlank(),
                onClick = onShare
            )
        }
        stats?.let {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrowthStatChip(
                    value = it.successfulReferrals.toString(),
                    label = "Successful",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                GrowthStatChip(
                    value = it.pendingReferrals.toString(),
                    label = "Pending",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                GrowthStatChip(
                    value = it.totalXpEarned.toString(),
                    label = "XP earned",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun AccountabilityCard(
    partners: List<AccountabilityPartnerSummary>,
    mentorships: List<MentorshipSummary>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onOpenProfile: (String) -> Unit,
    onCheckIn: (String) -> Unit
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor
    ) {
        GrowthSectionTitle(
            title = "Accountability",
            subtitle = "Partners are real on the backend, mentorships are mostly empty today.",
            contentColor = contentColor
        )
        Spacer(Modifier.height(12.dp))
        if (partners.isEmpty() && mentorships.isEmpty()) {
            GrowthEmptyState(
                text = "No partner data yet. This section is ready when matching fills in.",
                contentColor = contentColor
            )
        } else {
            partners.forEach { partner ->
                val profile = partner.partner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (profile != null && !profile.profileImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(profile.profileImage)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = profile.name,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        (profile?.name ?: "P").take(1).uppercase(),
                                        style = TextStyle(
                                            color = accentColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                BasicText(
                                    profile?.name ?: "Partner",
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                BasicText(
                                    profile?.headline ?: partner.goal,
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.58f),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GrowthStatChip(
                                value = partner.sharedStreak.toString(),
                                label = "Shared streak",
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                            GrowthStatChip(
                                value = partner.checkInsCompleted.toString(),
                                label = "Check-ins",
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (profile != null) {
                                GrowthActionPill(
                                    label = "Open profile",
                                    accentColor = accentColor,
                                    contentColor = contentColor,
                                    onClick = { onOpenProfile(profile.id) }
                                )
                            }
                            GrowthActionPill(
                                label = "Check in",
                                accentColor = accentColor,
                                contentColor = contentColor,
                                onClick = { onCheckIn(partner.id) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (mentorships.isNotEmpty()) {
                mentorships.forEach { mentorship ->
                    GrowthListItemCard(
                        accentColor = accentColor,
                        contentColor = contentColor,
                        emoji = "🧭",
                        title = mentorship.skill.ifBlank { "Mentorship" },
                        subtitle = mentorship.status.replaceFirstChar { it.uppercase() },
                        supporting = "${mentorship.sessionsCompleted} sessions completed"
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthSurfaceCard(
    backdrop: LayerBackdrop,
    accentColor: Color,
    contentColor: Color,
    tint: Color = Color.White.copy(alpha = 0.10f),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(22.dp) },
                effects = {
                    vibrancy()
                    blur(14f.dp.toPx())
                    lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(tint)
                }
            )
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun GrowthSectionTitle(
    title: String,
    subtitle: String,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        BasicText(
            subtitle,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.58f),
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun GrowthActionPill(
    label: String,
    accentColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (enabled) accentColor.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                if (enabled) accentColor.copy(alpha = 0.38f)
                else contentColor.copy(alpha = 0.10f),
                RoundedCornerShape(999.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = if (enabled) accentColor else contentColor.copy(alpha = 0.35f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun GrowthStatChip(
    value: String,
    label: String,
    contentColor: Color,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            BasicText(
                value,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            BasicText(
                label,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.52f),
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun GrowthTagRow(
    tags: List<String>,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                BasicText(
                    tag.replaceFirstChar { it.uppercase() },
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun GrowthListItemCard(
    accentColor: Color,
    contentColor: Color,
    emoji: String,
    title: String,
    subtitle: String,
    supporting: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, accentColor.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(emoji, style = TextStyle(fontSize = 18.sp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    title,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        subtitle,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.60f),
                            fontSize = 12.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (supporting.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        supporting,
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthChatBubble(
    message: GrowthHubChatMessage,
    contentColor: Color,
    accentColor: Color
) {
    val isUser = message.role.equals("user", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isUser) accentColor.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.12f)
                )
                .border(
                    1.dp,
                    if (isUser) accentColor.copy(alpha = 0.30f)
                    else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicText(
                message.content,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

@Composable
private fun GrowthInlineMessage(
    message: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    GrowthSurfaceCard(
        backdrop = backdrop,
        accentColor = accentColor,
        contentColor = contentColor,
        tint = Color(0xFFFFD7D4).copy(alpha = 0.26f)
    ) {
        BasicText(
            message,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            "Retry",
            modifier = Modifier.clickable(onClick = onRetry),
            style = TextStyle(
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun GrowthEmptyState(
    text: String,
    contentColor: Color
) {
    BasicText(
        text,
        style = TextStyle(
            color = contentColor.copy(alpha = 0.56f),
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    )
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

private fun buildShareMessage(
    shareLinks: ReferralShareLinks?,
    referralCode: String?
): String? {
    val code = shareLinks?.code ?: referralCode
    val link = shareLinks?.link
    if (code.isNullOrBlank() && link.isNullOrBlank()) return null
    return buildString {
        append("Join me on Vormex")
        if (!code.isNullOrBlank()) append(" with code $code")
        if (!link.isNullOrBlank()) append(". $link")
    }
}

private fun badgeEmoji(rarity: String): String {
    return when (rarity.lowercase()) {
        "epic" -> "🏆"
        "rare" -> "💎"
        else -> "⭐"
    }
}

private fun hasGrowthContent(state: GrowthHubUiState): Boolean {
    return state.dailyHooks.isNotEmpty() ||
        state.featuredJobs.isNotEmpty() ||
        state.featuredPaths.isNotEmpty() ||
        state.dailyChallenge != null ||
        state.interviewCategories.isNotEmpty() ||
        state.badges.isNotEmpty() ||
        state.storeItems.isNotEmpty() ||
        state.partners.isNotEmpty() ||
        state.referralCode != null
}
