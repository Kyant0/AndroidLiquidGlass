package com.kyant.backdrop.catalog.linkedin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import kotlin.math.roundToInt

// ==================== Main Profile Screen ====================

@Composable
fun ProfileScreen(
    userId: String? = null,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    onOpenFeedItem: (FeedItem) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(context))
    val uiState by viewModel.uiState.collectAsState()
    
    // Theme detection
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = "glass")
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    val isLightTheme = themeMode == "light"
    
    // Project screen state
    var showAddProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var viewingProject by remember { mutableStateOf<Project?>(null) }
    
    // Experience screen state
    var showAddExperience by remember { mutableStateOf(false) }
    var editingExperience by remember { mutableStateOf<Experience?>(null) }
    
    // Education screen state
    var showAddEducation by remember { mutableStateOf(false) }
    var editingEducation by remember { mutableStateOf<Education?>(null) }
    
    // Certificate screen state
    var showAddCertificate by remember { mutableStateOf(false) }
    var editingCertificate by remember { mutableStateOf<Certificate?>(null) }
    var viewingCertificate by remember { mutableStateOf<Certificate?>(null) }
    
    // Achievement screen state
    var showAddAchievement by remember { mutableStateOf(false) }
    var editingAchievement by remember { mutableStateOf<Achievement?>(null) }
    var viewingAchievement by remember { mutableStateOf<Achievement?>(null) }
    
    // Skills screen state
    var showAddSkill by remember { mutableStateOf(false) }
    
    // Handle back button for profile overlays, or navigate back from profile
    BackHandler(
        enabled = showAddProject || editingProject != null || viewingProject != null ||
                showAddExperience || editingExperience != null ||
                showAddEducation || editingEducation != null ||
                showAddCertificate || editingCertificate != null || viewingCertificate != null ||
                showAddAchievement || editingAchievement != null || viewingAchievement != null ||
                showAddSkill ||
                true // Always enabled to handle back from profile screen
    ) {
        when {
            // Close any open dialogs/overlays first
            showAddProject -> showAddProject = false
            editingProject != null -> editingProject = null
            viewingProject != null -> viewingProject = null
            showAddExperience -> showAddExperience = false
            editingExperience != null -> editingExperience = null
            showAddEducation -> showAddEducation = false
            editingEducation != null -> editingEducation = null
            showAddCertificate -> showAddCertificate = false
            editingCertificate != null -> editingCertificate = null
            viewingCertificate != null -> viewingCertificate = null
            showAddAchievement -> showAddAchievement = false
            editingAchievement != null -> editingAchievement = null
            viewingAchievement != null -> viewingAchievement = null
            showAddSkill -> showAddSkill = false
            // If no overlays open, navigate back from profile
            else -> onNavigateBack()
        }
    }
    
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }
    
    Box(Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                ProfileSkeleton(backdrop, isLightTheme, isGlassTheme)
            }
            uiState.error != null -> {
                ProfileError(
                    error = uiState.error!!,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onRetry = { viewModel.retry() }
                )
            }
            uiState.profile != null -> {
                ProfileContent(
                    uiState = uiState,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isDarkTheme = isDarkTheme,
                    showBackButton = userId != null,
                    onNavigateBack = onNavigateBack,
                    onConnect = { viewModel.sendConnectionRequest() },
                    onCancelRequest = { viewModel.cancelConnectionRequest() },
                    onAcceptRequest = { viewModel.acceptConnectionRequest() },
                    onRejectRequest = { viewModel.rejectConnectionRequest() },
                    onRemoveConnection = { viewModel.removeConnection() },
                    onToggleFollow = { viewModel.toggleFollow() },
                    onFilterChange = { viewModel.setFeedFilter(it) },
                    onLoadMore = { viewModel.loadMoreFeed() },
                    onYearChange = { viewModel.loadActivityForYear(it) },
                    onEditBio = { viewModel.startEditingBio() },
                    onSaveBio = { viewModel.saveEditedBio() },
                    onCancelEditBio = { viewModel.cancelEditingBio() },
                    onBioChange = { viewModel.updateEditedBio(it) },
                    onToggleOpenToWork = { viewModel.updateOpenToOpportunities(it) },
                    onMessage = onMessage,
                    onOpenFeedItem = onOpenFeedItem,
                    onVotePoll = { postId, optionId -> viewModel.votePoll(postId, optionId) },
                    onUploadAvatar = { viewModel.uploadAvatar(it) },
                    onUploadBanner = { viewModel.uploadBanner(it) },
                    // Project callbacks
                    onAddProject = { showAddProject = true },
                    onEditProject = { editingProject = it },
                    onViewProject = { viewingProject = it },
                    onToggleProjectFeatured = { viewModel.toggleProjectFeatured(it.id) },
                    // Experience callbacks
                    onAddExperience = { showAddExperience = true },
                    onEditExperience = { editingExperience = it },
                    onViewExperience = { /* Experiences view in place */ },
                    // Education callbacks
                    onAddEducation = { showAddEducation = true },
                    onEditEducation = { editingEducation = it },
                    onViewEducation = { /* Education view in place */ },
                    // Certificate callbacks
                    onAddCertificate = { showAddCertificate = true },
                    onEditCertificate = { editingCertificate = it },
                    onViewCertificate = { viewingCertificate = it },
                    // Achievement callbacks
                    onAddAchievement = { showAddAchievement = true },
                    onEditAchievement = { editingAchievement = it },
                    onViewAchievement = { viewingAchievement = it },
                    // Skills callbacks
                    onAddSkill = { showAddSkill = true },
                    onRemoveSkill = { skill -> viewModel.removeLocalSkill(skill.id) },
                    onDeleteFeedPost = { postId ->
                        viewModel.deleteFeedPost(
                            postId = postId,
                            onSuccess = {},
                            onError = { }
                        )
                    }
                )
            }
        }
        
        // Add Project Screen
        if (showAddProject) {
            AddEditProjectScreen(
                project = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { showAddProject = false },
                onDelete = null,
                onCancel = { showAddProject = false }
            )
        }
        
        // Edit Project Screen
        editingProject?.let { project ->
            AddEditProjectScreen(
                project = project,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { editingProject = null },
                onDelete = {
                    viewModel.deleteProject(
                        projectId = project.id,
                        onSuccess = { editingProject = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingProject = null }
            )
        }
        
        // Project Detail Screen
        viewingProject?.let { project ->
            ProjectDetailScreen(
                project = project,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = uiState.isOwner,
                onEdit = {
                    viewingProject = null
                    editingProject = project
                },
                onBack = { viewingProject = null }
            )
        }
        
        // Add Experience Screen
        if (showAddExperience) {
            AddEditExperienceScreen(
                experience = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedExperience ->
                    viewModel.addExperience(savedExperience)
                    showAddExperience = false
                },
                onDelete = null,
                onCancel = { showAddExperience = false }
            )
        }
        
        // Add Skill Screen
        if (showAddSkill) {
            AddSkillDialog(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { name, proficiency ->
                    viewModel.addLocalSkill(name = name, proficiency = proficiency)
                    showAddSkill = false
                },
                onCancel = { showAddSkill = false }
            )
        }
        
        // Edit Experience Screen
        editingExperience?.let { experience ->
            AddEditExperienceScreen(
                experience = experience,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedExperience ->
                    viewModel.updateExperience(savedExperience)
                    editingExperience = null
                },
                onDelete = {
                    viewModel.deleteExperience(
                        experienceId = experience.id,
                        onSuccess = { editingExperience = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingExperience = null }
            )
        }
        
        // Add Education Screen
        if (showAddEducation) {
            AddEditEducationScreen(
                education = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedEducation ->
                    viewModel.addEducation(savedEducation)
                    showAddEducation = false
                },
                onDelete = null,
                onCancel = { showAddEducation = false }
            )
        }
        
        // Edit Education Screen
        editingEducation?.let { education ->
            AddEditEducationScreen(
                education = education,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedEducation ->
                    viewModel.updateEducation(savedEducation)
                    editingEducation = null
                },
                onDelete = {
                    viewModel.deleteEducation(
                        educationId = education.id,
                        onSuccess = { editingEducation = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingEducation = null }
            )
        }
        
        // Add Certificate Screen
        if (showAddCertificate) {
            AddEditCertificateScreen(
                certificate = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedCertificate ->
                    viewModel.addCertificate(savedCertificate)
                    showAddCertificate = false
                },
                onDelete = null,
                onCancel = { showAddCertificate = false }
            )
        }
        
        // Edit Certificate Screen
        editingCertificate?.let { certificate ->
            AddEditCertificateScreen(
                certificate = certificate,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedCertificate ->
                    viewModel.updateCertificate(savedCertificate)
                    editingCertificate = null
                },
                onDelete = {
                    viewModel.deleteCertificate(
                        certificateId = certificate.id,
                        onSuccess = { editingCertificate = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingCertificate = null }
            )
        }
        
        // View Certificate Detail Modal
        viewingCertificate?.let { certificate ->
            CertificateDetailModal(
                certificate = certificate,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onDismiss = { viewingCertificate = null }
            )
        }
        
        // Add Achievement Screen
        if (showAddAchievement) {
            AddEditAchievementScreen(
                achievement = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedAchievement ->
                    viewModel.addAchievement(savedAchievement)
                    showAddAchievement = false
                },
                onDelete = null,
                onCancel = { showAddAchievement = false }
            )
        }
        
        // Edit Achievement Screen
        editingAchievement?.let { achievement ->
            AddEditAchievementScreen(
                achievement = achievement,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedAchievement ->
                    viewModel.updateAchievement(savedAchievement)
                    editingAchievement = null
                },
                onDelete = {
                    viewModel.deleteAchievement(
                        achievementId = achievement.id,
                        onSuccess = { editingAchievement = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingAchievement = null }
            )
        }
        
        // View Achievement Detail Modal
        viewingAchievement?.let { achievement ->
            AchievementDetailModal(
                achievement = achievement,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onDismiss = { viewingAchievement = null }
            )
        }
    }
}

