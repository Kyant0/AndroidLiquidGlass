package com.kyant.backdrop.catalog.linkedin

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.components.LiquidToggle
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.GrowthApiService
import kotlinx.coroutines.launch

private data class HelpFaqItemData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val answer: String
)

@Composable
private fun SettingsScreenContainer(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsIconBadge(
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun Modifier.settingsSurface(
    contentColor: Color,
    cornerRadius: Dp = 18.dp,
    containerColor: Color? = null,
    outlineColor: Color? = null
): Modifier = composed {
    val appearance = currentVormexAppearance()
    val isDarkSurface = appearance.isDarkTheme
    val resolvedOutline = outlineColor ?: if (appearance.isGlassTheme) {
        if (isDarkSurface) {
            Color.White.copy(alpha = 0.14f)
        } else {
            Color.White.copy(alpha = 0.42f)
        }
    } else {
        appearance.cardBorderColor
    }
    val shape = RoundedCornerShape(cornerRadius)

    val base = this.clip(shape)

    val withBackground = if (containerColor != null) {
        base.background(containerColor)
    } else if (!appearance.isGlassTheme) {
        base.background(appearance.cardColor)
    } else {
        base.background(
            brush = Brush.verticalGradient(
                colors = if (isDarkSurface) {
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.07f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.18f)
                    )
                }
            )
        )
    }

    withBackground
        .border(1.dp, resolvedOutline, shape)
}

// ==================== NOTIFICATION SETTINGS SCREEN ====================

@Composable
fun NotificationSettingsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Check if system notifications are enabled
    val systemNotificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    // Collect all notification preferences
    val pushEnabled by SettingsPreferences.pushNotificationsEnabled(context).collectAsState(initial = true)
    val dailyDigestEnabled by SettingsPreferences.dailyDigestEnabled(context).collectAsState(initial = true)
    val dailyDigestTime by SettingsPreferences.dailyDigestTime(context).collectAsState(initial = "09:00")
    val matchAlertsEnabled by SettingsPreferences.matchAlertsEnabled(context).collectAsState(initial = true)
    val messageNotificationsEnabled by SettingsPreferences.messageNotificationsEnabled(context).collectAsState(initial = true)
    val connectionNotificationsEnabled by SettingsPreferences.connectionNotificationsEnabled(context).collectAsState(initial = true)
    val likeNotificationsEnabled by SettingsPreferences.likeNotificationsEnabled(context).collectAsState(initial = true)
    val commentNotificationsEnabled by SettingsPreferences.commentNotificationsEnabled(context).collectAsState(initial = true)
    val streakRemindersEnabled by SettingsPreferences.streakRemindersEnabled(context).collectAsState(initial = true)
    val weeklySummaryEnabled by SettingsPreferences.weeklySummaryEnabled(context).collectAsState(initial = true)

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
            SettingsHeader(
                title = "Notifications",
                contentColor = contentColor,
                onBack = onNavigateBack
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .settingsSurface(
                        contentColor = contentColor,
                        cornerRadius = 18.dp,
                        containerColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.10f else 0.12f),
                        outlineColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.18f else 0.12f)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    BasicText(
                        "Notification control center",
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        "Fine tune alerts for messages, engagement, reminders, and digests.",
                        style = TextStyle(contentColor.copy(alpha = 0.66f), 12.sp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // System notifications
            item {
                SettingsSectionHeader("System", contentColor)
            }
            
            item {
                SettingsActionItem(
                    title = "System Notifications",
                    subtitle = if (systemNotificationsEnabled) "Enabled" else "Disabled - Tap to enable",
                    icon = Icons.Outlined.NotificationsActive,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (systemNotificationsEnabled) "On" else "Off",
                    onClick = {
                        openAppNotificationSettings(context)
                    }
                )
            }
            
            // Master toggle
            item {
                SettingsSectionHeader("General", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Push Notifications",
                    subtitle = "Receive push notifications",
                    icon = Icons.Outlined.Notifications,
                    checked = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setPushNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Activity notifications
            item {
                SettingsSectionHeader("Activity", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Messages",
                    subtitle = "New messages and chat requests",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    checked = messageNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setMessageNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Connections",
                    subtitle = "Connection requests and acceptances",
                    icon = Icons.Outlined.Groups,
                    checked = connectionNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setConnectionNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Likes",
                    subtitle = "When someone likes your posts",
                    icon = Icons.Outlined.FavoriteBorder,
                    checked = likeNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setLikeNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Comments",
                    subtitle = "When someone comments on your posts",
                    icon = Icons.Outlined.ModeComment,
                    checked = commentNotificationsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setCommentNotificationsEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Match alerts
            item {
                SettingsSectionHeader("Discovery", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Match Alerts",
                    subtitle = "Daily matches and recommendations",
                    icon = Icons.Outlined.TrackChanges,
                    checked = matchAlertsEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setMatchAlertsEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Engagement reminders
            item {
                SettingsSectionHeader("Reminders", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Streak Reminders",
                    subtitle = "Don't lose your streak!",
                    icon = Icons.Outlined.LocalFireDepartment,
                    checked = streakRemindersEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setStreakRemindersEnabled(context, it) 
                        }
                    }
                )
            }
            
            // Digests
            item {
                SettingsSectionHeader("Digests & Summaries", contentColor)
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Daily Digest",
                    subtitle = "Daily summary at $dailyDigestTime",
                    icon = Icons.AutoMirrored.Outlined.Article,
                    checked = dailyDigestEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setDailyDigestEnabled(context, it) 
                        }
                    }
                )
            }
            
            item {
                NotificationLiquidToggleItem(
                    title = "Weekly Summary",
                    subtitle = "Your week in review",
                    icon = Icons.Outlined.Insights,
                    checked = weeklySummaryEnabled && pushEnabled,
                    enabled = pushEnabled,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setWeeklySummaryEnabled(context, it) 
                        }
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
            }
    }
}

@Composable
private fun NotificationLiquidToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    val subtitleColor = contentColor.copy(alpha = 0.55f * alpha)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(14.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor.copy(alpha = alpha), 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(subtitleColor, 12.sp)
                        )
                    }
                    if (!enabled) {
                        BasicText(
                            "Disabled while push notifications are off",
                            style = TextStyle(accentColor.copy(alpha = 0.7f), 11.sp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.alpha(alpha)
            ) {
                LiquidToggle(
                    selected = { checked },
                    onSelect = { if (enabled) onCheckedChange(it) },
                    backdrop = backdrop
                )
            }
        }
    }
}

