package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.AgentApiService
import com.kyant.backdrop.catalog.network.GrowthApiService
import com.kyant.backdrop.catalog.network.models.AccountabilityPartnerSummary
import com.kyant.backdrop.catalog.network.models.BadgeSummary
import com.kyant.backdrop.catalog.network.models.CareerChatHistoryItem
import com.kyant.backdrop.catalog.network.models.DailyChallengeSummary
import com.kyant.backdrop.catalog.network.models.DailyHookSummary
import com.kyant.backdrop.catalog.network.models.GrowthJob
import com.kyant.backdrop.catalog.network.models.InterviewCategorySummary
import com.kyant.backdrop.catalog.network.models.InterviewStatsSummary
import com.kyant.backdrop.catalog.network.models.LearningPathSummary
import com.kyant.backdrop.catalog.network.models.MentorshipSummary
import com.kyant.backdrop.catalog.network.models.ReferralShareLinks
import com.kyant.backdrop.catalog.network.models.ReferralStatsSummary
import com.kyant.backdrop.catalog.network.models.StoreItemSummary
import com.kyant.backdrop.catalog.network.models.ChallengeStatsSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class GrowthHubChatMessage(
    val role: String,
    val content: String
)

data class GrowthHubUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val featuredJobs: List<GrowthJob> = emptyList(),
    val jobTypes: List<String> = emptyList(),
    val featuredPaths: List<LearningPathSummary> = emptyList(),
    val learningCategories: List<String> = emptyList(),
    val dailyChallenge: DailyChallengeSummary? = null,
    val challengeCategories: List<String> = emptyList(),
    val challengeStats: ChallengeStatsSummary? = null,
    val interviewCategories: List<InterviewCategorySummary> = emptyList(),
    val interviewStats: InterviewStatsSummary? = null,
    val storeItems: List<StoreItemSummary> = emptyList(),
    val xpBalance: Int = 0,
    val badges: List<BadgeSummary> = emptyList(),
    val badgeCategories: List<String> = emptyList(),
    val referralCode: String? = null,
    val referralStats: ReferralStatsSummary? = null,
    val referralShareLinks: ReferralShareLinks? = null,
    val dailyHooks: List<DailyHookSummary> = emptyList(),
    val hooksDate: String? = null,
    val partners: List<AccountabilityPartnerSummary> = emptyList(),
    val mentorships: List<MentorshipSummary> = emptyList(),
    val aiMessages: List<GrowthHubChatMessage> = listOf(
        GrowthHubChatMessage(
            role = "assistant",
            content = "Ask me about interviews, learning plans, networking moves, or how to use Vormex to grow faster."
        )
    ),
    val isSendingAiMessage: Boolean = false,
    val aiError: String? = null,
    val checkInMessage: String? = null
)

