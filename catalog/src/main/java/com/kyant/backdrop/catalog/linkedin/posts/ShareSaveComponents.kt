package com.kyant.backdrop.catalog.linkedin.posts

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.vormexSurface
import com.kyant.backdrop.catalog.network.models.MentionUser
import com.kyant.shapes.RoundedRectangle

private val ShareButtonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4B70E2), Color(0xFF3a5bc7))
)

/**
 * Share Post Modal - Share to connections or copy link (Glass Theme)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostModal(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean = true,
    connections: List<MentionUser>,
    isLoading: Boolean,
    isSharing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onShareToConnections: (List<String>, String?) -> Unit,
    onSearchConnections: (String) -> Unit,
    onClearError: () -> Unit
) {
    val appearance = currentVormexAppearance(
        fallbackThemeMode = if (isLightTheme) "light" else "dark"
    )
    val glassBackground = appearance.sheetColor
    val glassBubbleBackground = appearance.inputColor
    
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var searchQuery by remember { mutableStateOf("") }
    var shareMessage by remember { mutableStateOf("") }
    var selectedConnections by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCopiedToast by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .vormexSurface(
                    backdrop = backdrop,
                    tone = VormexSurfaceTone.Sheet,
                    cornerRadius = 28.dp,
                    blurRadius = 32.dp,
                    lensRadius = 16.dp,
                    lensDepth = 32.dp,
                    surfaceColor = glassBackground,
                    borderColor = appearance.sheetBorderColor
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Share Post",
                        style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                    )
                    
                    // Glass close button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(glassBubbleBackground.copy(alpha = 0.5f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                    }
                }
                
                // Error message
                error?.let { errorMsg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Red.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                text = errorMsg,
                                style = TextStyle(Color.Red.copy(alpha = 0.8f), 13.sp),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onClearError() }
                                    .padding(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Dismiss error",
                                    modifier = Modifier.size(14.dp),
                                    colorFilter = ColorFilter.tint(Color.Red)
                                )
                            }
                        }
                    }
                }
                
                // Quick share options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy link
                    ShareQuickAction(
                        iconRes = R.drawable.ic_link,
                        label = "Copy Link",
                        contentColor = contentColor,
                        glassBubbleBackground = glassBubbleBackground,
                        onClick = {
                            // In production, copy actual post URL
                            clipboardManager.setText(AnnotatedString("https://vormex.com/post/..."))
                            showCopiedToast = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Share to feed (repost)
                    ShareQuickAction(
                        iconRes = R.drawable.ic_repost,
                        label = "Repost",
                        contentColor = contentColor,
                        glassBubbleBackground = glassBubbleBackground,
                        onClick = {
                            onShareToConnections(emptyList(), null)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Copied toast
                if (showCopiedToast) {
                    LaunchedEffect(showCopiedToast) {
                        kotlinx.coroutines.delay(2000)
                        showCopiedToast = false
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF27ae60).copy(alpha = 0.2f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(Color(0xFF27ae60))
                            )
                            BasicText(
                                text = "Link copied to clipboard",
                                style = TextStyle(Color(0xFF27ae60), 14.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = contentColor.copy(alpha = 0.1f)
                )
                
                // Share message
                BasicText(
                    text = "Add a message (optional)",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                BasicTextField(
                    value = shareMessage,
                    onValueChange = { shareMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(glassBubbleBackground.copy(alpha = 0.5f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    maxLines = 3,
                    decorationBox = { innerTextField ->
                        Box {
                            if (shareMessage.isEmpty()) {
                                BasicText(
                                    text = "Say something about this post...",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Send to connections
                BasicText(
                    text = "Send to connections",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                // Connection search
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        onSearchConnections(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(glassBubbleBackground.copy(alpha = 0.5f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.5f))
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    BasicText(
                                        text = "Search connections...",
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )
                
                // Selected connections chips
                if (selectedConnections.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedConnections.take(3).forEach { connectionId ->
                            val connection = connections.find { it.id == connectionId }
                            connection?.let {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText(
                                            text = it.name?.split(" ")?.firstOrNull() ?: "User",
                                            style = TextStyle(accentColor, 12.sp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedConnections = selectedConnections - connectionId
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_close),
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(10.dp),
                                                colorFilter = ColorFilter.tint(accentColor.copy(alpha = 0.7f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (selectedConnections.size > 3) {
                            BasicText(
                                text = "+${selectedConnections.size - 3} more",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
                
                // Connection list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLoading) {
                        items(3) {
                            ConnectionSkeleton(contentColor)
                        }
                    } else {
                        items(connections, key = { it.id }) { connection ->
                            ConnectionItem(
                                connection = connection,
                                isSelected = connection.id in selectedConnections,
                                contentColor = contentColor,
                                accentColor = accentColor,
                                glassBubbleBackground = glassBubbleBackground,
                                onClick = {
                                    selectedConnections = if (connection.id in selectedConnections) {
                                        selectedConnections - connection.id
                                    } else {
                                        selectedConnections + connection.id
                                    }
                                }
                            )
                        }
                        
                        if (connections.isEmpty() && !isLoading && searchQuery.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        text = "No connections found",
                                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Share button
                if (selectedConnections.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ShareButtonGradient)
                            .clickable(enabled = !isSharing) {
                                onShareToConnections(
                                    selectedConnections.toList(),
                                    shareMessage.ifBlank { null }
                                )
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            BasicText(
                                text = "Send to ${selectedConnections.size} ${if (selectedConnections.size == 1) "person" else "people"}",
                                style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareQuickAction(
    @DrawableRes iconRes: Int,
    label: String,
    contentColor: Color,
    glassBubbleBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(glassBubbleBackground.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.8f))
            )
            BasicText(
                text = label,
                style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp, FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun ConnectionItem(
    connection: MentionUser,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    glassBubbleBackground: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else glassBubbleBackground.copy(alpha = 0.4f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!connection.avatar.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(connection.avatar).build(),
                    contentDescription = "${connection.name}'s avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initials = connection.name?.split(" ")
                    ?.mapNotNull { it.firstOrNull()?.uppercase() }
                    ?.take(2)
                    ?.joinToString("") ?: "?"
                BasicText(
                    text = initials,
                    style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                )
            }
        }
        
        // User info
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = connection.name ?: "Unknown",
                style = TextStyle(contentColor, 15.sp, FontWeight.Medium)
            )
            connection.headline?.let { headline ->
                BasicText(
                    text = headline,
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp),
                    maxLines = 1
                )
            }
        }
        
        // Selection indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) accentColor
                    else contentColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Selected",
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
    }
}

@Composable
private fun ConnectionSkeleton(contentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
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
 * Save/Bookmark Indicator - Visual feedback for saved state
 */
@Composable
fun SaveIndicator(
    isSaved: Boolean,
    contentColor: Color,
    accentColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isSaved) accentColor.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onToggle)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                if (isSaved) R.drawable.ic_bookmark
                else R.drawable.ic_bookmark_outline
            ),
            contentDescription = if (isSaved) "Saved" else "Save",
            modifier = Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(
                if (isSaved) accentColor else contentColor
            )
        )
    }
}

/**
 * Saved Posts Empty State
 */
@Composable
fun SavedPostsEmptyState(
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_bookmark_outline),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
            )
            BasicText(
                text = "No saved posts",
                style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
            )
            BasicText(
                text = "Save posts to read them later",
                style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
            )
        }
    }
}
