package com.kyant.backdrop.catalog.network

import android.content.Context
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object GrowthApiService {
    private val baseUrl = BuildConfig.API_BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.BODY
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 60000
        }
    }

    private suspend fun parseError(response: HttpResponse): String {
        return try {
            val error: ApiError = response.body()
            error.getErrorMessage()
        } catch (_: Exception) {
            response.bodyAsText().ifBlank { "Request failed with status ${response.status.value}" }
        }
    }

    private suspend fun authToken(context: Context): String? = ApiClient.getToken(context)

    suspend fun getJobTypes(): Result<List<String>> {
        return try {
            val response = client.get("$baseUrl/jobs/types")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeaturedJobs(): Result<List<GrowthJob>> {
        return try {
            val response = client.get("$baseUrl/jobs/featured") {
                parameter("limit", 4)
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLearningCategories(): Result<List<String>> {
        return try {
            val response = client.get("$baseUrl/learning/categories")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeaturedPaths(): Result<List<LearningPathSummary>> {
        return try {
            val response = client.get("$baseUrl/learning/featured") {
                parameter("limit", 4)
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyChallenge(): Result<DailyChallengeSummary> {
        return try {
            val response = client.get("$baseUrl/challenges/daily")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChallengeCategories(): Result<List<String>> {
        return try {
            val response = client.get("$baseUrl/challenges/categories")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChallengeStats(context: Context): Result<ChallengeStatsSummary> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/challenges/stats/me") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInterviewCategories(): Result<List<InterviewCategorySummary>> {
        return try {
            val response = client.get("$baseUrl/interviews/categories")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInterviewStats(context: Context): Result<InterviewStatsSummary> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/interviews/stats") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStoreItems(): Result<List<StoreItemSummary>> {
        return try {
            val response = client.get("$baseUrl/store/items")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getXpBalance(context: Context): Result<Int> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/store/balance") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBadges(): Result<List<BadgeSummary>> {
        return try {
            val response = client.get("$baseUrl/badges")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBadgeCategories(): Result<List<String>> {
        return try {
            val response = client.get("$baseUrl/badges/categories")
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReferralCode(context: Context): Result<String> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/referrals/code") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReferralStats(context: Context): Result<ReferralStatsSummary> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/referrals/stats") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReferralShareLinks(context: Context): Result<ReferralShareLinks> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/referrals/share") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyReferralCode(context: Context, code: String): Result<ApplyReferralResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/referrals/apply") {
                header("Authorization", "Bearer $token")
                setBody(ApplyReferralRequest(code = code))
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyHooks(context: Context): Result<DailyHooksResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/daily-hooks") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPartners(context: Context): Result<List<AccountabilityPartnerSummary>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/accountability/partners") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body<AccountabilityPartnersResponse>().partners)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMentorships(context: Context): Result<List<MentorshipSummary>> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$baseUrl/accountability/mentorships") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(response.body<MentorshipsResponse>().mentorships)
            } else {
                Result.failure(Exception(parseError(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkIn(context: Context, pairId: String): Result<CheckInSummary> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/accountability/partners/$pairId/check-in") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendCareerChatMessage(
        context: Context,
        message: String,
        history: List<CareerChatHistoryItem>
    ): Result<CareerChatResponse> {
        return try {
            val token = authToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$baseUrl/ai/chat/career-chat") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(CareerChatRequest(message = message, conversationHistory = history))
            }
            if (response.status.value in 200..299) Result.success(response.body())
            else Result.failure(Exception(parseError(response)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
