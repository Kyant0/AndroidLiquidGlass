package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.data.VormexPerformancePolicy
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.GrowthApiService
import com.kyant.backdrop.catalog.network.models.CollegeInfo
import com.kyant.backdrop.catalog.network.models.DailyMatchUser
import com.kyant.backdrop.catalog.network.models.FilterOptions
import com.kyant.backdrop.catalog.network.models.HiddenGemUser
import com.kyant.backdrop.catalog.network.models.LocationUpdateRequest
import com.kyant.backdrop.catalog.network.models.NearbyUser
import com.kyant.backdrop.catalog.network.models.NearbyUserLocation
import com.kyant.backdrop.catalog.network.models.PendingConnectionRequest
import com.kyant.backdrop.catalog.network.models.PersonInfo
import com.kyant.backdrop.catalog.network.models.PeopleYouKnowImportContact
import com.kyant.backdrop.catalog.network.models.PeopleYouKnowInvite
import com.kyant.backdrop.catalog.network.models.PeopleYouKnowResponse
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
    PEOPLE_YOU_KNOW,
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

    // People You Know
    val peopleYouKnowMatches: List<PersonInfo> = emptyList(),
    val peopleYouKnowInvites: List<PeopleYouKnowInvite> = emptyList(),
    val peopleYouKnowLastSyncedAt: String? = null,
    val peopleYouKnowStats: PeopleYouKnowStatsUi = PeopleYouKnowStatsUi(),
    val peopleYouKnowShareLink: String? = null,
    val isLoadingPeopleYouKnow: Boolean = false,
    val isDiscoveringPeopleYouKnow: Boolean = false,
    val isClearingPeopleYouKnow: Boolean = false,
    val peopleYouKnowError: String? = null,
    val isPeopleYouKnowGateVisible: Boolean = false,
    val peopleYouKnowInviteInProgress: Set<String> = emptySet(),

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

    // Pending connection requests
    val pendingConnectionRequests: List<PendingConnectionRequest> = emptyList(),
    val isLoadingPendingConnectionRequests: Boolean = false,
    val pendingConnectionRequestsError: String? = null,
    
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

data class PeopleYouKnowStatsUi(
    val totalContacts: Int = 0,
    val matchedCount: Int = 0,
    val inviteCount: Int = 0
)

class FindPeopleViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FindPeopleUiState())
    val uiState: StateFlow<FindPeopleUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val dataTtlMillis = VormexPerformancePolicy.FindPeopleDataTtlMillis
    private val searchResultsTtlMillis = VormexPerformancePolicy.SearchResultsTtlMillis
    private val nearbyTtlMillis = VormexPerformancePolicy.NearbyPeopleTtlMillis

    private var filterOptionsLoadedAt = 0L
    private var streakLoadedAt = 0L
    private var suggestionsLoadedAt = 0L
    private var sameCampusLoadedAt = 0L
    private var dailyMatchesLoadedAt = 0L
    private var hiddenGemLoadedAt = 0L
    private var trendingStatusLoadedAt = 0L
    private var nearbyLoadedAt = 0L
    private var peopleYouKnowLoadedAt = 0L
    private var pendingConnectionRequestsLoadedAt = 0L

    private var isFilterOptionsRequestInFlight = false
    private var isStreakRequestInFlight = false
    private var isSuggestionsRequestInFlight = false
    private var isSameCampusRequestInFlight = false
    private var isDailyMatchesRequestInFlight = false
    private var isHiddenGemRequestInFlight = false
    private var isTrendingStatusRequestInFlight = false
    private var isPeopleYouKnowRequestInFlight = false
    private var isPendingConnectionRequestsRequestInFlight = false

    private val smartMatchesCache = mutableMapOf<SmartMatchFilter, List<SmartMatch>>()
    private val smartMatchesLoadedAt = mutableMapOf<SmartMatchFilter, Long>()
    private val allPeopleCache = mutableMapOf<String, CachedPeopleResult>()
    private val smartMatchRequestsInFlight = mutableSetOf<SmartMatchFilter>()
    private val allPeopleRequestsInFlight = mutableSetOf<String>()
    private val nearbyRequestsInFlight = mutableSetOf<String>()
    private var lastNearbyKey: String? = null

    private data class CachedPeopleResult(
        val people: List<PersonInfo>,
        val total: Int,
        val hasMore: Boolean,
        val page: Int,
        val loadedAt: Long
    )
    
    init {
        ensureVariableRewardsLoaded()
    }

    fun prefetchInitialData(forceRefresh: Boolean = false) {
        ensureVariableRewardsLoaded(forceRefresh = forceRefresh)
        ensureFindSurfaceLoaded(forceRefresh = forceRefresh)
    }

    fun ensureFindSurfaceLoaded(forceRefresh: Boolean = false) {
        loadStreakData(forceRefresh = forceRefresh)
        loadPendingConnectionRequests(forceRefresh = forceRefresh)
        ensureTabDataLoaded(_uiState.value.selectedTab, forceRefresh = forceRefresh)
    }

    fun ensureTabDataLoaded(tab: FindPeopleTab, forceRefresh: Boolean = false) {
        when (tab) {
            FindPeopleTab.PEOPLE_YOU_KNOW -> loadPeopleYouKnow(forceRefresh = forceRefresh)
            FindPeopleTab.SMART_MATCHES -> loadSmartMatches(forceRefresh = forceRefresh)
            FindPeopleTab.ALL_PEOPLE -> {
                loadFilterOptions(forceRefresh = forceRefresh)
                loadAllPeople(resetPage = true, forceRefresh = forceRefresh)
            }
            FindPeopleTab.FOR_YOU -> loadSuggestions(forceRefresh = forceRefresh)
            FindPeopleTab.SAME_CAMPUS -> loadSameCampus(forceRefresh = forceRefresh)
            FindPeopleTab.NEARBY -> loadNearbyPeople(forceRefresh = forceRefresh)
        }
    }

    fun ensureVariableRewardsLoaded(forceRefresh: Boolean = false) {
        loadVariableRewards(forceRefresh = forceRefresh)
    }
    
    // ==================== Variable Rewards (Hook Model) ====================
    
    private fun loadVariableRewards(forceRefresh: Boolean = false) {
        // Load all variable rewards in parallel for faster UX
        loadDailyMatches(forceRefresh = forceRefresh)
        loadHiddenGem(forceRefresh = forceRefresh)
        loadTrendingStatus(forceRefresh = forceRefresh)
    }
    
    /**
     * Load daily matches with variable count (1-5).
     * Creates anticipation: "Will I get 1 or 5 matches today?"
     */
    private fun loadDailyMatches(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(dailyMatchesLoadedAt)) {
            return
        }
        if (isDailyMatchesRequestInFlight) return

        viewModelScope.launch {
            isDailyMatchesRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoadingDailyMatches = true)
            
            ApiClient.getDailyMatches(context)
                .onSuccess { data ->
                    dailyMatchesLoadedAt = System.currentTimeMillis()
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
            isDailyMatchesRequestInFlight = false
        }
    }
    
    /**
     * Load weekly hidden gem - a special match.
     * Creates scarcity: "This week's hidden gem!"
     */
    private fun loadHiddenGem(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(hiddenGemLoadedAt)) {
            return
        }
        if (isHiddenGemRequestInFlight) return

        viewModelScope.launch {
            isHiddenGemRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoadingHiddenGem = true)
            
            ApiClient.getHiddenGem(context)
                .onSuccess { data ->
                    hiddenGemLoadedAt = System.currentTimeMillis()
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
            isHiddenGemRequestInFlight = false
        }
    }
    
    /**
     * Check if user is trending today.
     * Creates excitement: "You're trending today!"
     */
    private fun loadTrendingStatus(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(trendingStatusLoadedAt)) {
            return
        }
        if (isTrendingStatusRequestInFlight) return

        viewModelScope.launch {
            isTrendingStatusRequestInFlight = true
            ApiClient.getTrendingStatus(context)
                .onSuccess { status ->
                    trendingStatusLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        isTrending = status.isTrending,
                        trendingRank = status.rank,
                        trendingViewsToday = status.viewsToday,
                        trendingMessage = status.message,
                        showTrendingBanner = status.isTrending
                    )
                }
            isTrendingStatusRequestInFlight = false
        }
    }
    
    // Expose refresh methods for pull-to-refresh
    fun refreshDailyMatches() = loadDailyMatches(forceRefresh = true)
    fun refreshHiddenGem() = loadHiddenGem(forceRefresh = true)
    fun refreshTrendingStatus() = loadTrendingStatus(forceRefresh = true)
    fun refreshAllVariableRewards() = loadVariableRewards(forceRefresh = true)
    
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

    fun loadPendingConnectionRequests(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(pendingConnectionRequestsLoadedAt)) {
            return
        }
        if (isPendingConnectionRequestsRequestInFlight) return

        viewModelScope.launch {
            isPendingConnectionRequestsRequestInFlight = true
            _uiState.value = _uiState.value.copy(
                isLoadingPendingConnectionRequests = true,
                pendingConnectionRequestsError = null
            )

            ApiClient.getPendingConnectionRequests(context)
                .onSuccess { response ->
                    pendingConnectionRequestsLoadedAt = System.currentTimeMillis()
                    val pendingRequests = response.connections
                    _uiState.value = _uiState.value.copy(
                        pendingConnectionRequests = pendingRequests,
                        isLoadingPendingConnectionRequests = false,
                        pendingConnectionRequestsError = null
                    )
                    syncPendingRequestStatuses(pendingRequests)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPendingConnectionRequests = false,
                        pendingConnectionRequestsError = error.message ?: "Failed to load connection requests"
                    )
                }

            isPendingConnectionRequestsRequestInFlight = false
        }
    }
    
    // ==================== Streak Data (from Backend API) ====================
    
    private fun loadStreakData(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(streakLoadedAt)) {
            return
        }
        if (isStreakRequestInFlight) return

        viewModelScope.launch {
            isStreakRequestInFlight = true
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
                    streakLoadedAt = System.currentTimeMillis()
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
            isStreakRequestInFlight = false
        }
    }
    
    fun refreshStreaks() {
        loadStreakData(forceRefresh = true)
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
            peopleYouKnowError = null,
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
            peopleYouKnowError = null,
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
        ensureTabDataLoaded(tab)
    }

    // ==================== People You Know ====================

    fun showPeopleYouKnowGate() {
        _uiState.value = _uiState.value.copy(
            isPeopleYouKnowGateVisible = true,
            peopleYouKnowError = null
        )
    }

    fun hidePeopleYouKnowGate() {
        _uiState.value = _uiState.value.copy(isPeopleYouKnowGateVisible = false)
    }

    fun loadPeopleYouKnow(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(peopleYouKnowLoadedAt)) {
            return
        }
        if (isPeopleYouKnowRequestInFlight) return

        viewModelScope.launch {
            isPeopleYouKnowRequestInFlight = true
            _uiState.value = _uiState.value.copy(
                isLoadingPeopleYouKnow = true,
                peopleYouKnowError = null
            )

            ApiClient.getPeopleYouKnow(context)
                .onSuccess { response ->
                    peopleYouKnowLoadedAt = System.currentTimeMillis()
                    applyPeopleYouKnowResponse(response)
                    if (response.stats.totalContacts > 0) {
                        loadPeopleYouKnowShareLink()
                    }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPeopleYouKnow = false,
                        peopleYouKnowError = error.message ?: "Failed to load people you know"
                    )
                }

            isPeopleYouKnowRequestInFlight = false
        }
    }

    fun discoverPeopleYouKnowFromDeviceContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDiscoveringPeopleYouKnow = true,
                peopleYouKnowError = null
            )

            val contacts = withContext(Dispatchers.IO) { readDeviceEmailContacts() }
            if (contacts.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isDiscoveringPeopleYouKnow = false,
                    peopleYouKnowError = "We couldn't find any email addresses in your contacts yet."
                )
                return@launch
            }

            submitPeopleYouKnowDiscovery(contacts = contacts, source = "picker")
        }
    }

    fun discoverPeopleYouKnowFromFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDiscoveringPeopleYouKnow = true,
                peopleYouKnowError = null
            )

            val contacts = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                    parseCsvContacts(reader.readText())
                }.orEmpty()
            }

            if (contacts.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isDiscoveringPeopleYouKnow = false,
                    peopleYouKnowError = "That file didn’t include any usable email contacts."
                )
                return@launch
            }

            submitPeopleYouKnowDiscovery(contacts = contacts, source = "file")
        }
    }

    fun clearPeopleYouKnow() {
        if (_uiState.value.isClearingPeopleYouKnow) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isClearingPeopleYouKnow = true,
                peopleYouKnowError = null
            )

            ApiClient.clearPeopleYouKnow(context)
                .onSuccess {
                    peopleYouKnowLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        peopleYouKnowMatches = emptyList(),
                        peopleYouKnowInvites = emptyList(),
                        peopleYouKnowLastSyncedAt = null,
                        peopleYouKnowStats = PeopleYouKnowStatsUi(),
                        peopleYouKnowShareLink = null,
                        isLoadingPeopleYouKnow = false,
                        isDiscoveringPeopleYouKnow = false,
                        isClearingPeopleYouKnow = false,
                        isPeopleYouKnowGateVisible = false,
                        peopleYouKnowInviteInProgress = emptySet()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isClearingPeopleYouKnow = false,
                        peopleYouKnowError = error.message ?: "Failed to clear your list"
                    )
                }
        }
    }

    fun markPeopleYouKnowInviteSent(entryId: String) {
        if (_uiState.value.peopleYouKnowInviteInProgress.contains(entryId)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                peopleYouKnowInviteInProgress = _uiState.value.peopleYouKnowInviteInProgress + entryId
            )

            ApiClient.markPeopleYouKnowInviteSent(context, entryId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        peopleYouKnowInvites = _uiState.value.peopleYouKnowInvites.map { invite ->
                            if (invite.id == entryId) invite.copy(invitedAt = response.invitedAt) else invite
                        }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        peopleYouKnowError = error.message ?: "Failed to update invite status"
                    )
                }

            _uiState.value = _uiState.value.copy(
                peopleYouKnowInviteInProgress = _uiState.value.peopleYouKnowInviteInProgress - entryId
            )
        }
    }

    private suspend fun submitPeopleYouKnowDiscovery(
        contacts: List<PeopleYouKnowImportContact>,
        source: String
    ) {
        ApiClient.discoverPeopleYouKnow(context, contacts = contacts, source = source)
            .onSuccess { response ->
                peopleYouKnowLoadedAt = System.currentTimeMillis()
                applyPeopleYouKnowResponse(response)
                loadPeopleYouKnowShareLink()
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDiscoveringPeopleYouKnow = false,
                    peopleYouKnowError = error.message ?: "Failed to find people you know"
                )
            }
    }

    private fun applyPeopleYouKnowResponse(response: PeopleYouKnowResponse) {
        _uiState.value = _uiState.value.copy(
            peopleYouKnowMatches = response.matched.distinctBy { it.id },
            peopleYouKnowInvites = response.invites.sortedWith(
                compareBy<PeopleYouKnowInvite> { if (it.invitedAt == null) 0 else 1 }
                    .thenBy { it.contactName.orEmpty().lowercase(Locale.getDefault()) }
            ),
            peopleYouKnowLastSyncedAt = response.lastSyncedAt,
            peopleYouKnowStats = PeopleYouKnowStatsUi(
                totalContacts = response.stats.totalContacts,
                matchedCount = response.stats.matchedCount,
                inviteCount = response.stats.inviteCount
            ),
            isLoadingPeopleYouKnow = false,
            isDiscoveringPeopleYouKnow = false,
            isClearingPeopleYouKnow = false,
            peopleYouKnowError = null,
            isPeopleYouKnowGateVisible = false
        )
    }

    private fun loadPeopleYouKnowShareLink() {
        if (!_uiState.value.peopleYouKnowShareLink.isNullOrBlank()) return

        viewModelScope.launch {
            GrowthApiService.getReferralShareLinks(context)
                .onSuccess { shareLinks ->
                    _uiState.value = _uiState.value.copy(
                        peopleYouKnowShareLink = shareLinks.link.ifBlank { null }
                    )
                }
        }
    }

    // ==================== Smart Matches ====================
    
    fun setSmartMatchFilter(filter: SmartMatchFilter) {
        _uiState.value = _uiState.value.copy(smartMatchFilter = filter)
        loadSmartMatches()
    }
    
    fun loadSmartMatches(forceRefresh: Boolean = false) {
        val filter = _uiState.value.smartMatchFilter
        val cached = smartMatchesCache[filter]
        if (!forceRefresh && cached != null && isFresh(smartMatchesLoadedAt[filter] ?: 0L)) {
            _uiState.value = _uiState.value.copy(
                smartMatches = cached,
                isLoadingSmartMatches = false,
                smartMatchError = null
            )
            return
        }
        if (!smartMatchRequestsInFlight.add(filter)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSmartMatches = true, smartMatchError = null)
            
            val type = when (filter) {
                SmartMatchFilter.ALL -> "all"
                SmartMatchFilter.SAME_CAMPUS -> "same_campus"
                SmartMatchFilter.SAME_GOAL -> "same_goal"
                SmartMatchFilter.FIND_MENTOR -> "mentor"
            }
            
            ApiClient.getSmartMatches(context, type)
                .onSuccess { response ->
                    val rankedMatches = rankSmartMatches(response.matches)
                    smartMatchesCache[filter] = rankedMatches
                    smartMatchesLoadedAt[filter] = System.currentTimeMillis()
                    if (_uiState.value.smartMatchFilter == filter) {
                        _uiState.value = _uiState.value.copy(
                            smartMatches = rankedMatches,
                            isLoadingSmartMatches = false
                        )
                    }
                }
                .onFailure { e ->
                    if (_uiState.value.smartMatchFilter == filter) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingSmartMatches = false,
                            smartMatchError = e.message ?: "Failed to load matches"
                        )
                    }
                }
            smartMatchRequestsInFlight.remove(filter)
        }
    }
    
    // ==================== All People ====================
    
    fun loadFilterOptions(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(filterOptionsLoadedAt)) {
            return
        }
        if (isFilterOptionsRequestInFlight) return

        viewModelScope.launch {
            isFilterOptionsRequestInFlight = true
            ApiClient.getFilterOptions(context)
                .onSuccess { options ->
                    filterOptionsLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(filterOptions = options)
                }
            isFilterOptionsRequestInFlight = false
        }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(VormexPerformancePolicy.SearchDebounceMillis)
            loadAllPeople(
                resetPage = true,
                forceRefresh = query.isNotBlank()
            )
        }
    }
    
    fun setCollegeFilter(college: String?) {
        _uiState.value = _uiState.value.copy(selectedCollege = college)
        loadAllPeople(resetPage = true, forceRefresh = true)
    }
    
    fun setBranchFilter(branch: String?) {
        _uiState.value = _uiState.value.copy(selectedBranch = branch)
        loadAllPeople(resetPage = true, forceRefresh = true)
    }
    
    fun setGraduationYearFilter(year: Int?) {
        _uiState.value = _uiState.value.copy(selectedGraduationYear = year)
        loadAllPeople(resetPage = true, forceRefresh = true)
    }
    
    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            selectedCollege = null,
            selectedBranch = null,
            selectedGraduationYear = null
        )
        loadAllPeople(resetPage = true, forceRefresh = true)
    }
    
    fun toggleFilterExpanded() {
        _uiState.value = _uiState.value.copy(isFilterExpanded = !_uiState.value.isFilterExpanded)
    }
    
    fun loadAllPeople(
        resetPage: Boolean = false,
        forceRefresh: Boolean = false,
        pageOverride: Int? = null
    ) {
        val state = _uiState.value
        val queryKey = buildAllPeopleQueryKey(state)
        val cached = allPeopleCache[queryKey]
        val previousPage = state.allPeoplePage

        if (resetPage && !forceRefresh && cached != null && isFresh(cached.loadedAt, searchResultsTtlMillis)) {
            _uiState.value = state.copy(
                allPeople = cached.people,
                isLoadingAllPeople = false,
                allPeopleError = null,
                hasMoreAllPeople = cached.hasMore,
                totalPeopleCount = cached.total,
                allPeoplePage = cached.page
            )
            return
        }
        if (!allPeopleRequestsInFlight.add(queryKey)) return

        viewModelScope.launch {
            val page = pageOverride ?: if (resetPage) 1 else state.allPeoplePage
            
            _uiState.value = _uiState.value.copy(
                isLoadingAllPeople = true,
                allPeopleError = null,
                allPeoplePage = page
            )
            
            val currentState = _uiState.value
            val search = currentState.searchQuery.trim().takeIf { it.isNotBlank() }
            val limit = when {
                search != null -> 30
                currentState.selectedCollege != null ||
                    currentState.selectedBranch != null ||
                    currentState.selectedGraduationYear != null -> 32
                else -> 40
            }
            ApiClient.getPeople(
                context = context,
                search = search,
                college = currentState.selectedCollege,
                branch = currentState.selectedBranch,
                graduationYear = currentState.selectedGraduationYear,
                page = page,
                limit = limit
            )
                .onSuccess { response ->
                    val rankedPeople = rankPeople(response.people)
                    val newPeople = if (resetPage) {
                        rankedPeople
                    } else {
                        mergePeople(_uiState.value.allPeople, rankedPeople)
                    }
                    val loadedAt = System.currentTimeMillis()
                    allPeopleCache[queryKey] = CachedPeopleResult(
                        people = newPeople,
                        total = response.total,
                        hasMore = response.hasMore,
                        page = page,
                        loadedAt = loadedAt
                    )
                    if (buildAllPeopleQueryKey(_uiState.value) == queryKey) {
                        _uiState.value = _uiState.value.copy(
                            allPeople = newPeople,
                            isLoadingAllPeople = false,
                            hasMoreAllPeople = response.hasMore,
                            totalPeopleCount = response.total,
                            allPeoplePage = page
                        )
                    }
                }
                .onFailure { e ->
                    if (buildAllPeopleQueryKey(_uiState.value) == queryKey) {
                        _uiState.value = _uiState.value.copy(
                            isLoadingAllPeople = false,
                            allPeopleError = e.message ?: "Failed to load people",
                            allPeoplePage = previousPage
                        )
                    }
                }
            allPeopleRequestsInFlight.remove(queryKey)
        }
    }
    
    fun loadMorePeople() {
        if (_uiState.value.isLoadingAllPeople || !_uiState.value.hasMoreAllPeople) return
        loadAllPeople(pageOverride = _uiState.value.allPeoplePage + 1)
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
    
    fun loadSuggestions(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(suggestionsLoadedAt)) {
            return
        }
        if (isSuggestionsRequestInFlight) return

        viewModelScope.launch {
            isSuggestionsRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoadingSuggestions = true, suggestionsError = null)
            
            ApiClient.getPeopleSuggestions(context)
                .onSuccess { response ->
                    suggestionsLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        suggestions = rankPeople(response.suggestions),
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
            isSuggestionsRequestInFlight = false
        }
    }
    
    // ==================== Same Campus ====================
    
    fun loadSameCampus(forceRefresh: Boolean = false) {
        if (!forceRefresh && isFresh(sameCampusLoadedAt)) {
            return
        }
        if (isSameCampusRequestInFlight) return

        viewModelScope.launch {
            isSameCampusRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoadingSameCampus = true, sameCampusError = null)
            
            ApiClient.getSameCollegePeople(context, limit = 20)
                .onSuccess { response ->
                    android.util.Log.d("FindPeopleVM", "Same campus loaded: ${response.people.size} people, userCollege: ${response.userCollege}")
                    sameCampusLoadedAt = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        sameCampusPeople = rankPeople(response.people),
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
            isSameCampusRequestInFlight = false
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
                    loadSameCampus(forceRefresh = true)
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
        if (_uiState.value.selectedRadius == radius) return
        _uiState.value = _uiState.value.copy(selectedRadius = radius)
        loadNearbyPeople(forceRefresh = true)
    }
    
    fun loadNearbyPeople(forceRefresh: Boolean = false) {
        val lat = _uiState.value.currentLat ?: return
        val lng = _uiState.value.currentLng ?: return
        val nearbyKey = buildNearbyKey(lat = lat, lng = lng, radius = _uiState.value.selectedRadius)
        if (
            !forceRefresh &&
            lastNearbyKey == nearbyKey &&
            isFresh(nearbyLoadedAt, nearbyTtlMillis)
        ) {
            return
        }
        if (!nearbyRequestsInFlight.add(nearbyKey)) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingNearby = true, nearbyError = null)
            
            ApiClient.getNearbyPeople(context, lat, lng, _uiState.value.selectedRadius)
                .onSuccess { response ->
                    android.util.Log.d("FindPeopleVM", "Nearby loaded: ${response.users.size} users, yourLocation: ${response.yourLocation}")
                    lastNearbyKey = nearbyKey
                    nearbyLoadedAt = System.currentTimeMillis()
                    if (buildNearbyKey(
                            lat = _uiState.value.currentLat ?: lat,
                            lng = _uiState.value.currentLng ?: lng,
                            radius = _uiState.value.selectedRadius
                        ) == nearbyKey
                    ) {
                        _uiState.value = _uiState.value.copy(
                            nearbyPeople = rankNearbyPeople(response.users),
                            currentCity = response.yourLocation?.city,
                            isLoadingNearby = false
                        )
                    }
                }
                .onFailure { e ->
                    android.util.Log.e("FindPeopleVM", "Nearby error: ${e.message}", e)
                    if (buildNearbyKey(
                            lat = _uiState.value.currentLat ?: lat,
                            lng = _uiState.value.currentLng ?: lng,
                            radius = _uiState.value.selectedRadius
                        ) == nearbyKey
                    ) {
                        _uiState.value = _uiState.value.copy(
                            nearbyPeople = emptyList(),
                            isLoadingNearby = false,
                            nearbyError = e.message ?: "Failed to load nearby users"
                        )
                    }
                }
            nearbyRequestsInFlight.remove(nearbyKey)
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
        _uiState.value.peopleYouKnowMatches.find { it.id == userId }?.let { return it }
        _uiState.value.allPeople.find { it.id == userId }?.let { return it }
        _uiState.value.suggestions.find { it.id == userId }?.let { return it }
        _uiState.value.sameCampusPeople.find { it.id == userId }?.let { return it }
        _uiState.value.pendingConnectionRequests.find { it.user.id == userId }?.let {
            return PersonInfo(
                id = it.user.id,
                username = it.user.username,
                name = it.user.name,
                profileImage = it.user.profileImage,
                headline = it.user.headline,
                college = it.user.college,
                branch = null,
                bio = null,
                interests = emptyList(),
                skills = emptyList(),
                bannerImageUrl = null,
                isOnline = false,
                mutualConnections = 0,
                connectionStatus = "pending_received"
            )
        }
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
                    removePendingConnectionRequest(connectionId)
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
                    removePendingConnectionRequest(connectionId)
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
                    removePendingConnectionRequest(connectionId)
                }
            
            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress - userId
            )
        }
    }
    
    private fun updatePersonConnectionStatus(userId: String, status: String) {
        // Update in all lists
        _uiState.value = _uiState.value.copy(
            peopleYouKnowMatches = _uiState.value.peopleYouKnowMatches.map {
                if (it.id == userId) it.copy(connectionStatus = status) else it
            },
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

    private fun removePendingConnectionRequest(connectionId: String) {
        _uiState.value = _uiState.value.copy(
            pendingConnectionRequests = _uiState.value.pendingConnectionRequests.filterNot { it.id == connectionId },
            pendingConnectionRequestsError = null
        )
    }

    private fun syncPendingRequestStatuses(requests: List<PendingConnectionRequest>) {
        val pendingUserIds = requests.map { it.user.id }.toSet()
        _uiState.value = _uiState.value.copy(
            peopleYouKnowMatches = _uiState.value.peopleYouKnowMatches.map {
                when {
                    pendingUserIds.contains(it.id) -> it.copy(connectionStatus = "pending_received")
                    it.connectionStatus == "pending_received" -> it.copy(connectionStatus = "none")
                    else -> it
                }
            },
            allPeople = _uiState.value.allPeople.map {
                when {
                    pendingUserIds.contains(it.id) -> it.copy(connectionStatus = "pending_received")
                    it.connectionStatus == "pending_received" -> it.copy(connectionStatus = "none")
                    else -> it
                }
            },
            suggestions = _uiState.value.suggestions.map {
                when {
                    pendingUserIds.contains(it.id) -> it.copy(connectionStatus = "pending_received")
                    it.connectionStatus == "pending_received" -> it.copy(connectionStatus = "none")
                    else -> it
                }
            },
            sameCampusPeople = _uiState.value.sameCampusPeople.map {
                when {
                    pendingUserIds.contains(it.id) -> it.copy(connectionStatus = "pending_received")
                    it.connectionStatus == "pending_received" -> it.copy(connectionStatus = "none")
                    else -> it
                }
            }
        )
    }

    private fun readDeviceEmailContacts(): List<PeopleYouKnowImportContact> {
        val contactsByEmail = linkedMapOf<String, PeopleYouKnowImportContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            if (emailIndex == -1) {
                return emptyList()
            }

            while (cursor.moveToNext()) {
                val rawEmail = cursor.getString(emailIndex)?.trim()?.lowercase(Locale.getDefault())
                if (rawEmail.isNullOrBlank()) continue

                val contactName = if (nameIndex >= 0) {
                    cursor.getString(nameIndex)?.trim()?.takeIf { it.isNotEmpty() }
                } else {
                    null
                }
                val existing = contactsByEmail[rawEmail]
                if (existing == null || (existing.name.isNullOrBlank() && !contactName.isNullOrBlank())) {
                    contactsByEmail[rawEmail] = PeopleYouKnowImportContact(
                        name = contactName,
                        email = rawEmail
                    )
                }
            }
        }

        return contactsByEmail.values.toList()
    }

    private fun parseCsvContacts(csvText: String): List<PeopleYouKnowImportContact> {
        val lines = csvText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return emptyList()

        val headers = parseCsvLine(lines.first())
        val emailIndex = headers.indexOfFirst { header ->
            val normalized = header.trim().lowercase(Locale.getDefault())
            normalized == "email" || normalized.contains("email")
        }
        if (emailIndex == -1) return emptyList()

        val nameIndex = headers.indexOfFirst { header ->
            val normalized = header.trim().lowercase(Locale.getDefault())
            normalized == "name" || normalized == "full name" || normalized.contains("name")
        }

        val contactsByEmail = linkedMapOf<String, PeopleYouKnowImportContact>()
        lines.drop(1).forEach { line ->
            val columns = parseCsvLine(line)
            if (emailIndex >= columns.size) return@forEach

            val email = columns[emailIndex].trim().lowercase(Locale.getDefault())
            if (email.isBlank()) return@forEach

            val name = columns.getOrNull(nameIndex)?.trim()?.takeIf { it.isNotEmpty() }
            val existing = contactsByEmail[email]
            if (existing == null || (existing.name.isNullOrBlank() && !name.isNullOrBlank())) {
                contactsByEmail[email] = PeopleYouKnowImportContact(name = name, email = email)
            }
        }

        return contactsByEmail.values.toList()
    }

    private fun parseCsvLine(line: String): List<String> {
        if (line.isBlank()) return emptyList()

        val columns = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' -> {
                    val nextIsQuote = index + 1 < line.length && line[index + 1] == '"'
                    if (inQuotes && nextIsQuote) {
                        current.append('"')
                        index += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    columns += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index += 1
        }

        columns += current.toString()
        return columns
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FindPeopleViewModel(context.applicationContext) as T
        }
    }

    private fun isFresh(loadedAt: Long, ttlMillis: Long = dataTtlMillis): Boolean {
        return loadedAt != 0L && System.currentTimeMillis() - loadedAt < ttlMillis
    }

    private fun buildAllPeopleQueryKey(state: FindPeopleUiState): String {
        return listOf(
            state.searchQuery.trim().lowercase(Locale.getDefault()),
            state.selectedCollege.orEmpty(),
            state.selectedBranch.orEmpty(),
            state.selectedGraduationYear?.toString().orEmpty()
        ).joinToString("|")
    }

    private fun buildNearbyKey(lat: Double, lng: Double, radius: Int): String {
        return String.format(Locale.US, "%.3f|%.3f|%d", lat, lng, radius)
    }

    private fun rankSmartMatches(matches: List<SmartMatch>): List<SmartMatch> {
        return matches
            .distinctBy { it.user.id }
            .sortedByDescending { match ->
                (match.matchPercentage * 1000) +
                    (match.user.stats?.xp ?: 0) * 2 +
                    match.user.skills.size * 24 +
                    match.user.interests.size * 20 +
                    match.tags.size * 18 +
                    match.reasons.size * 14 +
                    if (match.user.githubConnected) 40 else 0 +
                    if (!match.user.headline.isNullOrBlank()) 25 else 0 +
                    if (!match.user.bio.isNullOrBlank()) 15 else 0
            }
    }

    private fun rankPeople(people: List<PersonInfo>): List<PersonInfo> {
        return people
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<PersonInfo> { connectionPriority(it.connectionStatus) }
                    .thenByDescending { if (it.isOnline) 1 else 0 }
                    .thenByDescending { it.mutualConnections }
                    .thenByDescending { profileStrength(it) }
                    .thenBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
            )
    }

    private fun rankNearbyPeople(people: List<NearbyUser>): List<NearbyUser> {
        return people
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<NearbyUser> { if (it.isOnline) 1 else 0 }
                    .thenBy { it.distance }
                    .thenByDescending { it.skills.size + it.interests.size }
                    .thenBy { it.name.orEmpty().lowercase(Locale.getDefault()) }
            )
    }

    private fun mergePeople(existing: List<PersonInfo>, incoming: List<PersonInfo>): List<PersonInfo> {
        val merged = LinkedHashMap<String, PersonInfo>()
        existing.forEach { merged[it.id] = it }
        incoming.forEach { merged[it.id] = it }
        return merged.values.toList()
    }

    private fun connectionPriority(status: String): Int {
        return when (status) {
            "none" -> 4
            "pending_received" -> 3
            "pending_sent" -> 2
            "connected" -> 1
            else -> 0
        }
    }

    private fun profileStrength(person: PersonInfo): Int {
        return person.skills.size * 6 +
            person.interests.size * 5 +
            person.mutualConnections * 3 +
            if (!person.headline.isNullOrBlank()) 10 else 0 +
            if (!person.bio.isNullOrBlank()) 8 else 0 +
            if (!person.college.isNullOrBlank()) 6 else 0 +
            if (!person.branch.isNullOrBlank()) 4 else 0
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
