package com.kyant.backdrop.catalog.linkedin.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.network.models.*

private fun Modifier.groupSurface(
    contentColor: Color,
    cornerRadius: Dp = 16.dp,
    containerColor: Color? = null,
    outlineColor: Color? = null
): Modifier {
    val isDarkSurface = contentColor == Color.White
    val resolvedOutline = outlineColor ?: if (isDarkSurface) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.42f)
    }
    val shape = RoundedCornerShape(cornerRadius)
    val base = this.clip(shape)

    val withBackground = if (containerColor != null) {
        base.background(containerColor)
    } else {
        base.background(
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
    }

    return withBackground
        .border(1.dp, resolvedOutline, shape)
}

// ==================== Main Groups Screen ====================

@Composable
fun GroupsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateToGroupDetail: (String) -> Unit = {},
    onNavigateToGroupChat: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadMyGroups()
        viewModel.loadCategories()
    }
    
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Header
            GroupsHeader(
                backdrop = backdrop,
                contentColor = contentColor,
                onBackClick = onNavigateBack,
                onCreateClick = { viewModel.showCreateModal(true) }
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Tabs
            GroupsTabs(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                selectedTab = uiState.activeTab,
                pendingInvitesCount = uiState.pendingInvites.size,
                onTabSelected = { viewModel.setActiveTab(it) }
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Search bar (for Discover tab)
            AnimatedVisibility(
                visible = uiState.activeTab == GroupsTab.DISCOVER,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    GroupsSearchBar(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) }
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Categories filter
                    if (uiState.categories.isNotEmpty()) {
                        CategoriesFilter(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.setSelectedCategory(it) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            
            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                uiState.error != null -> {
                    ErrorContent(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        error = uiState.error!!,
                        onRetry = {
                            when (uiState.activeTab) {
                                GroupsTab.MY_GROUPS -> viewModel.loadMyGroups(refresh = true)
                                GroupsTab.DISCOVER -> viewModel.loadDiscoverGroups(refresh = true)
                                GroupsTab.INVITES -> viewModel.loadPendingInvites()
                            }
                        }
                    )
                }
                else -> {
                    when (uiState.activeTab) {
                        GroupsTab.MY_GROUPS -> MyGroupsContent(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            groups = uiState.myGroups,
                            joiningIds = uiState.joiningGroupIds,
                            onGroupClick = { onNavigateToGroupDetail(it.id) },
                            onChatClick = { onNavigateToGroupChat(it.id) }
                        )
                        
                        GroupsTab.DISCOVER -> DiscoverGroupsContent(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            groups = uiState.discoverGroups,
                            joiningIds = uiState.joiningGroupIds,
                            onGroupClick = { onNavigateToGroupDetail(it.id) },
                            onJoinGroup = { viewModel.joinGroup(it.id) }
                        )
                        
                        GroupsTab.INVITES -> InvitesContent(
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            invites = uiState.pendingInvites
                        )
                    }
                }
            }
        }
        
        // Create Group Modal
        if (uiState.showCreateModal) {
            CreateGroupModal(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isCreating = uiState.isCreatingGroup,
                onDismiss = { viewModel.showCreateModal(false) },
                onCreate = { name, description, privacy, category, rules ->
                    viewModel.createGroup(name, description, privacy, category, rules)
                }
            )
        }
    }
}

// ==================== Header ====================

