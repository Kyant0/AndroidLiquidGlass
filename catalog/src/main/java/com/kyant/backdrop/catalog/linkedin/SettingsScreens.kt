package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.launch

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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        SettingsHeader(
            title = "Notifications",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
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
                    icon = "📱",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    trailingText = if (systemNotificationsEnabled) "✅" else "⚠️",
                    onClick = {
                        // Open system notification settings
                        val intent = Intent().apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.parse("package:${context.packageName}")
                            }
                        }
                        context.startActivity(intent)
                    }
                )
            }
            
            // Master toggle
            item {
                SettingsSectionHeader("General", contentColor)
            }
            
            item {
                SettingsSwitchItem(
                    title = "Push Notifications",
                    subtitle = "Receive push notifications",
                    icon = "🔔",
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
                SettingsSwitchItem(
                    title = "Messages",
                    subtitle = "New messages and chat requests",
                    icon = "💬",
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
                SettingsSwitchItem(
                    title = "Connections",
                    subtitle = "Connection requests and acceptances",
                    icon = "👥",
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
                SettingsSwitchItem(
                    title = "Likes",
                    subtitle = "When someone likes your posts",
                    icon = "❤️",
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
                SettingsSwitchItem(
                    title = "Comments",
                    subtitle = "When someone comments on your posts",
                    icon = "💭",
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
                SettingsSwitchItem(
                    title = "Match Alerts",
                    subtitle = "Daily matches and recommendations",
                    icon = "🎯",
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
                SettingsSwitchItem(
                    title = "Streak Reminders",
                    subtitle = "Don't lose your streak!",
                    icon = "🔥",
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
                SettingsSwitchItem(
                    title = "Daily Digest",
                    subtitle = "Daily summary at $dailyDigestTime",
                    icon = "📰",
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
                SettingsSwitchItem(
                    title = "Weekly Summary",
                    subtitle = "Your week in review",
                    icon = "📊",
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        SettingsHeader(
            title = "Privacy",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
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
                    icon = "👁️",
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
                    icon = "💬",
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
                    icon = "🟢",
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
                    icon = "⏰",
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
                    icon = "👀",
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
                    icon = "📧",
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
                    icon = "📱",
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
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = "glass")
    val reduceAnimations by SettingsPreferences.reduceAnimations(context).collectAsState(initial = false)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
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
                        backdrop = backdrop,
                        accentColor = accentColor,
                        onClick = {
                            coroutineScope.launch {
                                SettingsPreferences.setThemeMode(context, "glass")
                                onThemeChange("glass")
                            }
                        }
                    )
                    
                    // Bright Theme Card
                    ThemePreviewCard(
                        modifier = Modifier.weight(1f),
                        themeName = "Bright",
                        themeKey = "light",
                        isSelected = themeMode == "light",
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
            
            // Spacer
            item { Spacer(Modifier.height(8.dp)) }
            
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
                    icon = "✨",
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.15f))
                }
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
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
            BasicText(
                when (themeKey) {
                    "glass" -> "✨"
                    "light" -> "☀️"
                    "dark" -> "🌙"
                    else -> "✨"
                },
                style = TextStyle(fontSize = 16.sp)
            )
            Spacer(Modifier.width(4.dp))
            BasicText(
                themeName,
                style = TextStyle(
                    color = if (isSelected) accentColor else Color.Black,
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
                BasicText(
                    "✓",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            Spacer(Modifier.height(20.dp))
        }
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        SettingsHeader(
            title = "Help & FAQ",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSectionHeader("Getting Started", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "How to create a post",
                    icon = "📝",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { /* Expand FAQ item */ }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "How to connect with others",
                    icon = "🤝",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { /* Expand FAQ item */ }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Understanding weekly goals",
                    icon = "🎯",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { /* Expand FAQ item */ }
                )
            }
            
            item {
                SettingsSectionHeader("Account", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "How to change password",
                    icon = "🔒",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { /* Expand FAQ item */ }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "How to delete account",
                    icon = "🗑️",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = { /* Expand FAQ item */ }
                )
            }
            
            item {
                SettingsSectionHeader("Need More Help?", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "Visit Help Center",
                    subtitle = "Full documentation and guides",
                    icon = "🌐",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vormex.in/help"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Contact Support",
                    subtitle = "Get help from our team",
                    icon = "📧",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@vormex.in")
                            putExtra(Intent.EXTRA_SUBJECT, "Help Request - Vormex App")
                        }
                        context.startActivity(intent)
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
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
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(24.dp) },
                        effects = {
                            vibrancy()
                            blur(10f.dp.toPx())
                            lens(4f.dp.toPx(), 8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(accentColor.copy(alpha = 0.1f))
                        }
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
            
            SettingsNavigationItem(
                title = "Terms of Service",
                icon = "📜",
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vormex.in/terms"))
                    context.startActivity(intent)
                }
            )
            
            SettingsNavigationItem(
                title = "Privacy Policy",
                icon = "🔐",
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vormex.in/privacy"))
                    context.startActivity(intent)
                }
            )
            
            SettingsNavigationItem(
                title = "Open Source Licenses",
                icon = "📄",
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = { /* Show licenses */ }
            )
            
            SettingsSectionHeader("Connect", contentColor)
            
            SettingsNavigationItem(
                title = "Follow us on Twitter",
                subtitle = "@VormexApp",
                icon = "🐦",
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/VormexApp"))
                    context.startActivity(intent)
                }
            )
            
            SettingsNavigationItem(
                title = "Visit our website",
                subtitle = "vormex.in",
                icon = "🌐",
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vormex.in"))
                    context.startActivity(intent)
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
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
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    "🎁",
                    style = TextStyle(fontSize = 64.sp)
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
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(16.dp) },
                        effects = {
                            vibrancy()
                            blur(10f.dp.toPx())
                            lens(4f.dp.toPx(), 8f.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(accentColor.copy(alpha = 0.15f))
                        }
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
                            referralCode,
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            // Share buttons
            SettingsSectionHeader("Share via", contentColor)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareButton(
                    icon = "💬",
                    label = "Message",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        shareInvite(context, "sms")
                    }
                )
                
                ShareButton(
                    icon = "📧",
                    label = "Email",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        shareInvite(context, "email")
                    }
                )
                
                ShareButton(
                    icon = "📤",
                    label = "Share",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        shareInvite(context, "share")
                    }
                )
            }
            
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ShareButton(
    icon: String,
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
            BasicText(icon, style = TextStyle(fontSize = 24.sp))
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

private fun shareInvite(context: Context, method: String) {
    val shareText = "Join me on Vormex! Connect with professionals, share your journey, and discover opportunities. Download now: https://vormex.in/download"
    
    when (method) {
        "share" -> {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }
        "email" -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "Join me on Vormex!")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(intent)
        }
        "sms" -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", shareText)
            }
            context.startActivity(intent)
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        SettingsHeader(
            title = "Contact Us",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSectionHeader("Get in Touch", contentColor)
            }
            
            item {
                SettingsNavigationItem(
                    title = "Email Support",
                    subtitle = "support@vormex.in",
                    icon = "📧",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@vormex.in")
                            putExtra(Intent.EXTRA_SUBJECT, "Support Request - Vormex App")
                        }
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Report a Bug",
                    subtitle = "Help us improve",
                    icon = "🐛",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:bugs@vormex.in")
                            putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Vormex App v${getAppVersion()}")
                            putExtra(Intent.EXTRA_TEXT, "Device: ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE}\n\nDescribe the issue:\n")
                        }
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Feature Request",
                    subtitle = "Suggest new features",
                    icon = "💡",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:feedback@vormex.in")
                            putExtra(Intent.EXTRA_SUBJECT, "Feature Request - Vormex App")
                        }
                        context.startActivity(intent)
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
                    icon = "🐦",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/VormexApp"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "Instagram",
                    subtitle = "@vormex.app",
                    icon = "📸",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/vormex.app"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                SettingsNavigationItem(
                    title = "LinkedIn",
                    subtitle = "Vormex",
                    icon = "💼",
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com/company/vormex"))
                        context.startActivity(intent)
                    }
                )
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ==================== SAVED POSTS SCREEN ====================

@Composable
fun SavedPostsScreen(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onNavigateBack: () -> Unit,
    onNavigateToPost: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: com.kyant.backdrop.catalog.linkedin.posts.PostsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.kyant.backdrop.catalog.linkedin.posts.PostsViewModel.Factory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts(refresh = true)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        SettingsHeader(
            title = "Saved Posts",
            contentColor = contentColor,
            onBack = onNavigateBack
        )
        
        if (uiState.isLoading && uiState.savedPosts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (uiState.savedPosts.isEmpty()) {
            com.kyant.backdrop.catalog.linkedin.posts.SavedPostsEmptyState(
                contentColor = contentColor
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
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
                    BasicText(
                        "❤️ ${post.likesCount}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                    BasicText(
                        "💬 ${post.commentsCount}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onUnsave)
                    .padding(8.dp)
            ) {
                BasicText(
                    "🔖",
                    style = TextStyle(fontSize = 20.sp)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Log Out",
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to log out? You'll need to sign in again to access your account.",
                color = contentColor.copy(alpha = 0.7f)
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
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// ==================== REUSABLE SETTINGS COMPONENTS ====================

@Composable
fun SettingsHeader(
    title: String,
    contentColor: Color,
    onBack: () -> Unit
) {
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
                .clickable(onClick = onBack)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicText(
                "←",
                style = TextStyle(contentColor, 24.sp, FontWeight.Bold)
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
    icon: String,
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
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
                BasicText(icon, style = TextStyle(fontSize = 24.sp))
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
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = contentColor.copy(alpha = 0.5f),
                    uncheckedTrackColor = contentColor.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    trailingText: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
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
                BasicText(icon, style = TextStyle(fontSize = 24.sp))
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
                    style = TextStyle(fontSize = 20.sp)
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationItem(
    title: String,
    icon: String,
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
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
                BasicText(icon, style = TextStyle(fontSize = 24.sp))
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
                BasicText(
                    "→",
                    style = TextStyle(contentColor.copy(alpha = 0.3f), 20.sp)
                )
            }
        }
    }
}

@Composable
fun SettingsOptionItem(
    title: String,
    subtitle: String,
    icon: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(4f.dp.toPx(), 8f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .clickable { expanded = true }
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
                BasicText(icon, style = TextStyle(fontSize = 24.sp))
                Spacer(Modifier.width(16.dp))
                Column {
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
            
            BasicText(
                "▼",
                style = TextStyle(contentColor.copy(alpha = 0.3f), 12.sp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (value == selectedOption) {
                                BasicText("✓ ", style = TextStyle(accentColor, 14.sp))
                            }
                            Text(label)
                        }
                    },
                    onClick = {
                        onOptionSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun getAppVersion(): String {
    return try {
        BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        "1.0.0"
    }
}
