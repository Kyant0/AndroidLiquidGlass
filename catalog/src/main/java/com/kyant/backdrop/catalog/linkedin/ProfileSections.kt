package com.kyant.backdrop.catalog.linkedin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.R
import com.kyant.backdrop.catalog.network.models.*
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ==================== Section Card Wrapper ====================

@Composable
private fun SectionCard(
    title: String,
    count: Int? = null,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean = false,
    onAdd: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(20f.dp) },
                effects = {
                    vibrancy()
                    blur(12f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.08f))
                }
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                    )
                    count?.let {
                        Spacer(Modifier.width(6.dp))
                        BasicText(
                            "($it)",
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 14.sp)
                        )
                    }
                }
                
                if (isOwner && onAdd != null) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onAdd)
                            .padding(8.dp)
                    ) {
                        BasicText("+", style = TextStyle(accentColor, 16.sp, FontWeight.Bold))
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            content()
        }
    }
}

// ==================== About Section ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSection(
    user: ProfileUser,
    stats: ProfileStats,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isEditingBio: Boolean,
    editedBio: String,
    onEditBio: () -> Unit,
    onSaveBio: () -> Unit,
    onCancelEditBio: () -> Unit,
    onBioChange: (String) -> Unit,
    onToggleOpenToWork: (Boolean) -> Unit
) {
    var showFullBio by remember { mutableStateOf(false) }
    
    SectionCard(
        title = "About",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = if (user.bio.isNullOrEmpty() && isOwner) onEditBio else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Bio
            if (isEditingBio) {
                Column {
                    BasicTextField(
                        value = editedBio,
                        onValueChange = onBioChange,
                        textStyle = TextStyle(contentColor, 14.sp),
                        cursorBrush = SolidColor(accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.05f))
                            .padding(12.dp)
                            .height(100.dp)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor)
                                .clickable(onClick = onSaveBio)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText("Save", style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold))
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable(onClick = onCancelEditBio)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText("Cancel", style = TextStyle(contentColor, 14.sp))
                        }
                    }
                }
            } else if (!user.bio.isNullOrEmpty()) {
                Column {
                    val displayBio = if (showFullBio || user.bio.length <= 200) {
                        user.bio
                    } else {
                        user.bio.take(200) + "..."
                    }
                    
                    BasicText(
                        displayBio,
                        style = TextStyle(contentColor.copy(alpha = 0.9f), 14.sp)
                    )
                    
                    if (user.bio.length > 200) {
                        BasicText(
                            if (showFullBio) "Show less" else "Show more",
                            style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                            modifier = Modifier.clickable { showFullBio = !showFullBio }
                        )
                    }
                    
                    if (isOwner) {
                        BasicText(
                            "Edit",
                            style = TextStyle(accentColor, 13.sp, FontWeight.Medium),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable(onClick = onEditBio)
                        )
                    }
                }
            }
            
            // Interests
            if (user.interests.isNotEmpty()) {
                Column {
                    BasicText(
                        "Interests",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        user.interests.forEach { interest ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText(
                                        interest,
                                        style = TextStyle(accentColor, 12.sp)
                                    )
                                    if (isOwner) {
                                        Spacer(Modifier.width(4.dp))
                                        BasicText(
                                            "×",
                                            style = TextStyle(accentColor.copy(alpha = 0.6f), 14.sp),
                                            modifier = Modifier.clickable { /* Remove interest */ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Quick stats grid
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStatBox("", "${stats.totalActiveDays}", "Active Days", contentColor, accentColor, R.drawable.ic_calendar)
                QuickStatBox("", "${stats.totalPosts}", "Posts", contentColor, accentColor, R.drawable.ic_post)
                QuickStatBox("", "${stats.totalArticles}", "Articles", contentColor, accentColor, R.drawable.ic_article)
                QuickStatBox("", "${stats.totalLikesReceived}", "Likes", contentColor, accentColor, R.drawable.ic_favorite)
            }
            
            // Education summary
            if (!user.college.isNullOrEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_education),
                            contentDescription = "Education",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(contentColor)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            BasicText(
                                user.college ?: "",
                                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                            )
                            val eduDetails = listOfNotNull(
                                user.degree,
                                user.branch?.takeIf { it.isNotEmpty() },
                                user.currentYear?.let { "Year $it" },
                                user.graduationYear?.let { "Class of $it" }
                            ).joinToString(" • ")
                            if (eduDetails.isNotEmpty()) {
                                BasicText(
                                    eduDetails,
                                    style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Open to opportunities toggle (owner only)
            if (isOwner) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (user.isOpenToOpportunities) Color(0xFF22C55E).copy(alpha = 0.15f)
                            else contentColor.copy(alpha = 0.05f)
                        )
                        .clickable { onToggleOpenToWork(!user.isOpenToOpportunities) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        BasicText(
                            "#OpenToWork",
                            style = TextStyle(
                                if (user.isOpenToOpportunities) Color(0xFF22C55E) else contentColor,
                                14.sp,
                                FontWeight.Medium
                            )
                        )
                        BasicText(
                            "Let recruiters know you're open to opportunities",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp)
                        )
                    }
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (user.isOpenToOpportunities) Color(0xFF22C55E)
                                else contentColor.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.isOpenToOpportunities) {
                            BasicText("✓", style = TextStyle(Color.White, 14.sp, FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatBox(
    icon: String,
    value: String,
    label: String,
    contentColor: Color,
    accentColor: Color,
    iconRes: Int? = null  // Optional drawable resource
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
        } else {
            BasicText(icon, style = TextStyle(fontSize = 16.sp))
        }
        BasicText(value, style = TextStyle(contentColor, 16.sp, FontWeight.Bold))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.6f), 10.sp))
    }
}

// ==================== GitHub Section ====================

@Composable
fun GitHubSection(
    github: GitHubProfile,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean
) {
    val context = LocalContext.current
    
    SectionCard(
        title = "GitHub",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        val stats = github.stats
        if (github.connected && stats != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // GitHub profile link
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!github.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = github.avatarUrl,
                                contentDescription = "GitHub Avatar",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Column {
                            BasicText(
                                "@${github.username}",
                                style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                            )
                            github.lastSyncedAt?.let {
                                BasicText(
                                    "Last synced: ${formatDate(it)}",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                                )
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isOwner) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .clickable { /* TODO: Sync */ }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                BasicText("🔄 Sync", style = TextStyle(accentColor, 12.sp))
                            }
                        }
                        
                        github.profileUrl?.let { url ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                BasicText("View", style = TextStyle(contentColor, 12.sp))
                            }
                        }
                    }
                }
                
                // Stats grid
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GitHubStatItem(iconRes = R.drawable.ic_code, value = "${stats.totalPublicRepos}", label = "Repos", contentColor = contentColor)
                    GitHubStatItem(iconRes = R.drawable.ic_favorite, value = "${stats.totalStars}", label = "Stars", contentColor = contentColor)
                    GitHubStatItem(iconRes = R.drawable.ic_share, value = "${stats.totalForks}", label = "Forks", contentColor = contentColor)
                    GitHubStatItem(iconRes = R.drawable.ic_users, value = "${stats.followers}", label = "Followers", contentColor = contentColor)
                }
                
                // Top languages
                if (stats.topLanguages.isNotEmpty()) {
                    Column {
                        BasicText(
                            "Top Languages",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            stats.topLanguages.entries.take(5).forEach { (lang, stat) ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(getLanguageColor(lang).copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    BasicText(
                                        "$lang ${stat.percentage.toInt()}%",
                                        style = TextStyle(getLanguageColor(lang), 11.sp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Top repos
                if (stats.topRepos.isNotEmpty()) {
                    Column {
                        BasicText(
                            "Top Repositories",
                            style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                        )
                        Spacer(Modifier.height(6.dp))
                        stats.topRepos.take(3).forEach { repo ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(contentColor.copy(alpha = 0.05f))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.url)))
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        BasicText(
                                            repo.name,
                                            style = TextStyle(contentColor, 13.sp, FontWeight.Medium)
                                        )
                                        Row {
                                            BasicText("⭐ ${repo.stars}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                                            Spacer(Modifier.width(8.dp))
                                            BasicText("🍴 ${repo.forks}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                                        }
                                    }
                                    repo.description?.let { desc ->
                                        BasicText(
                                            desc,
                                            style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        } else if (isOwner) {
            // Not connected - show connect button
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.05f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = "GitHub",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "Connect your GitHub",
                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium)
                    )
                    BasicText(
                        "Show your contributions and top repositories",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF24292E))
                            .clickable { /* TODO: Connect GitHub */ }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Connect GitHub",
                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GitHubStatItem(iconRes: Int, value: String, label: String, contentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            colorFilter = ColorFilter.tint(contentColor)
        )
        BasicText(value, style = TextStyle(contentColor, 16.sp, FontWeight.Bold))
        BasicText(label, style = TextStyle(contentColor.copy(alpha = 0.6f), 10.sp))
    }
}

private fun getLanguageColor(language: String): Color {
    return when (language.lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "python" -> Color(0xFF3572A5)
        "rust" -> Color(0xFFDEA584)
        "go" -> Color(0xFF00ADD8)
        "swift" -> Color(0xFFFA7343)
        "c++" -> Color(0xFFF34B7D)
        "c" -> Color(0xFF555555)
        else -> Color(0xFF858585)
    }
}

// ==================== Activity Calendar Section ====================

@Composable
fun ActivityCalendarSection(
    heatmap: List<ActivityHeatmapDay>,
    stats: ProfileStats,
    availableYears: List<Int>,
    selectedYear: Int?,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onYearChange: (Int) -> Unit
) {
    var showYearDropdown by remember { mutableStateOf(false) }
    
    SectionCard(
        title = "Activity",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with year selector and streak
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak info
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        BasicText("🔥 ${stats.currentStreak}", style = TextStyle(contentColor, 14.sp, FontWeight.Bold))
                        BasicText("Current", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                    }
                    Column {
                        BasicText("🏆 ${stats.longestStreak}", style = TextStyle(contentColor, 14.sp, FontWeight.Bold))
                        BasicText("Longest", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                    }
                }
                
                // Year selector
                if (availableYears.isNotEmpty()) {
                    Box {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .clickable { showYearDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText(
                                    "${selectedYear ?: availableYears.firstOrNull() ?: ""}",
                                    style = TextStyle(contentColor, 13.sp)
                                )
                                Spacer(Modifier.width(4.dp))
                                BasicText("▼", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false }
                        ) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { BasicText("$year", style = TextStyle(contentColor)) },
                                    onClick = {
                                        onYearChange(year)
                                        showYearDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Activity heatmap grid
            if (heatmap.isNotEmpty()) {
                ActivityHeatmapGrid(
                    days = heatmap,
                    accentColor = accentColor,
                    contentColor = contentColor
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "No activity data available",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
                    )
                }
            }
            
            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText("Less", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
                listOf(0, 1, 2, 3).forEach { level ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(level, accentColor))
                    )
                }
                BasicText("More", style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp))
            }
        }
    }
}

@Composable
private fun ActivityHeatmapGrid(
    days: List<ActivityHeatmapDay>,
    accentColor: Color,
    contentColor: Color
) {
    val scrollState = rememberScrollState()
    
    // Group days by week
    val weeks = days.chunked(7)
    
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        weeks.forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { day ->
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(day.level, accentColor))
                    )
                }
            }
        }
    }
}

