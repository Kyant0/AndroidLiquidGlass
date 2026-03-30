package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.RewardCard
import com.kyant.backdrop.catalog.network.models.RewardCardEventRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val REWARD_CARD_COOLDOWN_MS = 24 * 60 * 60 * 1000L
private const val REWARD_CARD_TIMELINE_JITTER_MS = 6 * 60 * 60 * 1000L
private const val REWARD_CARD_PREFS = "vormex_reward_cards"
private const val KEY_LAST_OVERLAY_SHOWN_AT = "last_overlay_shown_at_v3"
private const val KEY_NEXT_OVERLAY_ELIGIBLE_AT = "next_overlay_eligible_at_v3"
private const val KEY_LAST_OVERLAY_SHOWN_AT_LEGACY = "last_overlay_shown_at_v2"

data class RewardCardsUiState(
    val isLoading: Boolean = false,
    val sessionId: String? = null,
    val cards: List<RewardCard> = emptyList(),
    val shouldShowOverlay: Boolean = false,
    val trackedShownCardIds: Set<String> = emptySet(),
    val connectionActionInProgress: Set<String> = emptySet()
)

class RewardCardsViewModel(private val context: Context) : ViewModel() {
    private val prefs by lazy {
        context.getSharedPreferences(REWARD_CARD_PREFS, Context.MODE_PRIVATE)
    }

    private val _uiState = MutableStateFlow(RewardCardsUiState())
    val uiState: StateFlow<RewardCardsUiState> = _uiState.asStateFlow()

    private var hasAttemptedLoadThisSession = false
    private var isRequestInFlight = false

    fun maybeLoadOnAppOpen(isLoggedIn: Boolean) {
        if (!isLoggedIn || hasAttemptedLoadThisSession || isRequestInFlight) {
            return
        }

        hasAttemptedLoadThisSession = true
        if (isWithinCooldown()) {
            return
        }

        viewModelScope.launch {
            isRequestInFlight = true
            _uiState.value = _uiState.value.copy(isLoading = true)

            ApiClient.getRewardCards(context)
                .onSuccess { response ->
                    val shouldShow = shouldDisplayOverlay(response.cards)
                    if (shouldShow) {
                        recordOverlayShown()
                    }

                    _uiState.value = RewardCardsUiState(
                        isLoading = false,
                        sessionId = response.sessionId,
                        cards = response.cards,
                        shouldShowOverlay = shouldShow
                    )
                }
                .onFailure {
                    _uiState.value = RewardCardsUiState()
                }

            isRequestInFlight = false
        }
    }

    fun trackShownCardsOnce() {
        val state = _uiState.value
        val sessionId = state.sessionId ?: return
        if (!state.shouldShowOverlay || state.cards.isEmpty()) return

        val untrackedCards = state.cards.filterNot { state.trackedShownCardIds.contains(it.id) }
        if (untrackedCards.isEmpty()) return

        _uiState.value = state.copy(
            trackedShownCardIds = state.trackedShownCardIds + untrackedCards.map { it.id }
        )

        untrackedCards.forEach { card ->
            trackEvent(
                RewardCardEventRequest(
                    sessionId = sessionId,
                    cardId = card.id,
                    cardType = card.cardType,
                    action = "shown"
                )
            )
        }
    }

    fun trackSkipped(card: RewardCard) {
        val sessionId = _uiState.value.sessionId ?: return
        trackEvent(
            RewardCardEventRequest(
                sessionId = sessionId,
                cardId = card.id,
                cardType = card.cardType,
                action = "skipped"
            )
        )
    }

    fun openProfile(card: RewardCard) {
        val sessionId = _uiState.value.sessionId ?: return
        trackEvent(
            RewardCardEventRequest(
                sessionId = sessionId,
                cardId = card.id,
                cardType = card.cardType,
                action = "opened_profile"
            )
        )
        _uiState.value = _uiState.value.copy(shouldShowOverlay = false)
    }

    fun connect(card: RewardCard) {
        if (_uiState.value.connectionActionInProgress.contains(card.id)) return

        val sessionId = _uiState.value.sessionId ?: return
        _uiState.value = _uiState.value.copy(
            connectionActionInProgress = _uiState.value.connectionActionInProgress + card.id
        )

        viewModelScope.launch {
            ApiClient.sendConnectionRequest(context, card.id)
                .onSuccess {
                    trackEvent(
                        RewardCardEventRequest(
                            sessionId = sessionId,
                            cardId = card.id,
                            cardType = card.cardType,
                            action = "connected"
                        )
                    )
                    Toast.makeText(context, "Connection request sent", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        context,
                        error.message ?: "Could not send connection request",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            _uiState.value = _uiState.value.copy(
                connectionActionInProgress = _uiState.value.connectionActionInProgress - card.id
            )
        }
    }

    fun dismissAll() {
        val sessionId = _uiState.value.sessionId ?: return
        trackEvent(
            RewardCardEventRequest(
                sessionId = sessionId,
                action = "dismissed_all"
            )
        )
        _uiState.value = _uiState.value.copy(shouldShowOverlay = false)
    }

    fun dismissOverlay() {
        _uiState.value = _uiState.value.copy(shouldShowOverlay = false)
    }

    private fun trackEvent(request: RewardCardEventRequest) {
        viewModelScope.launch {
            ApiClient.trackRewardCardEvent(context, request)
        }
    }

    private fun isWithinCooldown(): Boolean {
        val now = System.currentTimeMillis()
        val nextEligibleAt = prefs.getLong(KEY_NEXT_OVERLAY_ELIGIBLE_AT, 0L)
        if (nextEligibleAt > 0L) {
            return now < nextEligibleAt
        }

        val lastShownAt = prefs.getLong(KEY_LAST_OVERLAY_SHOWN_AT, 0L)
            .takeIf { it > 0L }
            ?: prefs.getLong(KEY_LAST_OVERLAY_SHOWN_AT_LEGACY, 0L)

        if (lastShownAt == 0L) return false

        val migratedNextEligibleAt = lastShownAt + REWARD_CARD_COOLDOWN_MS + randomTimelineJitter()
        prefs.edit()
            .putLong(KEY_LAST_OVERLAY_SHOWN_AT, lastShownAt)
            .putLong(KEY_NEXT_OVERLAY_ELIGIBLE_AT, migratedNextEligibleAt)
            .apply()
        return now < migratedNextEligibleAt
    }

    private fun shouldDisplayOverlay(cards: List<RewardCard>): Boolean {
        return cards.isNotEmpty()
    }

    private fun recordOverlayShown() {
        val shownAt = System.currentTimeMillis()
        val nextEligibleAt = shownAt + REWARD_CARD_COOLDOWN_MS + randomTimelineJitter()
        prefs.edit()
            .putLong(KEY_LAST_OVERLAY_SHOWN_AT, shownAt)
            .putLong(KEY_NEXT_OVERLAY_ELIGIBLE_AT, nextEligibleAt)
            .apply()
    }

    private fun randomTimelineJitter(): Long {
        return if (REWARD_CARD_TIMELINE_JITTER_MS <= 0L) {
            0L
        } else {
            Random.nextLong(until = REWARD_CARD_TIMELINE_JITTER_MS + 1L)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RewardCardsViewModel(context.applicationContext) as T
        }
    }
}
