package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Project
import com.kyant.backdrop.catalog.network.models.ProjectInput
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// ==================== Add/Edit Project Screen ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditProjectScreen(
    project: Project? = null, // null = Add mode, non-null = Edit mode
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (Project) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = project != null
    val projectId = project?.id // Capture for use in lambdas

    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    val modalBackground = when {
        isDarkTheme -> Color(0xFF0E1014)
        isGlassTheme -> Color(0xFFF2F6FA)
        else -> Color(0xFFF8FAFC)
    }
    val sectionBorderColor = when {
        isGlassTheme -> Color.White.copy(alpha = 0.18f)
        isDarkTheme -> Color.White.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.06f)
    }
    val sectionSurfaceColor = when {
        isGlassTheme -> Color(0xFFF8FAFC)
        isDarkTheme -> Color(0xFF151922)
        else -> Color.White
    }
    val subduedTextColor = contentColor.copy(alpha = 0.62f)
    val featuredAccent = Color(0xFFFFD66B)
    
    // Form state
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var role by remember { mutableStateOf(project?.role ?: "") }
    val techStack = remember { mutableStateListOf<String>().apply { project?.techStack?.let { addAll(it) } } }
    var techInput by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(project?.startDate ?: "") }
    var endDate by remember { mutableStateOf(project?.endDate ?: "") }
    var isCurrent by remember { mutableStateOf(project?.isCurrent ?: false) }
    var projectUrl by remember { mutableStateOf(project?.projectUrl ?: "") }
    var githubUrl by remember { mutableStateOf(project?.githubUrl ?: "") }
    val images = remember { mutableStateListOf<String>().apply { project?.images?.let { addAll(it) } } }
    var featured by remember { mutableStateOf(project?.featured ?: false) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Validation - only name is required
    val isValid = name.isNotBlank()

    fun submitProject() {
        if (!isValid || isLoading) return
        scope.launch {
            isLoading = true
            val effectiveStartDate = startDate.takeIf { it.isNotBlank() }
                ?: LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val input = ProjectInput(
                name = name,
                description = description.takeIf { it.isNotBlank() } ?: "",
                role = role.takeIf { it.isNotBlank() },
                techStack = techStack.toList().takeIf { it.isNotEmpty() },
                startDate = effectiveStartDate,
                endDate = endDate.takeIf { it.isNotBlank() && !isCurrent },
                isCurrent = isCurrent,
                projectUrl = projectUrl.takeIf { it.isNotBlank() },
                githubUrl = githubUrl.takeIf { it.isNotBlank() },
                images = images.toList().takeIf { it.isNotEmpty() },
                featured = featured
            )

            val result = if (isEditMode && projectId != null) {
                ApiClient.updateProject(context, projectId, input)
            } else {
                ApiClient.createProject(context, input)
            }

            result
                .onSuccess { savedProject ->
                    onSave(savedProject)
                }
                .onFailure { e ->
                    errorMessage = e.message ?: "Failed to save project"
                    isLoading = false
                }
        }
    }
    
    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingImage = true
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    ApiClient.uploadProjectImage(context, bytes)
                        .onSuccess { imageUrl ->
                            if (imageUrl.isNotEmpty()) {
                                images.add(imageUrl)
                            }
                        }
                        .onFailure { e ->
                            errorMessage = e.message ?: "Failed to upload image"
                        }
                }
                isUploadingImage = false
            }
        }
    }
    
    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseIsoToMillis(startDate)
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = formatMillisToIso(it)
                    }
                    showStartDatePicker = false
                }) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseIsoToMillis(endDate)
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = formatMillisToIso(it)
                    }
                    showEndDatePicker = false
                }) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { BasicText("Delete Project", style = TextStyle(contentColor, 18.sp, FontWeight.Bold)) },
            text = { BasicText("Are you sure you want to delete this project?", style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete?.invoke()
                }) {
                    BasicText("Delete", style = TextStyle(Color.Red, 14.sp, FontWeight.Medium))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                }
            },
            containerColor = modalBackground
        )
    }
    
    Box(
        Modifier
            .fillMaxSize()
            .background(modalBackground)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        modalBackground,
                        modalBackground,
                        modalBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProjectEditorSection(
                title = if (isEditMode) "Edit Project" else "Create Project",
                subtitle = if (isEditMode) {
                    "Refresh the visuals, stack, story, and links so this work feels current."
                } else {
                    "Turn your work into a polished card with a strong story, visuals, timeline, and links."
                },
                iconRes = if (isEditMode) R.drawable.ic_edit else R.drawable.ic_sparkles,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                borderColor = sectionBorderColor,
                surfaceColor = sectionSurfaceColor
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProjectEditorMetricChip(
                        iconRes = R.drawable.ic_work,
                        label = name.takeIf { it.isNotBlank() } ?: "Untitled draft",
                        tint = accentColor,
                        contentColor = contentColor
                    )
                    ProjectEditorMetricChip(
                        iconRes = R.drawable.ic_image,
                        label = "${images.size} visual${if (images.size == 1) "" else "s"}",
                        tint = contentColor.copy(alpha = 0.74f),
                        contentColor = contentColor
                    )
                    ProjectEditorMetricChip(
                        iconRes = R.drawable.ic_code,
                        label = "${techStack.size} tag${if (techStack.size == 1) "" else "s"}",
                        tint = contentColor.copy(alpha = 0.74f),
                        contentColor = contentColor
                    )
                    if (featured) {
                        ProjectEditorMetricChip(
                            iconRes = R.drawable.ic_sparkles,
                            label = "Featured",
                            tint = featuredAccent,
                            contentColor = contentColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProjectEditorActionButton(
                        modifier = Modifier.weight(1f),
                        label = "Cancel",
                        iconRes = R.drawable.ic_close,
                        filled = false,
                        enabled = !isLoading,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = onCancel
                    )
                    ProjectEditorActionButton(
                        modifier = Modifier.weight(1f),
                        label = if (isLoading) "Saving..." else if (isEditMode) "Save changes" else "Save project",
                        iconRes = if (isLoading) null else R.drawable.ic_check,
                        filled = true,
                        enabled = isValid && !isLoading,
                        loading = isLoading,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { submitProject() }
                    )
                }
            }

            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFF6B6B).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_warning),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(Color(0xFFFF6B6B))
                        )
                        BasicText(
                            text = error,
                            style = TextStyle(Color(0xFFFF6B6B), 13.sp, FontWeight.Medium)
                        )
                    }
                }
            }

            ProjectEditorSection(
                title = "Project Story",
                subtitle = "Give this work a clear identity and explain why it matters.",
                iconRes = R.drawable.ic_work,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                borderColor = sectionBorderColor,
                surfaceColor = sectionSurfaceColor
            ) {
                FormField(
                    label = "Title *",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Campus placement prep platform",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    iconRes = R.drawable.ic_work,
                    helperText = "Use the name people should remember."
                )

                FormField(
                    label = "Description",
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "What did you build, solve, launch, or improve?",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = false,
                    minLines = 5,
                    iconRes = R.drawable.ic_file_text,
                    helperText = "Keep it outcome-focused and easy to scan."
                )

                FormField(
                    label = "Your Role",
                    value = role,
                    onValueChange = { role = it },
                    placeholder = "e.g. Product designer, Android developer, Founder",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    iconRes = R.drawable.ic_profile,
                    helperText = "Say how you contributed to this work."
                )
            }

            ProjectEditorSection(
                title = "Visuals",
                subtitle = "Add a strong cover image, screenshots, or proof of execution.",
                iconRes = R.drawable.ic_image,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                borderColor = sectionBorderColor,
                surfaceColor = sectionSurfaceColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    images.forEach { imageUrl ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, contentColor.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Project image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.16f)
                                            )
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.48f))
                                    .clickable { images.remove(imageUrl) },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Remove image",
                                    modifier = Modifier.size(12.dp),
                                    colorFilter = ColorFilter.tint(Color.White)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(contentColor.copy(alpha = 0.04f))
                            .border(1.dp, contentColor.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                            .clickable(enabled = !isUploadingImage) {
                                imagePicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = accentColor,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(contentColor.copy(alpha = 0.06f))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_upload),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        colorFilter = ColorFilter.tint(contentColor)
                                    )
                                }
                                BasicText(
                                    text = "Upload visual",
                                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                                )
                                BasicText(
                                    text = "Cover, screenshot, poster, or proof",
                                    style = TextStyle(subduedTextColor, 12.sp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            ProjectEditorSection(
                title = "Stack & Timeline",
                subtitle = "Show the tools you used and when this work happened.",
                iconRes = R.drawable.ic_code,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                borderColor = sectionBorderColor,
                surfaceColor = sectionSurfaceColor
            ) {
                if (techStack.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        techStack.forEach { tech ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accentColor.copy(alpha = 0.12f))
                                    .border(1.dp, accentColor.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                                    .padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    text = tech,
                                    style = TextStyle(contentColor, 12.sp, FontWeight.SemiBold)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.14f))
                                        .clickable { techStack.remove(tech) }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = "Remove tag",
                                        modifier = Modifier.size(10.dp),
                                        colorFilter = ColorFilter.tint(accentColor)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(contentColor.copy(alpha = 0.04f))
                        .border(1.dp, accentColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.14f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_tag),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 2.dp)
                        ) {
                            BasicTextField(
                                value = techInput,
                                onValueChange = { techInput = it },
                                textStyle = TextStyle(contentColor, 14.sp),
                                cursorBrush = SolidColor(accentColor),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                decorationBox = { innerTextField ->
                                    if (techInput.isEmpty()) {
                                        BasicText(
                                            text = "Add skill, tool, platform, or keyword",
                                            style = TextStyle(subduedTextColor, 14.sp)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        ProjectEditorActionButton(
                            label = "Add",
                            iconRes = R.drawable.ic_plus,
                            filled = true,
                            enabled = techInput.isNotBlank(),
                            contentColor = contentColor,
                            accentColor = accentColor,
                            onClick = {
                                val newTag = techInput.trim()
                                if (newTag.isNotBlank()) {
                                    techStack.add(newTag)
                                    techInput = ""
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProjectDateField(
                        modifier = Modifier.weight(1f),
                        label = "Start date",
                        value = startDate.takeIf { it.isNotBlank() }?.let(::formatDateDisplay) ?: "Pick month",
                        enabled = true,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { showStartDatePicker = true }
                    )
                    ProjectDateField(
                        modifier = Modifier.weight(1f),
                        label = "End date",
                        value = when {
                            isCurrent -> "Present"
                            endDate.isNotBlank() -> formatDateDisplay(endDate)
                            else -> "Pick month"
                        },
                        enabled = !isCurrent,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { showEndDatePicker = true }
                    )
                }

                ProjectToggleCard(
                    label = "Currently active",
                    description = "Use this if you are still building or maintaining the project.",
                    iconRes = R.drawable.ic_check,
                    checked = isCurrent,
                    accentColor = accentColor,
                    contentColor = contentColor,
                    onToggle = { isCurrent = !isCurrent }
                )

                ProjectToggleCard(
                    label = "Feature on profile",
                    description = "Highlight this project in your public profile showcase.",
                    iconRes = R.drawable.ic_sparkles,
                    checked = featured,
                    accentColor = featuredAccent,
                    contentColor = contentColor,
                    trailingNote = "Max 3",
                    onToggle = { featured = !featured }
                )
            }

            ProjectEditorSection(
                title = "Links",
                subtitle = "Send people to the live version, source, or portfolio page.",
                iconRes = R.drawable.ic_link,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                borderColor = sectionBorderColor,
                surfaceColor = sectionSurfaceColor
            ) {
                FormField(
                    label = "Live / Portfolio URL",
                    value = projectUrl,
                    onValueChange = { projectUrl = it },
                    placeholder = "https://your-project.com",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    keyboardType = KeyboardType.Uri,
                    iconRes = R.drawable.ic_open_in_browser,
                    helperText = "Share the link people should visit first."
                )

                FormField(
                    label = "Source / Repository URL",
                    value = githubUrl,
                    onValueChange = { githubUrl = it },
                    placeholder = "https://github.com/username/repo",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    keyboardType = KeyboardType.Uri,
                    iconRes = R.drawable.ic_github,
                    helperText = "Add code, docs, or any supporting source link."
                )
            }

            if (isEditMode && onDelete != null) {
                ProjectEditorSection(
                    title = "Danger Zone",
                    subtitle = "Remove this project permanently from your profile.",
                    iconRes = R.drawable.ic_delete,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = Color(0xFFFF6B6B),
                    borderColor = Color(0xFFFF6B6B).copy(alpha = 0.18f),
                    surfaceColor = sectionSurfaceColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.1f))
                            .border(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                            .clickable { showDeleteDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                colorFilter = ColorFilter.tint(Color(0xFFFF6B6B))
                            )
                            Column {
                                BasicText(
                                    text = "Delete project",
                                    style = TextStyle(Color(0xFFFF6B6B), 14.sp, FontWeight.SemiBold)
                                )
                                BasicText(
                                    text = "This action cannot be undone.",
                                    style = TextStyle(Color(0xFFFF6B6B).copy(alpha = 0.72f), 12.sp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== Project Detail Screen ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    project: Project,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    val heroAccent = if (project.featured) Color(0xFFFFD66B) else accentColor
    val surfaceBorder = if (project.featured) heroAccent.copy(alpha = 0.26f) else contentColor.copy(alpha = 0.1f)
    val baseBackground = when {
        isDarkTheme -> Color(0xFF101318)
        isGlassTheme -> Color(0xFFF2F6FA)
        else -> Color(0xFFF9FAFC)
    }
    val backgroundBrush = Brush.verticalGradient(
        colors = when {
            isDarkTheme -> listOf(
                accentColor.copy(alpha = 0.22f),
                Color(0xFF161C25),
                baseBackground
            )
            isGlassTheme -> listOf(
                accentColor.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.34f),
                baseBackground
            )
            else -> listOf(
                accentColor.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.72f),
                baseBackground
            )
        }
    )
    val timelineLabel = projectDetailTimeline(project)
    val detailLinks = buildList {
        project.projectUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                ProjectDetailLinkEntry(
                    title = "Live experience",
                    subtitle = projectLinkHost(url),
                    iconRes = R.drawable.ic_open_in_browser,
                    tint = heroAccent,
                    url = url,
                    isPrimary = true
                )
            )
        }
        project.githubUrl?.takeIf { it.isNotBlank() }?.let { url ->
            add(
                ProjectDetailLinkEntry(
                    title = "Source code",
                    subtitle = projectLinkHost(url),
                    iconRes = R.drawable.ic_github,
                    tint = contentColor,
                    url = url
                )
            )
        }
        project.otherLinks.orEmpty()
            .filter { it.url.isNotBlank() }
            .forEach { link ->
                add(
                    ProjectDetailLinkEntry(
                        title = link.name.ifBlank { "External link" },
                        subtitle = projectLinkHost(link.url),
                        iconRes = R.drawable.ic_link,
                        tint = contentColor,
                        url = link.url
                    )
                )
            }
    }
    val overviewItems = buildList {
        project.role?.takeIf { it.isNotBlank() }?.let { role ->
            add(
                ProjectDetailOverviewItem(
                    label = "Role",
                    value = role,
                    iconRes = R.drawable.ic_work,
                    tint = heroAccent
                )
            )
        }
        add(
            ProjectDetailOverviewItem(
                label = "Timeline",
                value = timelineLabel,
                iconRes = R.drawable.ic_calendar,
                tint = contentColor.copy(alpha = 0.72f)
            )
        )
        add(
            ProjectDetailOverviewItem(
                label = "Status",
                value = if (project.isCurrent) "Active now" else "Completed",
                iconRes = if (project.isCurrent) R.drawable.ic_check else R.drawable.ic_sparkles,
                tint = if (project.isCurrent) heroAccent else contentColor.copy(alpha = 0.72f)
            )
        )
        if (project.images.isNotEmpty()) {
            add(
                ProjectDetailOverviewItem(
                    label = "Media",
                    value = "${project.images.size} visual${if (project.images.size > 1) "s" else ""}",
                    iconRes = R.drawable.ic_image,
                    tint = contentColor.copy(alpha = 0.72f)
                )
            )
        }
        if (detailLinks.isNotEmpty()) {
            add(
                ProjectDetailOverviewItem(
                    label = "Links",
                    value = "${detailLinks.size} destination${if (detailLinks.size > 1) "s" else ""}",
                    iconRes = R.drawable.ic_link,
                    tint = contentColor.copy(alpha = 0.72f)
                )
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(baseBackground)
            .background(backgroundBrush)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ProjectDetailSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                backdrop = backdrop,
                borderColor = surfaceBorder
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                ) {
                    if (project.images.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(project.images.first())
                                .crossfade(true)
                                .build(),
                            contentDescription = project.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            heroAccent.copy(alpha = 0.42f),
                                            accentColor.copy(alpha = 0.2f),
                                            contentColor.copy(alpha = 0.08f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(
                                    when {
                                        !project.githubUrl.isNullOrBlank() -> R.drawable.ic_code
                                        !project.projectUrl.isNullOrBlank() -> R.drawable.ic_globe
                                        else -> R.drawable.ic_work
                                    }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                colorFilter = ColorFilter.tint(heroAccent)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.14f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.46f)
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        ProjectDetailHeroButton(
                            label = "Back",
                            onClick = onBack
                        )

                        if (isOwner) {
                            ProjectDetailHeroButton(
                                iconRes = R.drawable.ic_edit,
                                label = "Edit",
                                onClick = onEdit
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (project.featured) {
                                ProjectDetailBadge(
                                    iconRes = R.drawable.ic_sparkles,
                                    label = "Featured",
                                    tint = heroAccent,
                                    background = Color.Black.copy(alpha = 0.28f)
                                )
                            }
                            if (project.isCurrent) {
                                ProjectDetailBadge(
                                    iconRes = R.drawable.ic_check,
                                    label = "Active",
                                    tint = Color.White,
                                    background = Color.White.copy(alpha = 0.14f)
                                )
                            }
                        }

                        BasicText(
                            text = project.name,
                            style = TextStyle(Color.White, 28.sp, FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        project.role?.takeIf { it.isNotBlank() }?.let { role ->
                            BasicText(
                                text = role,
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.88f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        BasicText(
                            text = timelineLabel,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProjectDetailSectionSurface(
                    title = "Overview",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    borderColor = surfaceBorder
                ) {
                    ProjectDetailOverviewBlock(
                        project = project,
                        overviewItems = overviewItems,
                        contentColor = contentColor,
                        accentColor = heroAccent
                    )
                }

                if (project.description.isNotBlank()) {
                    ProjectDetailSectionSurface(
                        title = "About this project",
                        backdrop = backdrop,
                        contentColor = contentColor,
                        borderColor = surfaceBorder
                    ) {
                        BasicText(
                            text = project.description,
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.84f),
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        )
                    }
                }

                if (project.techStack.isNotEmpty()) {
                    ProjectDetailSectionSurface(
                        title = "Stack",
                        backdrop = backdrop,
                        contentColor = contentColor,
                        borderColor = surfaceBorder
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            project.techStack.forEach { tech ->
                                ProjectDetailBadge(
                                    iconRes = R.drawable.ic_code,
                                    label = tech,
                                    tint = heroAccent,
                                    background = heroAccent.copy(alpha = 0.12f),
                                    textColor = contentColor
                                )
                            }
                        }
                    }
                }

                if (detailLinks.isNotEmpty()) {
                    ProjectDetailSectionSurface(
                        title = "Explore",
                        backdrop = backdrop,
                        contentColor = contentColor,
                        borderColor = surfaceBorder
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            detailLinks.forEach { link ->
                                ProjectDetailLinkRow(
                                    link = link,
                                    contentColor = contentColor,
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                    }
                                )
                            }
                        }
                    }
                }

                if (project.images.size > 1) {
                    ProjectDetailSectionSurface(
                        title = "Gallery",
                        backdrop = backdrop,
                        contentColor = contentColor,
                        borderColor = surfaceBorder
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(project.images.drop(1)) { imageUrl ->
                                ProjectDetailGalleryCard(
                                    imageUrl = imageUrl,
                                    projectName = project.name
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

private data class ProjectDetailOverviewItem(
    val label: String,
    val value: String,
    val iconRes: Int,
    val tint: Color
)

private data class ProjectDetailLinkEntry(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val tint: Color,
    val url: String,
    val isPrimary: Boolean = false
)

@Composable
private fun ProjectDetailSurface(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(24f.dp) },
                effects = {
                    vibrancy()
                    blur(18f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        content()
    }
}

@Composable
private fun ProjectDetailSectionSurface(
    title: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    ProjectDetailSurface(
        modifier = Modifier.fillMaxWidth(),
        backdrop = backdrop,
        borderColor = borderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            content()
        }
    }
}

@Composable
private fun ProjectDetailHeroButton(
    label: String,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconRes?.let { res ->
            Image(
                painter = painterResource(res),
                contentDescription = label,
                modifier = Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        BasicText(
            text = label,
            style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold)
        )
    }
}

@Composable
private fun ProjectDetailBadge(
    iconRes: Int,
    label: String,
    tint: Color,
    background: Color,
    textColor: Color = tint
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, tint.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = label,
            style = TextStyle(textColor, 11.sp, FontWeight.SemiBold)
        )
    }
}

@Composable
private fun ProjectDetailOverviewBlock(
    project: Project,
    overviewItems: List<ProjectDetailOverviewItem>,
    contentColor: Color,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            accentColor.copy(alpha = 0.08f),
                            contentColor.copy(alpha = 0.03f)
                        )
                    )
                )
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    text = "Quick snapshot",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                BasicText(
                    text = project.role?.takeIf { it.isNotBlank() } ?: "Project highlight",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    overviewItems.forEach { item ->
                        ProjectDetailMetricPill(
                            iconRes = item.iconRes,
                            text = item.value,
                            tint = item.tint,
                            contentColor = contentColor
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(contentColor.copy(alpha = 0.04f))
                .border(1.dp, contentColor.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
        ) {
            overviewItems.forEachIndexed { index, item ->
                ProjectDetailOverviewRow(
                    item = item,
                    contentColor = contentColor
                )

                if (index != overviewItems.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(1.dp)
                            .background(contentColor.copy(alpha = 0.06f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailMetricPill(
    iconRes: Int,
    text: String,
    tint: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = text,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = text,
            style = TextStyle(
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProjectDetailOverviewRow(
    item: ProjectDetailOverviewItem,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(item.tint.copy(alpha = 0.14f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.iconRes),
                contentDescription = item.label,
                modifier = Modifier.size(14.dp),
                colorFilter = ColorFilter.tint(item.tint)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BasicText(
                text = item.label,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.56f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            BasicText(
                text = item.value,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectDetailLinkRow(
    link: ProjectDetailLinkEntry,
    contentColor: Color,
    onClick: () -> Unit
) {
    val rowBackground = if (link.isPrimary) link.tint.copy(alpha = 0.14f) else contentColor.copy(alpha = 0.04f)
    val rowBorder = if (link.isPrimary) link.tint.copy(alpha = 0.22f) else contentColor.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(rowBackground)
            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(link.tint.copy(alpha = if (link.isPrimary) 0.2f else 0.12f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(link.iconRes),
                contentDescription = link.title,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(link.tint)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = link.title,
                style = TextStyle(
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = link.subtitle,
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.56f),
                    fontSize = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        BasicText(
            text = "Open",
            style = TextStyle(
                color = if (link.isPrimary) link.tint else contentColor.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun ProjectDetailGalleryCard(
    imageUrl: String,
    projectName: String
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .height(150.dp)
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = projectName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f)
                        )
                    )
                )
        )
    }
}

private fun projectDetailTimeline(project: Project): String {
    val endLabel = if (project.isCurrent) "Present" else project.endDate?.let { formatDateDisplay(it) }
    return listOf(formatDateDisplay(project.startDate), endLabel)
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString(" • ")
}

private fun projectLinkHost(url: String): String {
    return Uri.parse(url).host?.removePrefix("www.") ?: url
}

// ==================== Helper Composables ====================

@Composable
private fun ProjectEditorSection(
    title: String,
    subtitle: String,
    iconRes: Int,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    borderColor: Color,
    surfaceColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f))
                        .border(1.dp, accentColor.copy(alpha = 0.18f), CircleShape)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(accentColor)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    BasicText(
                        text = subtitle,
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.62f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun ProjectEditorMetricChip(
    iconRes: Int,
    label: String,
    tint: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(tint)
        )
        BasicText(
            text = label,
            style = TextStyle(contentColor, 11.sp, FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProjectEditorActionButton(
    modifier: Modifier = Modifier,
    label: String,
    iconRes: Int? = null,
    filled: Boolean,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val background = when {
        filled && enabled -> accentColor
        filled -> accentColor.copy(alpha = 0.3f)
        else -> contentColor.copy(alpha = 0.05f)
    }
    val border = if (filled) Color.Transparent else contentColor.copy(alpha = 0.08f)
    val textColor = if (filled) Color.White else contentColor

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = textColor,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconRes?.let { res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = label,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(textColor)
                    )
                }
                BasicText(
                    text = label,
                    style = TextStyle(textColor, 14.sp, FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun ProjectDateField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    enabled: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(contentColor.copy(alpha = 0.62f), 12.sp, FontWeight.Medium)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (enabled) contentColor.copy(alpha = 0.04f)
                    else contentColor.copy(alpha = 0.025f)
                )
                .border(
                    1.dp,
                    if (enabled) accentColor.copy(alpha = 0.12f) else contentColor.copy(alpha = 0.06f),
                    RoundedCornerShape(18.dp)
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (enabled) 0.14f else 0.08f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = label,
                    modifier = Modifier.size(14.dp),
                    colorFilter = ColorFilter.tint(if (enabled) accentColor else contentColor.copy(alpha = 0.4f))
                )
            }
            BasicText(
                text = value,
                style = TextStyle(
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.42f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectToggleCard(
    label: String,
    description: String,
    iconRes: Int,
    checked: Boolean,
    accentColor: Color,
    contentColor: Color,
    trailingNote: String? = null,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (checked) accentColor.copy(alpha = 0.10f)
                else contentColor.copy(alpha = 0.04f)
            )
            .border(
                1.dp,
                if (checked) accentColor.copy(alpha = 0.2f)
                else contentColor.copy(alpha = 0.08f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onToggle)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(accentColor.copy(alpha = if (checked) 0.18f else 0.1f))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(accentColor)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = label,
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                )
                trailingNote?.let { note ->
                    BasicText(
                        text = note,
                        style = TextStyle(contentColor.copy(alpha = 0.48f), 11.sp, FontWeight.Medium)
                    )
                }
            }
            BasicText(
                text = description,
                style = TextStyle(contentColor.copy(alpha = 0.62f), 12.sp, lineHeight = 17.sp)
            )
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) accentColor else Color.Transparent)
                .border(
                    1.dp,
                    if (checked) accentColor else contentColor.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Image(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Selected",
                    modifier = Modifier.size(12.dp),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    iconRes: Int? = null,
    helperText: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            iconRes?.let { res ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(res),
                        contentDescription = label,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(accentColor)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    label,
                    style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold)
                )
                helperText?.let { helper ->
                    BasicText(
                        text = helper,
                        style = TextStyle(contentColor.copy(alpha = 0.56f), 12.sp, lineHeight = 17.sp)
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(contentColor.copy(alpha = 0.04f))
                .border(
                    1.dp,
                    if (value.isNotBlank()) accentColor.copy(alpha = 0.18f)
                    else contentColor.copy(alpha = 0.08f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(accentColor),
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!singleLine) Modifier.height((minLines * 24).dp) else Modifier),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        BasicText(
                            placeholder,
                            style = TextStyle(contentColor.copy(alpha = 0.38f), 14.sp)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

// ==================== Helper Functions ====================

private fun parseIsoToMillis(isoDate: String): Long? {
    return try {
        if (isoDate.isBlank()) return null
        val date = LocalDate.parse(isoDate.take(10))
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

private fun formatMillisToIso(millis: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(millis)
        val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: Exception) {
        ""
    }
}

private fun formatDateDisplay(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    } catch (e: Exception) {
        isoDate
    }
}