// ==================== PRIVACY SETTINGS SCREEN ====================

@Composable
fun PrivacySettingsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Collect privacy preferences
    val profileVisibility by SettingsPreferences.profileVisibility(context).collectAsState(initial = "public")
    val whoCanMessage by SettingsPreferences.whoCanMessage(context).collectAsState(initial = "everyone")
    val showOnlineStatus by SettingsPreferences.showOnlineStatus(context).collectAsState(initial = true)
    val showActivityStatus by SettingsPreferences.showActivityStatus(context).collectAsState(initial = true)
    val showProfileViews by SettingsPreferences.showProfileViews(context).collectAsState(initial = true)
    val discoverableByEmail by SettingsPreferences.discoverableByEmail(context).collectAsState(initial = true)
    val discoverableByPhone by SettingsPreferences.discoverableByPhone(context).collectAsState(initial = false)
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Privacy",
            contentColor = contentColor,
            onBack = onNavigateBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .settingsSurface(
                    contentColor = contentColor,
                    cornerRadius = 20.dp,
                    containerColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.10f else 0.12f),
                    outlineColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.18f else 0.12f)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    "Privacy controls that feel clear",
                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
                )
                BasicText(
                    "Choose who can find you, message you, and see your activity without digging through cramped menus.",
                    style = TextStyle(contentColor.copy(alpha = 0.66f), 12.sp)
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile visibility
            item {
                SettingsSectionHeader("Profile Visibility", contentColor)
            }
            
            item {
                SettingsOptionItem(
                    title = "Who can see your profile",
                    subtitle = when (profileVisibility) {
                        "public" -> "Everyone"
                        "connections" -> "Only connections"
                        "private" -> "Only you"
                        else -> "Everyone"
                    },
                    icon = Icons.Outlined.Visibility,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    options = listOf(
                        "public" to "Everyone",
                        "connections" to "Only connections",
                        "private" to "Only you"
                    ),
                    selectedOption = profileVisibility,
                    onOptionSelected = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setProfileVisibility(context, it) 
                        }
                    }
                )
            }
            
            // Messaging
            item {
                SettingsSectionHeader("Messaging", contentColor)
            }
            
            item {
                SettingsOptionItem(
                    title = "Who can message you",
                    subtitle = when (whoCanMessage) {
                        "everyone" -> "Everyone"
                        "connections" -> "Only connections"
                        "none" -> "No one"
                        else -> "Everyone"
                    },
                    icon = Icons.Outlined.ChatBubbleOutline,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    options = listOf(
                        "everyone" to "Everyone",
                        "connections" to "Only connections",
                        "none" to "No one"
                    ),
                    selectedOption = whoCanMessage,
                    onOptionSelected = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setWhoCanMessage(context, it) 
                        }
                    }
                )
            }
            
            // Activity status
            item {
                SettingsSectionHeader("Activity Status", contentColor)
            }
            
            item {
                SettingsSwitchItem(
                    title = "Show Online Status",
                    subtitle = "Let others see when you're online",
                    icon = Icons.Outlined.Lens,
                    checked = showOnlineStatus,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setShowOnlineStatus(context, it) 
                        }
                    }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "Show Activity Status",
                    subtitle = "Show \"Active X minutes ago\"",
                    icon = Icons.Outlined.Schedule,
                    checked = showActivityStatus,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setShowActivityStatus(context, it) 
                        }
                    }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "Show Profile Views",
                    subtitle = "See who viewed your profile",
                    icon = Icons.Outlined.Visibility,
                    checked = showProfileViews,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setShowProfileViews(context, it) 
                        }
                    }
                )
            }
            
            // Discoverability
            item {
                SettingsSectionHeader("Discoverability", contentColor)
            }
            
            item {
                SettingsSwitchItem(
                    title = "Discoverable by Email",
                    subtitle = "Let others find you by email",
                    icon = Icons.Outlined.AlternateEmail,
                    checked = discoverableByEmail,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setDiscoverableByEmail(context, it) 
                        }
                    }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "Discoverable by Phone",
                    subtitle = "Let others find you by phone number",
                    icon = Icons.Outlined.PhoneAndroid,
                    checked = discoverableByPhone,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setDiscoverableByPhone(context, it) 
                        }
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ==================== APPEARANCE SETTINGS SCREEN ====================