@Composable
private fun GroupsHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .groupSurface(contentColor, 20.dp)
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            BasicText(
                "Groups",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Box(
            Modifier
                .size(40.dp)
                .groupSurface(contentColor, 20.dp)
                .clickable(onClick = onCreateClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Create",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==================== Tabs ====================

@Composable
private fun GroupsTabs(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    selectedTab: GroupsTab,
    pendingInvitesCount: Int,
    onTabSelected: (GroupsTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .groupSurface(contentColor, 12.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GroupsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        text = when (tab) {
                            GroupsTab.MY_GROUPS -> "My Groups"
                            GroupsTab.DISCOVER -> "Discover"
                            GroupsTab.INVITES -> "Invites"
                        },
                        style = TextStyle(
                            color = if (isSelected) accentColor else contentColor.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                    
                    // Badge for invites
                    if (tab == GroupsTab.INVITES && pendingInvitesCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            Modifier
                                .size(18.dp)
                                .background(accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "$pendingInvitesCount",
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
        }
    }
}

// ==================== Search Bar ====================

@Composable
private fun GroupsSearchBar(
    backdrop: LayerBackdrop,
    contentColor: Color,
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .groupSurface(contentColor, 12.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(color = contentColor, fontSize = 15.sp),
            cursorBrush = SolidColor(contentColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        BasicText(
                            "Search groups...",
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear",
                tint = contentColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

// ==================== Categories Filter ====================

@Composable
private fun CategoriesFilter(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    categories: List<GroupCategory>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            CategoryChip(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                text = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        items(categories) { category ->
            CategoryChip(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                text = category.name,
                count = category.count,
                isSelected = selectedCategory == category.name,
                onClick = { 
                    onCategorySelected(if (selectedCategory == category.name) null else category.name)
                }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    text: String,
    count: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .groupSurface(
                contentColor = contentColor,
                cornerRadius = 20.dp,
                containerColor = if (isSelected) accentColor.copy(alpha = 0.16f) else null,
                outlineColor = if (isSelected) accentColor.copy(alpha = 0.26f) else null
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            if (count != null && count > 0) {
                Spacer(Modifier.width(4.dp))
                BasicText(
                    "($count)",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ==================== Content Sections ====================

@Composable
private fun MyGroupsContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    groups: List<Group>,
    joiningIds: Set<String>,
    onGroupClick: (Group) -> Unit,
    onChatClick: (Group) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyContent(
            backdrop = backdrop,
            contentColor = contentColor,
            title = "No groups yet",
            message = "Join or create a group to start connecting with communities"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                GroupCard(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    group = group,
                    isJoining = group.id in joiningIds,
                    showActions = true,
                    onClick = { onGroupClick(group) },
                    onChatClick = { onChatClick(group) }
                )
            }
        }
    }
}

@Composable
private fun DiscoverGroupsContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    groups: List<Group>,
    joiningIds: Set<String>,
    onGroupClick: (Group) -> Unit,
    onJoinGroup: (Group) -> Unit
) {
    if (groups.isEmpty()) {
        EmptyContent(
            backdrop = backdrop,
            contentColor = contentColor,
            title = "No groups found",
            message = "Try adjusting your search or filters"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(groups, key = { it.id }) { group ->
                GroupCard(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    group = group,
                    isJoining = group.id in joiningIds,
                    showJoinButton = !group.isMember,
                    onClick = { onGroupClick(group) },
                    onActionClick = { if (!group.isMember) onJoinGroup(group) }
                )
            }
        }
    }
}

@Composable
private fun InvitesContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    invites: List<GroupInvite>
) {
    if (invites.isEmpty()) {
        EmptyContent(
            backdrop = backdrop,
            contentColor = contentColor,
            title = "No pending invites",
            message = "You'll see group invitations here"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(invites, key = { it.id }) { invite ->
                InviteCard(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    invite = invite
                )
            }
        }
    }
}

// ==================== Group Card ====================

@Composable
fun GroupCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    group: Group,
    isJoining: Boolean = false,
    showActions: Boolean = false,
    showJoinButton: Boolean = false,
    onClick: () -> Unit,
    onChatClick: (() -> Unit)? = null,
    onActionClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(
        Modifier
            .fillMaxWidth()
            .groupSurface(contentColor, 16.dp)
            .clickable(onClick = onClick)
    ) {
        // Cover image
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
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
                // Default gradient
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor.copy(alpha = 0.1f)
                                )
                            )
                        )
                )
            }
            
            // Icon overlay
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .size(56.dp)
                    .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
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
                        tint = contentColor.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            
            // Privacy badge
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        when (group.privacy) {
                            "PUBLIC" -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                            "PRIVATE" -> Color(0xFFFFC107).copy(alpha = 0.9f)
                            else -> Color(0xFFF44336).copy(alpha = 0.9f)
                        },
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (group.privacy) {
                            "PUBLIC" -> Icons.Default.Public
                            "PRIVATE" -> Icons.Default.Lock
                            else -> Icons.Default.VisibilityOff
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        text = when (group.privacy) {
                            "PUBLIC" -> "Public"
                            "PRIVATE" -> "Private"
                            else -> "Secret"
                        },
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        
        // Content
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    BasicText(
                        group.name,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (group.description != null) {
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            group.description,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Stats and category
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        "${group.memberCount} members",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    )
                    
                    if (group.category != null) {
                        Spacer(Modifier.width(12.dp))
                        Box(
                            Modifier
                                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                group.category,
                                style = TextStyle(
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                
                // Member role badge
                if (group.memberRole != null) {
                    Box(
                        Modifier
                            .background(
                                when (group.memberRole) {
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
                            group.memberRole.replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                color = when (group.memberRole) {
                                    "owner" -> Color(0xFFE91E63)
                                    "admin" -> Color(0xFF9C27B0)
                                    "moderator" -> Color(0xFF2196F3)
                                    else -> contentColor.copy(alpha = 0.5f)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            // Action buttons
            if (showActions || showJoinButton) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showActions && onChatClick != null) {
                        Box(
                            Modifier
                                .weight(1f)
                                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onChatClick)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                BasicText(
                                    "Chat",
                                    style = TextStyle(
                                        color = accentColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                    
                    if (showJoinButton) {
                        Box(
                            Modifier
                                .weight(1f)
                                .background(accentColor, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isJoining, onClick = onActionClick)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isJoining) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                BasicText(
                                    "Join Group",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
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

// ==================== Invite Card ====================

@Composable
private fun InviteCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    invite: GroupInvite
) {
    Column(
        Modifier
            .fillMaxWidth()
            .groupSurface(contentColor, 16.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Group icon placeholder
            Box(
                Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                BasicText(
                    invite.group?.name ?: "Group Invitation",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (invite.invitedBy != null) {
                    BasicText(
                        "Invited by ${invite.invitedBy.name ?: invite.invitedBy.username}",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
        
        if (invite.message != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                "\"${invite.message}\"",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .background(accentColor, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* Accept */ }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Accept",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Box(
                Modifier
                    .weight(1f)
                    .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* Decline */ }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "Decline",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

// ==================== Helper Components ====================

@Composable
private fun EmptyContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    title: String,
    message: String
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            BasicText(
                title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                message,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun ErrorContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    error: String,
    onRetry: () -> Unit
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            BasicText(
                "Oops!",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                error,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .groupSurface(contentColor, 8.dp)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                BasicText(
                    "Retry",
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