private fun getHeatmapColor(level: Int, accentColor: Color): Color {
    return when (level) {
        0 -> Color(0xFF1E1E1E)
        1 -> accentColor.copy(alpha = 0.3f)
        2 -> accentColor.copy(alpha = 0.6f)
        3 -> accentColor
        else -> Color(0xFF1E1E1E)
    }
}

// ==================== Skills Section ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsSection(
    skills: List<UserSkill>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddSkill: () -> Unit = {},
    onRemoveSkill: (UserSkill) -> Unit = {}
) {
    SectionCard(
        title = "Skills & Expertise",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddSkill
    ) {
        if (skills.isEmpty()) {
            EmptySectionPlaceholder(
                icon = "💡",
                message = "No skills added yet",
                contentColor = contentColor
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { userSkill ->
                    SkillChip(
                        skill = userSkill,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onRemove = { onRemoveSkill(userSkill) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillChip(
    skill: UserSkill,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onRemove: () -> Unit = {}
) {
    val proficiencyColor = when (skill.proficiency?.lowercase()) {
        "expert" -> Color(0xFFFFD700)
        "advanced" -> Color(0xFF22C55E)
        "intermediate" -> Color(0xFF3B82F6)
        else -> contentColor.copy(alpha = 0.6f)
    }
    
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(proficiencyColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                skill.skill.name,
                style = TextStyle(contentColor, 13.sp)
            )
            skill.proficiency?.let { prof ->
                Spacer(Modifier.width(6.dp))
                BasicText(
                    "• $prof",
                    style = TextStyle(proficiencyColor, 11.sp)
                )
            }
            if (isOwner) {
                Spacer(Modifier.width(6.dp))
                BasicText(
                    "×",
                    style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp),
                    modifier = Modifier.clickable { onRemove() }
                )
            }
        }
    }
}

// ==================== Projects Section ====================

@Composable
fun ProjectsSection(
    projects: List<Project>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddProject: () -> Unit = {},
    onEditProject: (Project) -> Unit = {},
    onViewProject: (Project) -> Unit = {},
    onToggleFeatured: (Project) -> Unit = {}
) {
    val context = LocalContext.current
    
    SectionCard(
        title = "Projects & Work",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddProject
    ) {
        if (projects.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicText("✨", style = TextStyle(fontSize = 32.sp))
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No work showcased yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddProject() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add Your First Work",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "✨",
                    message = "No work showcased yet",
                    contentColor = contentColor
                )
            }
        } else {
            // Featured projects section
            val featured = projects.filter { it.featured }
            val regular = projects.filter { !it.featured }
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Featured Work (if any)
                if (featured.isNotEmpty()) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            BasicText("⭐", style = TextStyle(fontSize = 12.sp))
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                "Featured Work",
                                style = TextStyle(
                                    Color(0xFFFFD700),
                                    12.sp,
                                    FontWeight.SemiBold
                                )
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            featured.forEach { project ->
                                ProjectCard(
                                    project = project,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isOwner = isOwner,
                                    onView = { onViewProject(project) },
                                    onEdit = { onEditProject(project) },
                                    onToggleFeatured = { onToggleFeatured(project) }
                                )
                            }
                        }
                    }
                }
                
                // All Projects (non-featured)
                if (regular.isNotEmpty()) {
                    Column {
                        if (featured.isNotEmpty()) {
                            BasicText(
                                "All Projects",
                                style = TextStyle(
                                    contentColor.copy(alpha = 0.5f),
                                    12.sp,
                                    FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            regular.forEach { project ->
                                ProjectCard(
                                    project = project,
                                    backdrop = backdrop,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    isOwner = isOwner,
                                    onView = { onViewProject(project) },
                                    onEdit = { onEditProject(project) },
                                    onToggleFeatured = { onToggleFeatured(project) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCard(
    project: Project,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onToggleFeatured: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.08f))
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onView)
    ) {
        Column {
            // Project Image (if available)
            if (project.images.isNotEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(project.images.first())
                            .crossfade(true)
                            .build(),
                        contentDescription = project.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Featured badge overlay
                    if (project.featured) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.9f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("⭐", style = TextStyle(fontSize = 10.sp))
                                Spacer(Modifier.width(2.dp))
                                BasicText(
                                    "Featured",
                                    style = TextStyle(Color.Black, 10.sp, FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
            
            Column(Modifier.padding(12.dp)) {
                // Header
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                project.name,
                                style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                            )
                            if (project.featured && project.images.isEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                BasicText("⭐", style = TextStyle(fontSize = 12.sp))
                            }
                        }
                        project.role?.let { role ->
                            BasicText(
                                role,
                                style = TextStyle(accentColor, 12.sp)
                            )
                        }
                    }
                    
                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Featured toggle (owner only)
                        if (isOwner) {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (project.featured) Color(0xFFFFD700).copy(alpha = 0.2f)
                                        else contentColor.copy(alpha = 0.1f)
                                    )
                                    .clickable(onClick = onToggleFeatured)
                                    .padding(6.dp)
                            ) {
                                BasicText(
                                    if (project.featured) "⭐" else "☆",
                                    style = TextStyle(
                                        if (project.featured) Color(0xFFFFD700) else contentColor.copy(alpha = 0.5f),
                                        12.sp
                                    )
                                )
                            }
                        }
                        
                        // Edit button (owner only)
                        if (isOwner) {
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .clickable(onClick = onEdit)
                                    .padding(6.dp)
                            ) {
                                BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp))
                            }
                        }
                        
                        // Source/Repository link
                        project.githubUrl?.let { url ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    .padding(6.dp)
                            ) {
                                BasicText("📂", style = TextStyle(fontSize = 12.sp))
                            }
                        }
                        
                        // Live/Portfolio link
                        project.projectUrl?.let { url ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                    .padding(6.dp)
                            ) {
                                BasicText("↗", style = TextStyle(accentColor, 12.sp))
                            }
                        }
                    }
                }
                
                // Description
                if (project.description.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        project.description,
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Skills & Tags (show up to 3, then +N)
                if (project.techStack.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val displayTags = project.techStack.take(3)
                        val remaining = project.techStack.size - 3
                        
                        displayTags.forEach { tag ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    tag,
                                    style = TextStyle(accentColor, 10.sp)
                                )
                            }
                        }
                        
                        if (remaining > 0) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(contentColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    "+$remaining",
                                    style = TextStyle(contentColor.copy(alpha = 0.6f), 10.sp)
                                )
                            }
                        }
                    }
                }
                
                // Date range
                Spacer(Modifier.height(6.dp))
                BasicText(
                    "${formatDate(project.startDate)} — ${if (project.isCurrent) "Present" else project.endDate?.let { formatDate(it) } ?: ""}",
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                )
            }
        }
    }
}

