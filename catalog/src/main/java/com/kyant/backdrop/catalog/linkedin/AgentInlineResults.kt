package com.kyant.backdrop.catalog.linkedin

import kotlinx.serialization.Serializable

@Serializable
data class AgentInlinePerson(
    val id: String,
    val name: String? = null,
    val username: String? = null,
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null,
    val branch: String? = null,
    val bio: String? = null,
    val skills: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val sharedInterests: List<String> = emptyList(),
    val matchReasons: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val connectionStatus: String = "none",
    val mutualConnections: Int = 0
)

@Serializable
data class AgentInlineResultsPayload(
    val resultType: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val source: String = "",
    val people: List<AgentInlinePerson> = emptyList(),
    val shownCount: Int = 0,
    val totalCount: Int = 0,
    val fallbackNavigationTarget: String? = null
)

data class AgentInlineResultsPanel(
    val resultType: String = "people",
    val title: String = "",
    val subtitle: String? = null,
    val source: String = "",
    val people: List<AgentInlinePerson> = emptyList(),
    val shownCount: Int = 0,
    val totalCount: Int = 0,
    val fallbackNavigationTarget: String? = null
)

fun AgentInlineResultsPanel.visiblePeople(dismissedIds: Set<String>): List<AgentInlinePerson> {
    return people.filterNot { dismissedIds.contains(it.id) }
}
