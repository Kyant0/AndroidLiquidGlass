package com.kyant.backdrop.catalog.linkedin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Education
import com.kyant.backdrop.catalog.network.models.EducationInput
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ==================== Add/Edit Education Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEducationScreen(
    education: Education? = null, // null = Add mode, non-null = Edit mode
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (Education) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = education != null
    val educationId = education?.id
    
    // Form state
    var school by remember { mutableStateOf(education?.school ?: "") }
    var degree by remember { mutableStateOf(education?.degree ?: "") }
    var fieldOfStudy by remember { mutableStateOf(education?.fieldOfStudy ?: "") }
    var startDate by remember { mutableStateOf(education?.startDate ?: "") }
    var endDate by remember { mutableStateOf(education?.endDate ?: "") }
    var isCurrent by remember { mutableStateOf(education?.isCurrent ?: false) }
    var grade by remember { mutableStateOf(education?.grade ?: "") }
    var activities by remember { mutableStateOf(education?.activities ?: "") }
    var description by remember { mutableStateOf(education?.description ?: "") }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Validation
    val isSchoolValid = school.trim().length in 2..100
    val isDegreeValid = degree.trim().length in 2..100
    val isFieldValid = fieldOfStudy.trim().length in 2..100
    val isDescriptionValid = description.length <= 2000
    val isActivitiesValid = activities.length <= 2000
    val isStartDateValid = startDate.isNotBlank()
    val isEndDateValid = isCurrent || endDate.isBlank() || 
        (startDate.isNotBlank() && endDate.isNotBlank() && startDate <= endDate)
    val isValid = isSchoolValid && isDegreeValid && isFieldValid && 
        isStartDateValid && isEndDateValid && isDescriptionValid && isActivitiesValid
    
    // Date pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            startDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp, FontWeight.Medium))
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
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            endDate = date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp, FontWeight.Medium))
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
            title = {
                BasicText(
                    "Delete Education?",
                    style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)
                )
            },
            text = {
                BasicText(
                    "This will permanently delete this education entry. This action cannot be undone.",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    }
                ) {
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
    
    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"

    Box(
        Modifier
            .fillMaxSize()
            .then(
                when {
                    // Glass: frosted overlay
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
                        if (isEditMode) "Edit Education" else "Add Education",
                        style = TextStyle(
                            if (isGlassTheme || isDarkTheme) Color.White else Color.Black.copy(alpha = 0.9f),
                            16.sp,
                            FontWeight.SemiBold
                        )
                    )
                    
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isValid && !isLoading) accentColor 
                                else contentColor.copy(alpha = 0.2f)
                            )
                            .clickable(enabled = isValid && !isLoading) {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    val input = EducationInput(
                                        school = school.trim(),
                                        degree = degree.trim(),
                                        fieldOfStudy = fieldOfStudy.trim(),
                                        startDate = startDate,
                                        endDate = if (isCurrent) null else endDate.ifBlank { null },
                                        isCurrent = isCurrent,
                                        grade = grade.ifBlank { null },
                                        activities = activities.ifBlank { null },
                                        description = description.ifBlank { null }
                                    )
                                    
                                    val result = if (isEditMode && educationId != null) {
                                        ApiClient.updateEducation(context, educationId, input)
                                    } else {
                                        ApiClient.createEducation(context, input)
                                    }
                                    
                                    result
                                        .onSuccess { savedEducation ->
                                            isLoading = false
                                            onSave(savedEducation)
                                        }
                                        .onFailure { e ->
                                            isLoading = false
                                            errorMessage = e.message ?: "Failed to save education"
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
                            BasicText(
                                "Save",
                                style = TextStyle(
                                    if (isValid) Color.White else contentColor.copy(alpha = 0.5f),
                                    14.sp,
                                    FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Red.copy(alpha = 0.15f))
                        .padding(12.dp)
                ) {
                    BasicText(error, style = TextStyle(Color.Red.copy(alpha = 0.9f), 13.sp))
                }
            }
            
            // Form fields
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Education icon
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFFD700).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_education),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            colorFilter = ColorFilter.tint(Color(0xFFFFD700))
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // School / University (required)
                EducationFormField(
                    label = "School / University *",
                    value = school,
                    onValueChange = { school = it },
                    placeholder = "e.g. Stanford University, IIT Bombay, High School Name",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isError = school.isNotEmpty() && !isSchoolValid,
                    errorText = if (school.isNotEmpty() && school.trim().length < 2) "At least 2 characters" 
                               else if (school.trim().length > 100) "Maximum 100 characters" else null
                )
                
                // Degree (required)
                EducationFormField(
                    label = "Degree *",
                    value = degree,
                    onValueChange = { degree = it },
                    placeholder = "e.g. Bachelor's, Master's, Diploma, High School, Certificate",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isError = degree.isNotEmpty() && !isDegreeValid,
                    errorText = if (degree.isNotEmpty() && degree.trim().length < 2) "At least 2 characters"
                               else if (degree.trim().length > 100) "Maximum 100 characters" else null
                )
                
                // Field of Study (required)
                EducationFormField(
                    label = "Field of Study *",
                    value = fieldOfStudy,
                    onValueChange = { fieldOfStudy = it },
                    placeholder = "e.g. Computer Science, Business, Arts, Engineering",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isError = fieldOfStudy.isNotEmpty() && !isFieldValid,
                    errorText = if (fieldOfStudy.isNotEmpty() && fieldOfStudy.trim().length < 2) "At least 2 characters"
                               else if (fieldOfStudy.trim().length > 100) "Maximum 100 characters" else null
                )
                
                // Date fields
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start date
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    startDate.ifBlank { "Select date" },
                                    style = TextStyle(
                                        if (startDate.isBlank()) contentColor.copy(alpha = 0.4f) else contentColor,
                                        14.sp
                                    )
                                )
                                BasicText("📅", style = TextStyle(fontSize = 16.sp))
                            }
                        }
                    }
                    
                    // End date
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "End Date",
                            style = TextStyle(
                                if (isCurrent) contentColor.copy(alpha = 0.4f) else contentColor,
                                13.sp, FontWeight.Medium
                            )
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
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    if (isCurrent) "Present" else endDate.ifBlank { "Select date" },
                                    style = TextStyle(
                                        if (isCurrent) accentColor
                                        else if (endDate.isBlank()) contentColor.copy(alpha = 0.4f) 
                                        else contentColor,
                                        14.sp
                                    )
                                )
                                if (!isCurrent) {
                                    BasicText("📅", style = TextStyle(fontSize = 16.sp))
                                }
                            }
                        }
                    }
                }
                
                // Date validation error
                if (!isEndDateValid) {
                    BasicText(
                        "End date must be after start date",
                        style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp)
                    )
                }
                
                // Currently studying checkbox
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { 
                            isCurrent = !isCurrent
                            if (isCurrent) endDate = ""
                        }
                        .padding(vertical = 8.dp),
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
                        "I am currently studying here",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
                
                // Grade / GPA (optional)
                EducationFormField(
                    label = "Grade / GPA (Optional)",
                    value = grade,
                    onValueChange = { grade = it },
                    placeholder = "e.g. 3.8 GPA, First Class, 95%",
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                // Activities & Societies (optional)
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(
                            "Activities & Societies (Optional)",
                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                        )
                        BasicText(
                            "${activities.length}/2000",
                            style = TextStyle(
                                if (activities.length > 1800) Color.Red.copy(alpha = 0.7f) 
                                else contentColor.copy(alpha = 0.4f),
                                11.sp
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (!isActivitiesValid) Color.Red.copy(alpha = 0.5f) 
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = activities,
                            onValueChange = { if (it.length <= 2000) activities = it },
                            textStyle = TextStyle(contentColor, 14.sp),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { innerTextField ->
                                if (activities.isEmpty()) {
                                    BasicText(
                                        "e.g. CS Club, Student Council, Sports team...",
                                        style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    if (!isActivitiesValid) {
                        BasicText(
                            "Maximum 2000 characters",
                            style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Description (optional)
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicText(
                            "Description (Optional)",
                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                        )
                        BasicText(
                            "${description.length}/2000",
                            style = TextStyle(
                                if (description.length > 1800) Color.Red.copy(alpha = 0.7f) 
                                else contentColor.copy(alpha = 0.4f),
                                11.sp
                            )
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (!isDescriptionValid) Color.Red.copy(alpha = 0.5f) 
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = description,
                            onValueChange = { if (it.length <= 2000) description = it },
                            textStyle = TextStyle(contentColor, 14.sp),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { innerTextField ->
                                if (description.isEmpty()) {
                                    BasicText(
                                        "Coursework, achievements, highlights...",
                                        style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                    if (!isDescriptionValid) {
                        BasicText(
                            "Maximum 2000 characters",
                            style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp),
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                            "Delete Education",
                            style = TextStyle(Color.Red, 14.sp, FontWeight.Medium)
                        )
                    }
                }
                
                // Extra bottom spacing
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ==================== Helper Composables ====================

@Composable
private fun EducationFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    singleLine: Boolean = true,
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
                    if (isError) Modifier.border(
                        1.dp,
                        Color.Red.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    ) else Modifier
                )
                .padding(12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(accentColor),
                singleLine = singleLine,
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
        errorText?.let {
            BasicText(
                it,
                style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
