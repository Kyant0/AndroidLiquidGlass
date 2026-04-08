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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.components.LiquidSlider
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
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
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

// ==================== Main Profile Screen ====================

@Composable
fun ProfileScreen(
    userId: String? = null,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    profileViewModel: ProfileViewModel? = null,
    onNavigateBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onMessage: (String) -> Unit = {},
    onOpenFeedItem: (FeedItem) -> Unit = {},
    onOpenProfile: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val screenViewModel = profileViewModel ?: viewModel(
        key = "profile:${userId ?: "me"}",
        factory = ProfileViewModel.Factory(context)
    )
    val uiState by screenViewModel.uiState.collectAsState()
    
    // Theme detection
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    val isLightTheme = themeMode == "light"
    
    // Project screen state
    var showAddProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var projectDetailProject by remember { mutableStateOf<Project?>(null) }
    var projectDetailVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun dismissProjectDetail(afterDismiss: (() -> Unit)? = null) {
        scope.launch {
            projectDetailVisible = false
            delay(220)
            projectDetailProject = null
            afterDismiss?.invoke()
        }
    }
    
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
        enabled = showAddProject || editingProject != null || projectDetailProject != null ||
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
            projectDetailProject != null -> dismissProjectDetail()
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
        screenViewModel.loadProfile(userId)
    }
    
    Box(Modifier.fillMaxSize()) {
        // Prefer cached profile while refreshing (stale-while-revalidate). Checking isLoading first
        // would flash the skeleton on every tab revisit because loadProfile(forceRefresh) sets loading.
        when {
            uiState.profile != null -> {
                ProfileContent(
                    uiState = uiState,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isDarkTheme = isDarkTheme,
                    onEditProfile = onEditProfile,
                    onConnect = { screenViewModel.sendConnectionRequest() },
                    onCancelRequest = { screenViewModel.cancelConnectionRequest() },
                    onAcceptRequest = { screenViewModel.acceptConnectionRequest() },
                    onRejectRequest = { screenViewModel.rejectConnectionRequest() },
                    onRemoveConnection = { screenViewModel.removeConnection() },
                    onToggleFollow = { screenViewModel.toggleFollow() },
                    onFilterChange = { screenViewModel.setFeedFilter(it) },
                    onLoadMore = { screenViewModel.loadMoreFeed() },
                    onYearChange = { screenViewModel.loadActivityForYear(it) },
                    onEditBio = { screenViewModel.startEditingBio() },
                    onSaveBio = { screenViewModel.saveEditedBio() },
                    onCancelEditBio = { screenViewModel.cancelEditingBio() },
                    onBioChange = { screenViewModel.updateEditedBio(it) },
                    onToggleOpenToWork = { screenViewModel.updateOpenToOpportunities(it) },
                    onOpenConnections = { screenViewModel.openConnectionsSheet() },
                    onOpenFollowers = { screenViewModel.openFollowersSheet() },
                    onMessage = onMessage,
                    onOpenFeedItem = onOpenFeedItem,
                    onVotePoll = { postId, optionId -> screenViewModel.votePoll(postId, optionId) },
                    onUploadAvatar = { screenViewModel.uploadAvatar(it) },
                    onUploadBanner = { screenViewModel.uploadBanner(it) },
                    // Project callbacks
                    onAddProject = { showAddProject = true },
                    onEditProject = { editingProject = it },
                    onViewProject = {
                        projectDetailProject = it
                        projectDetailVisible = true
                    },
                    onToggleProjectFeatured = { screenViewModel.toggleProjectFeatured(it.id) },
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
                    onRemoveSkill = { skill -> screenViewModel.removeLocalSkill(skill.id) },
                    onDeleteFeedPost = { postId ->
                        screenViewModel.deleteFeedPost(
                            postId = postId,
                            onSuccess = {},
                            onError = { }
                        )
                    }
                )
            }
            uiState.error != null -> {
                ProfileError(
                    error = uiState.error!!,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onRetry = { screenViewModel.retry() }
                )
            }
            uiState.isLoading -> {
                ProfileLoadingContent(
                    contentColor = contentColor,
                    accentColor = accentColor,
                    visitLoaderGiftId = uiState.visitLoaderGiftIdHint,
                    reduceAnimations = reduceAnimations
                )
            }
            else -> {
                ProfileLoadingContent(
                    contentColor = contentColor,
                    accentColor = accentColor,
                    visitLoaderGiftId = uiState.visitLoaderGiftIdHint,
                    reduceAnimations = reduceAnimations
                )
            }
        }

        if (uiState.peopleSheet.isVisible) {
            ProfilePeopleSheetDialog(
                sheetState = uiState.peopleSheet,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isDarkTheme = isDarkTheme,
                onDismiss = { screenViewModel.dismissPeopleSheet() },
                onRetry = { screenViewModel.retryPeopleSheet() },
                onLoadMore = { screenViewModel.loadMorePeopleSheet() },
                onPersonClick = onOpenProfile?.let { callback ->
                    { personId ->
                        screenViewModel.dismissPeopleSheet()
                        callback(personId)
                    }
                }
            )
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
                    screenViewModel.deleteProject(
                        projectId = project.id,
                        onSuccess = { editingProject = null },
                        onError = { /* Show error toast */ }
                    )
                },
                onCancel = { editingProject = null }
            )
        }
        
        AnimatedVisibility(
            visible = projectDetailProject != null && projectDetailVisible,
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                initialScale = 0.94f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(180)) + scaleOut(
                targetScale = 0.98f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        ) {
            projectDetailProject?.let { project ->
                ProjectDetailScreen(
                    project = project,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isOwner = uiState.isOwner,
                    onEdit = {
                        dismissProjectDetail {
                            editingProject = project
                        }
                    },
                    onBack = { dismissProjectDetail() }
                )
            }
        }
        
        // Add Experience Screen
        if (showAddExperience) {
            AddEditExperienceScreen(
                experience = null,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onSave = { savedExperience ->
                    screenViewModel.addExperience(savedExperience)
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
                    screenViewModel.addLocalSkill(name = name, proficiency = proficiency)
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
                    screenViewModel.updateExperience(savedExperience)
                    editingExperience = null
                },
                onDelete = {
                    screenViewModel.deleteExperience(
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
                    screenViewModel.addEducation(savedEducation)
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
                    screenViewModel.updateEducation(savedEducation)
                    editingEducation = null
                },
                onDelete = {
                    screenViewModel.deleteEducation(
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
                    screenViewModel.addCertificate(savedCertificate)
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
                    screenViewModel.updateCertificate(savedCertificate)
                    editingCertificate = null
                },
                onDelete = {
                    screenViewModel.deleteCertificate(
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
                    screenViewModel.addAchievement(savedAchievement)
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
                    screenViewModel.updateAchievement(savedAchievement)
                    editingAchievement = null
                },
                onDelete = {
                    screenViewModel.deleteAchievement(
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
    onEditProfile: () -> Unit = {},
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
    onOpenConnections: () -> Unit,
    onOpenFollowers: () -> Unit,
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
                onEditProfile = onEditProfile,
                onConnect = onConnect,
                onCancelRequest = onCancelRequest,
                onAcceptRequest = onAcceptRequest,
                onRejectRequest = onRejectRequest,
                onRemoveConnection = onRemoveConnection,
                onToggleFollow = onToggleFollow,
                onOpenConnections = onOpenConnections,
                onOpenFollowers = onOpenFollowers,
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
    onEditProfile: () -> Unit = {},
    onConnect: () -> Unit,
    onCancelRequest: () -> Unit,
    onAcceptRequest: () -> Unit,
    onRejectRequest: () -> Unit,
    onRemoveConnection: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenFollowers: () -> Unit,
    onMessage: (String) -> Unit,
    onUploadAvatar: (ByteArray) -> Unit = {},
    onUploadBanner: (ByteArray) -> Unit = {}
) {
    val context = LocalContext.current
    val profileFrameEnabled by SettingsPreferences.profileFrameEnabled(context).collectAsState(initial = false)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    var showShareMenu by remember { mutableStateOf(false) }
    
    // Image editor state
    var showAvatarEditor by remember { mutableStateOf(false) }
    var showBannerEditor by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    
    // Cache key to force image refresh
    var avatarCacheKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var bannerCacheKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasAnimatedProfileFrame = isAnimatedProfileFrame(user.profileRing)
    val hasProfileRing = !user.profileRing.isNullOrBlank()
    val showProfileFrame = if (isOwner) profileFrameEnabled || hasAnimatedProfileFrame else hasAnimatedProfileFrame
    val avatarContainerSize = if (hasProfileRing) 92.dp else 88.dp
    val avatarOuterSize = if (showProfileFrame) 164.dp else avatarContainerSize
    val avatarImageSize = when {
        showProfileFrame -> 86.dp
        hasProfileRing -> 84.dp
        else -> 88.dp
    }
    val avatarOffsetX = if (showProfileFrame) 10.dp else 0.dp
    val isCurrentlyOnline = isProfileCurrentlyOnline(user, stats)
    val presenceLabel = buildProfilePresenceLabel(user, stats, isOwner)
    
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
                    Box(
                        modifier = Modifier
                            .size(avatarContainerSize)
                            .offset(x = avatarOffsetX)
                            .graphicsLayer { clip = false },
                        contentAlignment = Alignment.Center
                    ) {
                        // Profile ring
                        if (!showProfileFrame && hasProfileRing) {
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
                                .size(avatarImageSize)
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
                            
                        }

                        if (showProfileFrame) {
                            ProfileFrameLottie(
                                modifier = Modifier
                                    .requiredSize(avatarOuterSize)
                                    .graphicsLayer { clip = false },
                                isPlaying = !reduceAnimations
                            )
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
                                    .clickable(onClick = onEditProfile)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    "Edit Profile",
                                    style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                                )
                            }

                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.14f))
                                    .clickable(enabled = !isUploadingAvatar) {
                                        avatarPicker.launch("image/*")
                                    }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingAvatar) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = accentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.ic_camera),
                                        contentDescription = "Edit avatar",
                                        modifier = Modifier.size(16.dp),
                                        colorFilter = ColorFilter.tint(accentColor)
                                    )
                                }
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
                                    DropdownMenuItem(
                                        text = {
                                            BasicText(
                                                text = when (connectionStatus) {
                                                    "connected" -> "Connection: Connected"
                                                    "pending_sent" -> "Connection: Request sent"
                                                    "pending_received" -> "Connection: Request received"
                                                    else -> "Connection: Not connected"
                                                },
                                                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp)
                                            )
                                        },
                                        enabled = false,
                                        onClick = {}
                                    )
                                    when (connectionStatus) {
                                        "connected" -> {
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Remove Connection",
                                                        style = TextStyle(Color.Red, 14.sp)
                                                    )
                                                },
                                                enabled = !connectionActionInProgress,
                                                onClick = {
                                                    onRemoveConnection()
                                                    showShareMenu = false
                                                }
                                            )
                                        }
                                        "pending_sent" -> {
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Cancel Request",
                                                        style = TextStyle(contentColor, 14.sp)
                                                    )
                                                },
                                                enabled = !connectionActionInProgress,
                                                onClick = {
                                                    onCancelRequest()
                                                    showShareMenu = false
                                                }
                                            )
                                        }
                                        "pending_received" -> {
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Accept Request",
                                                        style = TextStyle(contentColor, 14.sp)
                                                    )
                                                },
                                                enabled = !connectionActionInProgress,
                                                onClick = {
                                                    onAcceptRequest()
                                                    showShareMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Ignore Request",
                                                        style = TextStyle(contentColor, 14.sp)
                                                    )
                                                },
                                                enabled = !connectionActionInProgress,
                                                onClick = {
                                                    onRejectRequest()
                                                    showShareMenu = false
                                                }
                                            )
                                        }
                                        else -> {
                                            DropdownMenuItem(
                                                text = {
                                                    BasicText(
                                                        "Connect",
                                                        style = TextStyle(contentColor, 14.sp)
                                                    )
                                                },
                                                enabled = !connectionActionInProgress,
                                                onClick = {
                                                    onConnect()
                                                    showShareMenu = false
                                                }
                                            )
                                        }
                                    }
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
                                        enabled = !followActionInProgress,
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

                Row(
                    modifier = Modifier
                        .offset(y = (-16).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isCurrentlyOnline) Color(0xFF22C55E).copy(alpha = 0.14f)
                            else contentColor.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        presenceLabel,
                        style = TextStyle(
                            color = if (isCurrentlyOnline) Color(0xFF22C55E) else contentColor.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                // Headline
                user.headline?.let { headline ->
                    BasicText(
                        headline,
                        style = TextStyle(contentColor, 14.sp),
                        modifier = Modifier.offset(y = (-12).dp)
                    )
                }
                
                // Location, College, Branch
                Row(
                    Modifier.offset(y = (-8).dp),
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
                                user.college,
                                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp)
                            )
                        }
                    }
                }
                
                // Social links
                Row(
                    Modifier.offset(y = (-4).dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    user.linkedinUrl?.let { url ->
                        SocialLinkChip(
                            icon = "in",
                            label = "LinkedIn",
                            url = url,
                            accentColor = accentColor
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
                
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatItem(
                        value = formatNumber(stats.connectionsCount),
                        label = "connections",
                        contentColor = contentColor,
                        onClick = onOpenConnections,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        value = formatNumber(stats.followersCount),
                        label = "followers",
                        contentColor = contentColor,
                        onClick = onOpenFollowers,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        value = formatNumber(stats.totalPosts),
                        label = "posts",
                        contentColor = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        value = formatNumber(stats.totalLikesReceived),
                        label = "likes",
                        contentColor = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ProfileGamificationRow(
                    stats = stats,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isDarkTheme = isDarkTheme
                )
                
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
private fun RowScope.StatItem(
    value: String,
    label: String,
    contentColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

@Composable
private fun ProfilePeopleSheetDialog(
    sheetState: ProfilePeopleSheetState,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onPersonClick: ((String) -> Unit)? = null
) {
    val kind = sheetState.kind ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .then(
                        when {
                            isGlassTheme -> Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(28f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(18f.dp.toPx())
                                    lens(10f.dp.toPx(), 18f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.12f))
                                }
                            )
                            isDarkTheme -> Modifier.background(Color(0xFF171717))
                            else -> Modifier.background(Color.White)
                        }
                    )
                    .clickable(enabled = false) { }
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(contentColor.copy(alpha = 0.18f))
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(
                            kind.title,
                            style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
                        )
                        BasicText(
                            if (sheetState.isLoading && sheetState.total == 0) "Loading people..."
                            else "${formatNumber(sheetState.total)} people",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        sheetState.isLoading && sheetState.people.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        sheetState.error != null && sheetState.people.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                BasicText(
                                    "Could not load ${kind.title.lowercase()}",
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                BasicText(
                                    sheetState.error,
                                    style = TextStyle(
                                        color = contentColor.copy(alpha = 0.62f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(accentColor)
                                        .clickable(onClick = onRetry)
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    BasicText(
                                        "Try again",
                                        style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        sheetState.people.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                BasicText(
                                    kind.emptyTitle,
                                    style = TextStyle(
                                        color = contentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                if (kind.emptyBody.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    BasicText(
                                        kind.emptyBody,
                                        style = TextStyle(
                                            color = contentColor.copy(alpha = 0.62f),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(sheetState.people, key = { it.id }) { person ->
                                    ProfilePeopleRow(
                                        person = person,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onClick = onPersonClick?.let { callback ->
                                            { callback(person.id) }
                                        }
                                    )
                                }

                                if (sheetState.isLoading) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = accentColor,
                                                strokeWidth = 2.5.dp
                                            )
                                        }
                                    }
                                } else if (sheetState.error != null) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(contentColor.copy(alpha = 0.06f))
                                                .clickable(onClick = onRetry)
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            BasicText(
                                                "Retry loading more",
                                                style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                } else if (sheetState.hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(accentColor.copy(alpha = 0.12f))
                                                .clickable(onClick = onLoadMore)
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            BasicText(
                                                "Load more",
                                                style = TextStyle(accentColor, 13.sp, FontWeight.SemiBold),
                                                modifier = Modifier.align(Alignment.Center)
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
}

@Composable
private fun ProfilePeopleRow(
    person: ProfilePeopleListItem,
    contentColor: Color,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    val secondaryLine = when {
        !person.headline.isNullOrBlank() -> person.headline
        !person.college.isNullOrBlank() -> person.college
        else -> "Vormex member"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            if (!person.profileImage.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(person.profileImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = person.name ?: "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val initials = (person.name ?: person.username ?: "U")
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2)
                    .joinToString("")
                BasicText(
                    initials,
                    style = TextStyle(accentColor, 15.sp, FontWeight.Bold)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = person.name ?: person.username ?: "Unknown member",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (person.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                }
            }

            person.username?.takeIf { it.isNotBlank() }?.let { username ->
                BasicText(
                    text = "@$username",
                    style = TextStyle(contentColor.copy(alpha = 0.52f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BasicText(
                text = secondaryLine,
                style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onClick != null) {
            BasicText(
                "View",
                style = TextStyle(accentColor, 12.sp, FontWeight.Medium)
            )
        }
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

private const val PROFILE_PRESENCE_ONLINE_WINDOW_MS = 5 * 60 * 1000L

private val profilePresenceTimestampPatterns = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
    "yyyy-MM-dd HH:mm:ss"
)

private fun parseProfilePresenceMillis(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    runCatching { Instant.parse(raw) }.getOrNull()?.toEpochMilli()?.let { return it }
    runCatching { OffsetDateTime.parse(raw) }.getOrNull()?.toInstant()?.toEpochMilli()?.let { return it }
    for (pattern in profilePresenceTimestampPatterns) {
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = true
            }.parse(raw)?.time
        }.getOrNull()?.let { return it }
    }
    runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(raw)?.time }
        .getOrNull()?.let { return it }
    return null
}

private fun isProfileCurrentlyOnline(user: ProfileUser, stats: ProfileStats): Boolean {
    if (user.isOnline) return true
    val lastSeenMillis = parseProfilePresenceMillis(user.lastActiveAt ?: stats.lastActiveDate)
    return lastSeenMillis != null && (System.currentTimeMillis() - lastSeenMillis) < PROFILE_PRESENCE_ONLINE_WINDOW_MS
}

private fun buildProfilePresenceLabel(user: ProfileUser, stats: ProfileStats, isOwner: Boolean): String {
    if (isProfileCurrentlyOnline(user, stats)) {
        return if (isOwner) "Online now" else "Active now"
    }

    val lastSeenMillis = parseProfilePresenceMillis(user.lastActiveAt ?: stats.lastActiveDate)
        ?: return if (isOwner) "Offline" else "Recently active"
    val diffMs = (System.currentTimeMillis() - lastSeenMillis).coerceAtLeast(0L)
    val minutes = diffMs / 60_000L

    val elapsed = when {
        minutes < 1L -> "just now"
        minutes < 60L -> "${minutes}m ago"
        minutes < 1_440L -> "${minutes / 60L}h ago"
        minutes < 10_080L -> "${minutes / 1_440L}d ago"
        else -> "recently"
    }

    return "Last active $elapsed"
}

// ==================== Loading (lightweight; avoids heavy shimmer + blur skeleton) ====================

@Composable
private fun ProfileLoadingContent(
    contentColor: Color,
    accentColor: Color,
    visitLoaderGiftId: String? = null,
    reduceAnimations: Boolean = false
) {
    val rawRes = ProfileLoaderGifts.resolvedVisitorRawRes(visitLoaderGiftId)
    val showLottie = !reduceAnimations
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showLottie) {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    isPlaying = true
                )
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(120.dp),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit,
                    clipToCompositionBounds = true
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = accentColor,
                    strokeWidth = 3.dp
                )
            }
            BasicText(
                "Loading profile…",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            )
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

@Composable
private fun ProfileGamificationRow(
    stats: ProfileStats,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isDarkTheme: Boolean
) {
    val streakColor = profileStreakAccentColor(stats.currentStreak, contentColor)

    val labelStyle = TextStyle(
        color = contentColor.copy(alpha = 0.45f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
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
                            drawRect(Color.White.copy(alpha = 0.08f))
                        }
                    )
                    isDarkTheme -> Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                    else -> Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.035f))
                }
            )
            .border(1.dp, contentColor.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText("LEVEL", style = labelStyle)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFE566), Color(0xFFFFA500))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    BasicText(
                        "${stats.level}",
                        style = TextStyle(
                            color = Color(0xFF1A1408),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            GamificationVDivider(contentColor)

            // XP
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText("EXPERIENCE", style = labelStyle)
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicText(
                        "⚡",
                        style = TextStyle(
                            color = accentColor.copy(alpha = 0.85f),
                            fontSize = 15.sp
                        )
                    )
                    BasicText(
                        formatNumber(stats.xp),
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                BasicText(
                    "next +${formatNumber(stats.xpToNextLevel)} XP",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.42f),
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            GamificationVDivider(contentColor)

            // Streak
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicText("STREAK", style = labelStyle)
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (stats.currentStreak > 0) {
                        StreakFireLottie(modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(4.dp))
                    } else {
                        BasicText("❄️", style = TextStyle(fontSize = 14.sp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        BasicText(
                            text = if (stats.currentStreak > 0) {
                                "${stats.currentStreak} day${if (stats.currentStreak > 1) "s" else ""}"
                            } else {
                                "No streak"
                            },
                            style = TextStyle(
                                color = streakColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (stats.currentStreak > 0 && stats.longestStreak > stats.currentStreak) {
                            BasicText(
                                "Best ${stats.longestStreak}",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.42f),
                                    fontSize = 9.sp
                                )
                            )
                        } else if (stats.totalActiveDays > 0) {
                            BasicText(
                                "${formatNumber(stats.totalActiveDays)} active days",
                                style = TextStyle(
                                    color = contentColor.copy(alpha = 0.42f),
                                    fontSize = 9.sp
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
private fun GamificationVDivider(contentColor: Color) {
    Box(
        Modifier
            .width(1.dp)
            .height(46.dp)
            .background(contentColor.copy(alpha = 0.12f))
    )
}

private fun profileStreakAccentColor(currentStreak: Int, contentColor: Color): Color = when {
    currentStreak >= 30 -> Color(0xFFFF4500)
    currentStreak >= 7 -> Color(0xFFFF8C00)
    currentStreak >= 3 -> Color(0xFFFFD700)
    currentStreak > 0 -> Color(0xFFFFA500)
    else -> contentColor.copy(alpha = 0.55f)
}
