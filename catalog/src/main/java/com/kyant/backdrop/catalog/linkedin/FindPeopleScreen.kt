package com.kyant.backdrop.catalog.linkedin

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.catalog.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.catalog.data.SettingsPreferences
import com.kyant.backdrop.catalog.network.models.CollegeInfo
import com.kyant.backdrop.catalog.network.models.NearbyUser
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.SmartMatch
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

// ==================== Main FindPeople Screen ====================

@Composable
fun FindPeopleScreenNew(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    findPeopleViewModel: FindPeopleViewModel,
    onNavigateToProfile: (String) -> Unit = {},
    reduceAnimations: Boolean = false
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val retentionViewModel: RetentionViewModel = viewModel(factory = RetentionViewModel.Factory(context))
    val uiState by findPeopleViewModel.uiState.collectAsState()
    val retentionState by retentionViewModel.uiState.collectAsState()
    
    // Theme preference: "glass", "light", "dark"
    val themeMode by SettingsPreferences.themeMode(context).collectAsState(initial = DefaultThemeModeKey)
    val isGlassTheme = themeMode == "glass"
    val isLightTheme = !isSystemInDarkTheme()
    // One shimmer for all Find skeletons (avoids N× infinite transitions).
    val listShimmerBrush = findPeopleShimmerBrush(isLightTheme)
    
    // Scroll state for collapsing header
    val scrollState = rememberScrollState()
    val isScrolled = scrollState.value > 50
    
    // Animated header height and alpha
    val headerHeight by animateDpAsState(
        targetValue = if (isScrolled) 0.dp else 56.dp,
        label = "headerHeight"
    )
    val headerAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0f else 1f,
        label = "headerAlpha"
    )
    val navigateToProfile: (String) -> Unit = { userId ->
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        onNavigateToProfile(userId)
    }
    
    // Clear any existing errors when this screen is opened
    LaunchedEffect(Unit) {
        findPeopleViewModel.clearAllErrors()
        findPeopleViewModel.ensureFindSurfaceLoaded()
        retentionViewModel.ensureRetentionLoaded()
    }

    LaunchedEffect(uiState.selectedTab) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }
    
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            
            // Collapsing Header
            if (headerHeight > 0.dp) {
                FindPeopleHeader(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    selectedTab = uiState.selectedTab,
                    totalCount = if (uiState.selectedTab == FindPeopleTab.ALL_PEOPLE) uiState.totalPeopleCount else null,
                    alpha = headerAlpha
                )
                Spacer(Modifier.height(8.dp))
            }
            
            // Tabs (always visible)
            FindPeopleTabs(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                selectedTab = uiState.selectedTab,
                onTabSelected = { findPeopleViewModel.selectTab(it) }
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Connection Limit Indicator (only show if not scrolled)
            AnimatedVisibility(visible = !isScrolled) {
                Column {
                    ConnectionLimitIndicator(
                        limitData = retentionState.connectionLimit,
                        contentColor = contentColor,
                        accentColor = accentColor
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            // Variable Rewards Section (collapse when scrolled)
            AnimatedVisibility(visible = !isScrolled) {
                VariableRewardsSection(
                    // Daily Matches
                    dailyMatches = uiState.dailyMatches,
                    dailyMatchCount = uiState.dailyMatchCount,
                    surpriseMessage = uiState.surpriseMessage,
                    showDailyMatchesBanner = uiState.showDailyMatchesBanner,
                    // Hidden Gem
                    hiddenGem = uiState.hiddenGem,
                    hiddenGemMessage = uiState.hiddenGemMessage,
                    showHiddenGemCard = uiState.showHiddenGemCard,
                    // Trending
                    isTrending = uiState.isTrending,
                    trendingRank = uiState.trendingRank,
                    trendingViewsToday = uiState.trendingViewsToday,
                    trendingMessage = uiState.trendingMessage,
                    showTrendingBanner = uiState.showTrendingBanner,
                    // Backdrop & styling
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    // Actions
                    onMatchClick = navigateToProfile,
                    onHiddenGemViewProfile = { 
                        uiState.hiddenGem?.id?.let(navigateToProfile)
                    },
                    onHiddenGemConnect = {
                        uiState.hiddenGem?.id?.let { findPeopleViewModel.sendConnectionRequest(it) }
                    },
                    onViewTrendingStats = { /* TODO: Navigate to profile stats */ },
                    onDismissDailyMatches = { findPeopleViewModel.dismissDailyMatchesBanner() },
                    onDismissHiddenGem = { findPeopleViewModel.dismissHiddenGemCard() },
                    onDismissTrending = { findPeopleViewModel.dismissTrendingBanner() }
                )
            }
            
            // Content based on selected tab
            when (uiState.selectedTab) {
                FindPeopleTab.PEOPLE_YOU_KNOW -> PeopleYouKnowContent(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isLightTheme = isLightTheme,
                    uiState = uiState,
                    viewModel = findPeopleViewModel,
                    onNavigateToProfile = navigateToProfile,
                    reduceAnimations = reduceAnimations
                )

                FindPeopleTab.SMART_MATCHES -> SmartMatchesContent(
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    isGlassTheme = isGlassTheme,
                    isLightTheme = isLightTheme,
                    listShimmerBrush = listShimmerBrush,
                    reduceAnimations = reduceAnimations,
                    matches = uiState.smartMatches,
                    isLoading = uiState.isLoadingSmartMatches,
                    error = uiState.smartMatchError,
                    selectedFilter = uiState.smartMatchFilter,
                    onFilterSelected = { findPeopleViewModel.setSmartMatchFilter(it) },
                    onNavigateToProfile = navigateToProfile,
                    onRetry = { findPeopleViewModel.loadSmartMatches(forceRefresh = true) },
                    onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
                )
            
            FindPeopleTab.ALL_PEOPLE -> AllPeopleContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                people = uiState.allPeople,
                isLoading = uiState.isLoadingAllPeople,
                error = uiState.allPeopleError,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { findPeopleViewModel.updateSearchQuery(it) },
                filterOptions = uiState.filterOptions,
                selectedCollege = uiState.selectedCollege,
                selectedBranch = uiState.selectedBranch,
                selectedGraduationYear = uiState.selectedGraduationYear,
                isFilterExpanded = uiState.isFilterExpanded,
                onToggleFilter = { findPeopleViewModel.toggleFilterExpanded() },
                onCollegeSelected = { findPeopleViewModel.setCollegeFilter(it) },
                onBranchSelected = { findPeopleViewModel.setBranchFilter(it) },
                onYearSelected = { findPeopleViewModel.setGraduationYearFilter(it) },
                onClearFilters = { findPeopleViewModel.clearFilters() },
                hasMore = uiState.hasMoreAllPeople,
                onLoadMore = { findPeopleViewModel.loadMorePeople() },
                connectionActionInProgress = uiState.connectionActionInProgress,
                onConnect = { findPeopleViewModel.sendConnectionRequest(it) },
                onNavigateToProfile = navigateToProfile,
                onRetry = { findPeopleViewModel.loadAllPeople(resetPage = true, forceRefresh = true) },
                onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
            )
            
            FindPeopleTab.FOR_YOU -> ForYouContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                people = uiState.suggestions,
                isLoading = uiState.isLoadingSuggestions,
                error = uiState.suggestionsError,
                connectionActionInProgress = uiState.connectionActionInProgress,
                onConnect = { findPeopleViewModel.sendConnectionRequest(it) },
                onNavigateToProfile = navigateToProfile,
                onRetry = { findPeopleViewModel.loadSuggestions(forceRefresh = true) },
                onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
            )
            
            FindPeopleTab.SAME_CAMPUS -> SameCampusContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                people = uiState.sameCampusPeople,
                isLoading = uiState.isLoadingSameCampus,
                error = uiState.sameCampusError,
                userCollege = uiState.userCollege,
                isSavingCollege = uiState.isSavingCollege,
                collegeSuggestions = uiState.collegeSuggestions,
                isSearchingColleges = uiState.isSearchingColleges,
                connectionActionInProgress = uiState.connectionActionInProgress,
                onConnect = { findPeopleViewModel.sendConnectionRequest(it) },
                onNavigateToProfile = navigateToProfile,
                onRetry = { findPeopleViewModel.loadSameCampus(forceRefresh = true) },
                onSaveCollege = { findPeopleViewModel.saveCollege(it) },
                onCollegeSearch = { findPeopleViewModel.searchColleges(it) },
                onDismissError = { findPeopleViewModel.dismissErrorsWithCooldown() }
            )
            
            FindPeopleTab.NEARBY -> NearbyContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                nearbyPeople = uiState.nearbyPeople,
                isLoading = uiState.isLoadingNearby,
                error = uiState.nearbyError,
                currentLat = uiState.currentLat,
                currentLng = uiState.currentLng,
                currentCity = uiState.currentCity,
                selectedRadius = uiState.selectedRadius,
                hasLocationPermission = uiState.hasLocationPermission,
                onPermissionGranted = { findPeopleViewModel.setLocationPermission(true) },
                onLocationUpdate = { lat, lng, acc -> findPeopleViewModel.updateLocation(lat, lng, acc) },
                onRadiusChange = { findPeopleViewModel.setRadius(it) },
                onNavigateToProfile = navigateToProfile,
                onRefresh = { findPeopleViewModel.loadNearbyPeople(forceRefresh = true) }
            )
        }
        }
        
        // Streak status banner (Duolingo Effect: Fear of loss)
        // Only show when streak is at risk - not just for having a streak
        if (uiState.isStreakAtRisk && uiState.connectionStreak > 0) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp) // Below header
            ) {
                StreakStatusBanner(
                    connectionStreak = uiState.connectionStreak,
                    isAtRisk = uiState.isStreakAtRisk,
                    backdrop = backdrop,
                    onTap = { /* Navigate to streak details */ },
                    onDismiss = { findPeopleViewModel.dismissErrorsWithCooldown() }
                )
            }
        }
        
        // Connection sent celebration overlay (Habit Loop: Reward)
        ConnectionSentCelebration(
            isVisible = uiState.showConnectionCelebration,
            recipientName = uiState.celebrationRecipientName,
            recipientImage = uiState.celebrationRecipientImage,
            replyRate = uiState.celebrationReplyRate,
            connectionStreak = uiState.connectionStreak,
            isNewStreakMilestone = uiState.isNewStreakMilestone,
            backdrop = backdrop,
            onDismiss = { findPeopleViewModel.dismissConnectionCelebration() }
        )
    }
}

