package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.vormexSurface
import com.kyant.backdrop.catalog.network.models.LikeUser
import com.kyant.backdrop.catalog.network.models.ReactionType

/**
 * Reaction Picker - Quick reaction selector displayed on long-press
 */
@Composable
fun ReactionPicker(
    backdrop: LayerBackdrop,
    contentColor: Color,
    isVisible: Boolean,
    currentReaction: ReactionType?,
    onReactionSelected: (ReactionType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )
    
    Box(
        modifier = modifier
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Overlay,
                cornerRadius = 24.dp,
                blurRadius = 16.dp,
                lensRadius = 8.dp,
                lensDepth = 16.dp,
                surfaceColor = appearance.overlayColor,
                borderColor = appearance.overlayBorderColor
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReactionType.entries.forEach { reaction ->
                val isSelected = reaction == currentReaction
                ReactionButton(
                    reaction = reaction,
                    isSelected = isSelected,
                    contentColor = contentColor,
                    onClick = { 
                        onReactionSelected(reaction)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionButton(
    reaction: ReactionType,
    isSelected: Boolean,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) contentColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = reaction.icon,
            style = TextStyle(fontSize = if (isSelected) 28.sp else 24.sp)
        )
    }
}

/**
 * Likes List Modal - Shows list of users who liked/reacted to a post
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesListModal(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    likes: List<LikeUser>,
    isLoading: Boolean,
    hasMore: Boolean,
    filterReaction: ReactionType?,
    onFilterChange: (ReactionType?) -> Unit,
    onLoadMore: () -> Unit,
    onUserClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .vormexSurface(
                    backdrop = backdrop,
                    tone = VormexSurfaceTone.Sheet,
                    cornerRadius = 24.dp,
                    blurRadius = 20.dp,
                    lensRadius = 12.dp,
                    lensDepth = 24.dp,
                    surfaceColor = appearance.sheetColor,
                    borderColor = appearance.sheetBorderColor
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Reactions",
                        style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("✕", style = TextStyle(contentColor, 18.sp))
                    }
                }
                
                // Reaction filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All tab
                    ReactionFilterTab(
                        label = "All",
                        emoji = null,
                        isSelected = filterReaction == null,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onFilterChange(null) }
                    )
                    
                    // Individual reaction tabs
                    ReactionType.entries.forEach { reaction ->
                        ReactionFilterTab(
                            label = null,
                            emoji = reaction.icon,
                            isSelected = filterReaction == reaction,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = { onFilterChange(reaction) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Users list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading && likes.isEmpty()) {
                        items(5) {
                            LikeUserSkeleton(contentColor)
                        }
                    } else {
                        items(likes, key = { it.id }) { user ->
                            LikeUserItem(
                                user = user,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                onClick = { onUserClick(user.id) }
                            )
                        }
                        
                        if (hasMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(onClick = onLoadMore)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = accentColor
                                        )
                                    } else {
                                        BasicText(
                                            text = "Load more",
                                            style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (likes.isEmpty() && !isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        BasicText("❤️", style = TextStyle(fontSize = 48.sp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        BasicText(
                                            text = "No reactions yet",
                                            style = TextStyle(contentColor.copy(alpha = 0.6f), 16.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionFilterTab(
    label: String?,
    emoji: String?,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else contentColor.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        } else {
            BasicText(
                text = emoji ?: "",
                style = TextStyle(fontSize = if (isSelected) 20.sp else 18.sp)
            )
        }
    }
}

@Composable
private fun LikeUserItem(
    user: LikeUser,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (contentColor == Color.White) "dark" else "light"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with reaction overlay
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(user.profileImage).build(),
                        contentDescription = "${user.name}'s avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initials = (user.name ?: "?").split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                    BasicText(
                        text = initials,
                        style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                    )
                }
            }
            
            // Reaction badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(appearance.sheetColor),
                contentAlignment = Alignment.Center
            ) {
                val reactionIcon = ReactionType.entries.find { it.name == user.reactionType }?.icon ?: "👍"
                BasicText(
                    text = reactionIcon,
                    style = TextStyle(fontSize = 12.sp)
                )
            }
        }
        
        // User info
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = user.name ?: "Unknown",
                style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
            )
            user.headline?.let { headline ->
                BasicText(
                    text = headline,
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LikeUserSkeleton(contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar skeleton
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.1f))
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(contentColor.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(contentColor.copy(alpha = 0.08f))
            )
        }
    }
}

/**
 * Engagement Summary - Shows reaction counts by type
 */
@Composable
fun EngagementSummary(
    reactions: Map<ReactionType, Int>,
    totalLikes: Int,
    commentCount: Int,
    shareCount: Int,
    contentColor: Color,
    onReactionsClick: () -> Unit,
    onCommentsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reactions summary
        if (totalLikes > 0) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onReactionsClick)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Show top 3 reaction icons
                reactions.entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .forEach { (reaction, _) ->
                        BasicText(
                            text = reaction.icon,
                            style = TextStyle(fontSize = 14.sp)
                        )
                    }
                
                BasicText(
                    text = formatCount(totalLikes),
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Comments and shares
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (commentCount > 0) {
                BasicText(
                    text = "${formatCount(commentCount)} comments",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCommentsClick)
                        .padding(4.dp)
                )
            }
            
            if (shareCount > 0) {
                BasicText(
                    text = "${formatCount(shareCount)} shares",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
