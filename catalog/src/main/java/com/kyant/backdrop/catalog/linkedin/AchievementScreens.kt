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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Achievement
import com.kyant.backdrop.catalog.network.models.AchievementInput
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Achievement type options with their display names and icons
val ACHIEVEMENT_TYPES = listOf(
    "Hackathon" to R.drawable.ic_target,
    "Competition" to R.drawable.ic_trophy,
    "Award" to R.drawable.ic_medal,
    "Scholarship" to R.drawable.ic_gift,
    "Recognition" to R.drawable.ic_sparkles
)

// Color options for achievement cards (same as certificates)
val ACHIEVEMENT_COLOR_OPTIONS = listOf(
    Color(0xFF6B7280) to "Neutral",
    Color(0xFFEF4444) to "Red",
    Color(0xFFF97316) to "Orange",
    Color(0xFFF59E0B) to "Amber",
    Color(0xFF22C55E) to "Green",
    Color(0xFF3B82F6) to "Blue",
    Color(0xFF6366F1) to "Indigo",
    Color(0xFFA855F7) to "Purple",
    Color(0xFFEC4899) to "Pink"
)

// Helper to get icon for achievement type
internal fun getAchievementTypeIcon(type: String): Int {
    return ACHIEVEMENT_TYPES.find { it.first == type }?.second ?: R.drawable.ic_trophy
}

// Helper to get achievement color from hex
internal fun getAchievementColor(colorHex: String?): Color {
    if (colorHex.isNullOrEmpty()) return ACHIEVEMENT_COLOR_OPTIONS[0].first
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        ACHIEVEMENT_COLOR_OPTIONS[0].first
    }
}

// Helper to convert Color to hex string
private fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

// Validation helpers
private fun isValidUrl(url: String): Boolean {
    return url.isEmpty() || url.startsWith("http://") || url.startsWith("https://")
}