// ==================== Header ====================

@Composable
private fun FindPeopleHeader(
    backdrop: LayerBackdrop,
    contentColor: Color,
    selectedTab: FindPeopleTab,
    totalCount: Int?,
    alpha: Float = 1f
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            "Find People",
            style = TextStyle(
                color = contentColor.copy(alpha = alpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        
        // Show total count for All People tab
        if (selectedTab == FindPeopleTab.ALL_PEOPLE && totalCount != null && totalCount > 0) {
            BasicText(
                "${formatCount(totalCount)} people",
                style = TextStyle(
                    color = contentColor.copy(alpha = 0.5f * alpha),
                    fontSize = 12.sp
                )
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

// ==================== Tabs ====================

@Composable
private fun FindPeopleTabs(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    selectedTab: FindPeopleTab,
    onTabSelected: (FindPeopleTab) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FindPeopleTabItem(
            icon = { color ->
                FindPeopleVectorTabIcon(
                    drawableRes = R.drawable.ic_search,
                    color = color
                )
            },
            label = "All",
            isSelected = selectedTab == FindPeopleTab.ALL_PEOPLE,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onTabSelected(FindPeopleTab.ALL_PEOPLE) }
        )

        FindPeopleTabItem(
            icon = { color ->
                FindPeopleVectorTabIcon(
                    drawableRes = R.drawable.ic_target,
                    color = color
                )
            },
            label = "Smart",
            isSelected = selectedTab == FindPeopleTab.SMART_MATCHES,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onTabSelected(FindPeopleTab.SMART_MATCHES) }
        )

        FindPeopleTabItem(
            icon = { color ->
                FindPeopleVectorTabIcon(
                    drawableRes = R.drawable.ic_network,
                    color = color
                )
            },
            label = "People You Know",
            isSelected = selectedTab == FindPeopleTab.PEOPLE_YOU_KNOW,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onTabSelected(FindPeopleTab.PEOPLE_YOU_KNOW) }
        )

        FindPeopleTabItem(
            icon = { color -> SparkleIcon(color = color, size = 14.dp) },
            label = "For You",
            isSelected = selectedTab == FindPeopleTab.FOR_YOU,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onTabSelected(FindPeopleTab.FOR_YOU) }
        )
        
        // Campus tab
        FindPeopleTabItem(
            icon = { color -> GraduationCapIcon(color = color, size = 14.dp) },
            label = "Campus",
            isSelected = selectedTab == FindPeopleTab.SAME_CAMPUS,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onTabSelected(FindPeopleTab.SAME_CAMPUS) }
        )
        
        // Nearby tab
        FindPeopleTabItem(
            icon = { color -> LocationPinIcon(color = color, size = 14.dp) },
            label = "Nearby",
            isSelected = selectedTab == FindPeopleTab.NEARBY,
            contentColor = contentColor,
            accentColor = accentColor,
            onClick = { onTabSelected(FindPeopleTab.NEARBY) }
        )
    }
}

@Composable
private fun FindPeopleVectorTabIcon(
    drawableRes: Int,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 14.dp
) {
    androidx.compose.foundation.Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(color)
    )
}

