package com.kyant.backdrop.catalog.linkedin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kyant.backdrop.catalog.network.ApiClient
import com.kyant.backdrop.catalog.network.models.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsInboxUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isMarkingAllRead: Boolean = false,
    val unreadOnly: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null
)

class NotificationsInboxViewModel(
    private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsInboxUiState())
    val uiState: StateFlow<NotificationsInboxUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(showLoader: Boolean = false) {
        viewModelScope.launch {
            val unreadOnly = _uiState.value.unreadOnly
            _uiState.value = _uiState.value.copy(
                isLoading = showLoader && _uiState.value.notifications.isEmpty(),
                isRefreshing = !showLoader || _uiState.value.notifications.isNotEmpty(),
                error = null
            )

            val notificationsResult = ApiClient.getNotifications(
                context = context,
                limit = 40,
                unreadOnly = unreadOnly
            )
            val unreadCountResult = ApiClient.getNotificationUnreadCount(context)

            val notifications = notificationsResult.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = it.message ?: "Failed to load notifications"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                notifications = notifications.notifications,
                unreadCount = unreadCountResult.getOrDefault(notifications.notifications.count { !it.isRead }),
                error = null
            )
        }
    }

    fun toggleUnreadOnly() {
        _uiState.value = _uiState.value.copy(unreadOnly = !_uiState.value.unreadOnly)
        refresh(showLoader = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun markAllAsRead() {
        if (_uiState.value.isMarkingAllRead || _uiState.value.unreadCount == 0) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingAllRead = true)
            ApiClient.markAllNotificationsAsRead(context)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMarkingAllRead = false,
                        notifications = _uiState.value.notifications.map { it.copy(isRead = true) },
                        unreadCount = 0
                    )
                    if (_uiState.value.unreadOnly) {
                        refresh()
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isMarkingAllRead = false,
                        error = it.message ?: "Failed to mark all as read"
                    )
                }
        }
    }

    fun markAsRead(notificationId: String) {
        val current = _uiState.value.notifications.find { it.id == notificationId } ?: return
        if (current.isRead) return

        val previous = _uiState.value
        _uiState.value = previous.copy(
            notifications = previous.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            },
            unreadCount = (previous.unreadCount - 1).coerceAtLeast(0)
        )

        viewModelScope.launch {
            ApiClient.markNotificationsAsRead(context, listOf(notificationId))
                .onFailure {
                    _uiState.value = previous.copy(error = it.message ?: "Failed to mark notification as read")
                }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationsInboxViewModel(context.applicationContext) as T
        }
    }
}