@Composable
fun AppearanceSettingsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onThemeChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Collect appearance preferences
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val glassBackgroundKey by SettingsPreferences.glassBackgroundPreset(context)
        .collectAsState(initial = DefaultGlassBackgroundPresetKey)
    val accentPaletteKey by SettingsPreferences.accentPalette(context)
        .collectAsState(initial = DefaultAccentPaletteKey)
    val glassMotionStyleKey by SettingsPreferences.glassMotionStyle(context)
        .collectAsState(initial = DefaultGlassMotionStyleKey)
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    val selectedAccentColor = glassAccentPalette(accentPaletteKey).color
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Appearance",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BasicText(
                        "Choose Theme",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    BasicText(
                        "Select how Vormex looks to you",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    )
                }
            }
            
            // Theme Preview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Glass Theme Card
                    ThemePreviewCard(
                        modifier = Modifier.weight(1f),
                        themeName = "Glass",
                        themeKey = "glass",
                        isSelected = themeMode == "glass",
                        contentColor = contentColor,
                        backdrop = backdrop,
                        accentColor = accentColor,
                        onClick = {
                            coroutineScope.launch {
                                SettingsPreferences.setThemeMode(context, "glass")
                                onThemeChange("glass")
                            }
                        }
                    )
                    
                    // White Theme Card
                    ThemePreviewCard(
                        modifier = Modifier.weight(1f),
                        themeName = "White",
                        themeKey = "light",
                        isSelected = themeMode == "light",
                        contentColor = contentColor,
                        backdrop = backdrop,
                        accentColor = accentColor,
                        onClick = {
                            coroutineScope.launch {
                                SettingsPreferences.setThemeMode(context, "light")
                                onThemeChange("light")
                            }
                        }
                    )
                    
                    // Dark Theme Card
                    ThemePreviewCard(
                        modifier = Modifier.weight(1f),
                        themeName = "Dark",
                        themeKey = "dark",
                        isSelected = themeMode == "dark",
                        contentColor = contentColor,
                        backdrop = backdrop,
                        accentColor = accentColor,
                        onClick = {
                            coroutineScope.launch {
                                SettingsPreferences.setThemeMode(context, "dark")
                                onThemeChange("dark")
                            }
                        }
                    )
                }
            }

            if (themeMode == "glass") {
                item {
                    SettingsSubsectionHeader(
                        title = "Glass Background",
                        subtitle = "Pick the scene behind every glass surface in the app.",
                        contentColor = contentColor
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassBackgroundPresets.forEach { preset ->
                            GlassBackgroundChoiceCard(
                                preset = preset,
                                isSelected = glassBackgroundKey == preset.key,
                                contentColor = contentColor,
                                backdrop = backdrop,
                                accentColor = accentColor,
                                previewAccentColor = selectedAccentColor,
                                onClick = {
                                    coroutineScope.launch {
                                        SettingsPreferences.setGlassBackgroundPreset(context, preset.key)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSubsectionHeader(
                    title = "Accent Color",
                    subtitle = "Color the active glass controls and highlights.",
                    contentColor = contentColor
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassAccentPalettes.forEach { palette ->
                        GlassAccentChoiceCard(
                            palette = palette,
                            isSelected = accentPaletteKey == palette.key,
                            contentColor = contentColor,
                            backdrop = backdrop,
                            selectionColor = accentColor,
                            onClick = {
                                coroutineScope.launch {
                                    SettingsPreferences.setAccentPalette(context, palette.key)
                                }
                            }
                        )
                    }
                }
            }

            if (themeMode == "glass") {
                item {
                    SettingsSubsectionHeader(
                        title = "Glass Motion",
                        subtitle = "Choose how lively the background feels.",
                        contentColor = contentColor
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassMotionStyles.forEach { motionStyle ->
                            GlassMotionChoiceCard(
                                motionStyle = motionStyle,
                                isSelected = glassMotionStyleKey == motionStyle.key,
                                reduceAnimations = reduceAnimations,
                                contentColor = contentColor,
                                backdrop = backdrop,
                                accentColor = accentColor,
                                onClick = {
                                    coroutineScope.launch {
                                        SettingsPreferences.setGlassMotionStyle(context, motionStyle.key)
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsSurface(
                                contentColor = contentColor,
                                cornerRadius = 16.dp
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            if (reduceAnimations) {
                                "Reduce Animations is on, so the glass scene stays still until you turn motion back on."
                            } else {
                                "Background changes crossfade automatically, and the selected motion style drives the ambient drift."
                            },
                            style = TextStyle(
                                color = contentColor.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Accessibility Section
            item {
                BasicText(
                    "Accessibility",
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "Reduce Animations",
                    subtitle = "Minimize motion effects for better accessibility",
                    icon = Icons.Outlined.Tune,
                    checked = reduceAnimations,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onCheckedChange = { 
                        coroutineScope.launch { 
                            SettingsPreferences.setReduceAnimations(context, it) 
                        }
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    modifier: Modifier = Modifier,
    themeName: String,
    themeKey: String,
    isSelected: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "borderColor"
    )
    
    Column(
        modifier = modifier
            .settingsSurface(
                contentColor = contentColor,
                cornerRadius = 20.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Theme Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (themeKey) {
                        "glass" -> Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF64B5F6),
                                Color(0xFF42A5F5),
                                Color(0xFF26C6DA),
                                Color(0xFF4DD0E1)
                            )
                        )
                        "light" -> Brush.linearGradient(
                            colors = listOf(Color.White, Color(0xFFF5F5F5))
                        )
                        "dark" -> Brush.linearGradient(
                            colors = listOf(Color(0xFF1A1A1A), Color.Black)
                        )
                        else -> Brush.linearGradient(
                            colors = listOf(Color(0xFF64B5F6), Color(0xFF26C6DA))
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Mini phone UI preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Status bar mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (themeKey) {
                                "dark" -> Color.White.copy(alpha = 0.2f)
                                else -> Color.Black.copy(alpha = 0.1f)
                            }
                        )
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Content preview cards
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when (themeKey) {
                                    "dark" -> Color.White.copy(alpha = 0.15f)
                                    "glass" -> Color.White.copy(alpha = 0.4f)
                                    else -> Color.Black.copy(alpha = 0.08f)
                                }
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                }
                
                Spacer(Modifier.weight(1f))
                
                // Nav bar mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(5) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (themeKey) {
                                        "dark" -> Color.White.copy(alpha = 0.3f)
                                        else -> Color.Black.copy(alpha = 0.2f)
                                    }
                                )
                        )
                    }
                }
            }
        }
        
        // Theme name and icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (themeKey) {
                    "glass" -> Icons.Outlined.BlurOn
                    "light" -> Icons.Outlined.LightMode
                    "dark" -> Icons.Outlined.DarkMode
                    else -> Icons.Outlined.BlurOn
                },
                contentDescription = null,
                tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                themeName,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
        
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsSubsectionHeader(
    title: String,
    subtitle: String,
    contentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BasicText(
            title,
            style = TextStyle(
                color = contentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        BasicText(
            subtitle,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun GlassBackgroundChoiceCard(
    preset: GlassBackgroundPreset,
    isSelected: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop,
    accentColor: Color,
    previewAccentColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "glassBackgroundBorder"
    )

    Column(
        modifier = Modifier
            .width(152.dp)
            .settingsSurface(
                contentColor = contentColor,
                cornerRadius = 20.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(186.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            GlassBackgroundPreview(
                modifier = Modifier.fillMaxSize(),
                presetKey = preset.key,
                accentColor = previewAccentColor
            )
        }

        BasicText(
            preset.name,
            style = TextStyle(
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        BasicText(
            preset.description,
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 12.sp
            )
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                BasicText(
                    "Selected",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun GlassAccentChoiceCard(
    palette: GlassAccentPalette,
    isSelected: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop,
    selectionColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) selectionColor else Color.Transparent,
        label = "accentChoiceBorder"
    )

    Column(
        modifier = Modifier
            .width(112.dp)
            .settingsSurface(
                contentColor = contentColor,
                cornerRadius = 18.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            palette.color,
                            palette.color.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        BasicText(
            palette.name,
            style = TextStyle(
                color = contentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun GlassMotionChoiceCard(
    motionStyle: GlassMotionStyle,
    isSelected: Boolean,
    reduceAnimations: Boolean,
    contentColor: Color,
    backdrop: LayerBackdrop,
    accentColor: Color,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "motionChoiceBorder"
    )

    Column(
        modifier = Modifier
            .width(144.dp)
            .settingsSurface(
                contentColor = contentColor,
                cornerRadius = 18.dp
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.26f),
                            accentColor.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .offset(x = if (motionStyle.key == "still") 0.dp else (index * 3).dp)
                            .size(if (index == 1) 12.dp else 9.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 1) accentColor else accentColor.copy(alpha = 0.55f)
                            )
                    )
                }
            }
        }

        BasicText(
            motionStyle.name,
            style = TextStyle(
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        )

        BasicText(
            if (reduceAnimations && motionStyle.key != "still") {
                "Saved, but currently paused by Reduce Animations."
            } else {
                motionStyle.description
            },
            style = TextStyle(
                color = contentColor.copy(alpha = 0.62f),
                fontSize = 12.sp
            )
        )
    }
}

// ==================== HELP & FAQ SCREEN ====================

@Composable
fun HelpScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var expandedFaqId by rememberSaveable { mutableStateOf<String?>(null) }
    val faqs = remember {
        listOf(
            HelpFaqItemData(
                id = "post",
                icon = Icons.Outlined.Edit,
                title = "How to create a post",
                answer = "Use the center create tab to publish text, images, videos, links, polls, or articles. After posting, go back to Home and your content should appear without reloading the entire app."
            ),
            HelpFaqItemData(
                id = "connect",
                icon = Icons.Outlined.PersonAddAlt,
                title = "How to connect with others",
                answer = "Open Find, choose Smart, For You, Campus, or Nearby, then tap Connect on people who look relevant. The app now ranks active and more complete profiles higher to make this section feel more useful."
            ),
            HelpFaqItemData(
                id = "notifications",
                icon = Icons.Outlined.NotificationsNone,
                title = "Why notifications may not arrive",
                answer = "Check Android system notifications first, then make sure push notifications are enabled inside Vormex. If the app was force-stopped from Android Settings, Firebase will stay blocked until the app is opened again."
            ),
            HelpFaqItemData(
                id = "goals",
                icon = Icons.Outlined.TrackChanges,
                title = "Understanding weekly goals",
                answer = "Weekly goals track activity like connections and posting. Progress, streaks, and retention cards now stay cached for a few minutes, so reopening these sections should feel much faster."
            ),
            HelpFaqItemData(
                id = "password",
                icon = Icons.Outlined.Lock,
                title = "How to change password",
                answer = "If you signed up with email and password, use the account flow on web or contact support from this screen and ask for a password reset. Include the email tied to your Vormex account."
            ),
            HelpFaqItemData(
                id = "account",
                icon = Icons.Outlined.DeleteOutline,
                title = "How to delete your account",
                answer = "Contact support and request account deletion from the email linked to your account. Include your username and a short confirmation that you want all account data removed."
            )
        )
    }
    val filteredFaqs = remember(query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            faqs
        } else {
            faqs.filter {
                it.title.lowercase().contains(normalizedQuery) ||
                    it.answer.lowercase().contains(normalizedQuery)
            }
        }
    }
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Help & FAQ",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingsSurface(
                            contentColor = contentColor,
                            cornerRadius = 20.dp,
                            containerColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.10f else 0.12f),
                            outlineColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.18f else 0.12f)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicText(
                            "Help that actually unblocks people",
                            style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
                        )
                        BasicText(
                            "Search common questions, copy diagnostics, or jump straight to support without leaving the app feeling stuck.",
                            style = TextStyle(contentColor.copy(alpha = 0.68f), 12.sp)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("Search help topics")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = contentColor.copy(alpha = 0.2f),
                        focusedTextColor = contentColor,
                        unfocusedTextColor = contentColor,
                        cursorColor = accentColor,
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedPlaceholderColor = contentColor.copy(alpha = 0.45f),
                        unfocusedPlaceholderColor = contentColor.copy(alpha = 0.45f)
                    )
                )
            }

            item {
                SettingsSectionHeader("Quick Fixes", contentColor)
            }

            item {
                SettingsActionItem(
                    title = "Notification troubleshooting",
                    subtitle = "Open Android notification settings for Vormex",
                    icon = Icons.Outlined.NotificationsActive,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { openAppNotificationSettings(context) }
                )
            }

            item {
                SettingsActionItem(
                    title = "Copy app diagnostics",
                    subtitle = "Version, device, Android, and backend endpoint",
                    icon = Icons.Outlined.Science,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        copyText(
                            context = context,
                            label = "Vormex diagnostics",
                            text = buildDiagnosticsText()
                        )
                    }
                )
            }

            item {
                SettingsActionItem(
                    title = "Contact support",
                    subtitle = "Open email with support details filled in",
                    icon = Icons.Outlined.Email,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "support@vormex.in",
                            subject = "Help Request - Vormex Android",
                            body = "Hi Vormex Support,\n\nI need help with:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
            }

            item {
                SettingsSectionHeader("FAQ", contentColor)
            }

            if (filteredFaqs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .settingsSurface(
                                contentColor = contentColor,
                                cornerRadius = 16.dp
                            )
                            .padding(16.dp)
                    ) {
                        BasicText(
                            "No help topics matched \"$query\". Try a shorter phrase like notifications, post, password, or account.",
                            style = TextStyle(contentColor.copy(alpha = 0.68f), 13.sp)
                        )
                    }
                }
            } else {
                items(filteredFaqs, key = { it.id }) { faq ->
                    HelpFaqCard(
                        faq = faq,
                        expanded = expandedFaqId == faq.id,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = {
                            expandedFaqId = if (expandedFaqId == faq.id) null else faq.id
                        }
                    )
                }
            }

            item {
                SettingsSectionHeader("Need More Help?", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "Visit Help Center",
                    subtitle = "Full documentation and guides",
                    icon = Icons.Outlined.Language,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://vormex.in/help")
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Contact Support",
                    subtitle = "Get help from our team",
                    icon = Icons.Outlined.SupportAgent,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "support@vormex.in",
                            subject = "Help Request - Vormex App",
                            body = buildDiagnosticsText()
                        )
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ==================== ABOUT SCREEN ====================

@Composable
fun AboutScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showOpenSourceDialog by rememberSaveable { mutableStateOf(false) }
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "About",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App info card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsSurface(
                        contentColor = contentColor,
                        cornerRadius = 24.dp,
                        containerColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.10f else 0.08f),
                        outlineColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.18f else 0.10f)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // App icon
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "V",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    BasicText(
                        "Vormex",
                        style = TextStyle(
                            color = contentColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    BasicText(
                        "Version ${getAppVersion()}",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    )
                    
                    BasicText(
                        "Build for dreamers, creators, and networkers",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            SettingsSectionHeader("Legal", contentColor)

            SettingsActionItem(
                title = "Copy Build Details",
                subtitle = "Version, package, API endpoint, and device context",
                icon = Icons.Outlined.Science,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    copyText(
                        context = context,
                        label = "Vormex build details",
                        text = buildDiagnosticsText()
                    )
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsSurface(
                        contentColor = contentColor,
                        cornerRadius = 16.dp
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BuildInfoRow("Package", BuildConfig.APPLICATION_ID, contentColor)
                    BuildInfoRow("Version", getAppVersion(), contentColor)
                    BuildInfoRow("Backend", BuildConfig.API_BASE_URL.removePrefix("https://"), contentColor)
                    BuildInfoRow("Android", "SDK ${Build.VERSION.SDK_INT}", contentColor)
                }
            }
            
            SettingsNavigationItem(
                title = "Terms of Service",
                icon = Icons.Outlined.Gavel,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://vormex.in/terms")
                }
            )
            
            SettingsNavigationItem(
                title = "Privacy Policy",
                icon = Icons.Outlined.Policy,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://vormex.in/privacy")
                }
            )
            
            SettingsNavigationItem(
                title = "Open Source Licenses",
                icon = Icons.Outlined.Description,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = { showOpenSourceDialog = true }
            )
            
            SettingsSectionHeader("Connect", contentColor)
            
            SettingsNavigationItem(
                title = "Follow us on Twitter",
                subtitle = "@VormexApp",
                icon = Icons.Outlined.AlternateEmail,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://twitter.com/VormexApp")
                }
            )
            
            SettingsNavigationItem(
                title = "Visit our website",
                subtitle = "vormex.in",
                icon = Icons.Outlined.Language,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    openUrl(context, "https://vormex.in")
                }
            )
            
            // Copyright
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "© 2026 Vormex. All rights reserved.",
                    style = TextStyle(
                        color = contentColor.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                )
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showOpenSourceDialog) {
        AlertDialog(
            onDismissRequest = { showOpenSourceDialog = false },
            title = {
                Text(
                    "Open Source Stack",
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "Jetpack Compose UI",
                        "Material 3",
                        "AndroidX DataStore",
                        "Ktor Networking",
                        "Coil Image Loading",
                        "Firebase Messaging",
                        "Media3 ExoPlayer"
                    ).forEach { library ->
                        Text(
                            "• $library",
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpenSourceDialog = false }) {
                    Text("Close", color = accentColor)
                }
            }
        )
    }
}

// ==================== INVITE FRIENDS SCREEN ====================

@Composable
fun InviteFriendsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    referralCode: String = "VORMEX2026"
) {
    val context = LocalContext.current
    var resolvedReferralCode by remember { mutableStateOf(referralCode) }
    var resolvedInviteLink by remember { mutableStateOf("https://vormex.in/login?mode=signup&ref=$referralCode") }
    var isLoadingReferral by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        GrowthApiService.getReferralShareLinks(context)
            .onSuccess { shareLinks ->
                resolvedReferralCode = shareLinks.code.ifBlank { referralCode }
                resolvedInviteLink = shareLinks.link.ifBlank {
                    "https://vormex.in/login?mode=signup&ref=$resolvedReferralCode"
                }
                isLoadingReferral = false
            }
            .onFailure {
                GrowthApiService.getReferralCode(context)
                    .onSuccess { code ->
                        resolvedReferralCode = code
                        resolvedInviteLink = "https://vormex.in/login?mode=signup&ref=$code"
                    }
                isLoadingReferral = false
            }
    }

    val inviteMessage = remember(resolvedReferralCode, resolvedInviteLink) {
        buildInviteMessage(resolvedReferralCode, resolvedInviteLink)
    }

    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Invite Friends",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            
            // Illustration
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GroupAdd,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(56.dp)
                )
            }
            
            BasicText(
                "Invite friends and grow together!",
                style = TextStyle(
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            
            BasicText(
                "Share Vormex with friends. Help them discover new connections and opportunities.",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            // Referral code card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsSurface(
                        contentColor = contentColor,
                        cornerRadius = 16.dp,
                        containerColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.10f else 0.12f),
                        outlineColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.18f else 0.12f)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText(
                        "Your referral code",
                        style = TextStyle(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .border(
                                width = 2.dp,
                                color = accentColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            resolvedReferralCode,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            SettingsActionItem(
                title = "Copy invite link",
                subtitle = if (isLoadingReferral) "Loading your share link..." else resolvedInviteLink,
                icon = Icons.Outlined.Link,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    copyText(context, "Invite link", resolvedInviteLink)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InviteUtilityCard(
                    modifier = Modifier.weight(1f),
                    title = "Copy Code",
                    subtitle = resolvedReferralCode,
                    icon = Icons.Outlined.ContentCopy,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        copyText(context, "Referral code", resolvedReferralCode)
                    }
                )
                InviteUtilityCard(
                    modifier = Modifier.weight(1f),
                    title = "Copy Message",
                    subtitle = "Ready to paste",
                    icon = Icons.Outlined.Drafts,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        copyText(context, "Invite message", inviteMessage)
                    }
                )
            }
            
            // Share buttons
            SettingsSectionHeader("Share via", contentColor)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareButton(
                    icon = Icons.Outlined.Sms,
                    label = "Message",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        shareInvite(context, "sms", inviteMessage)
                    }
                )
                
                ShareButton(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        shareInvite(context, "email", inviteMessage)
                    }
                )
                
                ShareButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        shareInvite(context, "share", inviteMessage)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsSurface(
                        contentColor = contentColor,
                        cornerRadius = 16.dp
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText(
                        "What friends receive",
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    BasicText(
                        "A direct signup link, your referral code, and a short note about networking, creators, and opportunities on Vormex.",
                        style = TextStyle(contentColor.copy(alpha = 0.68f), 13.sp)
                    )
                }
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun InviteUtilityCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsIconBadge(icon = icon, accentColor = accentColor, modifier = Modifier.size(38.dp))
            BasicText(
                title,
                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
            )
            BasicText(
                subtitle,
                style = TextStyle(contentColor.copy(alpha = 0.62f), 12.sp)
            )
        }
    }
}