// ==================== Experience Section ====================

@Composable
fun ExperienceSection(
    experiences: List<Experience>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddExperience: () -> Unit = {},
    onEditExperience: (Experience) -> Unit = {},
    onViewExperience: (Experience) -> Unit = {}
) {
    SectionCard(
        title = "Experience",
        count = if (experiences.isNotEmpty()) experiences.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddExperience
    ) {
        if (experiences.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_work),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No experience added yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddExperience() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first experience",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No experience added yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_work
                )
            }
        } else {
            Column {
                experiences.forEachIndexed { index, exp ->
                    ExperienceItem(
                        experience = exp,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        isLast = index == experiences.lastIndex,
                        onEdit = { onEditExperience(exp) },
                        onView = { onViewExperience(exp) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExperienceItem(
    experience: Experience,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
    ) {
        // Timeline dot and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                Modifier
                    .size(if (experience.isCurrent) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (experience.isCurrent) accentColor 
                        else contentColor.copy(alpha = 0.3f)
                    )
                    .then(
                        if (experience.isCurrent) Modifier.border(
                            2.dp,
                            accentColor.copy(alpha = 0.3f),
                            CircleShape
                        ) else Modifier
                    )
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(contentColor.copy(alpha = 0.15f))
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Logo or placeholder
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (experience.logo != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(experience.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = experience.company,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_work),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.4f))
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Content
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    // Title and type badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            experience.title,
                            style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(getExperienceTypeColor(experience.type).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                experience.type,
                                style = TextStyle(getExperienceTypeColor(experience.type), 10.sp, FontWeight.Medium)
                            )
                        }
                    }
                    
                    // Company
                    BasicText(
                        experience.company,
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Date and location
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dateText = buildString {
                            append(formatDate(experience.startDate))
                            append(" — ")
                            if (experience.isCurrent) {
                                append("Present")
                            } else {
                                experience.endDate?.let { append(formatDate(it)) }
                            }
                            // Add duration
                            val duration = calculateDuration(experience.startDate, experience.endDate, experience.isCurrent)
                            if (duration.isNotEmpty()) {
                                append(" · $duration")
                            }
                        }
                        BasicText(
                            dateText,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                        )
                    }
                    
                    experience.location?.let { loc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_location),
                                contentDescription = "Location",
                                modifier = Modifier.size(11.dp),
                                colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(2.dp))
                            BasicText(
                                loc,
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                    }
                }
                
                // Edit button for owner
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
            
            // Description with expand/collapse
            experience.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        desc,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (desc.length > 100) {
                        BasicText(
                            if (isExpanded) "See less" else "Read more",
                            style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Skills
            if (experience.skills.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    experience.skills.take(5).forEach { skill ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                skill,
                                style = TextStyle(accentColor, 10.sp)
                            )
                        }
                    }
                    if (experience.skills.size > 5) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(contentColor.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                "+${experience.skills.size - 5}",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculateDuration(startDate: String, endDate: String?, isCurrent: Boolean): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE
        val start = java.time.LocalDate.parse(startDate.take(10), formatter)
        val end = if (isCurrent) java.time.LocalDate.now() 
                  else endDate?.let { java.time.LocalDate.parse(it.take(10), formatter) } 
                  ?: return ""
        
        val period = java.time.Period.between(start, end)
        val years = period.years
        val months = period.months
        
        buildString {
            if (years > 0) {
                append("$years yr")
                if (years > 1) append("s")
            }
            if (months > 0) {
                if (years > 0) append(" ")
                append("$months mo")
                if (months > 1) append("s")
            }
            if (years == 0 && months == 0) {
                append("< 1 mo")
            }
        }
    } catch (e: Exception) {
        ""
    }
}

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

// ==================== Education Section ====================

@Composable
fun EducationSection(
    education: List<Education>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddEducation: () -> Unit = {},
    onEditEducation: (Education) -> Unit = {},
    onViewEducation: (Education) -> Unit = {}
) {
    SectionCard(
        title = "Education",
        count = if (education.isNotEmpty()) education.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddEducation
    ) {
        if (education.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_education),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No education added yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddEducation() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first education",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No education added yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_education
                )
            }
        } else {
            Column {
                education.forEachIndexed { index, edu ->
                    EducationItem(
                        education = edu,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        isLast = index == education.lastIndex,
                        onEdit = { onEditEducation(edu) },
                        onView = { onViewEducation(edu) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EducationItem(
    education: Education,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val hasLongDescription = (education.description?.length ?: 0) > 100 || education.description?.contains("\n") == true
    val hasLongActivities = (education.activities?.length ?: 0) > 100 || education.activities?.contains("\n") == true
    
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
    ) {
        // Timeline dot and line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                Modifier
                    .size(if (education.isCurrent) 14.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (education.isCurrent) Color(0xFFFFD700)
                        else contentColor.copy(alpha = 0.3f)
                    )
                    .then(
                        if (education.isCurrent) Modifier.border(
                            2.dp,
                            Color(0xFFFFD700).copy(alpha = 0.3f),
                            CircleShape
                        ) else Modifier
                    )
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(contentColor.copy(alpha = 0.15f))
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Icon placeholder (no logo for education)
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_education),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    if (education.isCurrent) Color(0xFFFFD700) else contentColor.copy(alpha = 0.4f)
                )
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Content
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    // School name
                    BasicText(
                        education.school,
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Degree · Field of Study
                    BasicText(
                        "${education.degree} · ${education.fieldOfStudy}",
                        style = TextStyle(contentColor.copy(alpha = 0.8f), 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Date range and grade
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dateText = buildString {
                            append(formatDate(education.startDate))
                            append(" — ")
                            if (education.isCurrent) {
                                append("Present")
                            } else {
                                education.endDate?.let { append(formatDate(it)) }
                            }
                        }
                        BasicText(
                            dateText,
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                        )
                        
                        education.grade?.let { grade ->
                            if (grade.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    "• Grade: $grade",
                                    style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                                )
                            }
                        }
                    }
                }
                
                // Edit button for owner
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
            
            // Description with expand/collapse
            education.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        desc,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasLongDescription) {
                        BasicText(
                            if (isExpanded) "See less" else "Read more",
                            style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
            
            // Activities & Societies
            education.activities?.let { activities ->
                if (activities.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        "Activities & Societies",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp, FontWeight.Medium)
                    )
                    BasicText(
                        activities,
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (hasLongActivities && !hasLongDescription) {
                        BasicText(
                            if (isExpanded) "See less" else "Read more",
                            style = TextStyle(accentColor, 11.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==================== Licenses & Certifications Section ====================

// Predefined color palette for certificate cards (client-side only)
internal val CERTIFICATE_COLORS = listOf(
    Color(0xFF6B7280) to "Neutral",    // Gray
    Color(0xFFEF4444) to "Red",
    Color(0xFFF97316) to "Orange",
    Color(0xFFF59E0B) to "Amber",
    Color(0xFF22C55E) to "Green",
    Color(0xFF3B82F6) to "Blue",
    Color(0xFF6366F1) to "Indigo",
    Color(0xFFA855F7) to "Purple",
    Color(0xFFEC4899) to "Pink"
)

internal fun getCertificateColor(colorHex: String?): Color {
    if (colorHex.isNullOrEmpty()) return CERTIFICATE_COLORS[0].first
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        CERTIFICATE_COLORS[0].first
    }
}

private fun isExpired(expiryDate: String?): Boolean {
    if (expiryDate.isNullOrEmpty()) return false
    return try {
        val expiry = java.time.LocalDate.parse(expiryDate.take(10))
        expiry.isBefore(java.time.LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

@Composable
fun CertificatesSection(
    certificates: List<Certificate>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddCertificate: () -> Unit = {},
    onEditCertificate: (Certificate) -> Unit = {},
    onViewCertificate: (Certificate) -> Unit = {}
) {
    SectionCard(
        title = "Licenses & Certifications",
        count = if (certificates.isNotEmpty()) certificates.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddCertificate
    ) {
        if (certificates.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_award),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No certifications added yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddCertificate() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first certification",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No certifications added yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_award
                )
            }
        } else {
            // Grid of certificate cards (1 column on small screens, 2 on larger)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                certificates.forEach { cert ->
                    CertificateCard(
                        certificate = cert,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditCertificate(cert) },
                        onView = { onViewCertificate(cert) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateCard(
    certificate: Certificate,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    val cardColor = getCertificateColor(certificate.color)
    val expired = !certificate.doesNotExpire && isExpired(certificate.expiryDate)
    val hasImage = certificate.credentialUrl?.let { isImageUrl(it) } ?: false
    
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onView)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Certificate image thumbnail or award icon
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && certificate.credentialUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(certificate.credentialUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = certificate.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_award),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(cardColor)
                    )
                }
            }
            
            // Right: Certificate info
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Name (title)
                BasicText(
                    certificate.name,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Issuing organization (subtitle)
                BasicText(
                    certificate.issuingOrg,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Meta: issue date, expiry status
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Issue date
                    BasicText(
                        "Issued ${formatDate(certificate.issueDate)}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                    
                    // Expiry status
                    when {
                        expired -> {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    "Expired",
                                    style = TextStyle(Color(0xFFEF4444), 10.sp, FontWeight.Medium)
                                )
                            }
                        }
                        certificate.doesNotExpire -> {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    "No Expiration",
                                    style = TextStyle(Color(0xFF22C55E), 10.sp, FontWeight.Medium)
                                )
                            }
                        }
                        certificate.expiryDate != null -> {
                            BasicText(
                                "• Exp: ${formatDate(certificate.expiryDate)}",
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                    }
                }
                
                // Credential ID (if present)
                certificate.credentialId?.takeIf { it.isNotBlank() }?.let { credentialId ->
                    BasicText(
                        "ID: $credentialId",
                        style = TextStyle(contentColor.copy(alpha = 0.4f), 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // View button (if credentialUrl present)
                certificate.credentialUrl?.let { url ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onView)
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_visibility),
                            contentDescription = "View credential",
                            modifier = Modifier.size(14.dp),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                    }
                }
                
                // Edit button (owner only)
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
        }
    }
}

// ==================== Achievements Section ====================

@Composable
fun AchievementsSection(
    achievements: List<Achievement>,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onAddAchievement: () -> Unit = {},
    onEditAchievement: (Achievement) -> Unit = {},
    onViewAchievement: (Achievement) -> Unit = {}
) {
    val context = LocalContext.current
    
    SectionCard(
        title = "Achievements",
        count = if (achievements.isNotEmpty()) achievements.size else null,
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isOwner = isOwner,
        onAdd = onAddAchievement
    ) {
        if (achievements.isEmpty()) {
            // Empty state for owner
            if (isOwner) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.height(8.dp))
                    BasicText(
                        "No achievements added yet",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 14.sp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor)
                            .clickable { onAddAchievement() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            "Add your first achievement",
                            style = TextStyle(Color.White, 14.sp, FontWeight.Medium)
                        )
                    }
                }
            } else {
                EmptySectionPlaceholder(
                    icon = "",
                    message = "No achievements added yet",
                    contentColor = contentColor,
                    iconRes = R.drawable.ic_trophy
                )
            }
        } else {
            // List of achievement cards
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                achievements.forEach { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isOwner = isOwner,
                        onEdit = { onEditAchievement(achievement) },
                        onView = { onViewAchievement(achievement) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onView: () -> Unit
) {
    val context = LocalContext.current
    val cardColor = getAchievementColor(achievement.color)
    val hasImage = achievement.certificateUrl?.let { isImageUrl(it) } ?: false
    
    // Get type icon
    val typeIcon = when (achievement.type) {
        "Hackathon" -> R.drawable.ic_target
        "Competition" -> R.drawable.ic_trophy
        "Award" -> R.drawable.ic_medal
        "Scholarship" -> R.drawable.ic_gift
        "Recognition" -> R.drawable.ic_sparkles
        else -> R.drawable.ic_trophy
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(12f.dp) },
                effects = {
                    vibrancy()
                    blur(16f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.06f))
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onView)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Achievement image thumbnail or type icon
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage && achievement.certificateUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(achievement.certificateUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = achievement.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(typeIcon),
                        contentDescription = achievement.type,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(cardColor)
                    )
                }
            }
            
            // Right: Achievement info
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Title
                BasicText(
                    achievement.title,
                    style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Organization
                BasicText(
                    achievement.organization,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Type badge + date
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(cardColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(
                            achievement.type,
                            style = TextStyle(cardColor, 10.sp, FontWeight.Medium)
                        )
                    }
                    
                    // Date
                    BasicText(
                        "• ${formatDate(achievement.date)}",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }
                
                // Description (if present, 2-line clamp)
                achievement.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        desc,
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // View button (if certificateUrl present)
                achievement.certificateUrl?.let { url ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onView)
                            .padding(6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_visibility),
                            contentDescription = "View proof",
                            modifier = Modifier.size(14.dp),
                            colorFilter = ColorFilter.tint(accentColor)
                        )
                    }
                }
                
                // Edit button (owner only)
                if (isOwner) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.08f))
                            .clickable(onClick = onEdit)
                            .padding(6.dp)
                    ) {
                        BasicText("✎", style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp))
                    }
                }
            }
        }
    }
}