@Composable
private fun AddSkillDialog(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (String, String?) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var proficiency by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.14f)) }
                )
                .padding(20.dp)
                .clickable(enabled = false) { }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    "Add skill",
                    style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
                )

                // Skill name
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (name.isEmpty()) {
                                BasicText(
                                    "Skill name (e.g. Kotlin, UI/UX)",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            inner()
                        }
                    }
                )

                // Proficiency (optional)
                BasicTextField(
                    value = proficiency,
                    onValueChange = { proficiency = it },
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(contentColor.copy(alpha = 0.06f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (proficiency.isEmpty()) {
                                BasicText(
                                    "Proficiency (e.g. Beginner, Intermediate, Expert)",
                                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                )
                            }
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    BasicText(
                        "Cancel",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
                        modifier = Modifier
                            .clickable { onCancel() }
                            .padding(8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (name.isNotBlank()) accentColor
                                else accentColor.copy(alpha = 0.4f)
                            )
                            .clickable(enabled = name.isNotBlank()) {
                                onSave(name, proficiency.takeIf { it.isNotBlank() })
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            "Save",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onConnect: () -> Unit,
    onCancelRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onRemoveConnection: () -> Unit,
    onToggleFollow: () -> Unit,
    onFilterChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onYearChange: (Int) -> Unit,
    onEditBio: () -> Unit,
    onSaveBio: () -> Unit,
    onCancelEditBio: () -> Unit,
    onBioChange: (String) -> Unit,
    onToggleOpenToWork: (Boolean) -> Unit,
    onMessage: (String) -> Unit,
    onOpenFeedItem: (FeedItem) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit,
    onUploadBanner: (ByteArray) -> Unit,
    // Project callbacks
    onAddProject: () -> Unit = {},
    onEditProject: (Project) -> Unit = {},
    onViewProject: (Project) -> Unit = {},
    onToggleProjectFeatured: (Project) -> Unit = {},
    // Experience callbacks
    onAddExperience: () -> Unit = {},
    onEditExperience: (Experience) -> Unit = {},
    onViewExperience: (Experience) -> Unit = {},
    // Education callbacks
    onAddEducation: () -> Unit = {},
    onEditEducation: (Education) -> Unit = {},
    onViewEducation: (Education) -> Unit = {},
    // Certificate callbacks
    onAddCertificate: () -> Unit = {},
    onEditCertificate: (Certificate) -> Unit = {},
    onViewCertificate: (Certificate) -> Unit = {},
    // Achievement callbacks
    onAddAchievement: () -> Unit = {},
    onEditAchievement: (Achievement) -> Unit = {},
    onViewAchievement: (Achievement) -> Unit = {},
    // Skills callbacks
    onAddSkill: () -> Unit = {},
    onRemoveSkill: (UserSkill) -> Unit = {},
    onDeleteFeedPost: (String) -> Unit = {}
) {
    val profile = uiState.profile!!
    // Derive isLightTheme for components that need it
    val isLightTheme = !isDarkTheme
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Back button for viewing other profiles
        if (showBackButton) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            when {
                                isGlassTheme -> Modifier.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedRectangle(0f.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(16f.dp.toPx())
                                        lens(8f.dp.toPx(), 16f.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.12f))
                                    }
                                )
                                isDarkTheme -> Modifier.background(Color(0xFF1E1E1E))
                                else -> Modifier.background(Color.White) // Light theme
                            }
                        )
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(
                                when {
                                    isGlassTheme -> Modifier.drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedRectangle(20f.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(12f.dp.toPx())
                                            lens(6f.dp.toPx(), 12f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.15f))
                                        }
                                    )
                                    isDarkTheme -> Modifier.background(Color.White.copy(alpha = 0.1f))
                                    else -> Modifier.background(Color.Black.copy(alpha = 0.05f)) // Light theme
                                }
                            )
                            .clickable(onClick = onNavigateBack)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    BasicText(
                        text = profile.user.name,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        // Header Section
        item {
            ProfileHeader(
                user = profile.user,
                stats = profile.stats,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isDarkTheme = isDarkTheme,
                isOwner = uiState.isOwner,
                connectionStatus = uiState.connectionStatus,
                isFollowing = uiState.isFollowing,
                isFollowedBy = uiState.isFollowedBy,
                connectionActionInProgress = uiState.connectionActionInProgress,
                followActionInProgress = uiState.followActionInProgress,
                mutualConnections = uiState.mutualConnections,
                mutualConnectionsCount = uiState.mutualConnectionsCount,
                isUploadingAvatar = uiState.isUploadingAvatar,
                isUploadingBanner = uiState.isUploadingBanner,
                onConnect = onConnect,
                onCancelRequest = onCancelRequest,
                onAcceptRequest = onAcceptRequest,
                onRemoveConnection = onRemoveConnection,
                    onToggleFollow = onToggleFollow,
                    onMessage = onMessage,
                    onUploadAvatar = onUploadAvatar,
                    onUploadBanner = onUploadBanner
            )
        }
        
        // About Section
        item {
            Spacer(Modifier.height(12.dp))
            AboutSection(
                user = profile.user,
                stats = profile.stats,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isOwner = uiState.isOwner,
                isEditingBio = uiState.isEditingBio,
                editedBio = uiState.editedBio,
                onEditBio = onEditBio,
                onSaveBio = onSaveBio,
                onCancelEditBio = onCancelEditBio,
                onBioChange = onBioChange,
                onToggleOpenToWork = onToggleOpenToWork
            )
        }
        
        // GitHub Stats Section
        if (profile.github.connected || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                GitHubSection(
                    github = profile.github,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner
                )
            }
        }
        
        // Activity Calendar Section
        item {
            Spacer(Modifier.height(12.dp))
            ActivityCalendarSection(
                heatmap = uiState.activityHeatmap,
                stats = profile.stats,
                availableYears = uiState.availableYears,
                selectedYear = uiState.selectedYear,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onYearChange = onYearChange
            )
        }
        
        // Skills Section
        if (profile.skills.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                SkillsSection(
                    skills = profile.skills,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onAddSkill = onAddSkill,
                    onRemoveSkill = onRemoveSkill
                )
            }
        }
        
        // Projects Section
        if (profile.projects.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                ProjectsSection(
                    projects = profile.projects,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onAddProject = onAddProject,
                    onEditProject = onEditProject,
                    onViewProject = onViewProject,
                    onToggleFeatured = onToggleProjectFeatured
                )
            }
        }
        
        // Experience Section
        if (profile.experiences.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                ExperienceSection(
                    experiences = profile.experiences,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onAddExperience = onAddExperience,
                    onEditExperience = onEditExperience,
                    onViewExperience = onViewExperience
                )
            }
        }
        
        // Education Section
        if (profile.education.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                EducationSection(
                    education = profile.education,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onAddEducation = onAddEducation,
                    onEditEducation = onEditEducation,
                    onViewEducation = onViewEducation
                )
            }
        }
        
        // Certificates Section
        if (profile.certificates.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                CertificatesSection(
                    certificates = profile.certificates,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onAddCertificate = onAddCertificate,
                    onEditCertificate = onEditCertificate,
                    onViewCertificate = onViewCertificate
                )
            }
        }
        
        // Achievements Section
        if (profile.achievements.isNotEmpty() || uiState.isOwner) {
            item {
                Spacer(Modifier.height(12.dp))
                AchievementsSection(
                    achievements = profile.achievements,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onAddAchievement = onAddAchievement,
                    onEditAchievement = onEditAchievement,
                    onViewAchievement = onViewAchievement
                )
            }
        }
        
        // Activity Feed Section
        item {
            Spacer(Modifier.height(12.dp))
            ActivityFeedSection(
                feedItems = uiState.feedItems,
                currentFilter = uiState.feedFilter,
                isLoading = uiState.isLoadingFeed,
                hasMore = uiState.feedHasMore,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isLightTheme = isLightTheme,
                isOwner = uiState.isOwner,
                onFilterChange = onFilterChange,
                onLoadMore = onLoadMore,
                onOpenItem = onOpenFeedItem,
                onVotePoll = onVotePoll,
                onDeletePost = onDeleteFeedPost
            )
        }
    }
}