class GrowthHubViewModel(
    private val context: Context
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow(GrowthHubUiState())
    val uiState: StateFlow<GrowthHubUiState> = _uiState.asStateFlow()

    init {
        refreshAll(showLoader = true)
    }

    fun refreshAll(showLoader: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = showLoader && !showingContent(),
                isRefreshing = !showLoader || showingContent(),
                error = null,
                checkInMessage = null
            )

            supervisorScope {
                val featuredJobsDeferred = async { GrowthApiService.getFeaturedJobs() }
                val jobTypesDeferred = async { GrowthApiService.getJobTypes() }
                val featuredPathsDeferred = async { GrowthApiService.getFeaturedPaths() }
                val learningCategoriesDeferred = async { GrowthApiService.getLearningCategories() }
                val dailyChallengeDeferred = async { GrowthApiService.getDailyChallenge() }
                val challengeCategoriesDeferred = async { GrowthApiService.getChallengeCategories() }
                val challengeStatsDeferred = async { GrowthApiService.getChallengeStats(applicationContext) }
                val interviewCategoriesDeferred = async { GrowthApiService.getInterviewCategories() }
                val interviewStatsDeferred = async { GrowthApiService.getInterviewStats(applicationContext) }
                val storeItemsDeferred = async { GrowthApiService.getStoreItems() }
                val xpBalanceDeferred = async { GrowthApiService.getXpBalance(applicationContext) }
                val badgesDeferred = async { GrowthApiService.getBadges() }
                val badgeCategoriesDeferred = async { GrowthApiService.getBadgeCategories() }
                val referralCodeDeferred = async { GrowthApiService.getReferralCode(applicationContext) }
                val referralStatsDeferred = async { GrowthApiService.getReferralStats(applicationContext) }
                val shareLinksDeferred = async { GrowthApiService.getReferralShareLinks(applicationContext) }
                val dailyHooksDeferred = async { GrowthApiService.getDailyHooks(applicationContext) }
                val partnersDeferred = async { GrowthApiService.getPartners(applicationContext) }
                val mentorshipsDeferred = async { GrowthApiService.getMentorships(applicationContext) }

                val errors = mutableListOf<String>()

                fun <T> Result<T>.orDefault(default: T): T {
                    return getOrElse {
                        it.message?.takeIf(String::isNotBlank)?.let(errors::add)
                        default
                    }
                }

                val dailyHooksResponse = dailyHooksDeferred.await().orDefault(
                    com.kyant.backdrop.catalog.network.models.DailyHooksResponse()
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    featuredJobs = featuredJobsDeferred.await().orDefault(emptyList()),
                    jobTypes = jobTypesDeferred.await().orDefault(emptyList()),
                    featuredPaths = featuredPathsDeferred.await().orDefault(emptyList()),
                    learningCategories = learningCategoriesDeferred.await().orDefault(emptyList()),
                    dailyChallenge = dailyChallengeDeferred.await().getOrNull(),
                    challengeCategories = challengeCategoriesDeferred.await().orDefault(emptyList()),
                    challengeStats = challengeStatsDeferred.await().getOrNull(),
                    interviewCategories = interviewCategoriesDeferred.await().orDefault(emptyList()),
                    interviewStats = interviewStatsDeferred.await().getOrNull(),
                    storeItems = storeItemsDeferred.await().orDefault(emptyList()),
                    xpBalance = xpBalanceDeferred.await().orDefault(0),
                    badges = badgesDeferred.await().orDefault(emptyList()),
                    badgeCategories = badgeCategoriesDeferred.await().orDefault(emptyList()),
                    referralCode = referralCodeDeferred.await().getOrNull(),
                    referralStats = referralStatsDeferred.await().getOrNull(),
                    referralShareLinks = shareLinksDeferred.await().getOrNull(),
                    dailyHooks = dailyHooksResponse.hooks,
                    hooksDate = dailyHooksResponse.date,
                    partners = partnersDeferred.await().orDefault(emptyList()),
                    mentorships = mentorshipsDeferred.await().orDefault(emptyList()),
                    error = errors.firstOrNull()
                )
            }
        }
    }

    fun sendCareerMessage(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || _uiState.value.isSendingAiMessage) return

        val updatedMessages = _uiState.value.aiMessages + GrowthHubChatMessage(
            role = "user",
            content = trimmed
        )

        _uiState.value = _uiState.value.copy(
            aiMessages = updatedMessages,
            isSendingAiMessage = true,
            aiError = null
        )

        viewModelScope.launch {
            AgentApiService.sendTurn(
                context = applicationContext,
                inputText = trimmed,
                surface = "growth_hub",
                surfaceContext = mapOf("source" to "growth_hub")
            ).onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        aiMessages = _uiState.value.aiMessages + GrowthHubChatMessage(
                            role = "assistant",
                            content = response.assistantMessage.ifBlank {
                                "I’m here. Try asking for a study plan, interview prep, or networking advice."
                            }
                        ),
                        isSendingAiMessage = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSendingAiMessage = false,
                        aiError = it.message ?: "AI is unavailable right now."
                    )
                }
        }
    }

    fun clearAiError() {
        _uiState.value = _uiState.value.copy(aiError = null)
    }

    fun checkIn(pairId: String) {
        viewModelScope.launch {
            GrowthApiService.checkIn(applicationContext, pairId)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        partners = _uiState.value.partners.map { partner ->
                            if (partner.id == pairId) {
                                partner.copy(
                                    sharedStreak = result.streak,
                                    bestStreak = result.bestStreak,
                                    checkInsCompleted = result.checkInsCompleted
                                )
                            } else {
                                partner
                            }
                        },
                        checkInMessage = "Checked in. Shared streak is now ${result.streak} days."
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        checkInMessage = it.message ?: "Couldn’t complete check-in."
                    )
                }
        }
    }

    fun clearCheckInMessage() {
        _uiState.value = _uiState.value.copy(checkInMessage = null)
    }

    private fun showingContent(): Boolean {
        val state = _uiState.value
        return state.dailyHooks.isNotEmpty() ||
            state.badges.isNotEmpty() ||
            state.storeItems.isNotEmpty() ||
            state.jobTypes.isNotEmpty() ||
            state.learningCategories.isNotEmpty() ||
            state.interviewCategories.isNotEmpty()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GrowthHubViewModel(context.applicationContext) as T
        }
    }
}
