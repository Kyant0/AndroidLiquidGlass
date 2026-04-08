package com.kyant.backdrop.catalog.linkedin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Experience
import com.kyant.backdrop.catalog.network.models.ExperienceInput
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Experience types matching backend enum
val EXPERIENCE_TYPES = listOf("Internship", "Part-time", "Full-time", "Freelance", "Contract")

// ==================== Add/Edit Experience Screen ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditExperienceScreen(
    experience: Experience? = null, // null = Add mode, non-null = Edit mode
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (Experience) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = experience != null
    val experienceId = experience?.id

    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    
    // Form state
    var title by remember { mutableStateOf(experience?.title ?: "") }
    var company by remember { mutableStateOf(experience?.company ?: "") }
    var type by remember { mutableStateOf(experience?.type ?: "Internship") }
    var location by remember { mutableStateOf(experience?.location ?: "") }
    var startDate by remember { mutableStateOf(experience?.startDate ?: "") }
    var endDate by remember { mutableStateOf(experience?.endDate ?: "") }
    var isCurrent by remember { mutableStateOf(experience?.isCurrent ?: false) }
    var description by remember { mutableStateOf(experience?.description ?: "") }
    val skills = remember { mutableStateListOf<String>().apply { experience?.skills?.let { addAll(it) } } }
    var skillInput by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf(experience?.logo) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingLogo by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Validation
    val isTitleValid = title.trim().length in 2..100
    val isCompanyValid = company.trim().length in 2..100
    val isValid = isTitleValid && isCompanyValid && startDate.isNotBlank()
    
    // Logo picker
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingLogo = true
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    // Note: Using project image upload endpoint for now
                    // In production, would use a dedicated logo upload endpoint
                    ApiClient.uploadProjectImage(context, bytes)
                        .onSuccess { imageUrl ->
                            if (imageUrl.isNotEmpty()) {
                                logo = imageUrl
                            }
                        }
                        .onFailure { e ->
                            errorMessage = e.message ?: "Failed to upload logo"
                        }
                }
                isUploadingLogo = false
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
            title = { BasicText("Delete Experience", style = TextStyle(contentColor, 18.sp, FontWeight.Bold)) },
            text = { BasicText("Are you sure you want to delete this experience?", style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)) },
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
                    // Glass: frosted overlay (no black screen)
                    isGlassTheme -> Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(0f.dp) },
                        effects = {
                            vibrancy()
                            blur(28f.dp.toPx())
                        },
                        onDrawSurface = {
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
                // Allow scrolling past bottom tab/footer area
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
                        if (isEditMode) "Edit Experience" else "Add Experience",
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
                                    val input = ExperienceInput(
                                        title = title.trim(),
                                        company = company.trim(),
                                        type = type,
                                        location = location.takeIf { it.isNotBlank() }?.trim(),
                                        startDate = startDate,
                                        endDate = endDate.takeIf { it.isNotBlank() && !isCurrent },
                                        isCurrent = isCurrent,
                                        description = description.takeIf { it.isNotBlank() }?.trim(),
                                        skills = skills.toList().takeIf { it.isNotEmpty() },
                                        logo = logo
                                    )
                                    
                                    val result = if (isEditMode && experienceId != null) {
                                        ApiClient.updateExperience(context, experienceId, input)
                                    } else {
                                        ApiClient.createExperience(context, input)
                                    }
                                    
                                    result
                                        .onSuccess { savedExperience ->
                                            onSave(savedExperience)
                                        }
                                        .onFailure { e ->
                                            errorMessage = e.message ?: "Failed to save experience"
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
                // Logo (optional)
                Column {
                    BasicText(
                        "Logo (optional)",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                            .border(
                                2.dp,
                                contentColor.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !isUploadingLogo) {
                                logoPicker.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isUploadingLogo -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentColor,
                                    strokeWidth = 2.dp
                                )
                            }
                            logo != null -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(logo)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_work),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.4f))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    BasicText(
                                        "Add logo",
                                        style = TextStyle(contentColor.copy(alpha = 0.4f), 10.sp)
                                    )
                                }
                            }
                        }
                    }
                    if (logo != null) {
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            "Remove",
                            style = TextStyle(Color.Red.copy(alpha = 0.7f), 11.sp),
                            modifier = Modifier.clickable { logo = null }
                        )
                    }
                }
                
                // Role/Position (Required)
                ExperienceFormField(
                    label = "Role / Position *",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "e.g. Marketing Intern, Team Captain, Volunteer, Designer",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    isError = title.isNotEmpty() && !isTitleValid,
                    errorText = if (title.isNotEmpty() && !isTitleValid) "Must be 2-100 characters" else null
                )
                
                // Organization (Required)
                ExperienceFormField(
                    label = "Organization *",
                    value = company,
                    onValueChange = { company = it },
                    placeholder = "e.g. Company, Club, NGO, Team, Band",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    isError = company.isNotEmpty() && !isCompanyValid,
                    errorText = if (company.isNotEmpty() && !isCompanyValid) "Must be 2-100 characters" else null
                )
                
                // Type (Required)
                Column {
                    BasicText(
                        "Type *",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    BasicText(
                        "Internship, Part-time, Full-time, Freelance, Contract",
                        style = TextStyle(contentColor.copy(alpha = 0.4f), 11.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EXPERIENCE_TYPES.forEach { expType ->
                            val isSelected = type == expType
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) getExperienceTypeColor(expType)
                                        else contentColor.copy(alpha = 0.08f)
                                    )
                                    .clickable { type = expType }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                BasicText(
                                    expType,
                                    style = TextStyle(
                                        if (isSelected) Color.White else contentColor.copy(alpha = 0.7f),
                                        13.sp,
                                        FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Location (Optional)
                ExperienceFormField(
                    label = "Location",
                    value = location,
                    onValueChange = { location = it },
                    placeholder = "e.g. Remote, City, Campus",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true
                )
                
                // Dates
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date (Required)
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "Start Date *",
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
                        .clickable { 
                            isCurrent = !isCurrent
                            if (isCurrent) endDate = ""
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCurrent,
                        onCheckedChange = { 
                            isCurrent = it
                            if (it) endDate = ""
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = contentColor.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "I currently work here",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
                
                // Description (Optional)
                ExperienceFormField(
                    label = "Description",
                    value = description,
                    onValueChange = { 
                        if (it.length <= 2000) description = it 
                    },
                    placeholder = "What you did and what you learned...",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = false,
                    minLines = 4
                )
                if (description.isNotEmpty()) {
                    BasicText(
                        "${description.length}/2000",
                        style = TextStyle(
                            if (description.length > 1800) Color.Red.copy(alpha = 0.7f) 
                            else contentColor.copy(alpha = 0.4f),
                            11.sp
                        ),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                
                // Skills (Optional)
                Column {
                    BasicText(
                        "Skills used",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Skill chips
                    if (skills.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            skills.forEach { skill ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accentColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BasicText(skill, style = TextStyle(accentColor, 12.sp))
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            Modifier
                                                .clip(CircleShape)
                                                .clickable { skills.remove(skill) }
                                                .padding(2.dp)
                                        ) {
                                            BasicText("×", style = TextStyle(accentColor, 12.sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Add skill input
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
                                value = skillInput,
                                onValueChange = { skillInput = it },
                                textStyle = TextStyle(contentColor, 14.sp),
                                cursorBrush = SolidColor(accentColor),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                decorationBox = { innerTextField ->
                                    if (skillInput.isEmpty()) {
                                        BasicText(
                                            "Add skill — e.g. Leadership, Python, Design",
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
                                .clickable(enabled = skillInput.isNotBlank()) {
                                    if (skillInput.isNotBlank()) {
                                        skills.add(skillInput.trim())
                                        skillInput = ""
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            BasicText("Add", style = TextStyle(Color.White, 14.sp))
                        }
                    }
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
                            "Delete Experience",
                            style = TextStyle(Color.Red, 14.sp, FontWeight.Medium)
                        )
                    }
                }
                
                // Extra bottom spacing to prevent content from being hidden under navigation bar
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ==================== Helper Composables ====================

@Composable
private fun ExperienceFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    errorText: String? = null
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
                .then(
                    if (isError) Modifier.border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    else Modifier
                )
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(accentColor),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else minLines,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        BasicText(
                            placeholder,
                            style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        errorText?.let {
            Spacer(Modifier.height(4.dp))
            BasicText(
                it,
                style = TextStyle(Color.Red.copy(alpha = 0.7f), 11.sp)
            )
        }
    }
}

// ==================== Helper Functions ====================

private fun getExperienceTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "full-time" -> Color(0xFF22C55E)
        "internship" -> Color(0xFF3B82F6)
        "part-time" -> Color(0xFFF59E0B)
        "freelance" -> Color(0xFFA855F7)
        "contract" -> Color(0xFFEC4899)
        else -> Color(0xFF6B7280)
    }
}

private fun parseIsoToMillis(isoDate: String): Long? {
    return try {
        if (isoDate.isBlank()) return null
        val formatter = DateTimeFormatter.ISO_DATE
        val localDate = LocalDate.parse(isoDate.take(10), formatter)
        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

private fun formatMillisToIso(millis: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(millis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(DateTimeFormatter.ISO_DATE)
    } catch (e: Exception) {
        ""
    }
}

private fun formatDateDisplay(isoDate: String): String {
    return try {
        if (isoDate.isBlank()) return ""
        val formatter = DateTimeFormatter.ISO_DATE
        val localDate = LocalDate.parse(isoDate.take(10), formatter)
        val displayFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
        localDate.format(displayFormatter)
    } catch (e: Exception) {
        isoDate
    }
}