// ==================== Activity Feed Section ====================

@Composable
fun ActivityFeedSection(
    feedItems: List<FeedItem>,
    currentFilter: String,
    isLoading: Boolean,
    hasMore: Boolean,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    isOwner: Boolean,
    onFilterChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    val filters = listOf(
        "all" to "All",
        "posts" to "Posts",
        "articles" to "Articles",
        "videos" to "Reels"
    )
    
    SectionCard(
        title = "Activity",
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor
    ) {
        Column {
            // Filter tabs
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (filter, label) ->
                    val isSelected = currentFilter == filter
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(
                                if (isSelected) accentColor else contentColor.copy(alpha = 0.6f),
                                13.sp,
                                if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Feed items
            if (feedItems.isEmpty() && !isLoading) {
                EmptySectionPlaceholder(
                    icon = "📭",
                    message = "No activity yet",
                    contentColor = contentColor
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    feedItems.forEach { item ->
                        FeedItemCard(
                            item = item,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isOwner = isOwner,
                            onOpenItem = onPostClick,
                            onDeletePost = onDeletePost
                        )
                    }
                    
                    if (isLoading) {
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = accentColor
                            )
                        }
                    }
                    
                    if (hasMore && !isLoading) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(contentColor.copy(alpha = 0.05f))
                                .clickable(onClick = onLoadMore)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "Load more",
                                style = TextStyle(accentColor, 13.sp, FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedItemCard(
    item: FeedItem,
    contentColor: Color,
    accentColor: Color,
    isOwner: Boolean,
    onOpenItem: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    val canDelete = item.contentType == "post" || item.contentType == "article" || item.contentType == "short_video"
    val mediaItems = when {
        !item.images.isNullOrEmpty() -> item.images
        !item.mediaUrls.isNullOrEmpty() -> item.mediaUrls
        else -> emptyList()
    }
    var showMenu by remember { mutableStateOf(false) }

    val typeIconRes: Int? = when (item.contentType) {
        "post" -> R.drawable.ic_post
        "article" -> R.drawable.ic_article
        "short_video" -> R.drawable.ic_video
        else -> null
    }
    val typeIconText = when (item.contentType) {
        "forum_question" -> "?"
        "forum_answer" -> "A"
        else -> ""
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.05f))
            .clickable { onOpenItem(item.id) }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (typeIconRes != null) {
                    Image(
                        painter = painterResource(typeIconRes),
                        contentDescription = item.contentType,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(contentColor)
                    )
                } else {
                    BasicText(typeIconText, style = TextStyle(contentColor, 14.sp, FontWeight.Bold))
                }
                Spacer(Modifier.width(8.dp))
                item.title?.let { title ->
                    BasicText(
                        title,
                        style = TextStyle(contentColor, 14.sp, FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } ?: run {
                    BasicText(
                        item.content.take(50) + if (item.content.length > 50) "..." else "",
                        style = TextStyle(contentColor, 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isOwner && canDelete) {
                    Box {
                        BasicText(
                            "⋯",
                            style = TextStyle(contentColor.copy(alpha = 0.7f), 18.sp, FontWeight.SemiBold),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showMenu = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { BasicText("Delete post", style = TextStyle(Color(0xFFDC2626), 13.sp)) },
                                onClick = {
                                    showMenu = false
                                    onDeletePost(item.id)
                                }
                            )
                        }
                    }
                }
            }
            
            if (item.title != null && item.content.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                BasicText(
                    item.content.take(100) + if (item.content.length > 100) "..." else "",
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Display media preview (images/videos)
            if (mediaItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val isVideo = item.contentType == "short_video"
                val imageCount = mediaItems.size
                
                if (imageCount == 1) {
                    // Single image/video
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(mediaItems.first())
                                .crossfade(true)
                                .build(),
                            contentDescription = if (isVideo) "Video thumbnail" else "Post image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Video play icon overlay
                        if (isVideo) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText(
                                        "▶",
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontSize = 20.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Multiple images - show grid preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Show first 2-3 images
                        mediaItems.take(3).forEachIndexed { index, imageUrl ->
                            val isLastVisible = index == 2 || index == mediaItems.lastIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Post image ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Show count overlay on last image if more images
                                if (isLastVisible && imageCount > 3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicText(
                                            "+${imageCount - 3}",
                                            style = TextStyle(
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText("❤️", style = TextStyle(fontSize = 12.sp))
                    Spacer(Modifier.width(4.dp))
                    BasicText("${item.likesCount}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText("💬", style = TextStyle(fontSize = 12.sp))
                    Spacer(Modifier.width(4.dp))
                    BasicText("${item.commentsCount}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                }
                if (item.viewsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText("👁", style = TextStyle(fontSize = 12.sp))
                        Spacer(Modifier.width(4.dp))
                        BasicText("${item.viewsCount}", style = TextStyle(contentColor.copy(alpha = 0.6f), 11.sp))
                    }
                }
                Spacer(Modifier.weight(1f))
                BasicText(
                    formatDate(item.createdAt),
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 10.sp)
                )
            }
        }
    }
}

// ==================== Helper Components ====================

@Composable
private fun EmptySectionPlaceholder(
    icon: String,
    message: String,
    contentColor: Color,
    iconRes: Int? = null  // Optional drawable resource
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = message,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(contentColor.copy(alpha = 0.5f))
                )
            } else {
                BasicText(icon, style = TextStyle(fontSize = 32.sp))
            }
            Spacer(Modifier.height(8.dp))
            BasicText(
                message,
                style = TextStyle(contentColor.copy(alpha = 0.5f), 13.sp)
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.take(10))
        date.format(DateTimeFormatter.ofPattern("MMM yyyy"))
    } catch (e: Exception) {
        dateString.take(10)
    }
}
