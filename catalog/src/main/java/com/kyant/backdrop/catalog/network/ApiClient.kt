package com.kyant.backdrop.catalog.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kyant.backdrop.catalog.BuildConfig
import com.kyant.backdrop.catalog.network.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vormex_prefs")

object ApiClient {
    private val BASE_URL = BuildConfig.API_BASE_URL
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false // Don't send null values to backend
    }
    
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        if (BuildConfig.DEBUG) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.BODY
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 180000 // 3 minutes for large uploads
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 120000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
    
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    
    private var cachedToken: String? = null
    
    // Token management
    suspend fun saveToken(context: Context, token: String, userId: String) {
        cachedToken = token
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
        }
    }
    
    suspend fun getToken(context: Context): String? {
        if (cachedToken != null) return cachedToken
        return context.dataStore.data.first()[TOKEN_KEY].also { cachedToken = it }
    }
    
    suspend fun getCurrentUserId(context: Context): String? {
        return context.dataStore.data.first()[USER_ID_KEY]
    }
    
    fun getTokenFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { it[TOKEN_KEY] }
    }
    
    suspend fun clearToken(context: Context) {
        cachedToken = null
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
        }
    }
    
    // Auth APIs
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = client.post("$BASE_URL/auth/login") {
                setBody(LoginRequest(email, password))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Google Sign-In
    suspend fun googleSignIn(idToken: String): Result<AuthResponse> {
        return try {
            val response = client.post("$BASE_URL/auth/google") {
                setBody(GoogleSignInRequest(idToken))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<MessageResponse> {
        return try {
            val response = client.post("$BASE_URL/auth/forgot-password") {
                setBody(ForgotPasswordRequest(email))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Register new user
    suspend fun register(
        email: String,
        password: String,
        name: String,
        username: String,
        college: String? = null,
        branch: String? = null
    ): Result<AuthResponse> {
        return try {
            val response = client.post("$BASE_URL/auth/register") {
                setBody(RegisterRequest(email, password, name, username, college, branch))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(context: Context): Result<User> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/auth/me") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPremiumSubscription(context: Context): Result<PremiumSubscriptionResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/premium/subscription") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPremiumCheckout(context: Context): Result<PremiumCheckoutResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/checkout") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPremiumCheckout(
        context: Context,
        request: PremiumVerifyRequest
    ): Result<PremiumVerifyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/verify") {
                header("Authorization", "Bearer $token")
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelPremiumSubscription(context: Context): Result<PremiumVerifyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/premium/cancel") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Feed APIs
    suspend fun getFeed(context: Context, cursor: String? = null, limit: Int = 20): Result<FeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/feed") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("limit", limit)
                parameter("_t", System.currentTimeMillis()) // Cache buster
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Create post (multipart FormData)
    suspend fun createPost(
        context: Context,
        type: String = "TEXT",
        content: String,
        visibility: String = "PUBLIC",
        imageBytes: List<Pair<ByteArray, String>> = emptyList(), // (bytes, filename)
        videoBytes: Pair<ByteArray, String>? = null // (bytes, filename)
    ): Result<Post> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append("type", type)
                    append("visibility", visibility)
                    append("content", content)
                    imageBytes.forEachIndexed { index, (bytes, filename) ->
                        append(
                            "media",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=${filename.ifEmpty { "image$index.jpg" }}")
                            }
                        )
                    }
                    videoBytes?.let { (bytes, filename) ->
                        append(
                            "video",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "video/mp4")
                                append(HttpHeaders.ContentDisposition, "filename=${filename.ifEmpty { "video.mp4" }}")
                            }
                        )
                    }
                }))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Like/Unlike post
    suspend fun toggleLike(context: Context, postId: String): Result<LikeResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Stories APIs
    suspend fun getStories(context: Context): Result<StoriesFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/stories/feed") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("_t", System.currentTimeMillis()) // Cache buster
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Profile APIs
    suspend fun getProfile(context: Context, userId: String = "me"): Result<FullProfileResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/profile") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOtherUserProfile(context: Context, userId: String): Result<FullProfileResponse> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/users/$userId/profile") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Comments APIs
    suspend fun getComments(context: Context, postId: String, cursor: String? = null): Result<CommentsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/posts/$postId/comments") {
                header("Authorization", "Bearer $token")
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createComment(context: Context, postId: String, content: String, parentId: String? = null): Result<CreateCommentResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/comments") {
                header("Authorization", "Bearer $token")
                setBody(CreateCommentRequest(content, parentId))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleCommentLike(context: Context, postId: String, commentId: String): Result<CommentLikeResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/comments/$commentId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to toggle comment like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteComment(context: Context, postId: String, commentId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/posts/$postId/comments/$commentId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Share post
    suspend fun sharePost(context: Context, postId: String): Result<ShareResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/posts/$postId/share") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get share URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Find People APIs ====================
    
    // Smart Matches
    suspend fun getSmartMatches(
        context: Context,
        type: String = "all",
        page: Int = 1,
        limit: Int = 20
    ): Result<SmartMatchResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/matching/smart") {
                header("Authorization", "Bearer $token")
                parameter("type", type)
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // All People
    suspend fun getPeople(
        context: Context,
        search: String? = null,
        college: String? = null,
        branch: String? = null,
        graduationYear: Int? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<PeopleResponse> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/people") {
                token?.let { header("Authorization", "Bearer $it") }
                search?.let { parameter("search", it) }
                college?.let { parameter("college", it) }
                branch?.let { parameter("branch", it) }
                graduationYear?.let { parameter("graduationYear", it) }
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Filter Options
    suspend fun getFilterOptions(context: Context): Result<FilterOptions> {
        return try {
            val response = client.get("$BASE_URL/people/filter-options")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get filter options"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Suggestions (For You)
    suspend fun getPeopleSuggestions(context: Context, limit: Int = 20): Result<SuggestionsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/suggestions") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPeopleYouKnow(context: Context): Result<PeopleYouKnowResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/contacts") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverPeopleYouKnow(
        context: Context,
        contacts: List<PeopleYouKnowImportContact>,
        source: String = "picker"
    ): Result<PeopleYouKnowResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/contacts/import") {
                header("Authorization", "Bearer $token")
                setBody(PeopleYouKnowImportRequest(source = source, contacts = contacts))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearPeopleYouKnow(context: Context): Result<MessageResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/people/contacts") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markPeopleYouKnowInviteSent(
        context: Context,
        entryId: String
    ): Result<PeopleYouKnowInviteResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/people/contacts/$entryId/invite") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Same College
    suspend fun getSameCollegePeople(context: Context, limit: Int = 20): Result<PeopleResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/same-college") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
            }
            android.util.Log.d("ApiClient", "Same college status: ${response.status}")
            if (response.status.isSuccess()) {
                val body: PeopleResponse = response.body()
                android.util.Log.d("ApiClient", "Same college people count: ${body.people.size}")
                Result.success(body)
            } else {
                android.util.Log.e("ApiClient", "Same college error status: ${response.status}")
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Same college exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Search colleges on the platform
    suspend fun searchColleges(context: Context, query: String, limit: Int = 10): Result<CollegeSearchResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/people/colleges") {
                header("Authorization", "Bearer $token")
                parameter("q", query)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update Location
    suspend fun updateLocation(context: Context, lat: Double, lng: Double, accuracy: Float?): Result<Unit> {
        return updateLocationWithDetails(context, lat, lng, accuracy, null, null, null, null)
    }
    
    // Update Location with geocoded details
    suspend fun updateLocationWithDetails(
        context: Context,
        lat: Double,
        lng: Double,
        accuracy: Float?,
        city: String?,
        state: String?,
        country: String?,
        countryCode: String?
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/location/update") {
                header("Authorization", "Bearer $token")
                setBody(LocationUpdateRequest(
                    lat = lat,
                    lng = lng,
                    accuracy = accuracy,
                    city = city,
                    state = state,
                    country = country,
                    countryCode = countryCode
                ))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Nearby People
    suspend fun getNearbyPeople(
        context: Context,
        lat: Double,
        lng: Double,
        radius: Int = 50,
        limit: Int = 20
    ): Result<NearbyResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/location/nearby") {
                header("Authorization", "Bearer $token")
                parameter("lat", lat)
                parameter("lng", lng)
                parameter("radius", radius)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Connection APIs
    suspend fun sendConnectionRequest(context: Context, receiverId: String): Result<ConnectionResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/connections/request") {
                header("Authorization", "Bearer $token")
                setBody(ConnectionRequest(receiverId))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun cancelConnectionRequest(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/connections/$connectionId/cancel") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to cancel request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun acceptConnection(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/connections/$connectionId/accept") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to accept connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectConnection(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/connections/$connectionId/reject") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to reject connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getConnectionStatus(context: Context, userId: String): Result<ConnectionStatusResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/connections/status/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get connection status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingConnectionRequests(
        context: Context,
        page: Int = 1,
        limit: Int = 20
    ): Result<PendingConnectionRequestsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/connections/pending") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserConnections(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<ProfileConnectionsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/connections/user/$userId") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load connections"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Profile APIs (Additional) ====================
    
    suspend fun getProfileFeed(
        context: Context, 
        userId: String, 
        page: Int = 1, 
        limit: Int = 20,
        filter: String = "all"
    ): Result<RecentActivity> {
        return try {
            val response = client.get("$BASE_URL/users/$userId/feed") {
                parameter("page", page)
                parameter("limit", limit)
                parameter("filter", filter)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActivityHeatmap(context: Context, userId: String, year: Int? = null): Result<ActivityHeatmapResponse> {
        return try {
            val response = client.get("$BASE_URL/users/$userId/activity") {
                year?.let { parameter("year", it) }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load activity"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActivityYears(context: Context, userId: String): Result<ActivityYearsResponse> {
        return try {
            val response = client.get("$BASE_URL/users/$userId/activity/years")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load activity years"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildProfileUpdatePayload(
        data: ProfileUpdateRequest,
        explicitNullFields: Set<String> = emptySet()
    ) = buildJsonObject {
        data.headline?.let { put("headline", it) }
        data.bio?.let { put("bio", it) }
        data.location?.let { put("location", it) }
        data.currentYear?.let { put("currentYear", it) }
        data.degree?.let { put("degree", it) }
        data.graduationYear?.let { put("graduationYear", it) }
        data.portfolioUrl?.let { put("portfolioUrl", it) }
        data.linkedinUrl?.let { put("linkedinUrl", it) }
        data.githubProfileUrl?.let { put("githubProfileUrl", it) }
        data.profileVisibility?.let { put("profileVisibility", it) }
        data.isOpenToOpportunities?.let { put("isOpenToOpportunities", it) }
        data.interests?.let { interests ->
            putJsonArray("interests") {
                interests.forEach { add(JsonPrimitive(it)) }
            }
        }
        when {
            data.profileRing != null -> put("profileRing", data.profileRing)
            "profileRing" in explicitNullFields -> put("profileRing", JsonNull)
        }
        data.hasClaimedWelcomeGift?.let { put("hasClaimedWelcomeGift", it) }
        when {
            data.visitLoaderGiftId != null -> put("visitLoaderGiftId", data.visitLoaderGiftId)
            "visitLoaderGiftId" in explicitNullFields -> put("visitLoaderGiftId", JsonNull)
        }
        data.college?.let { put("college", it) }
        data.branch?.let { put("branch", it) }
    }

    suspend fun updateProfile(
        context: Context,
        data: ProfileUpdateRequest,
        explicitNullFields: Set<String> = emptySet()
    ): Result<ProfileUser> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me") {
                header("Authorization", "Bearer $token")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(buildProfileUpdatePayload(data, explicitNullFields))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateAvatar(context: Context, avatarUrl: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/avatar") {
                header("Authorization", "Bearer $token")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(AvatarUpdateRequest(avatarUrl))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update avatar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBanner(context: Context, bannerUrl: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/banner") {
                header("Authorization", "Bearer $token")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(BannerUpdateRequest(bannerUrl))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update banner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload avatar image (multipart form data)
     */
    suspend fun uploadAvatarImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "avatar.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/upload/avatar") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: Map<String, String> = response.body()
                Result.success(result["avatar"] ?: result["url"] ?: "")
            } else {
                Result.failure(Exception("Failed to upload avatar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload banner image (multipart form data)
     */
    suspend fun uploadBannerImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "banner.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/upload/banner") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: Map<String, String> = response.body()
                Result.success(result["bannerImageUrl"] ?: result["url"] ?: "")
            } else {
                Result.failure(Exception("Failed to upload banner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Follow APIs ====================
    
    suspend fun followUser(context: Context, userId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/follow/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to follow user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unfollowUser(context: Context, userId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/follow/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unfollow user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFollowStatus(context: Context, userId: String): Result<FollowStatusResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/status/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get follow status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMutualInfo(context: Context, userId: String): Result<MutualInfoResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/mutual/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to get mutual info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowers(
        context: Context,
        userId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<FollowersListResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/follow/followers/$userId") {
                header("Authorization", "Bearer $token")
                parameter("page", page)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Failed to load followers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Connection Management APIs ====================
    
    suspend fun removeConnection(context: Context, connectionId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/connections/$connectionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove connection"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Engagement/Streak APIs ====================
    
    suspend fun getStreaks(context: Context): Result<StreakData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/streaks") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val streakResponse: StreakResponse = response.body()
                Result.success(streakResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch streaks"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun recordLogin(context: Context): Result<StreakData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/engagement/login") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val streakResponse: StreakResponse = response.body()
                Result.success(streakResponse.data)
            } else {
                Result.failure(Exception("Failed to record login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Variable Rewards APIs (Hook Model) ====================

    suspend fun getRewardCards(context: Context): Result<RewardCardsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/reward-cards") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trackRewardCardEvent(
        context: Context,
        request: RewardCardEventRequest
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/engagement/reward-events") {
                header("Authorization", "Bearer $token")
                setBody(request)
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get daily matches with variable rewards.
     * Returns 1-5 random matches with a surprise message.
     */
    suspend fun getDailyMatches(context: Context): Result<DailyMatchesData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/daily-matches") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val dailyMatchesResponse: DailyMatchesResponse = response.body()
                Result.success(dailyMatchesResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch daily matches"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get users similar to current user (social proof).
     * Returns users with same goals, interests, or college.
     */
    suspend fun getPeopleLikeYou(context: Context): Result<PeopleLikeYouData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/people-like-you") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val peopleLikeYouResponse: PeopleLikeYouResponse = response.body()
                Result.success(peopleLikeYouResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch similar users"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get weekly hidden gem - a highly connected professional match.
     * Creates anticipation through scarcity (weekly reveal).
     */
    suspend fun getHiddenGem(context: Context): Result<HiddenGemData?> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/hidden-gem") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val hiddenGemResponse: HiddenGemResponse = response.body()
                Result.success(hiddenGemResponse.data)
            } else {
                Result.failure(Exception("Failed to fetch hidden gem"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user is trending today (variable reward).
     * Randomly features profiles to create excitement.
     */
    suspend fun getTrendingStatus(context: Context): Result<TrendingStatus> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/trending-status") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val trendingResponse: TrendingStatusResponse = response.body()
                Result.success(trendingResponse.data)
            } else {
                // If endpoint doesn't exist yet, return not trending
                Result.success(TrendingStatus(isTrending = false))
            }
        } catch (e: Exception) {
            // Graceful degradation - trending is not critical
            Result.success(TrendingStatus(isTrending = false))
        }
    }

    // ==================== Chat / Messaging APIs ====================

    suspend fun getConversations(
        context: Context,
        limit: Int = 20,
        cursor: String? = null
    ): Result<ConversationsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/conversations") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateConversation(context: Context, participantId: String): Result<Conversation> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/chat/conversations") {
                header("Authorization", "Bearer $token")
                setBody(CreateConversationRequest(participantId))
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversation(context: Context, conversationId: String): Result<Conversation> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/conversations/$conversationId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(
        context: Context,
        conversationId: String,
        limit: Int = 50,
        cursor: String? = null
    ): Result<MessagesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/conversations/$conversationId/messages") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        context: Context,
        conversationId: String,
        content: String,
        contentType: String = "text",
        mediaUrl: String? = null,
        mediaType: String? = null,
        fileName: String? = null,
        fileSize: Int? = null,
        replyToId: String? = null
    ): Result<Message> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/chat/conversations/$conversationId/messages") {
                header("Authorization", "Bearer $token")
                setBody(SendMessageRequest(content, contentType, mediaUrl, mediaType, fileName, fileSize, replyToId))
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a file for chat (image, video, document, or audio).
     * Backend expects multipart field "file". Returns mediaUrl, fileName, fileSize, mediaType.
     */
    suspend fun uploadChatMedia(
        context: Context,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<UploadChatMediaResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/chat/upload") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "file",
                        fileBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=${fileName}")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(context: Context, conversationId: String): Result<MarkAsReadResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/chat/conversations/$conversationId/read") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(
        context: Context,
        messageId: String,
        forEveryone: Boolean = false
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/chat/messages/$messageId") {
                header("Authorization", "Bearer $token")
                setBody(DeleteMessageRequest(forEveryone))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(context: Context, conversationId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/chat/conversations/$conversationId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportChat(
        context: Context,
        conversationId: String,
        reason: String,
        description: String = ""
    ): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/reports/chat/$conversationId") {
                header("Authorization", "Bearer $token")
                setBody(ReportChatRequest(reason = reason, description = description))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editMessage(context: Context, messageId: String, content: String): Result<Message> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.patch("$BASE_URL/chat/messages/$messageId") {
                header("Authorization", "Bearer $token")
                setBody(EditMessageRequest(content))
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReaction(context: Context, messageId: String, emoji: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/chat/messages/$messageId/reactions") {
                header("Authorization", "Bearer $token")
                setBody(AddReactionRequest(emoji))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(context: Context): Result<Int> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/unread-count") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val body: UnreadCountResponse = response.body()
                Result.success(body.unreadCount)
            } else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMessages(context: Context, query: String, limit: Int = 20): Result<List<Message>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/search") {
                header("Authorization", "Bearer $token")
                parameter("q", query)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                val body = response.body<SearchMessagesResponse>()
                Result.success(body.messages)
            } else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessageLimitStatus(context: Context, userId: String): Result<MessageLimitStatus> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/message-limit/$userId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessageRequests(context: Context, limit: Int = 20, cursor: String? = null): Result<MessageRequestsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/requests") {
                header("Authorization", "Bearer $token")
                parameter("limit", limit)
                cursor?.let { parameter("cursor", it) }
            }
            if (response.status.isSuccess()) Result.success(response.body())
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessageRequestsCount(context: Context): Result<Int> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/chat/requests/count") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val body: MessageRequestsCountResponse = response.body()
                Result.success(body.count)
            } else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptMessageRequest(context: Context, conversationId: String): Result<Conversation> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/chat/requests/$conversationId/accept") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val body: AcceptMessageRequestResponse = response.body()
                Result.success(body.conversation)
            } else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineMessageRequest(context: Context, conversationId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/chat/requests/$conversationId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception((response.body<ApiError>()).getErrorMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Stories APIs ====================
    
    suspend fun getMyStories(context: Context): Result<MyStoriesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/stories/me") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createStory(
        context: Context,
        mediaBytes: Pair<ByteArray, String>? = null, // Pair of (bytes, mimeType)
        mediaType: String, // TEXT, IMAGE, VIDEO
        textContent: String? = null,
        backgroundColor: String? = null,
        category: String = "GENERAL",
        visibility: String = "PUBLIC",
        linkUrl: String? = null,
        linkTitle: String? = null
    ): Result<CreateStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.submitFormWithBinaryData(
                url = "$BASE_URL/stories",
                formData = formData {
                    append("mediaType", mediaType)
                    append("category", category)
                    append("visibility", visibility)
                    textContent?.let { append("textContent", it) }
                    backgroundColor?.let { append("backgroundColor", it) }
                    linkUrl?.let { append("linkUrl", it) }
                    linkTitle?.let { append("linkTitle", it) }
                    mediaBytes?.let { (bytes, mimeType) ->
                        val extension = when {
                            mimeType.contains("video") -> ".mp4"
                            mimeType.contains("png") -> ".png"
                            else -> ".jpg"
                        }
                        append("media", bytes, Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"story$extension\"")
                        })
                    }
                }
            ) {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun viewStory(context: Context, storyId: String): Result<ViewStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/stories/$storyId/view") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun reactToStory(context: Context, storyId: String, reactionType: String = "LIKE"): Result<ReactToStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/stories/$storyId/react") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("reactionType" to reactionType))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteStory(context: Context, storyId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/stories/$storyId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStoryViewers(context: Context, storyId: String, cursor: String? = null, limit: Int = 20): Result<StoryViewersResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/stories/$storyId/viewers") {
                header("Authorization", "Bearer $token")
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun replyToStory(context: Context, storyId: String, content: String): Result<ReplyToStoryResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/stories/$storyId/reply") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("content" to content))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Reels APIs ====================
    
    suspend fun getReelsFeed(context: Context, cursor: String? = null, limit: Int = 10, mode: String = "foryou"): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/reels/feed") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit.toString())
                parameter("mode", mode)
                parameter("_t", System.currentTimeMillis()) // Cache buster
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTrendingReels(context: Context, hours: Int = 48, limit: Int = 15): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/reels/trending") {
                header("Authorization", "Bearer $token")
                header("Cache-Control", "no-cache, no-store, must-revalidate")
                parameter("hours", hours.toString())
                parameter("limit", limit.toString())
                parameter("_t", System.currentTimeMillis()) // Cache buster
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReel(context: Context, reelId: String): Result<Reel> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/reels/$reelId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSavedReels(context: Context, userId: String, cursor: String? = null, limit: Int = 20): Result<ReelsFeedResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/reels/user/$userId/saved") {
                header("Authorization", "Bearer $token")
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit.toString())
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleReelLike(context: Context, reelId: String): Result<ReelLikeResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/reels/$reelId/like") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleReelSave(context: Context, reelId: String): Result<ReelSaveResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/reels/$reelId/save") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun trackReelView(context: Context, reelId: String, watchTimeMs: Long, completed: Boolean, source: String = "feed"): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/reels/$reelId/view") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(ReelViewRequest(watchTimeMs, completed, source))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to track view"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getReelComments(
        context: Context,
        reelId: String,
        cursor: String? = null,
        limit: Int = 20,
        parentId: String? = null
    ): Result<ReelCommentsResponse> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/reels/$reelId/comments") {
                token?.let { header("Authorization", "Bearer $it") }
                cursor?.let { parameter("cursor", it) }
                parentId?.let { parameter("parentId", it) }
                parameter("limit", limit.toString())
            }
            val responseText = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = runCatching {
                    json.decodeFromString<ReelCommentsResponse>(responseText)
                }.recoverCatching {
                    // Some deployments may wrap payload under `data`.
                    val root = json.parseToJsonElement(responseText).jsonObject
                    val wrapped = root["data"] ?: root["result"] ?: root["payload"]
                        ?: error("Missing comments payload")
                    json.decodeFromJsonElement<ReelCommentsResponse>(wrapped)
                }

                parsed.fold(
                    onSuccess = { Result.success(it) },
                    onFailure = { Result.failure(Exception("Failed to parse comments response")) }
                )
            } else {
                val errorMessage = runCatching {
                    json.decodeFromString<ApiError>(responseText).getErrorMessage()
                }.getOrNull() ?: "Failed to fetch comments"

                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createReelComment(context: Context, reelId: String, content: String, parentId: String? = null): Result<ReelComment> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val body = buildMap<String, String> {
                put("content", content)
                parentId?.let { put("parentId", it) }
            }
            val response = client.post("$BASE_URL/reels/$reelId/comments") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun voteReelPoll(context: Context, reelId: String, optionId: Int): Result<ReelPollVoteResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/reels/$reelId/poll/vote") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("optionId" to optionId))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Retention Feature APIs ====================
    
    /**
     * Get weekly goals (Zeigarnik Effect)
     * Shows incomplete progress to motivate completion
     */
    suspend fun getWeeklyGoals(context: Context): Result<WeeklyGoalsData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/weekly-goals") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val goalsResponse: WeeklyGoalsResponse = response.body()
                Result.success(goalsResponse.data)
            } else {
                // Return default if endpoint doesn't exist
                Result.success(WeeklyGoalsData())
            }
        } catch (e: Exception) {
            Result.success(WeeklyGoalsData())
        }
    }
    
    /**
     * Get top networkers leaderboard (Social Proof)
     * @param period "week" or "month"
     */
    suspend fun getLeaderboard(context: Context, period: String = "week"): Result<LeaderboardData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/leaderboard") {
                header("Authorization", "Bearer $token")
                parameter("period", period)
            }
            if (response.status.isSuccess()) {
                val leaderboardResponse: LeaderboardResponse = response.body()
                Result.success(leaderboardResponse.data)
            } else {
                Result.success(LeaderboardData())
            }
        } catch (e: Exception) {
            Result.success(LeaderboardData())
        }
    }
    
    /**
     * Get live activity count (FOMO / Social Proof)
     * Shows how many people are currently networking
     */
    suspend fun getLiveActivity(context: Context): Result<LiveActivityData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/live-activity") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val activityResponse: LiveActivityResponse = response.body()
                Result.success(activityResponse.data)
            } else {
                Result.success(LiveActivityData())
            }
        } catch (e: Exception) {
            Result.success(LiveActivityData())
        }
    }
    
    /**
     * Get session summary (Peak-End Rule)
     * Shows accomplishments when leaving the app
     */
    suspend fun getSessionSummary(context: Context): Result<SessionSummaryData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/session-summary") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val summaryResponse: SessionSummaryResponse = response.body()
                Result.success(summaryResponse.data)
            } else {
                Result.success(SessionSummaryData())
            }
        } catch (e: Exception) {
            Result.success(SessionSummaryData())
        }
    }
    
    /**
     * Get connection celebration data (Peak Moment)
     * For full-screen celebration when connection is accepted
     */
    suspend fun getConnectionCelebration(context: Context, connectionId: String): Result<ConnectionCelebrationData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/celebration/$connectionId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val celebrationResponse: ConnectionCelebrationResponse = response.body()
                Result.success(celebrationResponse.data)
            } else {
                Result.success(ConnectionCelebrationData())
            }
        } catch (e: Exception) {
            Result.success(ConnectionCelebrationData())
        }
    }
    
    /**
     * Get connection request limit (Scarcity)
     * Shows how many requests remaining today
     */
    suspend fun getConnectionLimit(context: Context): Result<ConnectionLimitData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/connection-limit") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val limitResponse: ConnectionLimitResponse = response.body()
                Result.success(limitResponse.data)
            } else {
                // Default: unlimited if endpoint doesn't exist
                Result.success(ConnectionLimitData(unlimitedRequests = true))
            }
        } catch (e: Exception) {
            Result.success(ConnectionLimitData(unlimitedRequests = true))
        }
    }
    
    /**
     * Get engagement dashboard (all retention data at once)
     */
    suspend fun getEngagementDashboard(context: Context): Result<EngagementDashboardData> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/engagement/dashboard") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val dashboardResponse: EngagementDashboardResponse = response.body()
                Result.success(dashboardResponse.data)
            } else {
                Result.success(EngagementDashboardData())
            }
        } catch (e: Exception) {
            Result.success(EngagementDashboardData())
        }
    }
    
    // ==================== Onboarding APIs ====================
    
    /**
     * Get current user's onboarding status and data
     */
    suspend fun getOnboarding(context: Context): Result<OnboardingResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/onboarding") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update onboarding step data
     * Step 0: Profile (college, primaryGoal, lookingFor)
     * Step 1: Interests (interests, canTeach)
     */
    suspend fun updateOnboardingStep(
        context: Context,
        step: Int,
        data: Map<String, Any?>
    ): Result<OnboardingStepResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/onboarding/step") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("step", step)
                    putJsonObject("data") {
                        data.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Int -> put(key, value)
                                is Boolean -> put(key, value)
                                is List<*> -> putJsonArray(key) {
                                    value.filterIsInstance<String>().forEach { 
                                        add(kotlinx.serialization.json.JsonPrimitive(it))
                                    }
                                }
                                // Skip null values - server will handle defaults
                                null -> { /* skip */ }
                            }
                        }
                    }
                })
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark onboarding as completed
     */
    suspend fun completeOnboarding(context: Context): Result<OnboardingCompleteResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/onboarding/complete") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { })
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get initial matches based on onboarding data
     */
    suspend fun getOnboardingMatches(context: Context): Result<OnboardingMatchesResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/onboarding/matches") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                // Return empty matches if endpoint fails
                Result.success(OnboardingMatchesResponse(emptyList()))
            }
        } catch (e: Exception) {
            Result.success(OnboardingMatchesResponse(emptyList()))
        }
    }
    
    // ==================== Projects APIs ====================
    
    /**
     * Get user's projects
     */
    suspend fun getUserProjects(context: Context, userId: String): Result<List<Project>> {
        return try {
            val token = getToken(context)
            val response = client.get("$BASE_URL/users/$userId/projects") {
                token?.let { header("Authorization", "Bearer $it") }
            }
            if (response.status.isSuccess()) {
                val projectsResponse: ProjectsResponse = response.body()
                Result.success(projectsResponse.projects)
            } else {
                Result.failure(Exception("Failed to get projects"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new project
     */
    suspend fun createProject(context: Context, input: ProjectInput): Result<Project> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/projects") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val project: Project = response.body()
                Result.success(project)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing project
     */
    suspend fun updateProject(context: Context, projectId: String, input: ProjectInput): Result<Project> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me/projects/$projectId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val project: Project = response.body()
                Result.success(project)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a project
     */
    suspend fun deleteProject(context: Context, projectId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/projects/$projectId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggle project featured status
     */
    suspend fun toggleProjectFeatured(context: Context, projectId: String): Result<Boolean> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/projects/$projectId/feature") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val featureResponse: FeatureProjectResponse = response.body()
                Result.success(featureResponse.featured)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload project image
     */
    suspend fun uploadProjectImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "project.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/upload/project") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: ProjectImageUploadResponse = response.body()
                Result.success(result.url ?: result.imageUrl ?: "")
            } else {
                Result.failure(Exception("Failed to upload project image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Experience API ====================
    
    /**
     * Get user experiences
     */
    suspend fun getUserExperiences(context: Context, userId: String): Result<List<Experience>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/experiences") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val experiences: List<Experience> = response.body()
                Result.success(experiences)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new experience
     */
    suspend fun createExperience(context: Context, input: ExperienceInput): Result<Experience> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/experiences") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val experience: Experience = response.body()
                Result.success(experience)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing experience
     */
    suspend fun updateExperience(context: Context, experienceId: String, input: ExperienceInput): Result<Experience> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me/experiences/$experienceId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val experience: Experience = response.body()
                Result.success(experience)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an experience
     */
    suspend fun deleteExperience(context: Context, experienceId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/experiences/$experienceId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Education API ====================
    
    /**
     * Get user education entries
     */
    suspend fun getUserEducation(context: Context, userId: String): Result<List<Education>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/education") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val education: List<Education> = response.body()
                Result.success(education)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new education entry
     */
    suspend fun createEducation(context: Context, input: EducationInput): Result<Education> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/education") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val education: Education = response.body()
                Result.success(education)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing education entry
     */
    suspend fun updateEducation(context: Context, educationId: String, input: EducationInput): Result<Education> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me/education/$educationId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val education: Education = response.body()
                Result.success(education)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an education entry
     */
    suspend fun deleteEducation(context: Context, educationId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/education/$educationId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Certificates API ====================
    
    /**
     * Get user certificates
     */
    suspend fun getUserCertificates(context: Context, userId: String): Result<List<Certificate>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/certificates") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val certificates: List<Certificate> = response.body()
                Result.success(certificates)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new certificate
     */
    suspend fun createCertificate(context: Context, input: CertificateInput): Result<Certificate> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/certificates") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val certificate: Certificate = response.body()
                Result.success(certificate)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing certificate
     */
    suspend fun updateCertificate(context: Context, certificateId: String, input: CertificateInput): Result<Certificate> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me/certificates/$certificateId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val certificate: Certificate = response.body()
                Result.success(certificate)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a certificate
     */
    suspend fun deleteCertificate(context: Context, certificateId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/certificates/$certificateId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Upload certificate image (multipart form data)
     * Returns the URL of the uploaded certificate image
     */
    suspend fun uploadCertificateImage(
        context: Context,
        imageBytes: ByteArray,
        filename: String = "certificate.jpg"
    ): Result<String> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/upload/certificate") {
                header("Authorization", "Bearer $token")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "image",
                        imageBytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=$filename")
                        }
                    )
                }))
            }
            if (response.status.isSuccess()) {
                val result: CertificateUploadResponse = response.body()
                Result.success(result.certificateUrl ?: result.url ?: "")
            } else {
                Result.failure(Exception("Failed to upload certificate image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Achievements API ====================
    
    /**
     * Get user achievements
     */
    suspend fun getUserAchievements(context: Context, userId: String): Result<List<Achievement>> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/users/$userId/achievements") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val achievements: List<Achievement> = response.body()
                Result.success(achievements)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new achievement
     */
    suspend fun createAchievement(context: Context, input: AchievementInput): Result<Achievement> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/users/me/achievements") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val achievement: Achievement = response.body()
                Result.success(achievement)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing achievement
     */
    suspend fun updateAchievement(context: Context, achievementId: String, input: AchievementInput): Result<Achievement> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/users/me/achievements/$achievementId") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            if (response.status.isSuccess()) {
                val achievement: Achievement = response.body()
                Result.success(achievement)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete an achievement
     */
    suspend fun deleteAchievement(context: Context, achievementId: String): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/users/me/achievements/$achievementId") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Device Token / Push Notification APIs ====================
    
    /**
     * Register FCM device token with backend for push notifications
     */
    suspend fun registerDeviceToken(context: Context, token: String, platform: String): Result<Unit> {
        return try {
            val authToken = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/devices/register") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to token, "platform" to platform))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to register device token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Register FCM device token asynchronously (fire and forget)
     */
    fun registerDeviceTokenAsync(context: Context, token: String, platform: String) {
        GlobalScope.launch(Dispatchers.IO) {
            registerDeviceToken(context, token, platform)
                .onSuccess { println("📱 Device token registered successfully") }
                .onFailure { e -> println("📱 Failed to register device token: ${e.message}") }
        }
    }
    
    /**
     * Unregister device token (e.g., on logout)
     */
    suspend fun unregisterDeviceToken(context: Context, token: String): Result<Unit> {
        return try {
            val authToken = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.delete("$BASE_URL/devices/unregister") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf("token" to token))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unregister device token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== Notification APIs ====================
    
    /**
     * Get notifications with pagination
     */
    suspend fun getNotifications(context: Context, cursor: String? = null, limit: Int = 20, unreadOnly: Boolean = false): Result<NotificationsResponse> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/notifications") {
                header("Authorization", "Bearer $token")
                cursor?.let { parameter("cursor", it) }
                parameter("limit", limit)
                if (unreadOnly) parameter("unreadOnly", "true")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                val error: ApiError = response.body()
                Result.failure(Exception(error.getErrorMessage()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getNotificationUnreadCount(context: Context): Result<Int> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/notifications/unread-count") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                val body: Map<String, Int> = response.body()
                Result.success(body["count"] ?: 0)
            } else {
                Result.failure(Exception("Failed to get unread count"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark notifications as read
     */
    suspend fun markNotificationsAsRead(context: Context, notificationIds: List<String>): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/notifications/read") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("notificationIds" to notificationIds))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllNotificationsAsRead(context: Context): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.post("$BASE_URL/notifications/read-all") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get notification settings/preferences
     */
    suspend fun getNotificationSettings(context: Context): Result<NotificationSettings> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.get("$BASE_URL/notifications/settings") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                // Return defaults if endpoint fails
                Result.success(NotificationSettings())
            }
        } catch (e: Exception) {
            Result.success(NotificationSettings())
        }
    }
    
    /**
     * Update notification settings/preferences
     */
    suspend fun updateNotificationSettings(context: Context, settings: NotificationSettings): Result<Unit> {
        return try {
            val token = getToken(context) ?: return Result.failure(Exception("Not logged in"))
            val response = client.put("$BASE_URL/notifications/settings") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(settings)
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
