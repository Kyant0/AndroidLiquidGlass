package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy

// ==================== Group Detail Screen ====================

@Composable
fun GroupDetailScreen(
    groupId: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateToChat: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(groupId) {
        viewModel.loadGroupDetail(groupId)
    }
    
    val group = uiState.selectedGroup
    
    Box(Modifier.fillMaxSize()) {
        if (uiState.isLoading && group == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (group != null) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Header with cover image
                item {
                    GroupDetailHeader(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        group = group,
                        onBackClick = onNavigateBack,
                        onSettingsClick = onNavigateToSettings
                    )
                }
                
                // Group info card
                item {
                    GroupInfoCard(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        group = group,
                        isJoining = group.id in uiState.joiningGroupIds,
                        onJoinClick = { viewModel.joinGroup(group.id) },
                        onChatClick = onNavigateToChat
                    )
                }
                
                // Tabs
                item {
                    GroupDetailTabs(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        selectedTab = uiState.detailTab,
                        onTabSelected = { viewModel.setDetailTab(it) }
                    )
                }
                
                // Content based on tab
                when (uiState.detailTab) {
                    GroupDetailTab.POSTS -> {
                        if (uiState.groupPosts.isEmpty()) {
                            item {
                                EmptyTabContent(
                                    contentColor = contentColor,
                                    title = "No posts yet",
                                    message = "Be the first to post in this group"
                                )
                            }
                        } else {
                            items(uiState.groupPosts, key = { it.id }) { post ->
                                GroupPostCard(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    post = post,
                                    onAuthorClick = { onNavigateToProfile(post.authorId) }
                                )
                            }
                        }
                    }
                    
                    GroupDetailTab.ABOUT -> {
                        item {
                            GroupAboutSection(
                                backdrop = backdrop,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                group = group
                            )
                        }
                    }
                    
                    GroupDetailTab.MEMBERS -> {
                        if (uiState.groupMembers.isEmpty()) {
                            item {
                                EmptyTabContent(
                                    contentColor = contentColor,
                                    title = "No members",
                                    message = "Members will appear here"
                                )
                            }
                        } else {
                            items(uiState.groupMembers, key = { it.id }) { member ->
                                GroupMemberCard(
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    member = member,
                                    onClick = { onNavigateToProfile(member.userId) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (uiState.error != null) {
            // Error state
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BasicText(
                        "Failed to load group",
                        style = TextStyle(color = contentColor, fontSize = 16.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .glassBackground(backdrop)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onNavigateBack)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            "Go Back",
                            style = TextStyle(color = contentColor)
                        )
                    }
                }
            }
        }
    }
}

// ==================== Header ====================

@Composable
private fun GroupDetailHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    group: Group,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val isAdminOrOwner = group.memberRole in listOf("owner", "admin")
    
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Cover image
        if (group.coverImage != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(group.coverImage)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.4f),
                                accentColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
        }
        
        // Gradient overlay
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )
        
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (isAdminOrOwner) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Group icon
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(72.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (group.iconImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(group.iconImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

// ==================== Info Card ====================

@Composable
private fun GroupInfoCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    group: Group,
    isJoining: Boolean,
    onJoinClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
    ) {
        // Name and privacy
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                group.name,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(Modifier.width(8.dp))
            
            // Privacy badge
            Row(
                Modifier
                    .background(
                        when (group.privacy) {
                            "PUBLIC" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                            "PRIVATE" -> Color(0xFFFFC107).copy(alpha = 0.15f)
                            else -> Color(0xFFF44336).copy(alpha = 0.15f)
                        },
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (group.privacy) {
                        "PUBLIC" -> Icons.Default.Public
                        "PRIVATE" -> Icons.Default.Lock
                        else -> Icons.Default.VisibilityOff
                    },
                    contentDescription = null,
                    tint = when (group.privacy) {
                        "PUBLIC" -> Color(0xFF4CAF50)
                        "PRIVATE" -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                BasicText(
                    when (group.privacy) {
                        "PUBLIC" -> "Public"
                        "PRIVATE" -> "Private"
                        else -> "Secret"
                    },
                    style = TextStyle(
                        color = when (group.privacy) {
                            "PUBLIC" -> Color(0xFF4CAF50)
                            "PRIVATE" -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Stats
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                "${group.memberCount} members",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            )
            
            if (group.category != null) {
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier
                        .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        group.category,
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        
        if (group.description != null) {
            Spacer(Modifier.height(12.dp))
            BasicText(
                group.description,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Action buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (group.isMember) {
                // Chat button
                Box(
                    Modifier
                        .weight(1f)
                        .background(accentColor, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onChatClick)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            "Chat",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
                
            } else {
                // Join button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(accentColor, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isJoining, onClick = onJoinClick)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicText(
                                if (group.privacy == "PRIVATE") "Request to Join" else "Join Group",
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
}

// ==================== Tabs ====================

@Composable
private fun GroupDetailTabs(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    selectedTab: GroupDetailTab,
    onTabSelected: (GroupDetailTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .glassBackground(backdrop, vibrancyAlpha = 0.08f)
            .clip(RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GroupDetailTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val alpha by animateFloatAsState(if (isSelected) 1f else 0f, label = "alpha")
            
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = when (tab) {
                        GroupDetailTab.POSTS -> "Posts"
                        GroupDetailTab.ABOUT -> "About"
                        GroupDetailTab.MEMBERS -> "Members"
                    },
                    style = TextStyle(
                        color = if (isSelected) accentColor else contentColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

// ==================== Content Sections ====================

@Composable
private fun GroupPostCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    post: GroupPost,
    onAuthorClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .glassBackground(backdrop, vibrancyAlpha = 0.08f)
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Author row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onAuthorClick)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.1f), CircleShape)
                    .clip(CircleShape)
            ) {
                if (post.author.profileImage != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(post.author.profileImage)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(Modifier.width(10.dp))
            
            Column {
                BasicText(
                    post.author.name ?: post.author.username ?: "Unknown",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                BasicText(
                    post.createdAt.take(10), // Simple date format
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                )
            }
            
            if (post.isPinned) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    BasicText(
                        "📌 Pinned",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        
        Spacer(Modifier.height(10.dp))
        
        // Content
        BasicText(
            post.content,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp
            )
        )
        
        // Media
        if (post.mediaUrls.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(post.mediaUrls.first())
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(Modifier.height(10.dp))
        
        // Engagement stats
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText(
                "❤️ ${post.likesCount}",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            )
            BasicText(
                "💬 ${post.commentsCount}",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun GroupAboutSection(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    group: Group
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Description
        if (group.description != null) {
            AboutCard(
                backdrop = backdrop,
                contentColor = contentColor,
                title = "About",
                content = group.description
            )
            Spacer(Modifier.height(12.dp))
        }
        
        // Rules
        if (group.rules.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassBackground(backdrop, vibrancyAlpha = 0.08f)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                BasicText(
                    "Group Rules",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(12.dp))
                group.rules.forEachIndexed { index, rule ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        BasicText(
                            "${index + 1}.",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            rule,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        
        // Tags
        if (group.tags.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassBackground(backdrop, vibrancyAlpha = 0.08f)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                BasicText(
                    "Tags",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.tags.forEach { tag ->
                        Box(
                            Modifier
                                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            BasicText(
                                tag,
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 12.sp
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
private fun AboutCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    title: String,
    content: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassBackground(backdrop, vibrancyAlpha = 0.08f)
            .clip(RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            content,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun GroupMemberCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    member: GroupMember,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .glassBackground(backdrop, vibrancyAlpha = 0.08f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            Modifier
                .size(48.dp)
                .background(accentColor.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
        ) {
            if (member.user.profileImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(member.user.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    member.user.name ?: member.user.username ?: "Unknown",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                
                if (member.role != "member") {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(
                                when (member.role) {
                                    "owner" -> Color(0xFFE91E63).copy(alpha = 0.15f)
                                    "admin" -> Color(0xFF9C27B0).copy(alpha = 0.15f)
                                    "moderator" -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            member.role.replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                color = when (member.role) {
                                    "owner" -> Color(0xFFE91E63)
                                    "admin" -> Color(0xFF9C27B0)
                                    "moderator" -> Color(0xFF2196F3)
                                    else -> contentColor
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            if (member.user.headline != null) {
                BasicText(
                    member.user.headline,
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyTabContent(
    contentColor: Color,
    title: String,
    message: String
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                message,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