@Composable
private fun FindPeopleTabItem(
    icon: @Composable (Color) -> Unit,
    label: String,
    isSelected: Boolean,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val iconColor = if (isSelected) accentColor else contentColor.copy(alpha = 0.6f)
    
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else contentColor.copy(alpha = 0.06f)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            icon(iconColor)
            BasicText(
                label,
                style = TextStyle(
                    color = if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            )
        }
    }
}

// ==================== Shimmer/Skeleton ====================

@Composable
private fun findPeopleShimmerBrush(isLightTheme: Boolean): Brush {
    val shimmerColors = if (isLightTheme) {
        listOf(
            Color.LightGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.5f),
            Color.LightGray.copy(alpha = 0.3f)
        )
    } else {
        listOf(
            Color.DarkGray.copy(alpha = 0.3f),
            Color.DarkGray.copy(alpha = 0.5f),
            Color.DarkGray.copy(alpha = 0.3f)
        )
    }
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation.value - 300f, translateAnimation.value - 300f),
        end = Offset(translateAnimation.value, translateAnimation.value)
    )
}

/** Glass blur is expensive; use a flat surface when [reduceAnimations] is enabled. */
private fun Modifier.findPeopleGlassSurface(
    backdrop: LayerBackdrop,
    cornerDp: Float,
    blurDp: Float,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    onDrawSurfaceColor: Color = Color.White.copy(alpha = 0.1f)
): Modifier {
    val shape = RoundedCornerShape(cornerDp.dp)
    return this.clip(shape).then(
        if (reduceAnimations) {
            Modifier.background(
                if (isLightTheme) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.09f)
            )
        } else {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(cornerDp.dp) },
                effects = {
                    vibrancy()
                    blur(blurDp.dp.toPx())
                },
                onDrawSurface = { drawRect(onDrawSurfaceColor) }
            )
        }
    )
}

@Composable
private fun PersonCardSkeleton(
    shimmerBrush: Brush,
    isLightTheme: Boolean
) {
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.04f))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: Avatar + Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                
                Spacer(Modifier.width(8.dp))
                
                Column {
                    Box(
                        Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(70.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            
            // Headline
            Box(
                Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            
            // Skills row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(50.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            
            // Connect button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
private fun SmartMatchCardSkeleton(
    shimmerBrush: Brush,
    isLightTheme: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            
            Spacer(Modifier.width(10.dp))
            
            Column(Modifier.weight(1f)) {
                // Name + badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(100.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .width(35.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(shimmerBrush)
                    )
                }
                
                Spacer(Modifier.height(6.dp))
                
                // Details
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                
                Spacer(Modifier.height(6.dp))
                
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(2) {
                        Box(
                            Modifier
                                .width(60.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            // View button
            Box(
                Modifier
                    .width(50.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// ==================== PersonCard Component ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonCard(
    person: PersonInfo,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean = false,
    isLightTheme: Boolean = true,
    isActionInProgress: Boolean = false,
    onConnect: () -> Unit = {},
    onCardClick: () -> Unit = {},
    reduceAnimations: Boolean = false
) {
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    Box(
        Modifier
            .fillMaxWidth()
            .then(
                if (isGlassTheme) {
                    if (reduceAnimations) {
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isLightTheme) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)
                            )
                    } else {
                        Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(16f.dp) },
                                effects = {
                                    vibrancy()
                                },
                                onDrawSurface = {
                                    drawRect(if (isLightTheme) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f))
                                }
                            )
                    }
                } else {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(contentColor.copy(alpha = 0.04f))
                }
            )
            .clickable(onClick = onCardClick)
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: Avatar + Name + Online indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!person.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(person.profileImage)
                                .crossfade(imageCrossfadeMs)
                                .build(),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        val initials = (person.name ?: person.username ?: "U")
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                        BasicText(
                            initials,
                            style = TextStyle(accentColor, 14.sp, FontWeight.SemiBold)
                        )
                    }
                }
                
                Spacer(Modifier.width(8.dp))
                
                // Name + Username
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            person.name ?: "Unknown",
                            style = TextStyle(contentColor, 13.sp, FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Online indicator
                        if (person.isOnline) {
                            Spacer(Modifier.width(4.dp))
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                        }

                        if (person.isInContacts) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(accentColor.copy(alpha = 0.14f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                BasicText(
                                    "In your contacts",
                                    style = TextStyle(accentColor, 9.sp, FontWeight.Medium)
                                )
                            }
                        }
                    }
                    
                    person.username?.let { username ->
                        BasicText(
                            "@$username",
                            style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Headline
            person.headline?.let { headline ->
                BasicText(
                    headline,
                    style = TextStyle(contentColor.copy(alpha = 0.7f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // College + Branch (compact)
            if (person.college != null || person.branch != null) {
                BasicText(
                    listOfNotNull(person.college, person.branch).joinToString(" · "),
                    style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Skills (max 3, compact)
            if (person.skills.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    person.skills.take(2).forEach { skill ->
                        SkillChip(skill, contentColor)
                    }
                    if (person.skills.size > 2) {
                        SkillChip("+${person.skills.size - 2}", contentColor)
                    }
                }
            }
            
            // Mutual connections (if any)
            if (person.mutualConnections > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    UsersIcon(color = contentColor.copy(alpha = 0.4f), size = 12.dp)
                    BasicText(
                        "${person.mutualConnections} mutual",
                        style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                    )
                }
            }
            
            // Connection button
            ConnectionButton(
                status = person.connectionStatus,
                contentColor = contentColor,
                accentColor = accentColor,
                isLoading = isActionInProgress,
                onConnect = onConnect
            )
        }
    }
}

@Composable
private fun SkillChip(text: String, contentColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        BasicText(
            text,
            style = TextStyle(contentColor.copy(alpha = 0.7f), 9.sp)
        )
    }
}

@Composable
private fun InterestChip(text: String, accentColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        BasicText(
            text,
            style = TextStyle(accentColor, 10.sp)
        )
    }
}

@Composable
private fun ConnectionButton(
    status: String,
    contentColor: Color,
    accentColor: Color,
    isLoading: Boolean,
    onConnect: () -> Unit
) {
    val (text, bgColor, textColor, enabled) = when (status) {
        "connected" -> listOf("Connected", contentColor.copy(alpha = 0.1f), contentColor.copy(alpha = 0.5f), false)
        "pending_sent" -> listOf("Pending", Color(0xFFFFA500).copy(alpha = 0.2f), Color(0xFFFFA500), true)
        "pending_received" -> listOf("Accept", Color(0xFF22C55E).copy(alpha = 0.2f), Color(0xFF22C55E), true)
        else -> listOf("Connect", accentColor.copy(alpha = 0.2f), accentColor, true)
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor as Color)
            .clickable(enabled = enabled as Boolean && !isLoading, onClick = onConnect)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = textColor as Color,
                strokeWidth = 2.dp
            )
        } else {
            BasicText(
                text as String,
                style = TextStyle(textColor as Color, 12.sp, FontWeight.Medium)
            )
        }
    }
}

