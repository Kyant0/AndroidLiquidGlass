package com.kyant.backdrop.catalog.linkedin.posts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.shapes.RoundedRectangle

// ==================== UI Styling Constants ====================

private val NeumorphicBackground = Color(0xFFecf0f3)
private val ShadowDark = Color(0xFFd1d9e6)
private val ShadowLight = Color(0xFFf9f9f9)
private val BorderColor = Color(0xFFd1d9e6)
private val PostButtonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4B70E2), Color(0xFF3a5bc7)),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(100f, 100f)
)

/**
 * Create Post Modal - Full-featured post creation with all post types
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostModal(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isCreating: Boolean,
    error: String?,
    userName: String,
    userAvatar: String?,
    initialPostType: PostType = PostType.TEXT,
    mentionSearchResults: List<MentionUser>,
    isSearchingMentions: Boolean,
    onDismiss: () -> Unit,
    onCreateTextPost: (content: String, visibility: String, mentions: List<String>) -> Unit,
    onCreateImagePost: (content: String?, visibility: String, images: List<Pair<ByteArray, String>>, mentions: List<String>) -> Unit,
    onCreateVideoPost: (content: String?, visibility: String, videoBytes: ByteArray, videoFilename: String, mentions: List<String>) -> Unit,
    onCreateLinkPost: (linkUrl: String, content: String?, visibility: String, mentions: List<String>) -> Unit,
    onCreatePollPost: (pollOptions: List<String>, pollDurationHours: Int, content: String?, visibility: String, showResultsBeforeVote: Boolean, mentions: List<String>) -> Unit,
    onCreateArticlePost: (articleTitle: String, content: String?, visibility: String, coverImage: Pair<ByteArray, String>?, articleTags: List<String>, mentions: List<String>) -> Unit,
    onCreateCelebrationPost: (celebrationType: String, content: String?, visibility: String, mentions: List<String>, celebrationGif: Pair<ByteArray, String>?) -> Unit,
    onSearchMentions: (String) -> Unit,
    onClearMentionSearch: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // State
    var selectedPostType by remember { mutableStateOf(initialPostType) }
    var content by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var showVisibilityDropdown by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    // Image state
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageBytes by remember { mutableStateOf<List<Pair<ByteArray, String>>>(emptyList()) }
    
    // Video state
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Link state
    var linkUrl by remember { mutableStateOf("") }
    
    // Poll state
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var pollDurationHours by remember { mutableIntStateOf(24) }
    var showResultsBeforeVote by remember { mutableStateOf(false) }
    
    // Article state
    var articleTitle by remember { mutableStateOf("") }
    var articleTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var articleTagInput by remember { mutableStateOf("") }
    var articleCoverUri by remember { mutableStateOf<Uri?>(null) }
    var articleCoverBytes by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    
    // Celebration state
    var selectedCelebrationType by remember { mutableStateOf<CelebrationType?>(null) }
    
    // Mention state
    var mentionQuery by remember { mutableStateOf("") }
    var selectedMentions by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMentionDropdown by remember { mutableStateOf(false) }
    
    // File pickers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newBytes = uris.mapNotNull { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val filename = uri.lastPathSegment ?: "image.jpg"
                        bytes to filename
                    }
                } catch (e: Exception) {
                    null
                }
            }
            imageUris = uris
            imageBytes = newBytes.take(10) // Max 10 images
        }
    }
    
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    val filename = it.lastPathSegment ?: "video.mp4"
                    videoUri = it
                    videoBytes = bytes to filename
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    val articleCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    val filename = it.lastPathSegment ?: "cover.jpg"
                    articleCoverUri = it
                    articleCoverBytes = bytes to filename
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Handle create post
    val handleCreatePost: () -> Unit = {
        when (selectedPostType) {
            PostType.TEXT -> {
                if (content.isNotBlank()) {
                    onCreateTextPost(content, visibility, selectedMentions)
                }
            }
            PostType.IMAGE -> {
                if (imageBytes.isNotEmpty()) {
                    onCreateImagePost(content.ifBlank { null }, visibility, imageBytes, selectedMentions)
                }
            }
            PostType.VIDEO -> {
                videoBytes?.let { (bytes, filename) ->
                    onCreateVideoPost(content.ifBlank { null }, visibility, bytes, filename, selectedMentions)
                }
                Unit
            }
            PostType.LINK -> {
                if (linkUrl.isNotBlank()) {
                    onCreateLinkPost(linkUrl, content.ifBlank { null }, visibility, selectedMentions)
                }
            }
            PostType.POLL -> {
                val validOptions = pollOptions.filter { it.isNotBlank() }
                if (validOptions.size >= 2) {
                    onCreatePollPost(validOptions, pollDurationHours, content.ifBlank { null }, visibility, showResultsBeforeVote, selectedMentions)
                }
            }
            PostType.ARTICLE -> {
                if (articleTitle.isNotBlank()) {
                    onCreateArticlePost(articleTitle, content.ifBlank { null }, visibility, articleCoverBytes, articleTags, selectedMentions)
                }
            }
            PostType.CELEBRATION -> {
                selectedCelebrationType?.let { type ->
                    onCreateCelebrationPost(type.name, content.ifBlank { null }, visibility, selectedMentions, null)
                }
                Unit
            }
            else -> {}
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(24f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(NeumorphicBackground.copy(alpha = 0.95f))
                    }
                )
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Header
                CreatePostHeader(
                    contentColor = contentColor,
                    isCreating = isCreating,
                    onClose = onDismiss,
                    onPost = handleCreatePost,
                    canPost = when (selectedPostType) {
                        PostType.TEXT -> content.isNotBlank()
                        PostType.IMAGE -> imageBytes.isNotEmpty()
                        PostType.VIDEO -> videoBytes != null
                        PostType.LINK -> linkUrl.isNotBlank()
                        PostType.POLL -> pollOptions.filter { it.isNotBlank() }.size >= 2
                        PostType.ARTICLE -> articleTitle.isNotBlank()
                        PostType.CELEBRATION -> selectedCelebrationType != null
                        else -> false
                    }
                )
                
                // Post type tabs
                PostTypeTabs(
                    selectedType = selectedPostType,
                    onTypeSelected = { selectedPostType = it },
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // User info and visibility
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userAvatar.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(userAvatar).build(),
                                    contentDescription = "Your profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = userName.split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString("")
                                BasicText(
                                    text = initials,
                                    style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                                )
                            }
                        }
                        
                        Column {
                            BasicText(
                                text = userName,
                                style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                            )
                            
                            // Visibility dropdown
                            Box {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(contentColor.copy(alpha = 0.08f))
                                        .clickable { showVisibilityDropdown = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText(
                                            text = when (visibility) {
                                                "PUBLIC" -> "🌐 Anyone"
                                                "CONNECTIONS" -> "👥 Connections"
                                                "PRIVATE" -> "🔒 Only me"
                                                else -> "🌐 Anyone"
                                            },
                                            style = TextStyle(contentColor.copy(alpha = 0.8f), 12.sp)
                                        )
                                        BasicText(" ▼", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = showVisibilityDropdown,
                                    onDismissRequest = { showVisibilityDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { BasicText("🌐 Anyone", style = TextStyle(contentColor, 14.sp)) },
                                        onClick = {
                                            visibility = "PUBLIC"
                                            showVisibilityDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { BasicText("👥 Connections only", style = TextStyle(contentColor, 14.sp)) },
                                        onClick = {
                                            visibility = "CONNECTIONS"
                                            showVisibilityDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { BasicText("🔒 Only me", style = TextStyle(contentColor, 14.sp)) },
                                        onClick = {
                                            visibility = "PRIVATE"
                                            showVisibilityDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Error message
                    error?.let { errorMsg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
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
                                    BasicText("✕", style = TextStyle(Color.Red.copy(alpha = 0.8f), 14.sp))
                                }
                            }
                        }
                    }
                    
                    // Content text area (for applicable post types)
                    if (selectedPostType != PostType.CELEBRATION) {
                        ContentTextArea(
                            content = content,
                            onContentChange = { newContent ->
                                content = newContent
                                // Check for @mention
                                val lastAtIndex = newContent.lastIndexOf('@')
                                if (lastAtIndex >= 0) {
                                    val textAfterAt = newContent.substring(lastAtIndex + 1)
                                    val spaceIndex = textAfterAt.indexOf(' ')
                                    val query = if (spaceIndex >= 0) null else textAfterAt
                                    if (query != null && query.length >= 2) {
                                        mentionQuery = query
                                        showMentionDropdown = true
                                        onSearchMentions(query)
                                    } else {
                                        showMentionDropdown = false
                                        onClearMentionSearch()
                                    }
                                }
                            },
                            placeholder = when (selectedPostType) {
                                PostType.TEXT -> "What's on your mind?"
                                PostType.IMAGE -> "Add a caption..."
                                PostType.VIDEO -> "Add a description..."
                                PostType.LINK -> "Add a comment about this link..."
                                PostType.POLL -> "Ask a question..."
                                PostType.ARTICLE -> "Write your article content..."
                                else -> "What's on your mind?"
                            },
                            contentColor = contentColor,
                            showColorPicker = showColorPicker,
                            onColorPickerToggle = { showColorPicker = it },
                            onColorSelected = { colorHex ->
                                // Insert color tag at cursor or wrap selection
                                content = "$content[color:$colorHex][/color]"
                            }
                        )
                        
                        // Mention dropdown
                        if (showMentionDropdown && mentionSearchResults.isNotEmpty()) {
                            MentionSuggestions(
                                suggestions = mentionSearchResults,
                                isLoading = isSearchingMentions,
                                contentColor = contentColor,
                                onSelect = { user ->
                                    // Replace @query with @username
                                    val lastAtIndex = content.lastIndexOf('@')
                                    if (lastAtIndex >= 0) {
                                        content = content.substring(0, lastAtIndex) + "@${user.username} "
                                        selectedMentions = selectedMentions + user.id
                                    }
                                    showMentionDropdown = false
                                    onClearMentionSearch()
                                }
                            )
                        }
                        
                        // Formatting toolbar
                        FormattingToolbar(
                            contentColor = contentColor,
                            onBold = { content = "$content**text**" },
                            onItalic = { content = "$content*text*" },
                            onList = { content = "$content\n- item" },
                            onCode = { content = "$content`code`" },
                            onMention = {
                                content = "$content@"
                                showMentionDropdown = true
                            },
                            onColor = { showColorPicker = true }
                        )
                    }
                    
                    // Type-specific fields
                    when (selectedPostType) {
                        PostType.IMAGE -> {
                            ImageTypeFields(
                                imageUris = imageUris,
                                onAddImages = { imagePicker.launch("image/*") },
                                onRemoveImage = { index ->
                                    imageUris = imageUris.filterIndexed { i, _ -> i != index }
                                    imageBytes = imageBytes.filterIndexed { i, _ -> i != index }
                                },
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                        PostType.VIDEO -> {
                            VideoTypeFields(
                                videoUri = videoUri,
                                onSelectVideo = { videoPicker.launch("video/*") },
                                onRemoveVideo = {
                                    videoUri = null
                                    videoBytes = null
                                },
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                        PostType.LINK -> {
                            LinkTypeFields(
                                linkUrl = linkUrl,
                                onLinkUrlChange = { linkUrl = it },
                                contentColor = contentColor
                            )
                        }
                        PostType.POLL -> {
                            PollTypeFields(
                                pollOptions = pollOptions,
                                onAddOption = {
                                    if (pollOptions.size < 6) {
                                        pollOptions = pollOptions + ""
                                    }
                                },
                                onRemoveOption = { index ->
                                    if (pollOptions.size > 2) {
                                        pollOptions = pollOptions.filterIndexed { i, _ -> i != index }
                                    }
                                },
                                onOptionChange = { index, value ->
                                    pollOptions = pollOptions.toMutableList().also { it[index] = value }
                                },
                                pollDurationHours = pollDurationHours,
                                onDurationChange = { pollDurationHours = it },
                                showResultsBeforeVote = showResultsBeforeVote,
                                onShowResultsChange = { showResultsBeforeVote = it },
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                        PostType.ARTICLE -> {
                            ArticleTypeFields(
                                articleTitle = articleTitle,
                                onTitleChange = { articleTitle = it },
                                articleTags = articleTags,
                                articleTagInput = articleTagInput,
                                onTagInputChange = { articleTagInput = it },
                                onAddTag = {
                                    if (articleTagInput.isNotBlank() && articleTags.size < 5) {
                                        articleTags = articleTags + articleTagInput.trim()
                                        articleTagInput = ""
                                    }
                                },
                                onRemoveTag = { tag ->
                                    articleTags = articleTags.filter { it != tag }
                                },
                                coverImageUri = articleCoverUri,
                                onSelectCover = { articleCoverPicker.launch("image/*") },
                                onRemoveCover = {
                                    articleCoverUri = null
                                    articleCoverBytes = null
                                },
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                        PostType.CELEBRATION -> {
                            CelebrationTypeFields(
                                selectedType = selectedCelebrationType,
                                onTypeSelected = { selectedCelebrationType = it },
                                content = content,
                                onContentChange = { content = it },
                                contentColor = contentColor,
                                accentColor = accentColor
                            )
                        }
                        else -> {}
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun CreatePostHeader(
    contentColor: Color,
    isCreating: Boolean,
    onClose: () -> Unit,
    onPost: () -> Unit,
    canPost: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.08f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            BasicText("✕", style = TextStyle(contentColor, 18.sp))
        }
        
        BasicText(
            text = "Create Post",
            style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
        )
        
        // Post button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (canPost && !isCreating) PostButtonGradient
                    else Brush.linearGradient(
                        colors = listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f))
                    )
                )
                .clickable(enabled = canPost && !isCreating, onClick = onPost)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    text = "Post",
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun PostTypeTabs(
    selectedType: PostType,
    onTypeSelected: (PostType) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    val types = listOf(
        PostType.TEXT to "Text",
        PostType.IMAGE to "Image",
        PostType.VIDEO to "Video",
        PostType.LINK to "Link",
        PostType.POLL to "Poll",
        PostType.ARTICLE to "Article",
        PostType.CELEBRATION to "Celebration"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (type, label) ->
            val isSelected = type == selectedType
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) accentColor.copy(alpha = 0.2f)
                        else contentColor.copy(alpha = 0.05f)
                    )
                    .clickable { onTypeSelected(type) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                BasicText(
                    text = label,
                    style = TextStyle(
                        color = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
private fun ContentTextArea(
    content: String,
    onContentChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    showColorPicker: Boolean,
    onColorPickerToggle: (Boolean) -> Unit,
    onColorSelected: (String) -> Unit
) {
    Column {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .padding(16.dp),
            textStyle = TextStyle(contentColor, 16.sp),
            cursorBrush = SolidColor(contentColor),
            decorationBox = { innerTextField ->
                Box {
                    if (content.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // Color picker
        if (showColorPicker) {
            ColorPicker(
                onColorSelected = { hex ->
                    onColorSelected(hex)
                    onColorPickerToggle(false)
                },
                onDismiss = { onColorPickerToggle(false) },
                contentColor = contentColor
            )
        }
    }
}

@Composable
private fun ColorPicker(
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    contentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "Select Color",
                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(4.dp)
            ) {
                BasicText("✕", style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp))
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Color grid (4x2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorPreset.presets.take(4).forEach { preset ->
                    ColorButton(
                        color = Color(preset.color),
                        onClick = { onColorSelected(preset.hex) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorPreset.presets.drop(4).forEach { preset ->
                    ColorButton(
                        color = Color(preset.color),
                        onClick = { onColorSelected(preset.hex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FormattingToolbar(
    contentColor: Color,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onList: () -> Unit,
    onCode: () -> Unit,
    onMention: () -> Unit,
    onColor: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormatButton("B", "Bold", contentColor, onBold)
        FormatButton("I", "Italic", contentColor, onItalic)
        FormatButton("•", "List", contentColor, onList)
        FormatButton("<>", "Code", contentColor, onCode)
        FormatButton("@", "Mention", contentColor, onMention)
        FormatButton("🎨", "Color", contentColor, onColor)
    }
}

@Composable
private fun FormatButton(
    icon: String,
    label: String,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        BasicText(
            text = icon,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun MentionSuggestions(
    suggestions: List<MentionUser>,
    isLoading: Boolean,
    contentColor: Color,
    onSelect: (MentionUser) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(8.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        } else {
            suggestions.take(5).forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(user) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!user.avatar.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(user.avatar)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            BasicText(
                                text = user.name?.firstOrNull()?.uppercase() ?: "?",
                                style = TextStyle(Color.White, 14.sp, FontWeight.Bold)
                            )
                        }
                    }
                    
                    Column {
                        BasicText(
                            text = user.name ?: "Unknown",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        user.username?.let {
                            BasicText(
                                text = "@$it",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Type-specific Fields ====================

@Composable
private fun ImageTypeFields(
    imageUris: List<Uri>,
    onAddImages: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "Images (${imageUris.size}/10)",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        if (imageUris.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageUris.forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.1f))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uri)
                                .build(),
                            contentDescription = "Selected image ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Remove button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onRemoveImage(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("✕", style = TextStyle(Color.White, 12.sp))
                        }
                    }
                }
            }
        }
        
        if (imageUris.size < 10) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable(onClick = onAddImages)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText("📷", style = TextStyle(fontSize = 20.sp))
                    BasicText(
                        text = "Add Images",
                        style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoTypeFields(
    videoUri: Uri?,
    onSelectVideo: () -> Unit,
    onRemoveVideo: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "Video",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        if (videoUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "🎥 Video selected",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onRemoveVideo)
                        .padding(8.dp)
                ) {
                    BasicText("✕", style = TextStyle(Color.White, 12.sp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable(onClick = onSelectVideo)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText("🎥", style = TextStyle(fontSize = 20.sp))
                    BasicText(
                        text = "Select Video (max 500MB)",
                        style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkTypeFields(
    linkUrl: String,
    onLinkUrlChange: (String) -> Unit,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "Link URL *",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        BasicTextField(
            value = linkUrl,
            onValueChange = onLinkUrlChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .padding(16.dp),
            textStyle = TextStyle(contentColor, 14.sp),
            cursorBrush = SolidColor(contentColor),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (linkUrl.isEmpty()) {
                        BasicText(
                            text = "https://example.com",
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PollTypeFields(
    pollOptions: List<String>,
    onAddOption: () -> Unit,
    onRemoveOption: (Int) -> Unit,
    onOptionChange: (Int, String) -> Unit,
    pollDurationHours: Int,
    onDurationChange: (Int) -> Unit,
    showResultsBeforeVote: Boolean,
    onShowResultsChange: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            text = "Poll Options (${pollOptions.size}/6)",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        pollOptions.forEachIndexed { index, option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = option,
                    onValueChange = { onOptionChange(index, it) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (option.isEmpty()) {
                                BasicText(
                                    text = "Option ${index + 1}",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                if (pollOptions.size > 2) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = 0.1f))
                            .clickable { onRemoveOption(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("✕", style = TextStyle(Color.Red, 14.sp))
                    }
                }
            }
        }
        
        if (pollOptions.size < 6) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable(onClick = onAddOption)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BasicText(
                    text = "+ Add Option",
                    style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                )
            }
        }
        
        // Duration selector
        BasicText(
            text = "Poll Duration",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PollDuration.entries.forEach { duration ->
                val isSelected = duration.hours == pollDurationHours
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.2f)
                            else contentColor.copy(alpha = 0.06f)
                        )
                        .clickable { onDurationChange(duration.hours) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        text = duration.label,
                        style = TextStyle(
                            color = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
        
        // Show results checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onShowResultsChange(!showResultsBeforeVote) }
        ) {
            Checkbox(
                checked = showResultsBeforeVote,
                onCheckedChange = onShowResultsChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicText(
                text = "Show results before voting",
                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp)
            )
        }
    }
}

@Composable
private fun ArticleTypeFields(
    articleTitle: String,
    onTitleChange: (String) -> Unit,
    articleTags: List<String>,
    articleTagInput: String,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    coverImageUri: Uri?,
    onSelectCover: () -> Unit,
    onRemoveCover: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title
        BasicText(
            text = "Article Title *",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        BasicTextField(
            value = articleTitle,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.06f))
                .padding(16.dp),
            textStyle = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
            cursorBrush = SolidColor(contentColor),
            decorationBox = { innerTextField ->
                Box {
                    if (articleTitle.isEmpty()) {
                        BasicText(
                            text = "Enter article title...",
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // Cover image
        BasicText(
            text = "Cover Image (optional)",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        if (coverImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverImageUri)
                        .build(),
                    contentDescription = "Cover image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onRemoveCover)
                        .padding(8.dp)
                ) {
                    BasicText("✕", style = TextStyle(Color.White, 12.sp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable(onClick = onSelectCover)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText("🖼️", style = TextStyle(fontSize = 18.sp))
                    BasicText(
                        text = "Add Cover Image",
                        style = TextStyle(accentColor, 14.sp, FontWeight.Medium)
                    )
                }
            }
        }
        
        // Tags
        BasicText(
            text = "Tags (${articleTags.size}/5)",
            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
        )
        
        if (articleTags.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                articleTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                text = tag,
                                style = TextStyle(accentColor, 12.sp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemoveTag(tag) },
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("✕", style = TextStyle(accentColor.copy(alpha = 0.7f), 12.sp))
                            }
                        }
                    }
                }
            }
        }
        
        if (articleTags.size < 5) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = articleTagInput,
                    onValueChange = onTagInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.06f))
                        .padding(12.dp),
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(contentColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box {
                            if (articleTagInput.isEmpty()) {
                                BasicText(
                                    text = "Add tag...",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .clickable(enabled = articleTagInput.isNotBlank(), onClick = onAddTag)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    BasicText(
                        text = "Add",
                        style = TextStyle(
                            color = if (articleTagInput.isNotBlank()) accentColor else contentColor.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CelebrationTypeFields(
    selectedType: CelebrationType?,
    onTypeSelected: (CelebrationType) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(
            text = "What are you celebrating?",
            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
        )
        
        // Celebration type grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CelebrationType.entries.forEach { type ->
                val isSelected = type == selectedType
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.15f)
                            else contentColor.copy(alpha = 0.06f)
                        )
                        .clickable { onTypeSelected(type) }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BasicText(
                            text = type.emoji,
                            style = TextStyle(fontSize = 28.sp)
                        )
                        Column {
                            BasicText(
                                text = type.label,
                                style = TextStyle(
                                    color = if (isSelected) accentColor else contentColor,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isSelected) {
                            BasicText(
                                text = "✓",
                                style = TextStyle(accentColor, 18.sp, FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
        
        // Optional message
        if (selectedType != null) {
            BasicText(
                text = "Add a message (optional)",
                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
            )
            
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(contentColor.copy(alpha = 0.06f))
                    .padding(16.dp),
                textStyle = TextStyle(contentColor, 16.sp),
                cursorBrush = SolidColor(contentColor),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            BasicText(
                                text = "Share more about your ${selectedType.label.lowercase()}...",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 16.sp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