@Composable
private fun ShareButton(
    icon: ImageVector,
    label: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        BasicText(
            label,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp
            )
        )
    }
}

private fun shareInvite(context: Context, method: String, shareText: String) {
    when (method) {
        "share" -> {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            launchIntentSafely(context, Intent.createChooser(intent, "Share via"))
        }
        "email" -> {
            launchEmail(
                context = context,
                to = "",
                subject = "Join me on Vormex!",
                body = shareText
            )
        }
        "sms" -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", shareText)
            }
            launchIntentSafely(context, intent)
        }
    }
}

// ==================== CONTACT US SCREEN ====================

@Composable
fun ContactScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Contact Us",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .settingsSurface(
                            contentColor = contentColor,
                            cornerRadius = 20.dp,
                            containerColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.10f else 0.12f),
                            outlineColor = accentColor.copy(alpha = if (contentColor == Color.White) 0.18f else 0.12f)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicText(
                            "Support toolkit",
                            style = TextStyle(contentColor, 16.sp, FontWeight.Bold)
                        )
                        BasicText(
                            "Send a bug report, suggest a feature, or copy diagnostics before you write in.",
                            style = TextStyle(contentColor.copy(alpha = 0.68f), 12.sp)
                        )
                    }
                }
            }

            item {
                SettingsSectionHeader("Quick Actions", contentColor)
            }

            item {
                SettingsActionItem(
                    title = "Copy app diagnostics",
                    subtitle = "Version, package, Android, device, and backend",
                    icon = Icons.Outlined.Science,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        copyText(context, "Vormex diagnostics", buildDiagnosticsText())
                    }
                )
            }

            item {
                SettingsActionItem(
                    title = "Open help center",
                    subtitle = "Read onboarding and troubleshooting guides",
                    icon = Icons.Outlined.Language,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://vormex.in/help")
                    }
                )
            }

            item {
                SettingsSectionHeader("Get in Touch", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "Email Support",
                    subtitle = "support@vormex.in",
                    icon = Icons.Outlined.Email,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "support@vormex.in",
                            subject = "Support Request - Vormex Android",
                            body = "Hi Vormex Support,\n\nI need help with:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Report a Bug",
                    subtitle = "Help us improve",
                    icon = Icons.Outlined.BugReport,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "bugs@vormex.in",
                            subject = "Bug Report - Vormex App v${getAppVersion()}",
                            body = "Describe the issue:\n\nExpected:\n\nActual:\n\nSteps to reproduce:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Feature Request",
                    subtitle = "Suggest new features",
                    icon = Icons.Outlined.TipsAndUpdates,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        launchEmail(
                            context = context,
                            to = "feedback@vormex.in",
                            subject = "Feature Request - Vormex App",
                            body = "I would love to see:\n\nWhy this helps:\n\n${buildDiagnosticsText()}"
                        )
                    }
                )
            }
            
            item {
                SettingsSectionHeader("Social Media", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "Twitter / X",
                    subtitle = "@VormexApp",
                    icon = Icons.Outlined.AlternateEmail,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://twitter.com/VormexApp")
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Instagram",
                    subtitle = "@vormex.app",
                    icon = Icons.Outlined.CameraAlt,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://instagram.com/vormex.app")
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "LinkedIn",
                    subtitle = "Vormex",
                    icon = Icons.Outlined.BusinessCenter,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        openUrl(context, "https://linkedin.com/company/vormex")
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HelpFaqCard(
    faq: HelpFaqItemData,
    expanded: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconBadge(icon = faq.icon, accentColor = accentColor)
                    Spacer(Modifier.width(12.dp))
                    BasicText(
                        faq.title,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.Remove else Icons.Outlined.Add,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (expanded) {
                BasicText(
                    faq.answer,
                    style = TextStyle(contentColor.copy(alpha = 0.72f), 13.sp)
                )
            }
        }
    }
}

@Composable
private fun BuildInfoRow(
    label: String,
    value: String,
    contentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            style = TextStyle(contentColor.copy(alpha = 0.55f), 12.sp, FontWeight.Medium)
        )
        BasicText(
            value,
            modifier = Modifier.padding(start = 12.dp),
            style = TextStyle(
                contentColor,
                12.sp,
                FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        )
    }
}