// Date formatting helpers
private fun parseIsoToMillis(isoDate: String?): Long? {
    if (isoDate.isNullOrEmpty()) return null
    return try {
        val date = LocalDate.parse(isoDate.take(10))
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

private fun formatMillisToIso(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

private fun formatFullDate(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate.take(10))
        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    } catch (e: Exception) {
        isoDate
    }
}

// ==================== Add/Edit Achievement Screen ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAchievementScreen(
    achievement: Achievement? = null, // null = Add mode, non-null = Edit mode
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (Achievement) -> Unit,
    onDelete: ((Achievement) -> Unit)? = null,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = achievement != null
    
    // Form state
    var selectedType by remember { mutableStateOf(achievement?.type ?: "Recognition") }
    var title by remember { mutableStateOf(achievement?.title ?: "") }
    var organization by remember { mutableStateOf(achievement?.organization ?: "") }
    var date by remember { mutableStateOf(achievement?.date ?: "") }
    var description by remember { mutableStateOf(achievement?.description ?: "") }
    var certificateUrl by remember { mutableStateOf(achievement?.certificateUrl ?: "") }
    var selectedColor by remember { mutableStateOf(achievement?.color ?: colorToHex(ACHIEVEMENT_COLOR_OPTIONS[0].first)) }
    
    // Track if the URL was uploaded (to always show preview for uploads)
    var isUploadedImage by remember { mutableStateOf(achievement?.certificateUrl?.let { isImageUrl(it) } ?: false) }
    
    // Proof input mode: "upload" or "url"
    var proofInputMode by remember { 
        mutableStateOf(if (achievement?.certificateUrl != null && !isImageUrl(achievement.certificateUrl)) "url" else "upload") 
    }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    
    // Validation
    val isTitleValid = title.trim().length in 2..100
    val isOrgValid = organization.trim().length in 2..100
    val isDescriptionValid = description.isEmpty() || description.length <= 2000
    val isUrlValid = certificateUrl.isEmpty() || isValidUrl(certificateUrl)
    val isValid = isTitleValid && isOrgValid && date.isNotBlank() && isDescriptionValid && isUrlValid
    
    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploadingImage = true
                errorMessage = null
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    // Check file size (max 5MB)
                    if (bytes.size > 5 * 1024 * 1024) {
                        errorMessage = "Image must be less than 5MB"
                        isUploadingImage = false
                        return@launch
                    }
                    ApiClient.uploadCertificateImage(context, bytes)
                        .onSuccess { imageUrl ->
                            if (imageUrl.isNotEmpty()) {
                                certificateUrl = imageUrl
                                proofInputMode = "upload"
                                isUploadedImage = true
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
    
    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseIsoToMillis(date)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = formatMillisToIso(it)
                    }
                    showDatePicker = false
                }) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp, FontWeight.Medium))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && achievement != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { BasicText("Delete Achievement", style = TextStyle(contentColor, 18.sp, FontWeight.SemiBold)) },
            text = { BasicText("Are you sure you want to delete this achievement? This action cannot be undone.", style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            isLoading = true
                            ApiClient.deleteAchievement(context, achievement.id)
                                .onSuccess {
                                    onDelete?.invoke(achievement)
                                }
                                .onFailure { e ->
                                    errorMessage = e.message ?: "Failed to delete achievement"
                                }
                            isLoading = false
                        }
                    }
                ) {
                    BasicText("Delete", style = TextStyle(Color.Red, 14.sp, FontWeight.Medium))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                }
            }
        )
    }
    
    // Main content - Full screen dialog
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    when {
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
            Column(Modifier.fillMaxSize()) {
                // Glass Header with actions
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button
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
                                .clickable(onClick = onCancel)
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
                        
                        // Title
                        BasicText(
                            if (isEditMode) "Edit Achievement" else "Add Achievement",
                            style = TextStyle(
                                if (isGlassTheme || isDarkTheme) Color.White else Color.Black.copy(alpha = 0.9f),
                                16.sp,
                                FontWeight.SemiBold
                            )
                        )
                        
                        // Save button
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isValid && !isLoading) accentColor else accentColor.copy(alpha = 0.5f))
                                .clickable(enabled = isValid && !isLoading) {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        
                                        val input = AchievementInput(
                                            title = title.trim(),
                                            type = selectedType,
                                            organization = organization.trim(),
                                            date = date,
                                            description = description.trim().ifEmpty { null },
                                            certificateUrl = certificateUrl.trim().ifEmpty { null },
                                            color = selectedColor
                                        )
                                        
                                        val result = if (isEditMode) {
                                            ApiClient.updateAchievement(context, achievement!!.id, input)
                                        } else {
                                            ApiClient.createAchievement(context, input)
                                        }
                                        
                                        result
                                            .onSuccess { savedAchievement ->
                                                onSave(savedAchievement)
                                            }
                                            .onFailure { e ->
                                                errorMessage = e.message ?: "Failed to save achievement"
                                            }
                                        
                                        isLoading = false
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                BasicText(
                                    if (isEditMode) "Save" else "Add",
                                    style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
                
                // Scrollable form content
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Error message
                    errorMessage?.let { error ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .padding(12.dp)
                        ) {
                            BasicText(error, style = TextStyle(Color.Red, 13.sp))
                        }
                    }
                    
                    // Type selector
                    Column {
                        BasicText(
                            "Type *",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ACHIEVEMENT_TYPES.forEach { (type, iconRes) ->
                                val isSelected = selectedType == type
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.2f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedType = type }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(iconRes),
                                            contentDescription = type,
                                            modifier = Modifier.size(18.dp),
                                            colorFilter = ColorFilter.tint(
                                                if (isSelected) accentColor else Color.White.copy(alpha = 0.7f)
                                            )
                                        )
                                        BasicText(
                                            type,
                                            style = TextStyle(
                                                if (isSelected) accentColor else Color.White,
                                                13.sp,
                                                FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Title field
                    Column {
                        BasicText(
                            "Title *",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(16.dp)
                        ) {
                            BasicTextField(
                                value = title,
                                onValueChange = { if (it.length <= 100) title = it },
                                textStyle = TextStyle(Color.White, 15.sp),
                                cursorBrush = SolidColor(accentColor),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (title.isEmpty()) {
                                            BasicText(
                                                "e.g. 1st Place Winner, Best Delegate",
                                                style = TextStyle(contentColor.copy(alpha = 0.6f), 15.sp)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        if (title.isNotEmpty() && !isTitleValid) {
                            BasicText(
                                "2-100 characters required",
                                style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                    
                    // Organization field
                    Column {
                        BasicText(
                            "Organization *",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(16.dp)
                        ) {
                            BasicTextField(
                                value = organization,
                                onValueChange = { if (it.length <= 100) organization = it },
                                textStyle = TextStyle(Color.White, 15.sp),
                                cursorBrush = SolidColor(accentColor),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (organization.isEmpty()) {
                                            BasicText(
                                                "e.g. Google, University, Hackathon name",
                                                style = TextStyle(contentColor.copy(alpha = 0.6f), 15.sp)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        if (organization.isNotEmpty() && !isOrgValid) {
                            BasicText(
                                "2-100 characters required",
                                style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                    
                    // Date picker
                    Column {
                        BasicText(
                            "Date *",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { showDatePicker = true }
                                .padding(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    if (date.isNotEmpty()) formatFullDate(date) else "Select date",
                                    style = TextStyle(
                                        if (date.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                                        15.sp
                                    )
                                )
                                Image(
                                    painter = painterResource(R.drawable.ic_calendar),
                                    contentDescription = "Select date",
                                    modifier = Modifier.size(20.dp),
                                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                    
                    // Card color
                    Column {
                        BasicText(
                            "Card color",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ACHIEVEMENT_COLOR_OPTIONS.forEach { (color, name) ->
                                val hex = colorToHex(color)
                                val isSelected = selectedColor == hex
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        BasicText("✓", style = TextStyle(Color.White, 16.sp, FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Description
                    Column {
                        BasicText(
                            "Description",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(16.dp)
                        ) {
                            BasicTextField(
                                value = description,
                                onValueChange = { if (it.length <= 2000) description = it },
                                textStyle = TextStyle(Color.White, 15.sp),
                                cursorBrush = SolidColor(accentColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (description.isEmpty()) {
                                            BasicText(
                                                "Describe your achievement...",
                                                style = TextStyle(contentColor.copy(alpha = 0.6f), 15.sp)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                        if (description.isNotEmpty()) {
                            BasicText(
                                "${description.length}/2000",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                    
                    // Proof / Certificate section
                    Column {
                        BasicText(
                            "Proof / Certificate",
                            style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        // Mode tabs
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (proofInputMode == "upload") accentColor.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable { proofInputMode = "upload" }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "Upload file",
                                    style = TextStyle(
                                        if (proofInputMode == "upload") accentColor else Color.White.copy(alpha = 0.6f),
                                        14.sp,
                                        FontWeight.Medium
                                    )
                                )
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (proofInputMode == "url") accentColor.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable { proofInputMode = "url" }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "Link URL",
                                    style = TextStyle(
                                        if (proofInputMode == "url") accentColor else Color.White.copy(alpha = 0.6f),
                                        14.sp,
                                        FontWeight.Medium
                                    )
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        if (proofInputMode == "upload") {
                            if (certificateUrl.isNotEmpty() && (isUploadedImage || isImageUrl(certificateUrl))) {
                                // Show preview
                                Column {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(certificateUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Proof preview",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        BasicText(
                                            "Replace",
                                            style = TextStyle(accentColor, 14.sp, FontWeight.Medium),
                                            modifier = Modifier.clickable { imagePicker.launch("image/*") }
                                        )
                                        BasicText(
                                            "Remove",
                                            style = TextStyle(Color.Red.copy(alpha = 0.8f), 14.sp),
                                            modifier = Modifier.clickable { 
                                                certificateUrl = ""
                                                isUploadedImage = false
                                            }
                                        )
                                    }
                                }
                            } else {
                                // Upload button
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(
                                            2.dp,
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !isUploadingImage) {
                                            imagePicker.launch("image/*")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUploadingImage) {
                                        CircularProgressIndicator(
                                            color = accentColor,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_upload),
                                                contentDescription = "Upload",
                                                modifier = Modifier.size(32.dp),
                                                colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.5f))
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            BasicText(
                                                "Tap to upload proof or certificate",
                                                style = TextStyle(Color.White.copy(alpha = 0.6f), 14.sp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // URL input mode
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(16.dp)
                            ) {
                                BasicTextField(
                                    value = certificateUrl,
                                    onValueChange = { certificateUrl = it },
                                    textStyle = TextStyle(Color.White, 15.sp),
                                    cursorBrush = SolidColor(accentColor),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (certificateUrl.isEmpty()) {
                                                BasicText(
                                                    "https://...",
                                                    style = TextStyle(contentColor.copy(alpha = 0.6f), 15.sp)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                            if (certificateUrl.isNotEmpty() && !isUrlValid) {
                                BasicText(
                                    "Invalid URL",
                                    style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp),
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // Delete button (edit mode only)
                    if (isEditMode && onDelete != null) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(alpha = 0.1f))
                                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { showDeleteDialog = true }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "Delete Achievement",
                                style = TextStyle(Color.Red, 14.sp, FontWeight.Medium)
                            )
                        }
                    }
                    
                    // Bottom spacing
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ==================== Achievement Detail Modal ====================

@Composable
fun AchievementDetailModal(
    achievement: Achievement,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isImage = isImageUrl(achievement.certificateUrl)
    val cardColor = getAchievementColor(achievement.color)
    val typeIcon = getAchievementTypeIcon(achievement.type)
    
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Single card containing everything
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {} // Prevent click propagation
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(20f.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.12f))
                    }
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Achievement Details",
                        style = TextStyle(Color.White, 16.sp, FontWeight.SemiBold)
                    )
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable(onClick = onDismiss)
                            .padding(8.dp)
                    ) {
                        BasicText("✕", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                    }
                }
                
                // Certificate/Proof image thumbnail (if image URL) - smaller size
                if (isImage && achievement.certificateUrl != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(achievement.certificateUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = achievement.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Achievement info with type icon and color accent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type icon with color
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(typeIcon),
                            contentDescription = achievement.type,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(cardColor)
                        )
                    }
                    Column {
                        BasicText(
                            achievement.title,
                            style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                        )
                        BasicText(
                            achievement.organization,
                            style = TextStyle(Color.White.copy(alpha = 0.7f), 14.sp)
                        )
                    }
                }
                
                // Info rows in a subtle box
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Type
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Type",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                            )
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cardColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                BasicText(
                                    achievement.type,
                                    style = TextStyle(cardColor, 12.sp, FontWeight.Medium)
                                )
                            }
                        }
                        
                        // Date
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Date",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                            )
                            BasicText(
                                formatFullDate(achievement.date),
                                style = TextStyle(Color.White.copy(alpha = 0.9f), 13.sp, FontWeight.Medium)
                            )
                        }
                        
                        // Description (if present)
                        achievement.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Column {
                                BasicText(
                                    "Description",
                                    style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                                )
                                Spacer(Modifier.height(4.dp))
                                BasicText(
                                    desc,
                                    style = TextStyle(Color.White.copy(alpha = 0.8f), 13.sp)
                                )
                            }
                        }
                    }
                }
                
                // Action button (Open link for non-image URL)
                if (!isImage && achievement.certificateUrl != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(achievement.certificateUrl))
                                )
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_open_in_browser),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicText(
                                "Open Proof Link",
                                style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== Form Field Component ====================

@Composable
private fun AchievementFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column {
        if (label.isNotEmpty()) {
            BasicText(
                label,
                style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp, FontWeight.Medium)
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(contentColor.copy(alpha = 0.08f))
                .then(
                    if (isError) Modifier.border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    else Modifier
                )
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(contentColor, 15.sp),
                cursorBrush = SolidColor(accentColor),
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            BasicText(
                                placeholder,
                                style = TextStyle(contentColor.copy(alpha = 0.4f), 15.sp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (errorMessage != null) {
            BasicText(
                errorMessage,
                style = TextStyle(Color.Red, 11.sp),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
