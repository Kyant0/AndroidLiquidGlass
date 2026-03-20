package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.PostsApiService
import com.kyant.backdrop.catalog.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    // Profile data
    val profile: FullProfileResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Relationship status
    val connectionStatus: String = "none", // none, pending_sent, pending_received, connected
    val connectionId: String? = null,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    
    // Mutual info
    val mutualConnections: List<MutualConnection> = emptyList(),
    val mutualFollowers: List<MutualConnection> = emptyList(),
    val mutualConnectionsCount: Int = 0,
    val mutualFollowersCount: Int = 0,
    
    // Activity heatmap
    val activityHeatmap: List<ActivityHeatmapDay> = emptyList(),
    val activityStats: ActivityHeatmapStats? = null,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    
    // Feed
    val feedItems: List<FeedItem> = emptyList(),
    val feedFilter: String = "all", // all, posts, articles, forum, videos
    val feedPage: Int = 1,
    val feedHasMore: Boolean = false,
    val isLoadingFeed: Boolean = false,
    
    // Is current user
    val isOwner: Boolean = false,
    val currentUserId: String? = null,
    
    // Action in progress
    val connectionActionInProgress: Boolean = false,
    val followActionInProgress: Boolean = false,
    
    // Edit mode
    val isEditingBio: Boolean = false,
    val editedBio: String = "",
    
    // Avatar/Banner upload
    val isUploadingAvatar: Boolean = false,
    val isUploadingBanner: Boolean = false,
    val uploadError: String? = null
)

class ProfileViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private var targetUserId: String? = null
    private var lastLoadedUserId: String? = null
    private var lastLoadedAtMillis: Long = 0L
    private var isProfileRequestInFlight: Boolean = false
    private val cacheTtlMillis: Long = 5 * 60 * 1000L
    
    fun loadProfile(userId: String? = null, forceRefresh: Boolean = false) {
        val effectiveUserId = userId ?: "me"
        targetUserId = effectiveUserId

        val now = System.currentTimeMillis()
        val isCacheFresh =
            !forceRefresh &&
            _uiState.value.profile != null &&
            lastLoadedUserId == effectiveUserId &&
            (now - lastLoadedAtMillis) < cacheTtlMillis

        if (isCacheFresh) {
            return
        }

        if (isProfileRequestInFlight && lastLoadedUserId == effectiveUserId && !forceRefresh) {
            return
        }
        
        viewModelScope.launch {
            isProfileRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Get current user ID
            val currentUserId = ApiClient.getCurrentUserId(context)
            val isOwner = effectiveUserId == "me" || effectiveUserId == currentUserId
            _uiState.value = _uiState.value.copy(
                isOwner = isOwner,
                currentUserId = currentUserId
            )
            
            // Load profile
            ApiClient.getProfile(context, effectiveUserId)
                .onSuccess { profile ->
                    lastLoadedUserId = effectiveUserId
                    lastLoadedAtMillis = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        profile = profile,
                        isLoading = false,
                        activityHeatmap = profile.activityHeatmap,
                        feedItems = profile.recentActivity.items,
                        feedHasMore = profile.recentActivity.hasMore
                    )
                    
                    // Load relationship status for non-owners
                    if (!isOwner && currentUserId != null) {
                        loadRelationshipStatus(profile.user.id)
                    }
                    
                    // Load activity years
                    loadActivityYears(profile.user.id)

                    isProfileRequestInFlight = false
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                    isProfileRequestInFlight = false
                }
        }
    }

    fun prefetchOwnProfile() {
        loadProfile(userId = null, forceRefresh = false)
    }
    
    private fun loadRelationshipStatus(userId: String) {
        viewModelScope.launch {
            // Get connection status
            ApiClient.getConnectionStatus(context, userId)
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = status.status,
                        connectionId = status.connectionId
                    )
                }
            
            // Get follow status
            ApiClient.getFollowStatus(context, userId)
                .onSuccess { status ->
                    _uiState.value = _uiState.value.copy(
                        isFollowing = status.isFollowing,
                        isFollowedBy = status.isFollowedBy
                    )
                }
            
            // Get mutual info
            ApiClient.getMutualInfo(context, userId)
                .onSuccess { info ->
                    _uiState.value = _uiState.value.copy(
                        mutualConnections = info.mutualConnections,
                        mutualFollowers = info.mutualFollowers,
                        mutualConnectionsCount = info.mutualConnectionsCount,
                        mutualFollowersCount = info.mutualFollowersCount
                    )
                }
        }
    }
    
    private fun loadActivityYears(userId: String) {
        viewModelScope.launch {
            ApiClient.getActivityYears(context, userId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        availableYears = response.years
                    )
                }
        }
    }
    
    fun loadActivityForYear(year: Int) {
        val userId = targetUserId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedYear = year)
            
            ApiClient.getActivityHeatmap(context, userId, year)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        activityHeatmap = response.days,
                        activityStats = response.stats
                    )
                }
        }
    }
    
    fun setFeedFilter(filter: String) {
        val userId = targetUserId ?: return
        
        _uiState.value = _uiState.value.copy(
            feedFilter = filter,
            feedPage = 1,
            feedItems = emptyList(),
            isLoadingFeed = true
        )
        
        viewModelScope.launch {
            ApiClient.getProfileFeed(context, userId, page = 1, filter = filter)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        feedItems = response.items,
                        feedHasMore = response.hasMore,
                        isLoadingFeed = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingFeed = false)
                }
        }
    }
    
    fun loadMoreFeed() {
        val userId = targetUserId ?: return
        if (_uiState.value.isLoadingFeed || !_uiState.value.feedHasMore) return
        
        val nextPage = _uiState.value.feedPage + 1
        _uiState.value = _uiState.value.copy(isLoadingFeed = true)
        
        viewModelScope.launch {
            ApiClient.getProfileFeed(
                context, 
                userId, 
                page = nextPage, 
                filter = _uiState.value.feedFilter
            )
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        feedItems = _uiState.value.feedItems + response.items,
                        feedPage = nextPage,
                        feedHasMore = response.hasMore,
                        isLoadingFeed = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingFeed = false)
                }
        }
    }

    fun deleteFeedPost(postId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            PostsApiService.deletePost(context, postId)
                .onSuccess {
                    val currentProfile = _uiState.value.profile
                    val deletedItem = _uiState.value.feedItems.find { it.id == postId }
                    val updatedFeedItems = _uiState.value.feedItems.filterNot { it.id == postId }

                    val updatedProfile = currentProfile?.let { profile ->
                        val updatedStats = when (deletedItem?.contentType) {
                            "article" -> profile.stats.copy(totalArticles = (profile.stats.totalArticles - 1).coerceAtLeast(0))
                            "short_video" -> profile.stats.copy(totalShortVideos = (profile.stats.totalShortVideos - 1).coerceAtLeast(0))
                            else -> profile.stats.copy(totalPosts = (profile.stats.totalPosts - 1).coerceAtLeast(0))
                        }

                        profile.copy(
                            stats = updatedStats,
                            recentActivity = profile.recentActivity.copy(
                                items = updatedFeedItems,
                                totalCount = (profile.recentActivity.totalCount - 1).coerceAtLeast(0),
                                hasMore = profile.recentActivity.hasMore
                            )
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        profile = updatedProfile,
                        feedItems = updatedFeedItems
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to delete post")
                }
        }
    }
    
    // ==================== Connection Actions ====================
    
    fun sendConnectionRequest() {
        val userId = _uiState.value.profile?.user?.id ?: return
        if (_uiState.value.connectionActionInProgress) return
        
        _uiState.value = _uiState.value.copy(connectionActionInProgress = true)
        
        viewModelScope.launch {
            ApiClient.sendConnectionRequest(context, userId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "pending_sent",
                        connectionId = response.connectionId,
                        connectionActionInProgress = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(connectionActionInProgress = false)
                }
        }
    }
    
    fun cancelConnectionRequest() {
        val connectionId = _uiState.value.connectionId ?: return
        if (_uiState.value.connectionActionInProgress) return
        
        _uiState.value = _uiState.value.copy(connectionActionInProgress = true)
        
        viewModelScope.launch {
            ApiClient.cancelConnectionRequest(context, connectionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "none",
                        connectionId = null,
                        connectionActionInProgress = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(connectionActionInProgress = false)
                }
        }
    }
    
    fun acceptConnectionRequest() {
        val connectionId = _uiState.value.connectionId ?: return
        if (_uiState.value.connectionActionInProgress) return
        
        _uiState.value = _uiState.value.copy(connectionActionInProgress = true)
        
        viewModelScope.launch {
            ApiClient.acceptConnection(context, connectionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "connected",
                        connectionActionInProgress = false
                    )
                    // Refresh profile to update connection count
                    targetUserId?.let { loadProfile(it) }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(connectionActionInProgress = false)
                }
        }
    }
    
    fun rejectConnectionRequest() {
        val connectionId = _uiState.value.connectionId ?: return
        if (_uiState.value.connectionActionInProgress) return
        
        _uiState.value = _uiState.value.copy(connectionActionInProgress = true)
        
        viewModelScope.launch {
            ApiClient.rejectConnection(context, connectionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "none",
                        connectionId = null,
                        connectionActionInProgress = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(connectionActionInProgress = false)
                }
        }
    }
    
    fun removeConnection() {
        val connectionId = _uiState.value.connectionId ?: return
        if (_uiState.value.connectionActionInProgress) return
        
        _uiState.value = _uiState.value.copy(connectionActionInProgress = true)
        
        viewModelScope.launch {
            ApiClient.removeConnection(context, connectionId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        connectionStatus = "none",
                        connectionId = null,
                        connectionActionInProgress = false
                    )
                    // Refresh profile to update connection count
                    targetUserId?.let { loadProfile(it) }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(connectionActionInProgress = false)
                }
        }
    }
    
    // ==================== Follow Actions ====================
    
    fun toggleFollow() {
        val userId = _uiState.value.profile?.user?.id ?: return
        if (_uiState.value.followActionInProgress) return
        
        _uiState.value = _uiState.value.copy(followActionInProgress = true)
        
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                ApiClient.unfollowUser(context, userId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isFollowing = false,
                            followActionInProgress = false
                        )
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(followActionInProgress = false)
                    }
            } else {
                ApiClient.followUser(context, userId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isFollowing = true,
                            followActionInProgress = false
                        )
                    }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(followActionInProgress = false)
                    }
            }
        }
    }
    
    // ==================== Profile Edit Actions ====================
    
    fun startEditingBio() {
        _uiState.value = _uiState.value.copy(
            isEditingBio = true,
            editedBio = _uiState.value.profile?.user?.bio ?: ""
        )
    }
    
    fun cancelEditingBio() {
        _uiState.value = _uiState.value.copy(isEditingBio = false)
    }
    
    fun updateEditedBio(bio: String) {
        _uiState.value = _uiState.value.copy(editedBio = bio)
    }
    
    fun saveEditedBio() {
        val newBio = _uiState.value.editedBio
        
        viewModelScope.launch {
            ApiClient.updateProfile(context, ProfileUpdateRequest(bio = newBio))
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isEditingBio = false,
                        profile = _uiState.value.profile?.copy(
                            user = _uiState.value.profile!!.user.copy(bio = newBio)
                        )
                    )
                }
                .onFailure {
                    // Handle error
                }
        }
    }
    
    fun updateOpenToOpportunities(isOpen: Boolean) {
        viewModelScope.launch {
            ApiClient.updateProfile(context, ProfileUpdateRequest(isOpenToOpportunities = isOpen))
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            user = _uiState.value.profile!!.user.copy(isOpenToOpportunities = isOpen)
                        )
                    )
                }
        }
    }
    
    // ==================== Avatar/Banner Upload ====================
    
    fun uploadAvatar(imageBytes: ByteArray) {
        if (_uiState.value.isUploadingAvatar) return
        
        _uiState.value = _uiState.value.copy(
            isUploadingAvatar = true,
            uploadError = null
        )
        
        viewModelScope.launch {
            ApiClient.uploadAvatarImage(context, imageBytes)
                .onSuccess { avatarUrl ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingAvatar = false,
                        profile = _uiState.value.profile?.copy(
                            user = _uiState.value.profile!!.user.copy(avatar = avatarUrl)
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingAvatar = false,
                        uploadError = e.message ?: "Failed to upload avatar"
                    )
                }
        }
    }
    
    fun uploadBanner(imageBytes: ByteArray) {
        if (_uiState.value.isUploadingBanner) return
        
        _uiState.value = _uiState.value.copy(
            isUploadingBanner = true,
            uploadError = null
        )
        
        viewModelScope.launch {
            ApiClient.uploadBannerImage(context, imageBytes)
                .onSuccess { bannerUrl ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingBanner = false,
                        profile = _uiState.value.profile?.copy(
                            user = _uiState.value.profile!!.user.copy(bannerImageUrl = bannerUrl)
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingBanner = false,
                        uploadError = e.message ?: "Failed to upload banner"
                    )
                }
        }
    }
    
    // ==================== Project Management ====================
    
    fun createProject(input: ProjectInput, onSuccess: (Project) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.createProject(context, input)
                .onSuccess { project ->
                    // Update local projects list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            projects = _uiState.value.profile!!.projects + project
                        )
                    )
                    onSuccess(project)
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to create project")
                }
        }
    }
    
    fun updateProject(projectId: String, input: ProjectInput, onSuccess: (Project) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.updateProject(context, projectId, input)
                .onSuccess { project ->
                    // Update local projects list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            projects = _uiState.value.profile!!.projects.map { 
                                if (it.id == projectId) project else it 
                            }
                        )
                    )
                    onSuccess(project)
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to update project")
                }
        }
    }
    
    fun deleteProject(projectId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.deleteProject(context, projectId)
                .onSuccess {
                    // Remove from local projects list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            projects = _uiState.value.profile!!.projects.filter { it.id != projectId }
                        )
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to delete project")
                }
        }
    }
    
    fun toggleProjectFeatured(projectId: String, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            ApiClient.toggleProjectFeatured(context, projectId)
                .onSuccess { newFeaturedStatus ->
                    // Update local projects list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            projects = _uiState.value.profile!!.projects.map { 
                                if (it.id == projectId) it.copy(featured = newFeaturedStatus) else it 
                            }
                        )
                    )
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to toggle featured (max 3)")
                }
        }
    }
    
    // ==================== Experience Management ====================
    
    fun addExperience(experience: Experience) {
        // Add to local experiences list (sorted by isCurrent desc, then startDate desc)
        val currentExperiences = _uiState.value.profile?.experiences ?: emptyList()
        val updatedExperiences = (currentExperiences + experience).sortedWith(
            compareByDescending<Experience> { it.isCurrent }
                .thenByDescending { it.startDate }
        )
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(experiences = updatedExperiences)
        )
    }
    
    fun updateExperience(experience: Experience) {
        // Update local experiences list
        val updatedExperiences = _uiState.value.profile?.experiences?.map {
            if (it.id == experience.id) experience else it
        }?.sortedWith(
            compareByDescending<Experience> { it.isCurrent }
                .thenByDescending { it.startDate }
        ) ?: emptyList()
        
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(experiences = updatedExperiences)
        )
    }
    
    fun deleteExperience(experienceId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.deleteExperience(context, experienceId)
                .onSuccess {
                    // Remove from local experiences list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            experiences = _uiState.value.profile!!.experiences.filter { it.id != experienceId }
                        )
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to delete experience")
                }
        }
    }
    
    // ==================== Education Management ====================
    
    fun addEducation(education: Education) {
        // Add to local education list (sorted by isCurrent desc, then startDate desc)
        val currentEducation = _uiState.value.profile?.education ?: emptyList()
        val updatedEducation = (currentEducation + education).sortedWith(
            compareByDescending<Education> { it.isCurrent }
                .thenByDescending { it.startDate }
        )
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(education = updatedEducation)
        )
    }
    
    fun updateEducation(education: Education) {
        // Update local education list
        val updatedEducation = _uiState.value.profile?.education?.map {
            if (it.id == education.id) education else it
        }?.sortedWith(
            compareByDescending<Education> { it.isCurrent }
                .thenByDescending { it.startDate }
        ) ?: emptyList()
        
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(education = updatedEducation)
        )
    }
    
    fun deleteEducation(educationId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.deleteEducation(context, educationId)
                .onSuccess {
                    // Remove from local education list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            education = _uiState.value.profile!!.education.filter { it.id != educationId }
                        )
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to delete education")
                }
        }
    }
    
    // ==================== Certificate Management ====================
    
    fun addCertificate(certificate: Certificate) {
        // Add to local certificates list (sorted by issueDate desc)
        val currentCertificates = _uiState.value.profile?.certificates ?: emptyList()
        val updatedCertificates = (currentCertificates + certificate).sortedByDescending { it.issueDate }
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(certificates = updatedCertificates)
        )
    }
    
    fun updateCertificate(certificate: Certificate) {
        // Update local certificates list
        val updatedCertificates = _uiState.value.profile?.certificates?.map {
            if (it.id == certificate.id) certificate else it
        }?.sortedByDescending { it.issueDate } ?: emptyList()
        
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(certificates = updatedCertificates)
        )
    }
    
    fun deleteCertificate(certificateId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.deleteCertificate(context, certificateId)
                .onSuccess {
                    // Remove from local certificates list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            certificates = _uiState.value.profile!!.certificates.filter { it.id != certificateId }
                        )
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to delete certificate")
                }
        }
    }
    
    // ==================== Achievement Management ====================
    
    fun addAchievement(achievement: Achievement) {
        // Add to local achievements list (sorted by date desc)
        val currentAchievements = _uiState.value.profile?.achievements ?: emptyList()
        val updatedAchievements = (currentAchievements + achievement).sortedByDescending { it.date }
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(achievements = updatedAchievements)
        )
    }
    
    fun updateAchievement(achievement: Achievement) {
        // Update local achievements list
        val updatedAchievements = _uiState.value.profile?.achievements?.map {
            if (it.id == achievement.id) achievement else it
        }?.sortedByDescending { it.date } ?: emptyList()
        
        _uiState.value = _uiState.value.copy(
            profile = _uiState.value.profile?.copy(achievements = updatedAchievements)
        )
    }
    
    fun deleteAchievement(achievementId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            ApiClient.deleteAchievement(context, achievementId)
                .onSuccess {
                    // Remove from local achievements list
                    _uiState.value = _uiState.value.copy(
                        profile = _uiState.value.profile?.copy(
                            achievements = _uiState.value.profile!!.achievements.filter { it.id != achievementId }
                        )
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to delete achievement")
                }
        }
    }

    // ==================== Skills Management (local only) ====================

    fun addLocalSkill(name: String, proficiency: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val currentProfile = _uiState.value.profile ?: return

        val newSkillId = "local-${System.currentTimeMillis()}"
        val newSkill = UserSkill(
            id = newSkillId,
            skill = Skill(id = newSkillId, name = trimmed),
            proficiency = proficiency?.takeIf { it.isNotBlank() },
            yearsOfExp = null
        )

        _uiState.value = _uiState.value.copy(
            profile = currentProfile.copy(
                skills = currentProfile.skills + newSkill
            )
        )
    }

    fun removeLocalSkill(skillId: String) {
        val currentProfile = _uiState.value.profile ?: return
        _uiState.value = _uiState.value.copy(
            profile = currentProfile.copy(
                skills = currentProfile.skills.filterNot { it.id == skillId }
            )
        )
    }
    
    fun clearUploadError() {
        _uiState.value = _uiState.value.copy(uploadError = null)
    }
    
    fun retry() {
        targetUserId?.let { loadProfile(it, forceRefresh = true) }
    }
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(context) as T
        }
    }
}