@Composable
private fun SavedMetric(
    icon: ImageVector,
    value: Int,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.52f),
            modifier = Modifier.size(14.dp)
        )
        BasicText(
            value.toString(),
            style = TextStyle(contentColor.copy(alpha = 0.52f), 12.sp)
        )
    }
}

// ==================== SAVED POSTS SCREEN ====================

@Composable
fun SavedPostsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onNavigateToPost: (String) -> Unit = {},
    onNavigateToReel: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: com.kyant.backdrop.catalog.linkedin.posts.PostsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.kyant.backdrop.catalog.linkedin.posts.PostsViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    var savedReels by remember { mutableStateOf<List<com.kyant.backdrop.catalog.network.models.Reel>>(emptyList()) }
    var isLoadingSavedReels by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts(refresh = true)

        val userId = ApiClient.getCurrentUserId(context)
        if (!userId.isNullOrBlank()) {
            isLoadingSavedReels = true
            ApiClient.getUserSavedReels(context, userId)
                .onSuccess { response ->
                    savedReels = response.reels
                    isLoadingSavedReels = false
                }
                .onFailure {
                    isLoadingSavedReels = false
                }
        }
    }
    
    SettingsScreenContainer(backdrop = backdrop, contentColor = contentColor, accentColor = accentColor) {
        SettingsHeader(
            title = "Saved Posts",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        if (uiState.isLoading && uiState.savedPosts.isEmpty() && isLoadingSavedReels) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.savedPosts.isEmpty() && savedReels.isEmpty()) {
            com.kyant.backdrop.catalog.linkedin.posts.SavedPostsEmptyState(
                contentColor = contentColor
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (savedReels.isNotEmpty()) {
                    item {
                        BasicText(
                            "Saved Reels",
                            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    items(savedReels.size) { index ->
                        val reel = savedReels[index]
                        SavedReelItem(
                            reel = reel,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            onClick = { onNavigateToReel(reel.id) },
                            onUnsave = {
                                    scope.launch {
                                        ApiClient.toggleReelSave(context, reel.id)
                                            .onSuccess {
                                                savedReels = savedReels.filterNot { it.id == reel.id }
                                            }
                                    }
                            }
                        )
                    }
                }

                if (uiState.savedPosts.isNotEmpty()) {
                    item {
                        BasicText(
                            "Saved Posts",
                            style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                items(uiState.savedPosts.size) { index ->
                    val post = uiState.savedPosts[index]
                    SavedPostItem(
                        post = post,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        onClick = { onNavigateToPost(post.id) },
                        onUnsave = { viewModel.toggleSave(post.id) }
                    )
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SavedReelItem(
    reel: com.kyant.backdrop.catalog.network.models.Reel,
    backdrop: LayerBackdrop,
    contentColor: Color,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    reel.author.name ?: "Unknown",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                )

                BasicText(
                    (reel.caption ?: "").take(100) + if ((reel.caption?.length ?: 0) > 100) "..." else "",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SavedMetric(icon = Icons.Outlined.FavoriteBorder, value = reel.likesCount, contentColor = contentColor)
                    SavedMetric(icon = Icons.Outlined.ChatBubbleOutline, value = reel.commentsCount, contentColor = contentColor)
                    SavedMetric(icon = Icons.Outlined.PlayCircleOutline, value = reel.viewsCount, contentColor = contentColor)
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUnsave)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "Unsave",
                    tint = contentColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedPostItem(
    post: com.kyant.backdrop.catalog.network.models.FullPost,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onUnsave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    post.author.name ?: "Unknown",
                    style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                )
                
                BasicText(
                    (post.content ?: "").take(100) + if ((post.content?.length ?: 0) > 100) "..." else "",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SavedMetric(icon = Icons.Outlined.FavoriteBorder, value = post.likesCount, contentColor = contentColor)
                    SavedMetric(icon = Icons.Outlined.ChatBubbleOutline, value = post.commentsCount, contentColor = contentColor)
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUnsave)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmark,
                    contentDescription = "Unsave",
                    tint = contentColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== LOGOUT DIALOG ====================

@Composable
fun LogoutConfirmationDialog(
    contentColor: Color,
    accentColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogSurfaceColor = MaterialTheme.colorScheme.surface
    val dialogTextColor = MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Log Out",
                color = dialogTextColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to log out? You'll need to sign in again to access your account.",
                color = dialogTextColor.copy(alpha = 0.74f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Log Out")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
            ) {
                Text("Cancel")
            }
        },
        containerColor = dialogSurfaceColor
    )
}

// ==================== REUSABLE SETTINGS COMPONENTS ====================

@Composable
fun SettingsHeader(
    title: String,
    contentColor: Color,
    onBack: () -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkSurface) Color.White.copy(alpha = 0.12f)
                    else Color.Black.copy(alpha = 0.05f)
                )
                .clickable(onClick = onBack)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        BasicText(
            title,
            style = TextStyle(contentColor, 20.sp, FontWeight.Bold)
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    contentColor: Color
) {
    BasicText(
        title,
        style = TextStyle(
            color = contentColor.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val alpha = if (enabled) 1f else 0.5f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor.copy(alpha = alpha), 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(contentColor.copy(alpha = 0.5f * alpha), 12.sp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.alpha(alpha)) {
                LiquidToggle(
                    selected = { checked },
                    onSelect = { if (enabled) onCheckedChange(it) },
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    trailingText: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                        )
                    }
                }
            }
            
            if (trailingText.isNotEmpty()) {
                BasicText(
                    trailingText,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    subtitle: String = "",
    trailing: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    if (subtitle.isNotEmpty()) {
                        BasicText(
                            subtitle,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailing.isNotEmpty()) {
                    BasicText(
                        trailing,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.34f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    val isDarkSurface = contentColor == Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .settingsSurface(contentColor = contentColor, cornerRadius = 18.dp)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBadge(icon = icon, accentColor = accentColor)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 16.sp, FontWeight.Medium)
                    )
                    BasicText(
                        subtitle,
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (value, label) ->
                    val isSelected = value == selectedOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.18f)
                                else if (isDarkSurface) Color.White.copy(alpha = 0.10f)
                                else Color.White.copy(alpha = 0.20f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) accentColor.copy(alpha = 0.36f)
                                else if (isDarkSurface) Color.White.copy(alpha = 0.14f)
                                else Color.White.copy(alpha = 0.34f),
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { onOptionSelected(value) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.78f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${context.packageName}")
        }
    }
    launchIntentSafely(context, intent)
}

private fun openUrl(context: Context, url: String) {
    launchIntentSafely(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun launchEmail(
    context: Context,
    to: String,
    subject: String,
    body: String
) {
    val mailTarget = if (to.isBlank()) "mailto:" else "mailto:$to"
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse(mailTarget)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    launchIntentSafely(context, intent, failureMessage = "No email app available on this device.")
}

private fun launchIntentSafely(
    context: Context,
    intent: Intent,
    failureMessage: String = "No app available to handle this action."
) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun copyText(
    context: Context,
    label: String,
    text: String
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private fun buildInviteMessage(
    referralCode: String,
    inviteLink: String = "https://vormex.in/login?mode=signup&ref=$referralCode"
): String {
    return "Join me on Vormex! Connect with professionals, share your journey, and discover opportunities. Use my code $referralCode and join here: $inviteLink"
}

private fun buildDiagnosticsText(): String {
    return buildString {
        appendLine("App Version: ${getAppVersion()}")
        appendLine("Package: ${BuildConfig.APPLICATION_ID}")
        appendLine("Backend: ${BuildConfig.API_BASE_URL}")
        appendLine("Device: ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    }.trim()
}

private fun getAppVersion(): String {
    return try {
        BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        "1.0.0"
    }
}
