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
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = "glass")
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    
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
            containerColor = Color(0xFF1A1A1A)
        )
    }
    
    Box(
        Modifier
            .fillMaxSize()
            .then(
                when {
                    // Glass: make this a true modal overlay (blur + dim) so the
                    // underlying profile never visually bleeds through.
                    isGlassTheme -> Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(0f.dp) },
                        effects = {
                            vibrancy()
                            blur(28f.dp.toPx())
                        },
                        onDrawSurface = {
                            // Frosted "ice" scrim (no black)
                            drawRect(Color(0xFFEAF2FF).copy(alpha = 0.55f))
                        }
                    )
                    isDarkTheme -> Modifier.background(Color(0xFF0E0E12))
                    else -> Modifier.background(Color(0xFFF7F7FA))
                }
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Allow scrolling past the bottom tab/footer area
                .padding(bottom = 120.dp)
        ) {
            // Header
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
                            drawRect(
                                when {
                                    isGlassTheme -> Color.White.copy(alpha = 0.14f)
                                    isDarkTheme -> Color.White.copy(alpha = 0.08f)
                                    else -> Color.Black.copy(alpha = 0.04f)
                                }
                            )
                        }
                    )
                    .padding(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    isGlassTheme -> Color.White.copy(alpha = 0.14f)
                                    isDarkTheme -> Color.White.copy(alpha = 0.10f)
                                    else -> Color.Black.copy(alpha = 0.06f)
                                }
                            )
                            .clickable { onCancel() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            "Cancel",
                            style = TextStyle(
                                if (isGlassTheme || isDarkTheme) Color.White else Color.Black.copy(alpha = 0.8f),
                                14.sp
                            )
                        )
                    }
                    
                    BasicText(
                        if (isEditMode) "Edit Project" else "Add Project",
                        style = TextStyle(
                            if (isGlassTheme || isDarkTheme) Color.White else Color.Black.copy(alpha = 0.9f),
                            18.sp,
                            FontWeight.SemiBold
                        )
                    )
                    
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isValid && !isLoading) accentColor else accentColor.copy(alpha = 0.3f))
                            .clickable(enabled = isValid && !isLoading) {
                                scope.launch {
                                    isLoading = true
                                    // Use current date if not provided (backend requires startDate)
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
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            BasicText("Save", style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold))
                        }
                    }
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Red.copy(alpha = 0.2f))
                        .padding(12.dp)
                ) {
                    BasicText(error, style = TextStyle(Color.Red, 13.sp))
                }
            }
            
            // Form content
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Project Name (Required)
                FormField(
                    label = "Title *",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Art Portfolio, Music Album, Research Paper...",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true
                )
                
                // Description (Optional)
                FormField(
                    label = "Description",
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Describe your work, goals, achievements...",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = false,
                    minLines = 4
                )
                
                // Project Images
                Column {
                    BasicText(
                        "Project Images",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images.toList()) { imageUrl ->
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                                // Remove button
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable { images.remove(imageUrl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("×", style = TextStyle(Color.White, 14.sp))
                                }
                            }
                        }
                        
                        // Add image tile
                        item {
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        2.dp,
                                        contentColor.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isUploadingImage) {
                                        imagePicker.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = accentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        BasicText("+", style = TextStyle(contentColor.copy(alpha = 0.5f), 24.sp))
                                        BasicText("Add", style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Role
                FormField(
                    label = "Your Role",
                    value = role,
                    onValueChange = { role = it },
                    placeholder = "e.g. Creator, Lead Artist, Contributor...",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true
                )
                
                // Skills & Tags (Universal)
                Column {
                    BasicText(
                        "Skills & Tags",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Tag chips
                    if (techStack.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            techStack.forEach { tech ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accentColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText(tech, style = TextStyle(accentColor, 12.sp))
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            Modifier
                                                .clip(CircleShape)
                                                .clickable { techStack.remove(tech) }
                                                .padding(2.dp)
                                        ) {
                                            BasicText("×", style = TextStyle(accentColor, 12.sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Add tag input
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.05f))
                                .padding(12.dp)
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
                                            "Add skill or tag...",
                                            style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .clickable(enabled = techInput.isNotBlank()) {
                                    if (techInput.isNotBlank()) {
                                        techStack.add(techInput.trim())
                                        techInput = ""
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            BasicText("Add", style = TextStyle(Color.White, 14.sp))
                        }
                    }
                }
                
                // Dates (Optional)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "Start Date",
                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.05f))
                                .clickable { showStartDatePicker = true }
                                .padding(12.dp)
                        ) {
                            BasicText(
                                if (startDate.isNotBlank()) formatDateDisplay(startDate) else "Select date",
                                style = TextStyle(
                                    if (startDate.isNotBlank()) contentColor else contentColor.copy(alpha = 0.4f),
                                    14.sp
                                )
                            )
                        }
                    }
                    
                    // End Date
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "End Date",
                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) contentColor.copy(alpha = 0.02f)
                                    else contentColor.copy(alpha = 0.05f)
                                )
                                .clickable(enabled = !isCurrent) { showEndDatePicker = true }
                                .padding(12.dp)
                        ) {
                            BasicText(
                                when {
                                    isCurrent -> "Present"
                                    endDate.isNotBlank() -> formatDateDisplay(endDate)
                                    else -> "Select date"
                                },
                                style = TextStyle(
                                    when {
                                        isCurrent -> contentColor.copy(alpha = 0.4f)
                                        endDate.isNotBlank() -> contentColor
                                        else -> contentColor.copy(alpha = 0.4f)
                                    },
                                    14.sp
                                )
                            )
                        }
                    }
                }
                
                // Currently working checkbox
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isCurrent = !isCurrent }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCurrent,
                        onCheckedChange = { isCurrent = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = contentColor.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "Currently working on this project",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
                
                // Featured checkbox
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { featured = !featured }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = featured,
                        onCheckedChange = { featured = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFFD700),
                            uncheckedColor = contentColor.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "Mark as Featured ⭐",
                        style = TextStyle(contentColor, 14.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "(max 3)",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
                
                // Project Links Section
                Column {
                    BasicText(
                        "LINKS",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp, FontWeight.Bold)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    FormField(
                        label = "Live/Portfolio URL",
                        value = projectUrl,
                        onValueChange = { projectUrl = it },
                        placeholder = "https://your-work.com",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        singleLine = true,
                        keyboardType = KeyboardType.Uri
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    FormField(
                        label = "Source/Repository URL",
                        value = githubUrl,
                        onValueChange = { githubUrl = it },
                        placeholder = "https://github.com/... or other source link",
                        contentColor = contentColor,
                        accentColor = accentColor,
                        singleLine = true,
                        keyboardType = KeyboardType.Uri
                    )
                }
                
                // Delete button (Edit mode only)
                if (isEditMode && onDelete != null) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.1f))
                            .clickable { showDeleteDialog = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "Delete Project",
                            style = TextStyle(Color.Red, 14.sp, FontWeight.Medium)
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
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
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header image
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
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
                    // Gradient overlay
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                } else {
                    // Placeholder
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = 0.3f),
                                        accentColor.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("🚀", style = TextStyle(fontSize = 48.sp))
                    }
                }
                
                // Back button
                Box(
                    Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("←", style = TextStyle(Color.White, 20.sp))
                }
                
                // Featured badge
                if (project.featured) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFD700).copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText("⭐", style = TextStyle(fontSize = 12.sp))
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                "Featured",
                                style = TextStyle(Color.Black, 12.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
            
            // Content
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title and edit button
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            project.name,
                            style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
                        )
                        project.role?.let { role ->
                            BasicText(
                                role,
                                style = TextStyle(accentColor, 14.sp)
                            )
                        }
                    }
                    
                    if (isOwner) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable { onEdit() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                "Edit",
                                style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                // Date range
                BasicText(
                    "${formatDateDisplay(project.startDate)} — ${if (project.isCurrent) "Present" else project.endDate?.let { formatDateDisplay(it) } ?: ""}",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Description
                BasicText(
                    project.description,
                    style = TextStyle(contentColor.copy(alpha = 0.9f), 15.sp)
                )
                
                // Skills & Tags
                if (project.techStack.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    BasicText(
                        "SKILLS & TAGS",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp, FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        project.techStack.forEach { tech ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                BasicText(
                                    tech,
                                    style = TextStyle(accentColor, 13.sp)
                                )
                            }
                        }
                    }
                }
                
                // More images
                if (project.images.size > 1) {
                    Spacer(Modifier.height(20.dp))
                    BasicText(
                        "GALLERY",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp, FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(project.images.drop(1)) { imageUrl ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Project image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(120.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
                
                // Links
                if (project.projectUrl != null || project.githubUrl != null) {
                    Spacer(Modifier.height(24.dp))
                    BasicText(
                        "LINKS",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp, FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        project.projectUrl?.let { url ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(accentColor)
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText("🔗", style = TextStyle(fontSize = 14.sp))
                                    Spacer(Modifier.width(8.dp))
                                    BasicText(
                                        "View Live",
                                        style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                        }
                        
                        project.githubUrl?.let { url ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText("📂", style = TextStyle(fontSize = 14.sp))
                                    Spacer(Modifier.width(8.dp))
                                    BasicText(
                                        "Source",
                                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ==================== Helper Composables ====================

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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        BasicText(
            label,
            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.05f))
                .padding(12.dp)
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
                    .then(if (!singleLine) Modifier.height((minLines * 20).dp) else Modifier),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        BasicText(
                            placeholder,
                            style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
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