// ==================== Smart Match Card ====================

@Composable
fun SmartMatchCard(
    match: SmartMatch,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    onViewClick: () -> Unit = {},
    reduceAnimations: Boolean = false
) {
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    val percentageColor = when {
        match.matchPercentage >= 60 -> Color(0xFF22C55E) // Green
        match.matchPercentage >= 35 -> Color(0xFF3B82F6) // Blue
        else -> Color(0xFFF97316) // Orange
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(contentColor.copy(alpha = 0.04f))
            .clickable(onClick = onViewClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!match.user.profileImage.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(match.user.profileImage)
                            .crossfade(imageCrossfadeMs)
                            .build(),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    val initials = (match.user.name ?: match.user.username ?: "U")
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .take(2)
                        .joinToString("")
                    BasicText(
                        initials,
                        style = TextStyle(accentColor, 16.sp, FontWeight.SemiBold)
                    )
                }
            }
            
            Spacer(Modifier.width(10.dp))
            
            Column(Modifier.weight(1f)) {
                // Name row with match badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        match.user.name ?: match.user.username ?: "Unknown",
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // Match percentage badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(percentageColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        BasicText(
                            "${match.matchPercentage}%",
                            style = TextStyle(percentageColor, 11.sp, FontWeight.Bold)
                        )
                    }
                }
                
                // College + Primary Goal
                val details = mutableListOf<String>()
                match.user.college?.let { details.add(it) }
                match.user.onboarding?.primaryGoal?.let { 
                    details.add(mapPrimaryGoalToDisplay(it)) 
                }
                if (details.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    BasicText(
                        details.joinToString(" · "),
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Match tags/reasons
                if (match.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        match.tags.take(3).forEach { tag ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                BasicText(
                                    tag,
                                    style = TextStyle(accentColor, 10.sp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.width(8.dp))
            
            // View button
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .clickable(onClick = onViewClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BasicText(
                    "View",
                    style = TextStyle(accentColor, 12.sp, FontWeight.Medium)
                )
            }
        }
    }
}

// ==================== Tab Contents ====================

@Composable
private fun SmartMatchesContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    matches: List<SmartMatch>,
    isLoading: Boolean,
    error: String?,
    selectedFilter: SmartMatchFilter,
    onFilterSelected: (SmartMatchFilter) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        // Sub-filters
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                SmartMatchFilter.ALL to "Best Matches",
                SmartMatchFilter.SAME_CAMPUS to "Same Campus",
                SmartMatchFilter.SAME_GOAL to "Same Goal",
                SmartMatchFilter.FIND_MENTOR to "Find Mentor"
            ).forEach { (filter, label) ->
                val isSelected = selectedFilter == filter
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.2f)
                            else contentColor.copy(alpha = 0.08f)
                        )
                        .clickable { onFilterSelected(filter) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        label,
                        style = TextStyle(
                            if (isSelected) accentColor else contentColor.copy(alpha = 0.7f),
                            12.sp,
                            if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Content
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isLoading -> {
                    items(count = 4, key = { "smart_match_sk_$it" }) {
                        SmartMatchCardSkeleton(listShimmerBrush, isLightTheme)
                    }
                }
                error != null -> {
                    item {
                        ErrorState(
                            message = error,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            reduceAnimations = reduceAnimations,
                            onRetry = onRetry,
                            onDismiss = onDismissError
                        )
                    }
                }
                matches.isEmpty() -> {
                    item {
                        EmptyState(
                            iconRes = R.drawable.ic_search,
                            title = "No matches found",
                            subtitle = "Complete your profile and add interests to get matched",
                            backdrop = backdrop,
                            contentColor = contentColor
                        )
                    }
                }
                else -> {
                    items(matches, key = { it.user.id }) { match ->
                        SmartMatchCard(
                            match = match,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            reduceAnimations = reduceAnimations,
                            onViewClick = { onNavigateToProfile(match.user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllPeopleContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterOptions: com.kyant.backdrop.catalog.network.models.FilterOptions,
    selectedCollege: String?,
    selectedBranch: String?,
    selectedGraduationYear: Int?,
    isFilterExpanded: Boolean,
    onToggleFilter: () -> Unit,
    onCollegeSelected: (String?) -> Unit,
    onBranchSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize()) {
        val normalizedQuery = searchQuery.trim().lowercase()

        // Search bar
        Box(
            Modifier
                .fillMaxWidth()
                .findPeopleGlassSurface(
                    backdrop = backdrop,
                    cornerDp = 16f,
                    blurDp = 8f,
                    isLightTheme = isLightTheme,
                    reduceAnimations = reduceAnimations,
                    onDrawSurfaceColor = Color.White.copy(alpha = 0.1f)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search",
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Spacer(Modifier.width(8.dp))
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(contentColor, 14.sp),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            BasicText(
                                "Search by name, username, college, skills...",
                                style = TextStyle(contentColor.copy(alpha = 0.4f), 14.sp)
                            )
                        }
                        innerTextField()
                    }
                )

                if (isLoading && normalizedQuery.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                }
                
                if (searchQuery.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .clickable { onSearchQueryChange("") }
                            .padding(4.dp)
                    ) {
                        BasicText("✕", style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp))
                    }
                }
                
                Spacer(Modifier.width(8.dp))
                
                // Filter button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isFilterExpanded || selectedCollege != null || selectedBranch != null || selectedGraduationYear != null)
                                accentColor.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .clickable(onClick = onToggleFilter)
                        .padding(8.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_list),
                        contentDescription = "Filters",
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(
                            if (isFilterExpanded || selectedCollege != null || selectedBranch != null || selectedGraduationYear != null)
                                contentColor
                            else
                                contentColor.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        }
        
        // Filter panel
        AnimatedVisibility(
            visible = isFilterExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FilterPanel(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                filterOptions = filterOptions,
                selectedCollege = selectedCollege,
                selectedBranch = selectedBranch,
                selectedGraduationYear = selectedGraduationYear,
                onCollegeSelected = onCollegeSelected,
                onBranchSelected = onBranchSelected,
                onYearSelected = onYearSelected,
                onClearFilters = onClearFilters
            )
        }
        
        // Active filter chips
        val activeFilters = listOfNotNull(
            selectedCollege?.let { "College: $it" to { onCollegeSelected(null) } },
            selectedBranch?.let { "Branch: $it" to { onBranchSelected(null) } },
            selectedGraduationYear?.let { "Year: $it" to { onYearSelected(null) } }
        )
        val displayedPeople = remember(people, normalizedQuery) {
            if (normalizedQuery.isBlank()) {
                people
            } else {
                people.filter { person ->
                    person.name.orEmpty().lowercase().contains(normalizedQuery) ||
                        person.username.orEmpty().lowercase().contains(normalizedQuery) ||
                        person.headline.orEmpty().lowercase().contains(normalizedQuery) ||
                        person.college.orEmpty().lowercase().contains(normalizedQuery) ||
                        person.branch.orEmpty().lowercase().contains(normalizedQuery) ||
                        person.skills.any { it.lowercase().contains(normalizedQuery) } ||
                        person.interests.any { it.lowercase().contains(normalizedQuery) }
                }
            }
        }
        if (activeFilters.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeFilters.forEach { (label, onRemove) ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicText(
                                label,
                                style = TextStyle(accentColor, 11.sp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = onRemove)
                                    .padding(2.dp)
                            ) {
                                BasicText("✕", style = TextStyle(accentColor, 10.sp))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        val showInitialLoading = isLoading && people.isEmpty()
        
        // People grid
        when {
            showInitialLoading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = 8, key = { "all_people_sk_$it" }) {
                        PersonCardSkeleton(listShimmerBrush, isLightTheme)
                    }
                }
            }
            error != null -> {
                ErrorState(
                    message = error,
                    backdrop = backdrop,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    reduceAnimations = reduceAnimations,
                    onRetry = onRetry,
                    onDismiss = onDismissError
                )
            }
            displayedPeople.isEmpty() -> {
                EmptyState(
                    iconRes = R.drawable.ic_users,
                    title = "No people found",
                    subtitle = if (normalizedQuery.isNotBlank() && isLoading) {
                        "Looking for matches across the network..."
                    } else if (normalizedQuery.isNotBlank()) {
                        "Try another name, username, college, or skill."
                    } else {
                        "Try adjusting your search or filters"
                    },
                    backdrop = backdrop,
                    contentColor = contentColor
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = displayedPeople, key = { it.id }) { person ->
                        PersonCard(
                            person = person,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            isLightTheme = isLightTheme,
                            isActionInProgress = connectionActionInProgress.contains(person.id),
                            onConnect = { onConnect(person.id) },
                            onCardClick = { onNavigateToProfile(person.id) },
                            reduceAnimations = reduceAnimations
                        )
                    }
                    
                    if (hasMore) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(displayedPeople.size, hasMore, normalizedQuery) {
                                onLoadMore()
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    filterOptions: com.kyant.backdrop.catalog.network.models.FilterOptions,
    selectedCollege: String?,
    selectedBranch: String?,
    selectedGraduationYear: Int?,
    onCollegeSelected: (String?) -> Unit,
    onBranchSelected: (String?) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dropdowns row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // College dropdown
            FilterDropdown(
                label = "College",
                selectedValue = selectedCollege,
                options = filterOptions.colleges,
                onSelected = onCollegeSelected,
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            
            // Branch dropdown  
            FilterDropdown(
                label = "Branch",
                selectedValue = selectedBranch,
                options = filterOptions.branches,
                onSelected = onBranchSelected,
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
            
            // Year dropdown
            FilterDropdown(
                label = "Year",
                selectedValue = selectedGraduationYear?.toString(),
                options = filterOptions.graduationYears.map { it.toString() },
                onSelected = { onYearSelected(it?.toIntOrNull()) },
                contentColor = contentColor,
                accentColor = accentColor,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Clear filters button
        if (selectedCollege != null || selectedBranch != null || selectedGraduationYear != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Red.copy(alpha = 0.15f))
                    .clickable(onClick = onClearFilters)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                BasicText(
                    "Clear All Filters",
                    style = TextStyle(Color.Red.copy(alpha = 0.8f), 12.sp)
                )
            }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
    contentColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(contentColor.copy(alpha = 0.08f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicText(
                selectedValue ?: label,
                style = TextStyle(
                    if (selectedValue != null) contentColor else contentColor.copy(alpha = 0.5f),
                    12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    BasicText(
                        "All $label",
                        style = TextStyle(contentColor, 14.sp)
                    )
                },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        BasicText(
                            option,
                            style = TextStyle(
                                if (option == selectedValue) accentColor else contentColor,
                                14.sp
                            )
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ForYouContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    PeopleGridContent(
        backdrop = backdrop,
        contentColor = contentColor,
        accentColor = accentColor,
        isGlassTheme = isGlassTheme,
        isLightTheme = isLightTheme,
        listShimmerBrush = listShimmerBrush,
        reduceAnimations = reduceAnimations,
        people = people,
        isLoading = isLoading,
        error = error,
        emptyIcon = R.drawable.ic_sparkles,
        emptyTitle = "No suggestions yet",
        emptySubtitle = "We'll suggest people based on your profile and interests",
        connectionActionInProgress = connectionActionInProgress,
        onConnect = onConnect,
        onNavigateToProfile = onNavigateToProfile,
        onRetry = onRetry,
        onDismissError = onDismissError
    )
}

@Composable
private fun SameCampusContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    userCollege: String?,
    isSavingCollege: Boolean,
    collegeSuggestions: List<CollegeInfo>,
    isSearchingColleges: Boolean,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onSaveCollege: (String) -> Unit,
    onCollegeSearch: (String) -> Unit,
    onDismissError: () -> Unit = {}
) {
    // If user doesn't have a college set, show the college input form
    if (userCollege == null && !isLoading) {
        CollegeInputForm(
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isSaving = isSavingCollege,
            error = error,
            collegeSuggestions = collegeSuggestions,
            isSearchingColleges = isSearchingColleges,
            onSaveCollege = onSaveCollege,
            onCollegeSearch = onCollegeSearch,
            isLightTheme = isLightTheme,
            reduceAnimations = reduceAnimations
        )
    } else {
        // Show results with college name header
        Column(Modifier.fillMaxSize()) {
            // Show college header if set
            if (userCollege != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .findPeopleGlassSurface(
                            backdrop = backdrop,
                            cornerDp = 12f,
                            blurDp = 12f,
                            isLightTheme = isLightTheme,
                            reduceAnimations = reduceAnimations,
                            onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicText("🎓", style = TextStyle(fontSize = 20.sp))
                        Column(Modifier.weight(1f)) {
                            BasicText(
                                userCollege,
                                style = TextStyle(contentColor, 14.sp, FontWeight.SemiBold)
                            )
                            BasicText(
                                "${people.size} campus mates found",
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                            )
                        }
                    }
                }
            }
            
            // Show people grid
                PeopleGridContent(
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                isGlassTheme = isGlassTheme,
                isLightTheme = isLightTheme,
                listShimmerBrush = listShimmerBrush,
                reduceAnimations = reduceAnimations,
                    people = people,
                    isLoading = isLoading,
                    error = error,
                    emptyIcon = R.drawable.ic_education,
                emptyTitle = "No campus mates found",
                emptySubtitle = "Be the first from your campus to join Vormex!",
                connectionActionInProgress = connectionActionInProgress,
                onConnect = onConnect,
                onNavigateToProfile = onNavigateToProfile,
                onRetry = onRetry,
                onDismissError = onDismissError
            )
        }
    }
}

@Composable
private fun CollegeInputForm(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isSaving: Boolean,
    error: String?,
    collegeSuggestions: List<CollegeInfo>,
    isSearchingColleges: Boolean,
    onSaveCollege: (String) -> Unit,
    onCollegeSearch: (String) -> Unit,
    isLightTheme: Boolean,
    reduceAnimations: Boolean
) {
    var collegeName by remember { mutableStateOf("") }
    val isValid = collegeName.trim().length >= 3
    var showSuggestions by remember { mutableStateOf(false) }
    
    // Trigger search when text changes
    LaunchedEffect(collegeName) {
        if (collegeName.length >= 2) {
            onCollegeSearch(collegeName)
            showSuggestions = true
        } else {
            showSuggestions = false
        }
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        BasicText("🎓", style = TextStyle(fontSize = 64.sp))
        
        Spacer(Modifier.height(24.dp))
        
        // Title
        BasicText(
            "Find your campus mates",
            style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Subtitle
        BasicText(
            "Enter your college/university name to discover people from the same campus",
            style = TextStyle(contentColor.copy(alpha = 0.7f), 14.sp),
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Input field with suggestions
        Box(Modifier.fillMaxWidth()) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .findPeopleGlassSurface(
                            backdrop = backdrop,
                            cornerDp = 12f,
                            blurDp = 12f,
                            isLightTheme = isLightTheme,
                            reduceAnimations = reduceAnimations,
                            onDrawSurfaceColor = Color.White.copy(alpha = 0.1f)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = collegeName,
                            onValueChange = { collegeName = it },
                            textStyle = TextStyle(contentColor, 16.sp),
                            cursorBrush = SolidColor(accentColor),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (collegeName.isEmpty()) {
                                        BasicText(
                                            "e.g. Stanford University, IIT Delhi",
                                            style = TextStyle(contentColor.copy(alpha = 0.4f), 16.sp)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (isSearchingColleges) {
                            Spacer(Modifier.width(8.dp))
                            CircularProgressIndicator(
                                color = contentColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                // Suggestions dropdown
                if (showSuggestions && collegeSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .findPeopleGlassSurface(
                                backdrop = backdrop,
                                cornerDp = 12f,
                                blurDp = 12f,
                                isLightTheme = isLightTheme,
                                reduceAnimations = reduceAnimations,
                                onDrawSurfaceColor = Color.White.copy(alpha = 0.15f)
                            )
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            collegeSuggestions.forEach { college ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            collegeName = college.name
                                            showSuggestions = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicText(
                                            college.name,
                                            style = TextStyle(contentColor, 14.sp)
                                        )
                                        BasicText(
                                            "${college.count} ${if (college.count == 1) "member" else "members"}",
                                            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Error message
        error?.let {
            Spacer(Modifier.height(8.dp))
            BasicText(
                it,
                style = TextStyle(Color.Red, 13.sp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Save button
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isValid && !isSaving) accentColor else accentColor.copy(alpha = 0.5f))
                .clickable(enabled = isValid && !isSaving) {
                    onSaveCollege(collegeName)
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                BasicText(
                    "Save & Find Campus Mates",
                    style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Hint
        BasicText(
            "You can change this later in your profile settings",
            style = TextStyle(contentColor.copy(alpha = 0.5f), 12.sp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeopleGridContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isGlassTheme: Boolean,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    people: List<PersonInfo>,
    isLoading: Boolean,
    error: String?,
    emptyIcon: Int,
    emptyTitle: String,
    emptySubtitle: String,
    connectionActionInProgress: Set<String>,
    onConnect: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRetry: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Reset refreshing state when loading completes
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }
    
    when {
        isLoading && !isRefreshing -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(count = 6, key = { "people_grid_sk_$it" }) {
                    PersonCardSkeleton(listShimmerBrush, isLightTheme)
                }
            }
        }
        error != null && people.isEmpty() -> {
            ErrorState(
                message = error,
                backdrop = backdrop,
                contentColor = contentColor,
                accentColor = accentColor,
                reduceAnimations = reduceAnimations,
                onRetry = onRetry,
                onDismiss = onDismissError
            )
        }
        people.isEmpty() && !isLoading -> {
            EmptyState(
                iconRes = emptyIcon,
                title = emptyTitle,
                subtitle = emptySubtitle,
                backdrop = backdrop,
                contentColor = contentColor
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    onRetry()
                },
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = people, key = { it.id }) { person ->
                        PersonCard(
                            person = person,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isGlassTheme = isGlassTheme,
                            isLightTheme = isLightTheme,
                            isActionInProgress = connectionActionInProgress.contains(person.id),
                            onConnect = { onConnect(person.id) },
                            onCardClick = { onNavigateToProfile(person.id) },
                            reduceAnimations = reduceAnimations
                        )
                    }
                }
            }
        }
    }
}

// ==================== Nearby Content ====================

@SuppressLint("MissingPermission")
@Composable
private fun NearbyContent(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    listShimmerBrush: Brush,
    reduceAnimations: Boolean,
    nearbyPeople: List<NearbyUser>,
    isLoading: Boolean,
    error: String?,
    currentLat: Double?,
    currentLng: Double?,
    currentCity: String?,
    selectedRadius: Int,
    hasLocationPermission: Boolean,
    onPermissionGranted: () -> Unit,
    onLocationUpdate: (Double, Double, Float?) -> Unit,
    onRadiusChange: (Int) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    
    // Helper function to get current location
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        // Try lastLocation first, if null request fresh location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationUpdate(location.latitude, location.longitude, location.accuracy)
            } else {
                // Request fresh location with high accuracy
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    5000L // 5 second interval
                ).setMaxUpdates(1).build()
                
                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        result.lastLocation?.let { loc ->
                            onLocationUpdate(loc.latitude, loc.longitude, loc.accuracy)
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
            }
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionGranted()
            fetchCurrentLocation()
        }
    }
    
    // Check permission when page loads
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            onPermissionGranted()
            fetchCurrentLocation()
        }
    }
    
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (!hasLocationPermission) {
            // Permission request UI (clean, no animations)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (reduceAnimations) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                        } else {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedRectangle(24f.dp) },
                                effects = {
                                    vibrancy()
                                    blur(16f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(
                                        Brush.verticalGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                }
                            )
                        }
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Static location icon
                    Box(contentAlignment = Alignment.Center) {
                        // Outer ring
                        Box(
                            Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.1f))
                        )
                        // Middle ring
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f))
                        )
                        // Inner circle with icon
                        Box(
                            Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("📍", style = TextStyle(fontSize = 28.sp))
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BasicText(
                        "Discover People Nearby",
                        style = TextStyle(contentColor, 22.sp, FontWeight.Bold)
                    )
                    BasicText(
                        "Enable location to find and connect with\npeople around you",
                        style = TextStyle(
                            contentColor.copy(alpha = 0.7f),
                            14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Enable button
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor)
                            .clickable { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            .padding(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicText("📍", style = TextStyle(fontSize = 16.sp))
                            BasicText(
                                "Enable Location",
                                style = TextStyle(Color.White, 15.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                    
                    BasicText(
                        "Your location is only visible to others\nwhen you choose to share it",
                        style = TextStyle(
                            contentColor.copy(alpha = 0.5f),
                            12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        } else {
            // Location header with city name
            Box(
                Modifier
                    .fillMaxWidth()
                    .findPeopleGlassSurface(
                        backdrop = backdrop,
                        cornerDp = 20f,
                        blurDp = 12f,
                        isLightTheme = isLightTheme,
                        reduceAnimations = reduceAnimations,
                        onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Static location indicator
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("📍", style = TextStyle(fontSize = 18.sp))
                            }
                            
                            Column {
                                BasicText(
                                    currentCity ?: "Your Location",
                                    style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
                                )
                                if (currentLat != null && currentLng != null) {
                                    BasicText(
                                        "${nearbyPeople.size} people within ${selectedRadius}km",
                                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp)
                                    )
                                }
                            }
                        }
                        
                        // Refresh button
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f))
                                .clickable { fetchCurrentLocation() }
                                .padding(10.dp)
                        ) {
                            BasicText("🔄", style = TextStyle(fontSize = 18.sp))
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Radius selector with animated selection
                    BasicText(
                        "Search Radius",
                        style = TextStyle(contentColor.copy(alpha = 0.6f), 12.sp, FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 25, 50, 100, 200).forEach { radius ->
                            val isSelected = selectedRadius == radius
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) 
                                            Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f)))
                                        else 
                                            Brush.horizontalGradient(listOf(contentColor.copy(alpha = 0.08f), contentColor.copy(alpha = 0.08f)))
                                    )
                                    .clickable { onRadiusChange(radius) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    "${radius}km",
                                    style = TextStyle(
                                        if (isSelected) Color.White else contentColor.copy(alpha = 0.7f),
                                        13.sp,
                                        FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Map View with OpenStreetMap
            if (currentLat != null && currentLng != null) {
                NearbyMapView(
                    currentLat = currentLat,
                    currentLng = currentLng,
                    nearbyPeople = nearbyPeople,
                    selectedRadius = selectedRadius,
                    backdrop = backdrop,
                    accentColor = accentColor,
                    isLightTheme = isLightTheme,
                    reduceAnimations = reduceAnimations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                
                Spacer(Modifier.height(16.dp))
            }
            
            // Nearby people list with animation
            when {
                isLoading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { index ->
                            AnimatedNearbyCardSkeleton(
                                backdrop = backdrop,
                                listShimmerBrush = listShimmerBrush,
                                isLightTheme = isLightTheme,
                                reduceAnimations = reduceAnimations,
                                delay = index * 100
                            )
                        }
                    }
                }
                error != null -> {
                    ErrorState(
                        message = error,
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        reduceAnimations = reduceAnimations,
                        onRetry = onRefresh
                    )
                }
                nearbyPeople.isEmpty() -> {
                    NearbyEmptyState(
                        backdrop = backdrop,
                        contentColor = contentColor,
                        accentColor = accentColor,
                        isLightTheme = isLightTheme,
                        reduceAnimations = reduceAnimations,
                        selectedRadius = selectedRadius
                    )
                }
                else -> {
                    // Section header with count badge
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            "People Near You",
                            style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            BasicText(
                                "${nearbyPeople.size} found",
                                style = TextStyle(accentColor, 12.sp, FontWeight.SemiBold)
                            )
                        }
                    }
                    
                    nearbyPeople.forEachIndexed { index, user ->
                        AnimatedNearbyPersonCard(
                            user = user,
                            backdrop = backdrop,
                            contentColor = contentColor,
                            accentColor = accentColor,
                            isLightTheme = isLightTheme,
                            index = index,
                            reduceAnimations = reduceAnimations,
                            onCardClick = { onNavigateToProfile(user.id) }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(80.dp))
    }
}

// Animated nearby card with stagger effect
@Composable
private fun AnimatedNearbyPersonCard(
    user: NearbyUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    index: Int,
    reduceAnimations: Boolean,
    onCardClick: () -> Unit
) {
    // Simple fade-in, no bouncing
    var visible by remember { mutableStateOf(reduceAnimations) }
    
    LaunchedEffect(Unit) {
        if (!reduceAnimations) {
            kotlinx.coroutines.delay(index * 50L)
        }
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(if (reduceAnimations) 0 else 200)
        )
    ) {
        NearbyPersonCard(
            user = user,
            backdrop = backdrop,
            contentColor = contentColor,
            accentColor = accentColor,
            isLightTheme = isLightTheme,
            reduceAnimations = reduceAnimations,
            onCardClick = onCardClick
        )
    }
}

// Empty state specific to nearby
@Composable
private fun NearbyEmptyState(
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    selectedRadius: Int
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 24f,
                blurDp = 12f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = Color.White.copy(alpha = 0.08f)
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BasicText("🌍", style = TextStyle(fontSize = 56.sp))
            BasicText(
                "No one nearby yet",
                style = TextStyle(contentColor, 18.sp, FontWeight.Bold)
            )
            BasicText(
                "Try increasing the search radius above ${selectedRadius}km\nor check back later",
                style = TextStyle(
                    contentColor.copy(alpha = 0.6f),
                    14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}

// Animated skeleton for loading
@Composable
private fun AnimatedNearbyCardSkeleton(
    backdrop: LayerBackdrop,
    listShimmerBrush: Brush,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    delay: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = if (reduceAnimations) 0 else delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val lineBrush = if (reduceAnimations) {
        listShimmerBrush
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.Gray.copy(alpha = 0.2f),
                Color.Gray.copy(alpha = 0.4f),
                Color.Gray.copy(alpha = 0.2f)
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 200f, 0f)
        )
    }
    
    Box(
        Modifier
            .fillMaxWidth()
            .height(90.dp)
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 16f,
                blurDp = 8f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar skeleton
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(lineBrush)
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(lineBrush)
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(lineBrush)
                )
            }
        }
    }
}

// ==================== Nearby Map View ====================

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NearbyMapView(
    currentLat: Double,
    currentLng: Double,
    nearbyPeople: List<NearbyUser>,
    selectedRadius: Int,
    backdrop: LayerBackdrop,
    accentColor: Color,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    modifier: Modifier = Modifier
) {
    val mapHtml = remember(currentLat, currentLng, nearbyPeople, selectedRadius) {
        buildMapHtml(currentLat, currentLng, nearbyPeople, selectedRadius)
    }
    
    Box(
        modifier
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 20f,
                blurDp = 10f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = Color.White.copy(alpha = 0.08f)
            )
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "https://unpkg.com/",
                    mapHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

private fun buildMapHtml(
    centerLat: Double,
    centerLng: Double,
    nearbyPeople: List<NearbyUser>,
    radiusKm: Int
): String {
    val markersJs = nearbyPeople.mapNotNull { user ->
        user.location?.let { loc ->
            val name = user.name ?: user.username ?: "User"
            val distance = String.format("%.1f", user.distance)
            """
            L.marker([${loc.lat}, ${loc.lng}], {icon: personIcon})
                .addTo(map)
                .bindPopup('<div style="text-align:center;"><b>$name</b><br/>${distance}km away</div>');
            """.trimIndent()
        }
    }.joinToString("\n")
    
    return """
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        body { margin: 0; padding: 0; background: transparent; }
        #map { width: 100%; height: 100vh; border-radius: 16px; }
        .leaflet-popup-content-wrapper {
            background: rgba(0, 0, 0, 0.8);
            color: white;
            border-radius: 12px;
        }
        .leaflet-popup-tip { background: rgba(0, 0, 0, 0.8); }
        .leaflet-popup-content { margin: 8px 12px; }
        .you-marker {
            background: #3B82F6;
            border: 3px solid white;
            border-radius: 50%;
            width: 20px;
            height: 20px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.3);
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var map = L.map('map', {
            zoomControl: false,
            attributionControl: false
        }).setView([$centerLat, $centerLng], 12);
        
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19
        }).addTo(map);
        
        // Your location marker
        var youIcon = L.divIcon({
            className: 'you-marker-wrapper',
            html: '<div class="you-marker"></div>',
            iconSize: [20, 20],
            iconAnchor: [10, 10]
        });
        
        L.marker([$centerLat, $centerLng], {icon: youIcon})
            .addTo(map)
            .bindPopup('<div style="text-align:center;"><b>You</b></div>');
        
        // Radius circle
        L.circle([$centerLat, $centerLng], {
            radius: ${radiusKm * 1000},
            color: '#3B82F6',
            fillColor: '#3B82F6',
            fillOpacity: 0.1,
            weight: 2,
            dashArray: '5, 5'
        }).addTo(map);
        
        // Person icon
        var personIcon = L.divIcon({
            className: 'person-marker',
            html: '<div style="background: #10B981; border: 2px solid white; border-radius: 50%; width: 16px; height: 16px; box-shadow: 0 2px 6px rgba(0,0,0,0.3);"></div>',
            iconSize: [16, 16],
            iconAnchor: [8, 8]
        });
        
        // Add nearby people markers
        $markersJs
    </script>
</body>
</html>
    """.trimIndent()
}

@Composable
private fun NearbyPersonCard(
    user: NearbyUser,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    isLightTheme: Boolean,
    reduceAnimations: Boolean,
    onCardClick: () -> Unit = {}
) {
    val imageCrossfadeMs = if (reduceAnimations) 0 else 300
    Box(
        Modifier
            .fillMaxWidth()
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 20f,
                blurDp = 12f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = if (isLightTheme) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f)
            )
            .clickable(onClick = onCardClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar with online indicator
            Box(contentAlignment = Alignment.Center) {
                // Outer glow for online users
                if (user.isOnline) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                    )
                }
                
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.profileImage.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.profileImage)
                                .crossfade(imageCrossfadeMs)
                                .build(),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val initials = (user.name ?: user.username ?: "U")
                            .split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                        BasicText(
                            initials,
                            style = TextStyle(Color.White, 18.sp, FontWeight.Bold)
                        )
                    }
                }
                
                // Online indicator dot
                if (user.isOnline) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(14.dp))
            
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicText(
                        user.name ?: user.username ?: "Unknown",
                        style = TextStyle(contentColor, 15.sp, FontWeight.SemiBold)
                    )
                    if (user.isOnline) {
                        BasicText(
                            "• Online",
                            style = TextStyle(Color(0xFF22C55E), 11.sp, FontWeight.Medium)
                        )
                    }
                }
                
                user.headline?.let { headline ->
                    BasicText(
                        headline,
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Location and interests row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    user.location?.city?.let { city ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BasicText("📍", style = TextStyle(fontSize = 10.sp))
                            BasicText(
                                city,
                                style = TextStyle(contentColor.copy(alpha = 0.5f), 11.sp)
                            )
                        }
                    }
                    
                    // Show first interest if available
                    if (user.interests.isNotEmpty()) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(contentColor.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            BasicText(
                                user.interests.first(),
                                style = TextStyle(contentColor.copy(alpha = 0.6f), 10.sp)
                            )
                        }
                    }
                }
            }
            
            // Distance badge with icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.15f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            formatDistance(user.distance),
                            style = TextStyle(accentColor, 13.sp, FontWeight.Bold)
                        )
                        BasicText(
                            "away",
                            style = TextStyle(accentColor.copy(alpha = 0.7f), 9.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyPersonCardSkeleton(
    backdrop: LayerBackdrop,
    shimmerBrush: Brush,
    isLightTheme: Boolean,
    reduceAnimations: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .findPeopleGlassSurface(
                backdrop = backdrop,
                cornerDp = 20f,
                blurDp = 12f,
                isLightTheme = isLightTheme,
                reduceAnimations = reduceAnimations,
                onDrawSurfaceColor = Color.White.copy(alpha = 0.1f)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(150.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            
            Box(
                Modifier
                    .width(50.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

private fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 1 -> "${(distanceKm * 1000).toInt()}m"
        distanceKm < 10 -> String.format("%.1fkm", distanceKm)
        else -> "${distanceKm.toInt()}km"
    }
}

// ==================== Common States ====================

@Composable
private fun EmptyState(
    iconRes: Int,
    title: String,
    subtitle: String,
    backdrop: LayerBackdrop,
    contentColor: Color
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(contentColor)
            )
            BasicText(
                title,
                style = TextStyle(contentColor, 16.sp, FontWeight.SemiBold)
            )
            BasicText(
                subtitle,
                style = TextStyle(contentColor.copy(alpha = 0.6f), 13.sp)
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    backdrop: LayerBackdrop,
    contentColor: Color,
    accentColor: Color,
    reduceAnimations: Boolean = false,
    onRetry: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (reduceAnimations) {
                    Modifier.background(Color.Red.copy(alpha = 0.12f))
                } else {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(16f.dp) },
                        effects = { blur(8f.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.Red.copy(alpha = 0.1f)) }
                    )
                }
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = "Error",
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(accentColor)
            )
            BasicText(
                message,
                style = TextStyle(contentColor, 14.sp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dismiss button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = 0.1f))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Dismiss",
                        style = TextStyle(contentColor.copy(alpha = 0.7f), 13.sp, FontWeight.Medium)
                    )
                }
                // Retry button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .clickable(onClick = onRetry)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BasicText(
                        "Retry",
                        style = TextStyle(accentColor, 13.sp, FontWeight.Medium)
                    )
                }
            }
        }
    }
}
