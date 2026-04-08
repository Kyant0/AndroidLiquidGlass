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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Certificate
import com.kyant.backdrop.catalog.network.models.CertificateInput
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Predefined color palette for certificate cards
val CERTIFICATE_COLOR_OPTIONS = listOf(
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

// Image file extensions (including modern formats)
internal val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".avif", ".heic", ".heif")

internal fun isImageUrl(url: String?): Boolean {
    if (url.isNullOrEmpty()) return false
    val lowerUrl = url.lowercase()
    return IMAGE_EXTENSIONS.any { lowerUrl.endsWith(it) } ||
            lowerUrl.contains("cloudinary.com") ||
            lowerUrl.contains("bunnycdn.com") ||
            lowerUrl.contains("b-cdn.net") ||
            lowerUrl.contains("imgur.com") ||
            lowerUrl.contains("/image/") ||
            lowerUrl.contains("/photo/")
}

// ==================== Add/Edit Certificate Screen ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCertificateScreen(
    certificate: Certificate? = null, // null = Add mode, non-null = Edit mode
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onSave: (Certificate) -> Unit,
    onDelete: (() -> Unit)? = null,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditMode = certificate != null
    val certificateId = certificate?.id
    
    // Form state
    var name by remember { mutableStateOf(certificate?.name ?: "") }
    var issuingOrg by remember { mutableStateOf(certificate?.issuingOrg ?: "") }
    var issueDate by remember { mutableStateOf(certificate?.issueDate ?: "") }
    var expiryDate by remember { mutableStateOf(certificate?.expiryDate ?: "") }
    var doesNotExpire by remember { mutableStateOf(certificate?.doesNotExpire ?: false) }
    var credentialId by remember { mutableStateOf(certificate?.credentialId ?: "") }
    var credentialUrl by remember { mutableStateOf(certificate?.credentialUrl ?: "") }
    var selectedColor by remember { mutableStateOf(certificate?.color ?: colorToHex(CERTIFICATE_COLOR_OPTIONS[0].first)) }
    
    // Track if the URL was uploaded (to always show preview for uploads)
    var isUploadedImage by remember { mutableStateOf(certificate?.credentialUrl?.let { isImageUrl(it) } ?: false) }
    
    // Credential input mode: "upload" or "url"
    var credentialInputMode by remember { 
        mutableStateOf(if (certificate?.credentialUrl != null && !isImageUrl(certificate.credentialUrl)) "url" else "upload") 
    }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showExpiryDatePicker by remember { mutableStateOf(false) }

    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isDarkTheme = themeMode == "dark"
    
    // Validation
    val isNameValid = name.trim().length in 2..100
    val isIssuingOrgValid = issuingOrg.trim().length in 2..100
    val isUrlValid = credentialUrl.isEmpty() || isValidUrl(credentialUrl)
    val isExpiryValid = doesNotExpire || expiryDate.isEmpty() || 
        (issueDate.isNotEmpty() && compareDates(issueDate, expiryDate) < 0)
    val isValid = isNameValid && isIssuingOrgValid && issueDate.isNotBlank() && isUrlValid && isExpiryValid
    
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
                                credentialUrl = imageUrl
                                credentialInputMode = "upload"
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
    
    // Date pickers
    if (showIssueDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseIsoToMillis(issueDate)
        )
        DatePickerDialog(
            onDismissRequest = { showIssueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        issueDate = formatMillisToIso(it)
                    }
                    showIssueDatePicker = false
                }) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIssueDatePicker = false }) {
                    BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showExpiryDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseIsoToMillis(expiryDate)
        )
        DatePickerDialog(
            onDismissRequest = { showExpiryDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        expiryDate = formatMillisToIso(it)
                    }
                    showExpiryDatePicker = false
                }) {
                    BasicText("OK", style = TextStyle(accentColor, 14.sp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryDatePicker = false }) {
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
            title = { BasicText("Delete Certification", style = TextStyle(contentColor, 18.sp, FontWeight.Bold)) },
            text = { BasicText("Are you sure you want to delete this certification? This action cannot be undone.", style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp)) },
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
                        if (isEditMode) "Edit Certification" else "Add Certification",
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
                                    val input = CertificateInput(
                                        name = name.trim(),
                                        issuingOrg = issuingOrg.trim(),
                                        issueDate = issueDate,
                                        expiryDate = if (doesNotExpire) null else expiryDate.takeIf { it.isNotBlank() },
                                        doesNotExpire = doesNotExpire,
                                        credentialId = credentialId.takeIf { it.isNotBlank() }?.trim(),
                                        credentialUrl = credentialUrl.takeIf { it.isNotBlank() }?.trim(),
                                        color = selectedColor
                                    )
                                    
                                    val result = if (isEditMode && certificateId != null) {
                                        ApiClient.updateCertificate(context, certificateId, input)
                                    } else {
                                        ApiClient.createCertificate(context, input)
                                    }
                                    
                                    result
                                        .onSuccess { savedCertificate ->
                                            onSave(savedCertificate)
                                        }
                                        .onFailure { e ->
                                            errorMessage = e.message ?: "Failed to save certification"
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
                // Certificate Name (Required)
                CertificateFormField(
                    label = "Certificate Name *",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. AWS Solutions Architect, First Aid, IELTS, Driving License",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    isError = name.isNotEmpty() && !isNameValid,
                    errorText = if (name.isNotEmpty() && !isNameValid) "Must be 2-100 characters" else null
                )
                
                // Issuing Organization (Required)
                CertificateFormField(
                    label = "Issuing Organization *",
                    value = issuingOrg,
                    onValueChange = { issuingOrg = it },
                    placeholder = "e.g. Amazon Web Services, Red Cross, British Council",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true,
                    isError = issuingOrg.isNotEmpty() && !isIssuingOrgValid,
                    errorText = if (issuingOrg.isNotEmpty() && !isIssuingOrgValid) "Must be 2-100 characters" else null
                )
                
                // Issue Date (Required)
                Column {
                    BasicText(
                        "Issue Date *",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.08f))
                            .border(
                                2.dp,
                                contentColor.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showIssueDatePicker = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                if (issueDate.isNotEmpty()) formatDisplayDate(issueDate) else "Select issue date",
                                style = TextStyle(
                                    if (issueDate.isNotEmpty()) contentColor else contentColor.copy(alpha = 0.4f),
                                    14.sp
                                )
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = "Pick date",
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
                
                // Expiry Date (Optional)
                Column {
                    BasicText(
                        "Expiry Date",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (doesNotExpire) contentColor.copy(alpha = 0.04f) 
                                else contentColor.copy(alpha = 0.08f)
                            )
                            .border(
                                2.dp,
                                contentColor.copy(alpha = if (doesNotExpire) 0.1f else 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = !doesNotExpire) { showExpiryDatePicker = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                when {
                                    doesNotExpire -> "Not applicable"
                                    expiryDate.isNotEmpty() -> formatDisplayDate(expiryDate)
                                    else -> "Select expiry date"
                                },
                                style = TextStyle(
                                    contentColor.copy(alpha = if (doesNotExpire) 0.3f else if (expiryDate.isNotEmpty()) 1f else 0.4f),
                                    14.sp
                                )
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = "Pick date",
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = if (doesNotExpire) 0.2f else 0.5f))
                            )
                        }
                    }
                    
                    // Validation error for expiry date
                    if (expiryDate.isNotEmpty() && !doesNotExpire && !isExpiryValid) {
                        Spacer(Modifier.height(4.dp))
                        BasicText(
                            "Expiry date must be after issue date",
                            style = TextStyle(Color.Red, 11.sp)
                        )
                    }
                }
                
                // This credential does not expire checkbox
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { 
                            doesNotExpire = !doesNotExpire
                            if (doesNotExpire) expiryDate = ""
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = doesNotExpire,
                        onCheckedChange = { 
                            doesNotExpire = it
                            if (it) expiryDate = ""
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = contentColor.copy(alpha = 0.4f),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        "This credential does not expire",
                        style = TextStyle(contentColor, 14.sp)
                    )
                }
                
                // Card Color (Optional - client-side only)
                Column {
                    BasicText(
                        "Card Color (optional)",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    BasicText(
                        "Choose an accent color for the certificate card",
                        style = TextStyle(contentColor.copy(alpha = 0.4f), 11.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CERTIFICATE_COLOR_OPTIONS.forEach { (color, name) ->
                            val isSelected = selectedColor == colorToHex(color)
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorToHex(color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    BasicText(
                                        "✓",
                                        style = TextStyle(Color.White, 16.sp, FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Credential ID (Optional)
                CertificateFormField(
                    label = "Credential ID (optional)",
                    value = credentialId,
                    onValueChange = { credentialId = it },
                    placeholder = "e.g. verification code or ID from issuer",
                    contentColor = contentColor,
                    accentColor = accentColor,
                    singleLine = true
                )
                
                // Credential URL / File
                Column {
                    BasicText(
                        "Credential URL / File (optional)",
                        style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                    )
                    BasicText(
                        "Upload certificate image or paste verification URL",
                        style = TextStyle(contentColor.copy(alpha = 0.4f), 11.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    // Toggle between upload and URL
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (credentialInputMode == "upload") accentColor.copy(alpha = 0.2f)
                                    else contentColor.copy(alpha = 0.08f)
                                )
                                .clickable { credentialInputMode = "upload" }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "Upload File",
                                style = TextStyle(
                                    if (credentialInputMode == "upload") accentColor else contentColor.copy(alpha = 0.6f),
                                    13.sp,
                                    FontWeight.Medium
                                )
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (credentialInputMode == "url") accentColor.copy(alpha = 0.2f)
                                    else contentColor.copy(alpha = 0.08f)
                                )
                                .clickable { credentialInputMode = "url" }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "Link URL",
                                style = TextStyle(
                                    if (credentialInputMode == "url") accentColor else contentColor.copy(alpha = 0.6f),
                                    13.sp,
                                    FontWeight.Medium
                                )
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    if (credentialInputMode == "upload") {
                        // Upload file mode
                        if (credentialUrl.isNotEmpty() && (isUploadedImage || isImageUrl(credentialUrl))) {
                            // Show preview
                            Column {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(contentColor.copy(alpha = 0.08f))
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(credentialUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Certificate preview",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BasicText(
                                        "Replace",
                                        style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                                        modifier = Modifier.clickable { imagePicker.launch("image/*") }
                                    )
                                    BasicText(
                                        "Remove",
                                        style = TextStyle(Color.Red.copy(alpha = 0.7f), 13.sp),
                                        modifier = Modifier.clickable { 
                                            credentialUrl = ""
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
                                    .background(contentColor.copy(alpha = 0.08f))
                                    .border(
                                        2.dp,
                                        contentColor.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !isUploadingImage) {
                                        imagePicker.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = accentColor,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_upload),
                                            contentDescription = "Upload",
                                            modifier = Modifier.size(32.dp),
                                            colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.4f))
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        BasicText(
                                            "Tap to upload certificate image",
                                            style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
                                        )
                                        BasicText(
                                            "Max 5MB • JPG, PNG, GIF",
                                            style = TextStyle(contentColor.copy(alpha = 0.3f), 11.sp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // URL input mode
                        CertificateFormField(
                            label = "",
                            value = credentialUrl,
                            onValueChange = { credentialUrl = it },
                            placeholder = "https://example.com/verify/...",
                            contentColor = contentColor,
                            accentColor = accentColor,
                            singleLine = true,
                            keyboardType = KeyboardType.Uri,
                            isError = credentialUrl.isNotEmpty() && !isUrlValid,
                            errorText = if (credentialUrl.isNotEmpty() && !isUrlValid) "Please enter a valid URL" else null
                        )
                    }
                }
                
                // Delete button (edit mode only)
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
                            "Delete Certification",
                            style = TextStyle(Color.Red, 14.sp, FontWeight.Medium)
                        )
                    }
                }
                
                // Bottom padding
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ==================== Certificate Detail Modal ====================

@Composable
fun CertificateDetailModal(
    certificate: Certificate,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isImage = isImageUrl(certificate.credentialUrl)
    val cardColor = getCertificateColor(certificate.color)
    val isExpired = !certificate.doesNotExpire && 
        certificate.expiryDate != null && 
        isExpiredDate(certificate.expiryDate)
    
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
                        "Certificate Details",
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
                
                // Certificate image thumbnail (if image URL) - smaller size
                if (isImage && certificate.credentialUrl != null) {
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
                                .data(certificate.credentialUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = certificate.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Certificate name with color accent
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Color bar accent
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cardColor)
                    )
                    Column {
                        BasicText(
                            certificate.name,
                            style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                        )
                        BasicText(
                            certificate.issuingOrg,
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
                        // Issue date
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Issued",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                            )
                            BasicText(
                                formatFullDate(certificate.issueDate),
                                style = TextStyle(Color.White.copy(alpha = 0.9f), 13.sp, FontWeight.Medium)
                            )
                        }
                        
                        // Expiry status
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicText(
                                "Expires",
                                style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                when {
                                    certificate.doesNotExpire -> {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            BasicText(
                                                "Never",
                                                style = TextStyle(Color(0xFF22C55E), 12.sp, FontWeight.Medium)
                                            )
                                        }
                                    }
                                    isExpired -> {
                                        Box(
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            BasicText(
                                                "Expired",
                                                style = TextStyle(Color(0xFFEF4444), 12.sp, FontWeight.Medium)
                                            )
                                        }
                                    }
                                    certificate.expiryDate != null -> {
                                        BasicText(
                                            formatFullDate(certificate.expiryDate),
                                            style = TextStyle(Color.White.copy(alpha = 0.9f), 13.sp, FontWeight.Medium)
                                        )
                                    }
                                    else -> {
                                        BasicText(
                                            "Not specified",
                                            style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Credential ID (if present)
                        certificate.credentialId?.takeIf { it.isNotBlank() }?.let { credId ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicText(
                                    "Credential ID",
                                    style = TextStyle(Color.White.copy(alpha = 0.5f), 13.sp)
                                )
                                BasicText(
                                    credId,
                                    style = TextStyle(Color.White.copy(alpha = 0.9f), 13.sp, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
                
                // Action button (Open link for non-image URL)
                if (!isImage && certificate.credentialUrl != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(certificate.credentialUrl))
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
                                "Open Certificate Link",
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
private fun CertificateFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    accentColor: Color,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null
) {
    Column {
        if (label.isNotEmpty()) {
            BasicText(
                label,
                style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(contentColor.copy(alpha = 0.08f))
                .border(
                    2.dp,
                    if (isError) Color.Red.copy(alpha = 0.5f) else contentColor.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(contentColor, 14.sp),
                cursorBrush = SolidColor(accentColor),
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
                                style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        errorText?.let { error ->
            Spacer(Modifier.height(4.dp))
            BasicText(error, style = TextStyle(Color.Red, 11.sp))
        }
    }
}

// ==================== Helper Functions ====================

private fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

private fun isValidUrl(url: String): Boolean {
    return try {
        val pattern = "^(https?://)?[\\w.-]+\\.[a-z]{2,}(/.*)?$".toRegex(RegexOption.IGNORE_CASE)
        pattern.matches(url) || android.util.Patterns.WEB_URL.matcher(url).matches()
    } catch (e: Exception) {
        false
    }
}

private fun compareDates(date1: String, date2: String): Int {
    return try {
        val d1 = LocalDate.parse(date1.take(10))
        val d2 = LocalDate.parse(date2.take(10))
        d1.compareTo(d2)
    } catch (e: Exception) {
        0
    }
}

private fun isExpiredDate(dateString: String): Boolean {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

private fun formatDisplayDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun formatFullDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun parseIsoToMillis(isoDate: String): Long? {
    return try {
        if (isoDate.isBlank()) return null
        val localDate = LocalDate.parse(isoDate.take(10))
        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

private fun formatMillisToIso(millis: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(millis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: Exception) {
        ""
    }
}
