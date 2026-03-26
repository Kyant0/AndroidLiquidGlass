package com.kyant.backdrop.catalog.network.models

import kotlinx.serialization.Serializable

@Serializable
data class GrowthCompany(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val logo: String? = null,
    val location: String? = null,
    val isVerified: Boolean = false
)

@Serializable
data class GrowthJob(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val location: String = "",
    val isRemote: Boolean = false,
    val experienceLevel: String = "",
    val skills: List<String> = emptyList(),
    val company: GrowthCompany? = null,
    val isFeatured: Boolean = false
)

@Serializable
data class LearningPathSummary(
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: String = "",
    val estimatedHours: Int = 0,
    val xpReward: Int = 0,
    val thumbnail: String? = null,
    val isFeatured: Boolean = false
)

@Serializable
data class DailyChallengeSummary(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val difficulty: String = "",
    val xpReward: Int = 0,
    val points: Int = 0
)

@Serializable
data class ChallengeStatsSummary(
    val totalSolved: Int = 0,
    val totalAttempted: Int = 0,
    val easyCount: Int = 0,
    val mediumCount: Int = 0,
    val hardCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val rank: Int? = null
)

@Serializable
data class InterviewCategorySummary(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String? = null,
    val questionCount: Int = 0,
    val order: Int = 0
)

@Serializable
data class InterviewStatsSummary(
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val averageScore: Int = 0,
    val totalTimeSpent: Int = 0,
    val strongCategories: List<String> = emptyList(),
    val weakCategories: List<String> = emptyList()
)

@Serializable
data class StoreItemSummary(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val description: String? = null,
    val category: String = "",
    val price: Int? = null,
    val xpCost: Int? = null,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val isFeatured: Boolean = false
)

@Serializable
data class BadgeSummary(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val iconUrl: String? = null,
    val xpReward: Int = 0,
    val rarity: String = ""
)

@Serializable
data class ReferralStatsSummary(
    val totalReferrals: Int = 0,
    val successfulReferrals: Int = 0,
    val pendingReferrals: Int = 0,
    val totalXpEarned: Int = 0,
    val thisMonthReferrals: Int = 0,
    val rank: Int? = null
)

@Serializable
data class ReferralShareLinks(
    val code: String = "",
    val link: String = "",
    val whatsapp: String = "",
    val twitter: String = "",
    val linkedin: String = ""
)

@Serializable
data class ApplyReferralRequest(
    val code: String
)

@Serializable
data class ApplyReferralResponse(
    val success: Boolean = false,
    val message: String = "",
    val xpEarned: Int = 0
)

@Serializable
data class DailyHookAction(
    val label: String = "",
    val href: String = ""
)

@Serializable
data class DailyHookSummary(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val action: DailyHookAction = DailyHookAction(),
    val emoji: String = "",
    val priority: Int = 0
)

@Serializable
data class DailyHooksResponse(
    val hooks: List<DailyHookSummary> = emptyList(),
    val date: String = ""
)

@Serializable
data class AccountabilityPartnerProfile(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val profileImage: String? = null,
    val headline: String? = null,
    val college: String? = null
)

@Serializable
data class AccountabilityPartnerSummary(
    val id: String = "",
    val partner: AccountabilityPartnerProfile? = null,
    val goal: String = "",
    val sharedStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCheckIn: String? = null,
    val checkInsCompleted: Int = 0,
    val startedAt: String? = null
)

@Serializable
data class AccountabilityPartnersResponse(
    val partners: List<AccountabilityPartnerSummary> = emptyList()
)

@Serializable
data class MentorshipSummary(
    val id: String = "",
    val skill: String = "",
    val status: String = "",
    val sessionsCompleted: Int = 0,
    val rating: Double? = null
)

@Serializable
data class MentorshipsResponse(
    val mentorships: List<MentorshipSummary> = emptyList()
)

@Serializable
data class CheckInSummary(
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val checkInsCompleted: Int = 0
)

@Serializable
data class CareerChatHistoryItem(
    val content: String,
    val role: String
)

@Serializable
data class CareerChatRequest(
    val message: String,
    val conversationHistory: List<CareerChatHistoryItem>
)

@Serializable
data class CareerChatResponse(
    val reply: String = ""
)
