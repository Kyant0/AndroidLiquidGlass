package com.kyant.backdrop.catalog.linkedin.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.linkedin.VormexSurfaceTone
import com.kyant.backdrop.catalog.linkedin.currentVormexAppearance
import com.kyant.backdrop.catalog.linkedin.vormexSurface

/**
 * Create post card shown at top of feed
 * Shows avatar + "What's on your mind, {firstName}?" → opens Create Post
 */
@Composable
fun CreatePostCard(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    userInitials: String,
    userAvatar: String? = null,
    userName: String,
    onCreatePost: () -> Unit,
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onArticleClick: () -> Unit
) {
    val firstName = userName.split(" ").firstOrNull() ?: userName
    val appearance = currentVormexAppearance()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .vormexSurface(
                backdrop = backdrop,
                tone = VormexSurfaceTone.Card,
                cornerRadius = 24.dp,
                blurRadius = 16.dp,
                lensRadius = 0.dp,
                lensDepth = 0.dp
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Main input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreatePost),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatar.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userAvatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Your profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BasicText(
                            text = userInitials,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Placeholder text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(appearance.inputColor)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicText(
                        text = "What's on your mind, $firstName?",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(appearance.dividerColor)
            )
            
            // Quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = "📷",
                    label = "Photo",
                    contentColor = contentColor,
                    onClick = onPhotoClick
                )
                QuickActionButton(
                    icon = "🎥",
                    label = "Video",
                    contentColor = contentColor,
                    onClick = onVideoClick
                )
                QuickActionButton(
                    icon = "📝",
                    label = "Article",
                    contentColor = contentColor,
                    onClick = onArticleClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = icon,
            style = TextStyle(fontSize = 18.sp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        BasicText(
            text = label,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
