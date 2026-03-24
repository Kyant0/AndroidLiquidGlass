package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.Notification
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsInboxScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onUnreadCountChanged: (Int) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onOpenReel: (String) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    onOpenNetwork: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: NotificationsInboxViewModel =
        viewModel(factory = NotificationsInboxViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(uiState.unreadCount) {
        onUnreadCountChanged(uiState.unreadCount)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(0.dp) },
                effects = {
                    vibrancy()
                    blur(22f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFEAF4FF).copy(alpha = 0.74f),
                                Color(0xFFF8FBFF).copy(alpha = 0.58f),
                                Color(0xFFE9F2FF).copy(alpha = 0.72f)
                            )
                        )
                    )
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            SettingsHeader(
                title = "Notifications",
                contentColor = contentColor,
                onBack = onNavigateBack
            )

            NotificationsHeroCard(
                unreadCount = uiState.unreadCount,
                unreadOnly = uiState.unreadOnly,
                isMarkingAllRead = uiState.isMarkingAllRead,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onToggleUnreadOnly = viewModel::toggleUnreadOnly,
                onMarkAllRead = viewModel::markAllAsRead
            )

            Spacer(Modifier.height(12.dp))

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                state = refreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.notifications.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }

                    uiState.notifications.isEmpty() -> {
                        EmptyNotificationsState(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            unreadOnly = uiState.unreadOnly,
                            error = uiState.error,
                            onRetry = { viewModel.refresh(showLoader = true) }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                if (uiState.error != null) {
                                    NotificationsInlineError(
                                        message = uiState.error ?: "Something went wrong",
                                        backdrop = backdrop,
                                        contentColor = contentColor,
                                        onRetry = {
                                            viewModel.clearError()
                                            viewModel.refresh(showLoader = true)
                                        }
                                    )
                                }
                            }

                            items(uiState.notifications, key = { it.id }) { notification ->
                                NotificationInboxCard(
                                    notification = notification,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onClick = {
                                        viewModel.markAsRead(notification.id)
                                        routeNotification(
                                            notification = notification,
                                            onOpenProfile = onOpenProfile,
                                            onOpenPost = onOpenPost,
                                            onOpenReel = onOpenReel,
                                            onOpenConversation = onOpenConversation,
                                            onOpenNetwork = onOpenNetwork
                                        )
                                    }
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
private fun NotificationsHeroCard(
    unreadCount: Int,
    unreadOnly: Boolean,
    isMarkingAllRead: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onToggleUnreadOnly: () -> Unit,
    onMarkAllRead: () -> Unit
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
                    drawRect(Color.White.copy(alpha = 0.16f))
                }
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    BasicText(
                        "Activity inbox",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        if (unreadCount > 0) "$unreadCount unread updates waiting for you"
                        else "You’re caught up for now",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (unreadCount > 0) unreadCount.coerceAtMost(99).toString() else "0",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InboxActionPill(
                    label = if (unreadOnly) "Showing unread" else "Show unread only",
                    active = unreadOnly,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onClick = onToggleUnreadOnly
                )
                InboxActionPill(
                    label = if (isMarkingAllRead) "Clearing..." else "Mark all read",
                    active = false,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    enabled = unreadCount > 0 && !isMarkingAllRead,
                    onClick = onMarkAllRead
                )
            }
        }
    }
}

@Composable
private fun InboxActionPill(
    label: String,
    active: Boolean,
    accentColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val background = if (active) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.12f)
    val border = if (active) accentColor.copy(alpha = 0.45f) else contentColor.copy(alpha = 0.12f)
    val textColor = if (enabled) {
        if (active) accentColor else contentColor
    } else {
        contentColor.copy(alpha = 0.35f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        BasicText(
            label,
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun NotificationInboxCard(
    notification: Notification,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(5f.dp.toPx(), 10f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(
                        if (notification.isRead) Color.White.copy(alpha = 0.10f)
                        else accentColor.copy(alpha = 0.08f)
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (notification.isRead) Color.White.copy(alpha = 0.08f) else accentColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box {
                if (notification.actor?.profileImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(notification.actor.profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            notificationEmoji(notification.type),
                            style = TextStyle(fontSize = 20.sp)
                        )
                    }
                }

                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5A5F))
                            .border(2.dp, Color.White.copy(alpha = 0.78f), CircleShape)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicText(
                    notification.title,
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold
                    )
                )
                BasicText(
                    notification.body,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                val preview = notification.post?.content ?: notification.reel?.title
                if (!preview.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            preview,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.62f),
                                fontSize = 11.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            BasicText(
                compactTimestamp(notification.createdAt),
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.44f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun NotificationsInlineError(
    message: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color(0xFFFF8A80).copy(alpha = 0.12f))
                }
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                message,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            BasicText(
                "Tap to retry",
                modifier = Modifier.clickable(onClick = onRetry),
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun EmptyNotificationsState(
    backdrop: LayerBackdrop,
    contentColor: Color,
    unreadOnly: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
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
                        drawRect(Color.White.copy(alpha = 0.14f))
                    }
                )
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    if (unreadOnly) "No unread notifications" else "Your inbox is quiet",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                BasicText(
                    error ?: if (unreadOnly) {
                        "You’ve already caught up on everything."
                    } else {
                        "Likes, comments, follows, and reel activity will land here."
                    },
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.66f),
                        fontSize = 12.sp
                    )
                )
                if (error != null) {
                    BasicText(
                        "Retry",
                        modifier = Modifier.clickable(onClick = onRetry),
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

private fun notificationEmoji(type: String): String {
    val normalized = type.lowercase()
    return when {
        "message" in normalized -> "💬"
        "mention" in normalized -> "📣"
        "follow" in normalized -> "👋"
        "connection" in normalized -> "🤝"
        "comment" in normalized -> "🗨️"
        "reel" in normalized -> "🎬"
        "share" in normalized -> "🔁"
        "streak" in normalized -> "🔥"
        "xp" in normalized -> "✨"
        else -> "🔔"
    }
}

private fun compactTimestamp(createdAt: String): String {
    val date = createdAt.substringBefore('T')
    return if (date.length >= 10) date.substring(5) else createdAt.take(10)
}

private fun routeNotification(
    notification: Notification,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenReel: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenNetwork: () -> Unit
) {
    val data = notification.data.orEmpty()
    val conversationId = data["conversationId"]
    val postId = notification.post?.id ?: data["postId"]
    val reelId = notification.reel?.id ?: data["reelId"]
    val actorId = notification.actor?.id ?: data["userId"] ?: data["actorId"]

    when {
        !conversationId.isNullOrBlank() -> onOpenConversation(conversationId)
        !postId.isNullOrBlank() -> onOpenPost(postId)
        !reelId.isNullOrBlank() -> onOpenReel(reelId)
        notification.type.contains("connection", ignoreCase = true) -> {
            if (!actorId.isNullOrBlank()) onOpenProfile(actorId) else onOpenNetwork()
        }
        !actorId.isNullOrBlank() -> onOpenProfile(actorId)
        else -> onOpenNetwork()
    }
}