// ==================== Profile Header ====================

@Composable
private fun ProfileHeader(
    user: ProfileUser,
    stats: ProfileStats,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    isOwner: Boolean,
    connectionStatus: String,
    isFollowing: Boolean,
    isFollowedBy: Boolean,
    connectionActionInProgress: Boolean,
    followActionInProgress: Boolean,
    mutualConnections: List<MutualConnection>,
    mutualConnectionsCount: Int,
    isUploadingAvatar: Boolean = false,
    isUploadingBanner: Boolean = false,
    onConnect: () -> Unit,
    onCancelRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRemoveConnection: () -> Unit,
    onToggleFollow: () -> Unit,
    onMessage: (String) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit = {},
    onUploadBanner: (ByteArray) -> Unit = {}
) {
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }
    var showConnectionMenu by remember { mutableStateOf(false) }
    
    // Image editor state
    var showAvatarEditor by remember { mutableStateOf(false) }
    var showBannerEditor by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    
    // Cache key to force image refresh
    var avatarCacheKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var bannerCacheKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Image pickers
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                selectedImageBytes = bytes
                showAvatarEditor = true
            }
        }
    }
    
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bytes = stream.readBytes()
                selectedImageBytes = bytes
                showBannerEditor = true
            }
        }
    }
    
    // Avatar Editor Dialog
    if (showAvatarEditor && selectedImageBytes != null) {
        ImageEditorDialog(
            imageBytes = selectedImageBytes!!,
            isForAvatar = true,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onSave = { bytes ->
                onUploadAvatar(bytes)
                avatarCacheKey = System.currentTimeMillis() // Force cache refresh
                showAvatarEditor = false
                selectedImageBytes = null
            },
            onDismiss = {
                showAvatarEditor = false
                selectedImageBytes = null
            }
        )
    }
    
    // Banner Editor Dialog
    if (showBannerEditor && selectedImageBytes != null) {
        ImageEditorDialog(
            imageBytes = selectedImageBytes!!,
            isForAvatar = false,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            onSave = { bytes ->
                onUploadBanner(bytes)
                bannerCacheKey = System.currentTimeMillis() // Force cache refresh
                showBannerEditor = false
                selectedImageBytes = null
            },
            onDismiss = {
                showBannerEditor = false
                selectedImageBytes = null
            }
        )
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                when {
                    isGlassTheme -> Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24f.dp) },
                        effects = {
                            vibrancy()
                            blur(16f.dp.toPx())
                            lens(8f.dp.toPx(), 16f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.12f))
                        }
                    )
                    isDarkTheme -> Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E1E1E))
                    else -> Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                }
            )
    ) {
        Column {
            // Banner
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .then(
                        if (user.bannerImageUrl != null)
                            Modifier.background(Color.Transparent)
                        else when {
                            isGlassTheme -> Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(0f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(20f.dp.toPx())
                                    lens(10f.dp.toPx(), 20f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(
                                        Brush.verticalGradient(
                                            listOf(accentColor.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f))
                                        )
                                    )
                                }
                            )
                            isDarkTheme -> Modifier.background(
                                Brush.verticalGradient(
                                    listOf(accentColor.copy(alpha = 0.4f), Color(0xFF2D2D2D))
                                )
                            )
                            else -> Modifier.background(
                                Brush.verticalGradient(
                                    listOf(accentColor.copy(alpha = 0.3f), Color(0xFFF0F0F0))
                                )
                            )
                        }
                    )
            ) {
                user.bannerImageUrl?.let { url ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCacheKey("banner_${user.id}_$bannerCacheKey")
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Edit cover button (owner only)
                if (isOwner) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = !isUploadingBanner) { 
                                bannerPicker.launch("image/*") 
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isUploadingBanner) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_camera),
                                    contentDescription = "Edit cover",
                                    modifier = Modifier.size(14.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                                BasicText(
                                    "Edit cover",
                                    style = TextStyle(Color.White, 12.sp)
                                )
                            }
                        }
                    }
                }
            }
            
            Column(Modifier.padding(horizontal = 16.dp)) {
                // Avatar Row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = (-36).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Avatar
                    Box(contentAlignment = Alignment.Center) {
                        // Profile ring
                        if (user.profileRing != null) {
                            Box(
                                Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(
                                                Color(0xFFFF6B6B),
                                                Color(0xFFFFE66D),
                                                Color(0xFF4ECDC4),
                                                Color(0xFF9B59B6),
                                                Color(0xFFFF6B6B)
                                            )
                                        )
                                    )
                            )
                        }
                        
                        Box(
                            Modifier
                                .size(if (user.profileRing != null) 84.dp else 88.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user.avatar.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(user.avatar)
                                        .memoryCacheKey("avatar_${user.id}_$avatarCacheKey")
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = user.name
                                    .split(" ")
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .take(2)
                                    .joinToString("")
                                BasicText(
                                    initials,
                                    style = TextStyle(Color.White, 28.sp, FontWeight.Bold)
                                )
                            }
                            
                            // Verified badge
                            if (user.verified) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("✓", style = TextStyle(Color.White, 12.sp, FontWeight.Bold))
                                }
                            }
                        }
                        
                        // Edit avatar button (owner)
                        if (isOwner) {
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                                    .clickable(enabled = !isUploadingAvatar) { 
                                        avatarPicker.launch("image/*") 
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.ic_camera),
                                        contentDescription = "Edit avatar",
                                        modifier = Modifier.size(14.dp),
                                        colorFilter = ColorFilter.tint(Color.White)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwner) {
                            // Edit Profile button
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(accentColor)
                                    .clickable { /* TODO: Edit profile */ }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    "Edit Profile",
                                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                )
                            }
                            
                            // Share button
                            Box {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(contentColor.copy(alpha = 0.1f))
                                        .clickable { showShareMenu = true }
                                        .padding(10.dp)
                                ) {
                                    BasicText("↗", style = TextStyle(contentColor, 16.sp))
                                }
                                
                                DropdownMenu(
                                    expanded = showShareMenu,
                                    onDismissRequest = { showShareMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { BasicText("Copy profile link", style = TextStyle(contentColor)) },
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", "https://vormex.com/@${user.username}"))
                                            showShareMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { BasicText("Share profile", style = TextStyle(contentColor)) },
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Check out ${user.name}'s profile on Vormex: https://vormex.com/@${user.username}")
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share profile"))
                                            showShareMenu = false
                                        }
                                    )
                                }
                            }
                        } else {
                            // Connection button
                            ConnectionButton(
                                status = connectionStatus,
                                isLoading = connectionActionInProgress,
                                accentColor = accentColor,
                                contentColor = contentColor,
                                onConnect = onConnect,
                                onCancel = onCancelRequest,
                                onAccept = onAcceptRequest,
                                onRemove = onRemoveConnection,
                                showMenu = showConnectionMenu,
                                onMenuChange = { showConnectionMenu = it }
                            )

                            // Message button
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .then(
                                        when {
                                            isGlassTheme -> Modifier.drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { RoundedRectangle(20f.dp) },
                                                effects = {
                                                    vibrancy()
                                                    blur(12f.dp.toPx())
                                                    lens(6f.dp.toPx(), 12f.dp.toPx())
                                                },
                                                onDrawSurface = {
                                                    drawRect(Color.White.copy(alpha = 0.15f))
                                                }
                                            )
                                            isDarkTheme -> Modifier.background(Color.White.copy(alpha = 0.1f))
                                            else -> Modifier.background(Color.Black.copy(alpha = 0.05f))
                                        }
                                    )
                                    .clickable { onMessage(user.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_message),
                                        contentDescription = "Message",
                                        modifier = Modifier.size(16.dp),
                                        colorFilter = ColorFilter.tint(contentColor)
                                    )
                                    BasicText(
                                        "Message",
                                        style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                                    )
                                }
                            }
                            
                            // Menu button (Follow + Share profile)
                            Box {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .then(
                                            when {
                                                isGlassTheme -> Modifier.drawBackdrop(
                                                    backdrop = backdrop,
                                                    shape = { RoundedRectangle(18f.dp) },
                                                    effects = {
                                                        vibrancy()
                                                        blur(12f.dp.toPx())
                                                        lens(6f.dp.toPx(), 12f.dp.toPx())
                                                    },
                                                    onDrawSurface = {
                                                        drawRect(Color.White.copy(alpha = 0.15f))
                                                    }
                                                )
                                                isDarkTheme -> Modifier.background(Color.White.copy(alpha = 0.1f))
                                                else -> Modifier.background(Color.Black.copy(alpha = 0.05f))
                                            }
                                        )
                                        .clickable { showShareMenu = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_more),
                                        contentDescription = "More options",
                                        modifier = Modifier.size(20.dp),
                                        colorFilter = ColorFilter.tint(contentColor)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = showShareMenu,
                                    onDismissRequest = { showShareMenu = false }
                                ) {
                                    // Follow option
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_users),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(contentColor)
                                                )
                                                BasicText(
                                                    if (isFollowing) "Unfollow" else "Follow",
                                                    style = TextStyle(contentColor, 14.sp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            onToggleFollow()
                                            showShareMenu = false
                                        }
                                    )
                                    // Copy profile link
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_link),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(contentColor)
                                                )
                                                BasicText("Copy profile link", style = TextStyle(contentColor, 14.sp))
                                            }
                                        },
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Profile URL", "https://vormex.com/@${user.username}"))
                                            showShareMenu = false
                                        }
                                    )
                                    // Share profile
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(R.drawable.ic_share),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    colorFilter = ColorFilter.tint(contentColor)
                                                )
                                                BasicText("Share profile", style = TextStyle(contentColor, 14.sp))
                                            }
                                        },
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Check out ${user.name}'s profile on Vormex: https://vormex.com/@${user.username}")
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share profile"))
                                            showShareMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Name and badges
                Row(
                    Modifier.offset(y = (-24).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        user.name,
                        style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
                    )
                    
                    if (user.isOpenToOpportunities) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "#OpenToWork",
                                style = TextStyle(Color(0xFF22C55E), 12.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                // Username
                BasicText(
                    "@${user.username}",
                    style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp),
                    modifier = Modifier.offset(y = (-20).dp)
                )
                
                // Headline
                user.headline?.let { headline ->
                    BasicText(
                        headline,
                        style = TextStyle(contentColor, 14.sp),
                        modifier = Modifier.offset(y = (-16).dp)
                    )
                }
                
                // Location, College, Branch
                Row(
                    Modifier.offset(y = (-12).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    user.location?.let { location ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = "Location",
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.7f))
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                location,
                                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
                            )
                        }
                    }
                    
                    if (!user.college.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_education),
                                contentDescription = "Education",
                                modifier = Modifier.size(14.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.7f))
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                user.college ?: "",
                                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
                            )
                        }
                    }
                }
                
                // Social links
                Row(
                    Modifier.offset(y = (-8).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    user.linkedinUrl?.let { url ->
                        SocialLinkChip(
                            icon = "in",
                            label = "LinkedIn",
                            url = url,
                            accentColor = Color(0xFF0A66C2)
                        )
                    }
                    user.githubProfileUrl?.let { url ->
                        SocialLinkChip(
                            icon = "⌘",
                            label = "GitHub",
                            url = url,
                            accentColor = contentColor
                        )
                    }
                    user.portfolioUrl?.let { url ->
                        SocialLinkChip(
                            icon = "",
                            label = "Portfolio",
                            url = url,
                            accentColor = accentColor,
                            iconRes = R.drawable.ic_link
                        )
                    }
                }
                
                // Stats row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem(
                        value = formatNumber(stats.connectionsCount),
                        label = "connections",
                        contentColor = contentColor
                    )
                    StatItem(
                        value = formatNumber(stats.followersCount),
                        label = "followers",
                        contentColor = contentColor
                    )
                    StatItem(
                        value = formatNumber(stats.totalPosts),
                        label = "posts",
                        contentColor = contentColor
                    )
                    StatItem(
                        value = formatNumber(stats.totalLikesReceived),
                        label = "likes",
                        contentColor = contentColor
                    )
                }
                
                // Gamification row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Level badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        BasicText(
                            "Lvl ${stats.level}",
                            style = TextStyle(Color.Black, 12.sp, FontWeight.Bold)
                        )
                    }
                    
                    // XP
                    BasicText(
                        "⚡ ${formatNumber(stats.xp)} XP",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
                    )
                    
                    // Enhanced Streak Badge (clickable)
                    ProfileStreakBadge(
                        currentStreak = stats.currentStreak,
                        longestStreak = stats.longestStreak,
                        totalActiveDays = stats.totalActiveDays,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { /* Handled by onStreakClick callback */ }
                    )
                }
                
                // Mutual info (visitor only)
                if (!isOwner && mutualConnectionsCount > 0) {
                    Row(
                        Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mutual avatars
                        Row {
                            mutualConnections.take(3).forEachIndexed { index, mutual ->
                                Box(
                                    Modifier
                                        .offset(x = (-index * 8).dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!mutual.avatar.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = mutual.avatar,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        BasicText(
                                            mutual.name?.firstOrNull()?.uppercase() ?: "?",
                                            style = TextStyle(Color.White, 10.sp, FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        BasicText(
                            "$mutualConnectionsCount mutual connections",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                        )
                    }
                    
                    // Follows you badge
                    if (isFollowedBy) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "Follows you",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ConnectionButton(
    status: String,
    isLoading: Boolean,
    accentColor: Color,
    contentColor: Color,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onRemove: () -> Unit,
    showMenu: Boolean,
    onMenuChange: (Boolean) -> Unit
) {
    Box {
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when (status) {
                        "connected" -> Color(0xFF22C55E).copy(alpha = 0.2f)
                        "pending_sent" -> contentColor.copy(alpha = 0.1f)
                        "pending_received" -> accentColor
                        else -> accentColor
                    }
                )
                .clickable(enabled = !isLoading) {
                    when (status) {
                        "none" -> onConnect()
                        "pending_sent" -> onCancel()
                        "pending_received" -> onAccept()
                        "connected" -> onMenuChange(true)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (status == "pending_received") Color.White else accentColor
                )
            } else {
                BasicText(
                    when (status) {
                        "connected" -> "✓ Connected"
                        "pending_sent" -> "Pending"
                        "pending_received" -> "Accept"
                        else -> "Connect"
                    },
                    style = TextStyle(
                        when (status) {
                            "pending_received" -> Color.White
                            "connected" -> Color(0xFF22C55E)
                            else -> Color.White
                        },
                        14.sp,
                        FontWeight.SemiBold
                    )
                )
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onMenuChange(false) }
        ) {
            DropdownMenuItem(
                text = { BasicText("Remove Connection", style = TextStyle(Color.Red)) },
                onClick = {
                    onRemove()
                    onMenuChange(false)
                }
            )
        }
    }
}

@Composable
private fun SocialLinkChip(
    icon: String,
    label: String,
    url: String,
    accentColor: Color,
    iconRes: Int? = null  // Optional drawable resource
) {
    val context = LocalContext.current
    
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(accentColor)
                )
            } else {
                BasicText(icon, style = TextStyle(accentColor, 12.sp, FontWeight.Bold))
            }
            Spacer(Modifier.width(4.dp))
            BasicText(label, style = TextStyle(accentColor, 12.sp))
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    contentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            value,
            style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
        )
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
        )
    }
}

