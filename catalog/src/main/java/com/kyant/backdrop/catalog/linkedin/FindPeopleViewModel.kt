package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.CollegeInfo
import com.kyant.backdrop.catalog.network.models.DailyMatchUser
import com.kyant.backdrop.catalog.network.models.FilterOptions
import com.kyant.backdrop.catalog.network.models.HiddenGemUser
import com.kyant.backdrop.catalog.network.models.LocationUpdateRequest
import com.kyant.backdrop.catalog.network.models.NearbyUser
import com.kyant.backdrop.catalog.network.models.NearbyUserLocation
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.ProfileUpdateRequest
import com.kyant.backdrop.catalog.network.models.SmartMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class FindPeopleTab {
    SMART_MATCHES,
    ALL_PEOPLE,
    FOR_YOU,
    SAME_CAMPUS,
    NEARBY
}

enum class SmartMatchFilter {
    ALL,
    SAME_CAMPUS,
    SAME_GOAL,
    FIND_MENTOR
}

data class FindPeopleUiState(
    val selectedTab: FindPeopleTab = FindPeopleTab.ALL_PEOPLE,
    
    // Smart Matches
    val smartMatches: List<SmartMatch> = emptyList(),
    val isLoadingSmartMatches: Boolean = false,
    val smartMatchError: String? = null,
    val smartMatchFilter: SmartMatchFilter = SmartMatchFilter.ALL,
    
    // All People
    val allPeople: List<PersonInfo> = emptyList(),
    val isLoadingAllPeople: Boolean = false,
    val allPeopleError: String? = null,
    val searchQuery: String = "",
    val allPeoplePage: Int = 1,
    val hasMoreAllPeople: Boolean = false,
    val totalPeopleCount: Int = 0,
    
    // Filters
    val filterOptions: FilterOptions = FilterOptions(),
    val selectedCollege: String? = null,
    val selectedBranch: String? = null,
    val selectedGraduationYear: Int? = null,
    val isFilterExpanded: Boolean = false,
    
    // For You
    val suggestions: List<PersonInfo> = emptyList(),
    val isLoadingSuggestions: Boolean = false,
    val suggestionsError: String? = null,
    
    // Same Campus
    val sameCampusPeople: List<PersonInfo> = emptyList(),
    val isLoadingSameCampus: Boolean = false,
    val sameCampusError: String? = null,
    val userCollege: String? = null,
    val isSavingCollege: Boolean = false,
    val collegeSuggestions: List<CollegeInfo> = emptyList(),
    val isSearchingColleges: Boolean = false,
    
    // Nearby
    val nearbyPeople: List<NearbyUser> = emptyList(),
    val isLoadingNearby: Boolean = false,
    val nearbyError: String? = null,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val currentCity: String? = null,
    val selectedRadius: Int = 50,
    val hasLocationPermission: Boolean = false,
    
    // Connection actions
    val connectionActionInProgress: Set<String> = emptySet(),
    
    // Connection celebration (Habit Loop: Reward)
    val showConnectionCelebration: Boolean = false,
    val celebrationRecipientName: String = "",
    val celebrationRecipientImage: String? = null,
    val celebrationReplyRate: Int = 75, // Mock reply rate - in production, fetch from API
    
    // Streak tracking (Duolingo Effect: Fear of loss)
    val connectionStreak: Int = 0,
    val isStreakAtRisk: Boolean = false,
    val isNewStreakMilestone: Boolean = false,
    val lastConnectionDate: String? = null,
    
    // Variable Rewards (Hook Model: The Slot Machine Trick)
    val dailyMatches: List<DailyMatchUser> = emptyList(),
    val dailyMatchCount: Int = 0,
    val surpriseMessage: String = "",
    val isLoadingDailyMatches: Boolean = false,
    val showDailyMatchesBanner: Boolean = false,
    
    // Hidden Gem (Weekly Surprise)
    val hiddenGem: HiddenGemUser? = null,
    val hiddenGemMessage: String = "",
    val isLoadingHiddenGem: Boolean = false,
    val showHiddenGemCard: Boolean = false,
    
    // Trending Status
    val isTrending: Boolean = false,
    val trendingRank: Int? = null,
    val trendingViewsToday: Int = 0,
    val trendingMessage: String? = null,
    val showTrendingBanner: Boolean = false
)

class FindPeopleViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FindPeopleUiState())
    val uiState: StateFlow<FindPeopleUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private var hasPrefetchedInitialData: Boolean = false
    
    init {
        loadFilterOptions()
        loadAllPeople(resetPage = true)
        loadStreakData()
        loadVariableRewards() // Variable rewards on screen load
    }

    fun prefetchInitialData() {
        if (hasPrefetchedInitialData) return
        hasPrefetchedInitialData = true

        if (_uiState.value.smartMatches.isEmpty() && !_uiState.value.isLoadingSmartMatches) {
            loadSmartMatches()
        }
        if (_uiState.value.allPeople.isEmpty() && !_uiState.value.isLoadingAllPeople) {
            loadAllPeople(resetPage = true)
        }
        if (_uiState.value.suggestions.isEmpty() && !_uiState.value.isLoadingSuggestions) {
            loadSuggestions()
        }
    }
    
    // ==================== Variable Rewards (Hook Model) ====================
    
    private fun loadVariableRewards() {
        // Load all variable rewards in parallel for faster UX
        loadDailyMatches()
        loadHiddenGem()
        loadTrendingStatus()
    }
    
    /**
     * Load daily matches with variable count (1-5).
     * Creates anticipation: "Will I get 1 or 5 matches today?"
     */
    private fun loadDailyMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingDailyMatches = true)
            
            ApiClient.getDailyMatches(context)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        dailyMatches = data.matches,
                        dailyMatchCount = data.matchCount,
                        surpriseMessage = data.surpriseMessage,
                        isLoadingDailyMatches = false,
                        showDailyMatchesBanner = data.matchCount > 0
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingDailyMatches = false,
                        showDailyMatchesBanner = false
                    )
                }
        }
    }
    
    /**
     * Load weekly hidden gem - a special match.
     * Creates scarcity: "This week's hidden gem!"
     */
    private fun loadHiddenGem() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHiddenGem = true)
            
            ApiClient.getHiddenGem(context)
                .onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        hiddenGem = data?.match,
                        hiddenGemMessage = data?.message ?: "",
                        isLoadingHiddenGem = false,
                        showHiddenGemCard = data?.match != null
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingHiddenGem = false,
                        showHiddenGemCard = false
                    )
                }
        }
    }
    
    /**
     * Check if user is trending today.
     * Creates excitement: "You're trending today!"
     */
    private fun loadTrendingStatus() {
        viewModelScope.launch {
            ApiClient.getTrendingStatus(context)
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(
                        isTrending = status.isTrending,
                        trendingRank = status.rank,
                        trendingViewsToday = status.viewsToday,
                        trendingMessage = status.message,
                        showTrendingBanner = status.isTrending
                    )
                }
        }
    }
    
    // Expose refresh methods for pull-to-refresh
    fun refreshDailyMatches() = loadDailyMatches()
    fun refreshHiddenGem() = loadHiddenGem()
    fun refreshTrendingStatus() = loadTrendingStatus()
    fun refreshAllVariableRewards() = loadVariableRewards()
    
    // Dismiss banners
    fun dismissDailyMatchesBanner() {
        _uiState.value = _uiState.value.copy(showDailyMatchesBanner = false)
    }
    
    fun dismissHiddenGemCard() {
        _uiState.value = _uiState.value.copy(showHiddenGemCard = false)
    }
    
    fun dismissTrendingBanner() {
        _uiState.value = _uiState.value.copy(showTrendingBanner = false)
    }
    
    // ==================== Streak Data (from Backend API) ====================
    
    private fun loadStreakData() {
        viewModelScope.launch {
            // Check if we're in 24-hour cooldown period (user dismissed or connected recently)
            val prefs = context.getSharedPreferences("vormex_find_people", Context.MODE_PRIVATE)
            val lastDismissTime = prefs.getLong("last_error_dismiss_time", 0)
            val cooldownHours = 24
            val isInCooldown = System.currentTimeMillis() - lastDismissTime < cooldownHours * 60 * 60 * 1000
            
            // Fetch streaks from backend API (same source as web)
            ApiClient.getStreaks(context)
                .onSuccess { streakData ->
                    // If in cooldown, don't show as at risk
                    val showAtRisk = streakData.isAtRisk.connection && !isInCooldown
                    
                    _uiState.value = _uiState.value.copy(
                        connectionStreak = streakData.connectionStreak,
                        isStreakAtRisk = showAtRisk,
                        lastConnectionDate = null // Backend handles this
                    )
                }
                .onFailure {
                    // Silently fail - streaks are not critical
                    println("Failed to load streaks: ${it.message}")
                }
        }
    }
    
    fun refreshStreaks() {
        loadStreakData()
    }
    
    /**
     * Dismiss all errors and start 24-hour cooldown.
     * Called when user swipes/dismisses error OR connects with someone.
     * Also hides the footer badge by setting isStreakAtRisk to false.
     */
    fun dismissErrorsWithCooldown() {
        // Save dismiss time for 24-hour cooldown
        val prefs = context.getSharedPreferences("vormex_find_people", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_error_dismiss_time", System.currentTimeMillis()).apply()
        
        // Clear errors and hide the at-risk badge
        _uiState.value = _uiState.value.copy(
            smartMatchError = null,
            allPeopleError = null,
            suggestionsError = null,
            sameCampusError = null,
            nearbyError = null,
            isStreakAtRisk = false // Hide footer badge too
        )
    }
    
    fun clearAllErrors() {
        _uiState.value = _uiState.value.copy(
            smartMatchError = null,
            allPeopleError = null,
            suggestionsError = null,
            sameCampusError = null,
            nearbyError = null
        )
    }
    
    private fun checkForNewMilestone(oldStreak: Int, newStreak: Int): Boolean {
        val milestones = listOf(3, 7, 14, 30)
        return milestones.contains(newStreak) && newStreak > oldStreak
    }
    
    fun selectTab(tab: FindPeopleTab) {
        // Clear errors when switching tabs for clean UX
        clearAllErrors()
        
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        
        when (tab) {
            FindPeopleTab.SMART_MATCHES -> {
                if (_uiState.value.smartMatches.isEmpty()) loadSmartMatches()
            }
            FindPeopleTab.ALL_PEOPLE -> {
                if (_uiState.value.allPeople.isEmpty()) loadAllPeople()
            }
            FindPeopleTab.FOR_YOU -> {
                if (_uiState.value.suggestions.isEmpty()) loadSuggestions()
            }
            FindPeopleTab.SAME_CAMPUS -> {
                if (_uiState.value.sameCampusPeople.isEmpty()) loadSameCampus()
            }
            FindPeopleTab.NEARBY -> {
                // Will be loaded when location is available
            }
        }
    }
    
    // ==================== Smart Matches ====================
    
    fun setSmartMatchFilter(filter: SmartMatchFilter) {
        _uiState.value = _uiState.value.copy(smartMatchFilter = filter)
        loadSmartMatches()
    }
    
    fun loadSmartMatches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSmartMatches = true, smartMatchError = null)
            
            val type = when (_uiState.value.smartMatchFilter) {
                SmartMatchFilter.ALL -> "all"
                SmartMatchFilter.SAME_CAMPUS -> "same_campus"
                SmartMatchFilter.SAME_GOAL -> "same_goal"
                SmartMatchFilter.FIND_MENTOR -> "mentor"
            }
            
            ApiClient.getSmartMatches(context, type)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        smartMatches = response.matches,
                        isLoadingSmartMatches = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSmartMatches = false,
                        smartMatchError = e.message ?: "Failed to load matches"
                    )
                }
        }
    }
    
    // ==================== All People ====================
    
    fun loadFilterOptions() {
        viewModelScope.launch {
            ApiClient.getFilterOptions(context)
                .onSuccess { options ->
                    _uiState.value = _uiState.value.copy(filterOptions = options)
                }
        }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadAllPeople(resetPage = true)
        }
    }
    
    fun setCollegeFilter(college: String?) {
        _uiState.value = _uiState.value.copy(selectedCollege = college)
        loadAllPeople(resetPage = true)
    }
    
    fun setBranchFilter(branch: String?) {
        _uiState.value = _uiState.value.copy(selectedBranch = branch)
        loadAllPeople(resetPage = true)
    }
    
    fun setGraduationYearFilter(year: Int?) {
        _uiState.value = _uiState.value.copy(selectedGraduationYear = year)
        loadAllPeople(resetPage = true)
    }
    
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCollege = null,
            selectedBranch = null,
            selectedGraduationYear = null
        )
        loadAllPeople(resetPage = true)
    }
    
    fun toggleFilterExpanded() {
        _uiState.value = _uiState.value.copy(isFilterExpanded = !_uiState.value.isFilterExpanded)
    }
    
    fun loadAllPeople(resetPage: Boolean = false) {
        viewModelScope.launch {
            val page = if (resetPage) 1 else _uiState.value.allPeoplePage
            
            _uiState.value = _uiState.value.copy(
                isLoadingAllPeople = true,
                allPeopleError = null,
                allPeoplePage = page
            )
            
            val state = _uiState.value
            ApiClient.getPeople(
                context = context,
                search = state.searchQuery.takeIf { it.isNotBlank() },
                college = state.selectedCollege,
                branch = state.selectedBranch,
                graduationYear = state.selectedGraduationYear,
                page = page
            )
                .onSuccess { response ->
                    val newPeople = if (resetPage) response.people else _uiState.value.allPeople + response.people
                    _uiState.value = _uiState.value.copy(
                        allPeople = newPeople,
                        isLoadingAllPeople = false,
                        hasMoreAllPeople = response.hasMore,
                        totalPeopleCount = response.total,
                        allPeoplePage = page
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingAllPeople = false,
                        allPeopleError = e.message ?: "Failed to load people"
                    )
                }
        }
    }
    
    fun loadMorePeople() {
        if (_uiState.value.isLoadingAllPeople || !_uiState.value.hasMoreAllPeople) return
        _uiState.value = _uiState.value.copy(allPeoplePage = _uiState.value.allPeoplePage + 1)
        loadAllPeople()
    }
    
    // ==================== Mock Data Helpers ====================
    
    private fun getMockPeople(): List<PersonInfo> {
        return MockData.mockPeople.map { mock ->
            PersonInfo(
                id = mock.id,
                username = mock.username,
                name = mock.name,
                profileImage = mock.profileImage,
                bannerImageUrl = mock.bannerImageUrl,
                headline = mock.headline,
                college = mock.college,
                branch = mock.branch,
                bio = mock.bio,
                skills = mock.skills,
                interests = mock.interests,
                isOnline = mock.isOnline,
                connectionStatus = mock.connectionStatus,
                mutualConnections = mock.mutualConnections
            )
        }
    }
    
    private fun getMockNearbyPeople(): List<NearbyUser> {
        return MockData.mockNearbyPeople.map { mock ->
            NearbyUser(
                id = mock.id,
                name = mock.name,
                username = mock.username,
                profileImage = mock.profileImage,
                bannerImage = mock.bannerImage,
                headline = mock.headline,
                skills = mock.skills,
                interests = mock.interests,
                distance = mock.distance,
                isOnline = mock.isOnline,
                location = NearbyUserLocation(
                    lat = mock.lat,
                    lng = mock.lng,
                    city = mock.city,
                    state = mock.state,
                    country = "India"
                )
            )
        }
    }
    
    // ==================== For You ====================
    
    fun loadSuggestions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSuggestions = true, suggestionsError = null)
            
            ApiClient.getPeopleSuggestions(context)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        suggestions = response.suggestions,
                        isLoadingSuggestions = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        suggestions = emptyList(),
                        isLoadingSuggestions = false,
                        suggestionsError = e.message ?: "Failed to load suggestions"
                    )
                }
        }
    }
    
    // ==================== Same Campus ====================
    
    fun loadSameCampus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSameCampus = true, sameCampusError = null)
            
            ApiClient.getSameCollegePeople(context, limit = 20)
                .onSuccess { response ->
                    android.util.Log.d("FindPeopleVM", "Same campus loaded: ${response.people.size} people, userCollege: ${response.userCollege}")
                    _uiState.value = _uiState.value.copy(
                        sameCampusPeople = response.people,
                        userCollege = response.userCollege,
                        isLoadingSameCampus = false
                    )
                }
                .onFailure { e ->
                    android.util.Log.e("FindPeopleVM", "Same campus error: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        sameCampusPeople = emptyList(),
                        isLoadingSameCampus = false,
                        sameCampusError = e.message ?: "Failed to load campus mates"
                    )
                }
        }
    }
    
    fun saveCollege(collegeName: String) {
        if (collegeName.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingCollege = true, sameCampusError = null)
            
            ApiClient.updateProfile(context, ProfileUpdateRequest(college = collegeName.trim()))
                .onSuccess {
                    android.util.Log.d("FindPeopleVM", "College saved: $collegeName")
                    _uiState.value = _uiState.value.copy(
                        userCollege = collegeName.trim(),
                        isSavingCollege = false,
                        collegeSuggestions = emptyList()
                    )
                    // Reload same campus people after saving college
                    loadSameCampus()
                }
                .onFailure { e ->
                    android.util.Log.e("FindPeopleVM", "Save college error: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        isSavingCollege = false,
                        sameCampusError = e.message ?: "Failed to save college"
                    )
                }
        }
    }
    
    private var searchCollegesJob: Job? = null
    
    fun searchColleges(query: String) {
        searchCollegesJob?.cancel()
        
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(collegeSuggestions = emptyList())
            return
        }
        
        searchCollegesJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.value = _uiState.value.copy(isSearchingColleges = true)
            
            ApiClient.searchColleges(context, query, limit = 10)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        collegeSuggestions = response.colleges,
                        isSearchingColleges = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        collegeSuggestions = emptyList(),
                        isSearchingColleges = false
                    )
                }
        }
    }
    
    fun clearCollegeSuggestions() {
        _uiState.value = _uiState.value.copy(collegeSuggestions = emptyList())
    }
    
    // ==================== Nearby ====================
    
    fun setLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
    }
    
    fun updateLocation(lat: Double, lng: Double, accuracy: Float?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentLat = lat, currentLng = lng)
            
            // Reverse geocode to get city/country
            val locationInfo = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var result: List<android.location.Address>? = null
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            result = addresses
                        }
                        // Wait briefly for callback
                        kotlinx.coroutines.delay(500)
                        result
                    } else {
                        geocoder.getFromLocation(lat, lng, 1)
                    }
                    
                    addresses?.firstOrNull()?.let { address ->
                        LocationInfo(
                            city = address.locality ?: address.subAdminArea,
                            state = address.adminArea,
                            country = address.countryName,
                            countryCode = address.countryCode
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FindPeopleVM", "Geocoding failed: ${e.message}")
                    null
                }
            }
            
            // Update location on server with geocoded info
            ApiClient.updateLocationWithDetails(
                context = context,
                lat = lat,
                lng = lng,
                accuracy = accuracy,
                city = locationInfo?.city,
                state = locationInfo?.state,
                country = locationInfo?.country,
                countryCode = locationInfo?.countryCode
            )
            
            // Load nearby people
            loadNearbyPeople()
        }
    }
    
    private data class LocationInfo(
        val city: String?,
        val state: String?,
        val country: String?,
        val countryCode: String?
    )
    
    fun setRadius(radius: Int) {
        _uiState.value = _uiState.value.copy(selectedRadius = radius)
        loadNearbyPeople()
    }
    
    fun loadNearbyPeople() {
        val lat = _uiState.value.currentLat ?: return
        val lng = _uiState.value.currentLng ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingNearby = true, nearbyError = null)
            
            ApiClient.getNearbyPeople(context, lat, lng, _uiState.value.selectedRadius)
                .onSuccess { response ->
                    android.util.Log.d("FindPeopleVM", "Nearby loaded: ${response.users.size} users, yourLocation: ${response.yourLocation}")
                    _uiState.value = _uiState.value.copy(
                        nearbyPeople = response.users,
                        currentCity = response.yourLocation?.city,
                        isLoadingNearby = false
                    )
                }
                .onFailure { e ->
                    android.util.Log.e("FindPeopleVM", "Nearby error: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(
                        nearbyPeople = emptyList(),
                        isLoadingNearby = false,
                        nearbyError = e.message ?: "Failed to load nearby users"
                    )
                }
        }
    }
    
    // ==================== Connection Actions ====================
    
    fun sendConnectionRequest(userId: String) {
        if (_uiState.value.connectionActionInProgress.contains(userId)) return
        
        // Find the person info to use in celebration
        val person = findPersonById(userId)
        val oldStreak = _uiState.value.connectionStreak
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress + userId
            )
            
            ApiClient.sendConnectionRequest(context, userId)
                .onSuccess {
                    // Clear all errors AND start 24-hour cooldown on successful connection
                    dismissErrorsWithCooldown()
                    
                    updatePersonConnectionStatus(userId, "pending_sent")
                    
                    // Refresh streaks from backend (backend tracks connection requests)
                    ApiClient.getStreaks(context)
                        .onSuccess { streakData ->
                            val isNewMilestone = checkForNewMilestone(oldStreak, streakData.connectionStreak)
                            _uiState.value = _uiState.value.copy(
                                connectionStreak = streakData.connectionStreak,
                                isStreakAtRisk = false,
                                isNewStreakMilestone = isNewMilestone
                            )
                        }
                    
                    // Trigger celebration (Habit Loop: Reward) 
                    // Variable reward: Random reply rate creates anticipation
                    val mockReplyRate = (60..95).random()
                    _uiState.value = _uiState.value.copy(
                        showConnectionCelebration = true,
                        celebrationRecipientName = person?.name ?: "User",
                        celebrationRecipientImage = person?.profileImage,
                        celebrationReplyRate = mockReplyRate
                    )
                }
                .onFailure {
                    // Handle error
                }
            
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress - userId
            )
        }
    }
    
    fun dismissConnectionCelebration() {
        _uiState.value = _uiState.value.copy(
            showConnectionCelebration = false,
            isNewStreakMilestone = false // Reset milestone flag
        )
    }
    
    private fun findPersonById(userId: String): PersonInfo? {
        // Search all lists for the person
        _uiState.value.allPeople.find { it.id == userId }?.let { return it }
        _uiState.value.suggestions.find { it.id == userId }?.let { return it }
        _uiState.value.sameCampusPeople.find { it.id == userId }?.let { return it }
        _uiState.value.smartMatches.find { it.user.id == userId }?.let { match ->
            // Convert SmartMatchUser to PersonInfo
            return PersonInfo(
                id = match.user.id,
                username = match.user.username,
                name = match.user.name,
                profileImage = match.user.profileImage,
                headline = match.user.headline,
                college = match.user.college,
                branch = match.user.branch,
                bio = match.user.bio,
                interests = match.user.interests,
                skills = match.user.skills,
                bannerImageUrl = null,
                isOnline = false,
                mutualConnections = 0,
                connectionStatus = "none"
            )
        }
        _uiState.value.nearbyPeople.find { it.id == userId }?.let {
            // Convert NearbyUser to PersonInfo
            return PersonInfo(
                id = it.id,
                username = it.username,
                name = it.name,
                profileImage = it.profileImage,
                headline = it.headline,
                college = it.location?.city, // Use city as college approximation
                branch = null,
                bio = null,
                interests = it.interests,
                skills = it.skills,
                bannerImageUrl = it.bannerImage,
                isOnline = it.isOnline,
                mutualConnections = 0,
                connectionStatus = "none"
            )
        }
        return null
    }
    
    fun cancelConnectionRequest(userId: String, connectionId: String) {
        if (_uiState.value.connectionActionInProgress.contains(userId)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress + userId
            )
            
            ApiClient.cancelConnectionRequest(context, connectionId)
                .onSuccess {
                    updatePersonConnectionStatus(userId, "none")
                }
            
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress - userId
            )
        }
    }
    
    fun acceptConnection(userId: String, connectionId: String) {
        if (_uiState.value.connectionActionInProgress.contains(userId)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress + userId
            )
            
            ApiClient.acceptConnection(context, connectionId)
                .onSuccess {
                    updatePersonConnectionStatus(userId, "connected")
                }
            
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress - userId
            )
        }
    }
    
    fun rejectConnection(userId: String, connectionId: String) {
        if (_uiState.value.connectionActionInProgress.contains(userId)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress + userId
            )
            
            ApiClient.rejectConnection(context, connectionId)
                .onSuccess {
                    updatePersonConnectionStatus(userId, "none")
                }
            
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress - userId
            )
        }
    }
    
    private fun updatePersonConnectionStatus(userId: String, status: String) {
        // Update in all lists
        _uiState.value = _uiState.value.copy(
            allPeople = _uiState.value.allPeople.map {
                if (it.id == userId) it.copy(connectionStatus = status) else it
            },
            suggestions = _uiState.value.suggestions.map {
                if (it.id == userId) it.copy(connectionStatus = status) else it
            },
            sameCampusPeople = _uiState.value.sameCampusPeople.map {
                if (it.id == userId) it.copy(connectionStatus = status) else it
            }
        )
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FindPeopleViewModel(context.applicationContext) as T
        }
    }
}

// Helper function to map primaryGoal to display text
fun mapPrimaryGoalToDisplay(goal: String?): String {
    return when (goal) {
        "learn_coding" -> "Coding & Tech"
        "web_dev" -> "Web Dev"
        "mobile_dev" -> "Mobile Dev"
        "ai_ml" -> "AI & ML"
        "competitive_programming" -> "Competitive Coding"
        "start_business" -> "Business"
        "get_internship" -> "Career"
        "design" -> "Design"
        "data_science" -> "Data Science"
        "cybersecurity" -> "Cybersecurity"
        "devops" -> "DevOps"
        "content_creation" -> "Content"
        "research" -> "Research"
        "freelance" -> "Freelancing"
        "sports_fitness" -> "Sports & Fitness"
        "music_arts" -> "Music & Arts"
        "photography" -> "Photography"
        else -> goal ?: "Unknown"
    }
}