// ==================== Image Editor Dialog ====================

@Composable
private fun ImageEditorDialog(
    imageBytes: ByteArray,
    isForAvatar: Boolean, // true for avatar (square), false for banner (landscape)
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    if (bitmap == null) {
        onDismiss()
        return
    }
    
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Animated values for smooth transitions
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "scale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "offsetX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "offsetY"
    )
    
    // Reset function
    fun resetTransform() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
    
    // Crop and save function
    fun cropAndSave() {
        try {
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            
            // Target dimensions
            val targetWidth = if (isForAvatar) 400 else 1500
            val targetHeight = if (isForAvatar) 400 else 500
            val targetAspect = targetWidth.toFloat() / targetHeight
            
            // At scale 1f, the entire image is visible (ContentScale.Fit)
            // When user zooms in (scale > 1f), they select a portion of the image
            // The visible portion is 1/scale of the original dimensions
            
            // Calculate the crop dimensions based on target aspect ratio and zoom
            val imageAspect = bitmapWidth.toFloat() / bitmapHeight
            
            // Determine base crop size (at scale 1f, this would be the full image with target aspect)
            val baseCropWidth: Float
            val baseCropHeight: Float
            
            if (imageAspect > targetAspect) {
                // Image is wider than target - crop width
                baseCropHeight = bitmapHeight.toFloat()
                baseCropWidth = baseCropHeight * targetAspect
            } else {
                // Image is taller than target - crop height
                baseCropWidth = bitmapWidth.toFloat()
                baseCropHeight = baseCropWidth / targetAspect
            }
            
            // Apply zoom - zooming in means we take a smaller portion
            val cropWidth = (baseCropWidth / animatedScale).roundToInt().coerceAtLeast(1)
            val cropHeight = (baseCropHeight / animatedScale).roundToInt().coerceAtLeast(1)
            
            // Calculate center position with pan offset
            // Pan offset is in screen coordinates, convert to bitmap coordinates
            val panScaleFactor = minOf(baseCropWidth / bitmapWidth, baseCropHeight / bitmapHeight) / animatedScale
            val centerX = (bitmapWidth / 2f - animatedOffsetX * panScaleFactor).roundToInt()
            val centerY = (bitmapHeight / 2f - animatedOffsetY * panScaleFactor).roundToInt()
            
            // Calculate crop bounds ensuring they stay within image
            val cropX = (centerX - cropWidth / 2).coerceIn(0, (bitmapWidth - cropWidth).coerceAtLeast(0))
            val cropY = (centerY - cropHeight / 2).coerceIn(0, (bitmapHeight - cropHeight).coerceAtLeast(0))
            val finalCropW = cropWidth.coerceAtMost(bitmapWidth - cropX)
            val finalCropH = cropHeight.coerceAtMost(bitmapHeight - cropY)
            
            // Ensure valid crop dimensions
            if (finalCropW <= 0 || finalCropH <= 0) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, if (isForAvatar) 80 else 90, outputStream)
                onSave(outputStream.toByteArray())
                return
            }
            
            // Create cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, finalCropW, finalCropH)
            
            // Scale to final target size
            val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)
            
            // Compress
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, if (isForAvatar) 80 else 90, outputStream)
            
            // Cleanup
            if (croppedBitmap != bitmap) croppedBitmap.recycle()
            if (finalBitmap != croppedBitmap) finalBitmap.recycle()
            
            onSave(outputStream.toByteArray())
        } catch (e: Exception) {
            // Fallback: just compress original
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, if (isForAvatar) 80 else 90, outputStream)
            onSave(outputStream.toByteArray())
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glass Header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(20f.dp) },
                            effects = {
                                vibrancy()
                                blur(16f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button with glass style
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "Cancel",
                                style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                            )
                        }
                        
                        // Title
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText(
                                if (isForAvatar) "Edit Profile Photo" else "Edit Cover Photo",
                                style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                "Pinch to zoom • Drag to position",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                        
                        // Save button
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor)
                                .clickable { cropAndSave() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "Save",
                                style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                }
                
                // Image Editor Area
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Editor container with crop overlay
                    Box(
                        Modifier
                            .then(
                                if (isForAvatar) {
                                    Modifier.size(300.dp)
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .aspectRatio(3f / 1f)
                                }
                            )
                            .clip(if (isForAvatar) CircleShape else RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Transformable image
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        
                                        // Limit pan based on scale
                                        val maxOffset = 300f * (scale - 1f)
                                        offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                                        offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Editable image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        translationX = animatedOffsetX
                                        translationY = animatedOffsetY
                                    }
                            )
                        }
                        
                        // Crop overlay grid (rule of thirds)
                        if (scale > 1f) {
                            Canvas(Modifier.fillMaxSize()) {
                                val strokeWidth = 1f
                                val color = Color.White.copy(alpha = 0.3f)
                                
                                // Vertical lines
                                drawLine(
                                    color = color,
                                    start = Offset(size.width / 3, 0f),
                                    end = Offset(size.width / 3, size.height),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = color,
                                    start = Offset(size.width * 2 / 3, 0f),
                                    end = Offset(size.width * 2 / 3, size.height),
                                    strokeWidth = strokeWidth
                                )
                                
                                // Horizontal lines
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height / 3),
                                    end = Offset(size.width, size.height / 3),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = color,
                                    start = Offset(0f, size.height * 2 / 3),
                                    end = Offset(size.width, size.height * 2 / 3),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                        
                        // Border glow effect
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (isForAvatar) {
                                        Modifier.border(2.dp, accentColor.copy(alpha = 0.6f), CircleShape)
                                    } else {
                                        Modifier.border(2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    }
                                )
                        )
                    }
                    
                    // Hint text below avatar
                    if (isForAvatar) {
                        BasicText(
                            "Your photo will appear as a circle",
                            style = TextStyle(Color.White.copy(alpha = 0.5f), 12.sp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        )
                    }
                }
                
                // Glass Controls Panel
                val sliderBackdrop = rememberLayerBackdrop()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(20f.dp) },
                            effects = {
                                vibrancy()
                                blur(16f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.08f))
                            }
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Zoom label and value
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Zoom",
                                style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                            )
                            BasicText(
                                "${(animatedScale * 100).roundToInt()}%",
                                style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                            )
                        }
                        
                        // Liquid Glass Slider for zoom
                        LiquidSlider(
                            value = { scale },
                            onValueChange = { newScale ->
                                scale = newScale
                                // Reset offset when zooming out to prevent image going out of bounds
                                val maxOffset = 300f * (newScale - 1f)
                                offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                                offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                            },
                            valueRange = 1f..4f,
                            visibilityThreshold = 0.01f,
                            backdrop = sliderBackdrop,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        // Action buttons row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reset button
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { resetTransform() }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText(
                                        "↻",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                    BasicText(
                                        "Reset",
                                        style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                            
                            // Fit button
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { 
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText(
                                        "⊡",
                                        style = TextStyle(Color.White, 16.sp)
                                    )
                                    BasicText(
                                        "Fit",
                                        style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                        }
                        
                        // Size recommendation
                        BasicText(
                            if (isForAvatar) "Recommended: 400 × 400 px" else "Recommended: 1500 × 500 px",
                            style = TextStyle(Color.White.copy(alpha = 0.4f), 11.sp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== Helper Functions ====================

private fun formatNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> "${num / 1_000_000}M"
        num >= 1_000 -> "${num / 1_000}K"
        else -> num.toString()
    }
}

// ==================== Skeleton Loading ====================

@Composable
private fun profileShimmerBrush(
    isLightTheme: Boolean,
    isGlassTheme: Boolean
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val colors =
        if (isGlassTheme) {
            // Soft translucent whites over the glass backdrop
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.25f)
            )
        } else if (isLightTheme) {
            listOf(
                Color(0xFFE0E0E0),
                Color(0xFFF5F5F5),
                Color(0xFFE0E0E0)
            )
        } else {
            listOf(
                Color(0xFF2A2A2A),
                Color(0xFF3A3A3A),
                Color(0xFF2A2A2A)
            )
        }
    
    return Brush.linearGradient(
        colors = colors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
private fun ProfileSkeleton(
    backdrop: LayerBackdrop,
    isLightTheme: Boolean,
    isGlassTheme: Boolean
) {
    val shimmer = profileShimmerBrush(isLightTheme = isLightTheme, isGlassTheme = isGlassTheme)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when {
                    isGlassTheme -> Color.Transparent
                    isLightTheme -> Color(0xFFF5F5F7)
                    else -> Color(0xFF121218)
                }
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header skeleton
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24f.dp) },
                            effects = { vibrancy(); blur(12f.dp.toPx()) },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
                        )
                ) {
                    Column {
                        // Banner skeleton
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(shimmer)
                        )
                        
                        Column(Modifier.padding(16.dp)) {
                            // Avatar skeleton
                            Box(
                                Modifier
                                    .offset(y = (-40).dp)
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(shimmer)
                            )
                            
                            // Name skeleton
                            Box(
                                Modifier
                                    .offset(y = (-32).dp)
                                    .width(150.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmer)
                            )
                            
                            // Username skeleton
                            Box(
                                Modifier
                                    .offset(y = (-24).dp)
                                    .width(100.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmer)
                            )
                            
                            // Headline skeleton
                            Box(
                                Modifier
                                    .offset(y = (-16).dp)
                                    .fillMaxWidth(0.8f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmer)
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Stats skeleton
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                repeat(4) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            Modifier
                                                .width(40.dp)
                                                .height(18.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(shimmer)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            Modifier
                                                .width(60.dp)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(shimmer)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Section skeletons
            items(4) {
                Spacer(Modifier.height(12.dp))
                SectionSkeleton(backdrop, shimmer)
            }
        }
    }
}

@Composable
private fun SectionSkeleton(backdrop: LayerBackdrop, shimmer: Brush) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = { vibrancy(); blur(12f.dp.toPx()) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
            )
            .padding(16.dp)
    ) {
        Column {
            // Title skeleton
            Box(
                Modifier
                    .width(120.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Content skeleton
            repeat(3) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ==================== Error State ====================

@Composable
private fun ProfileError(
    error: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BasicText("😕", style = TextStyle(fontSize = 48.sp))
            
            BasicText(
                when {
                    error.contains("404") || error.contains("not found", ignoreCase = true) -> "User not found"
                    error.contains("403") || error.contains("private", ignoreCase = true) -> "This profile is private"
                    else -> "Failed to load profile"
                },
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
            )
            
            BasicText(
                error,
                style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                BasicText(
                    "Retry",
                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

// Profile Streak Badge - Interactive streak display
@Composable
private fun ProfileStreakBadge(
    currentStreak: Int,
    longestStreak: Int,
    totalActiveDays: Int,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isOnFire = currentStreak >= 3
    val streakColor = when {
        currentStreak >= 30 -> Color(0xFFFF4500) // Red-orange for long streaks
        currentStreak >= 7 -> Color(0xFFFF8C00) // Orange for week+ streaks
        currentStreak >= 3 -> Color(0xFFFFD700) // Gold for 3+ day streaks
        currentStreak > 0 -> Color(0xFFFFA500) // Orange for active streaks
        else -> contentColor.copy(alpha = 0.5f)
    }
    
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isOnFire)
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF6B35).copy(alpha = 0.3f),
                            Color(0xFFFF4500).copy(alpha = 0.2f)
                        )
                    )
                else
                    Brush.horizontalGradient(
                        listOf(
                            contentColor.copy(alpha = 0.1f),
                            contentColor.copy(alpha = 0.05f)
                        )
                    )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Animated fire emoji for active streaks
        if (currentStreak > 0) {
            val infiniteTransition = rememberInfiniteTransition(label = "streak")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isOnFire) 1.15f else 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fire_scale"
            )
            
            BasicText(
                "🔥",
                style = TextStyle(fontSize = (14 * scale).sp)
            )
        } else {
            BasicText(
                "❄️",
                style = TextStyle(fontSize = 14.sp)
            )
        }
        
        // Streak count with styling
        Column {
            BasicText(
                text = if (currentStreak > 0) "$currentStreak day${if (currentStreak > 1) "s" else ""}"
                       else "No streak",
                style = TextStyle(
                    color = streakColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            if (currentStreak > 0 && longestStreak > currentStreak) {
                BasicText(
                    text = "Best: $longestStreak",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                )
            }
        }
        
        // Chevron indicator
        BasicText(
            "›",
            style = TextStyle(
                color = contentColor.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        )
    }
}
