package com.kyant.backdrop.catalog.linkedin.posts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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

/**
 * UI State for Posts feature
 */
data class PostsUiState(
    // Feed state
    val posts: List<FullPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val feedError: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    
    // Current user
    val currentUserId: String? = null,
    val currentUserName: String? = null,
    val currentUserAvatar: String? = null,
    
    // Create post state
    val isCreatingPost: Boolean = false,
    val createPostError: String? = null,
    val showCreatePostModal: Boolean = false,
    val selectedPostType: PostType = PostType.TEXT,
    
    // Single post detail
    val selectedPost: FullPost? = null,
    val isLoadingPost: Boolean = false,
    val postError: String? = null,
    
    // Comments state
    val comments: List<FullComment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val commentsError: String? = null,
    val hasMoreComments: Boolean = false,
    val commentsPage: Int = 1,
    val isSubmittingComment: Boolean = false,
    val showCommentsSheet: Boolean = false,
    val selectedPostIdForComments: String? = null,
    
    // Edit post state
    val isEditingPost: Boolean = false,
    val editPostError: String? = null,
    val postBeingEdited: FullPost? = null,
    
    // Delete post state
    val isDeletingPost: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val postIdToDelete: String? = null,
    
    // Likes modal
    val showLikesModal: Boolean = false,
    val likesList: List<LikeUser> = emptyList(),
    val isLoadingLikes: Boolean = false,
    
    // Report modal
    val showReportModal: Boolean = false,
    val reportReasons: List<ReportReason> = emptyList(),
    val postIdToReport: String? = null,
    val isReporting: Boolean = false,
    
    // Share modal
    val showShareModal: Boolean = false,
    val postToShare: FullPost? = null,
    
    // Mention search
    val mentionSearchResults: List<MentionUser> = emptyList(),
    val isSearchingMentions: Boolean = false,
    
    // Saved posts
    val savedPosts: List<FullPost> = emptyList(),
    val isLoadingSaved: Boolean = false,
    val savedNextCursor: String? = null,
    val hasSavedMore: Boolean = false
)

/**
 * ViewModel for Posts feature - handles all post-related operations
 */
class PostsViewModel(private val context: Context) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PostsUiState())
    val uiState: StateFlow<PostsUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentUser()
    }
    
    // ==================== User Info ====================
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = ApiClient.getCurrentUserId(context)
            ApiClient.getCurrentUser(context)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        currentUserId = userId,
                        currentUserName = user.name,
                        currentUserAvatar = user.profileImage
                    )
                }
        }
    }
    
    // ==================== Feed Operations ====================
    
    fun loadFeed(refresh: Boolean = false) {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                feedError = null,
                posts = if (refresh) emptyList() else _uiState.value.posts
            )
            
            PostsApiService.getFeed(context)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        posts = response.posts,
                        nextCursor = response.nextCursor,
                        hasMore = response.hasMore,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        feedError = e.message ?: "Failed to load feed"
                    )
                }
        }
    }
    
    fun loadMorePosts() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            PostsApiService.getFeed(context, cursor)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        posts = _uiState.value.posts + response.posts,
                        nextCursor = response.nextCursor,
                        hasMore = response.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
        }
    }
    
    // ==================== Create Post Operations ====================
    
    fun showCreatePostModal(postType: PostType = PostType.TEXT) {
        _uiState.value = _uiState.value.copy(
            showCreatePostModal = true,
            selectedPostType = postType,
            createPostError = null
        )
    }
    
    fun hideCreatePostModal() {
        _uiState.value = _uiState.value.copy(
            showCreatePostModal = false,
            createPostError = null
        )
    }
    
    fun setSelectedPostType(type: PostType) {
        _uiState.value = _uiState.value.copy(selectedPostType = type)
    }
    
    fun createTextPost(
        content: String,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (content.isBlank()) {
            _uiState.value = _uiState.value.copy(createPostError = "Content is required")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createTextPost(context, content, visibility, mentions)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    fun createImagePost(
        content: String?,
        visibility: String = "PUBLIC",
        images: List<Pair<ByteArray, String>>,
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (images.isEmpty()) {
            _uiState.value = _uiState.value.copy(createPostError = "At least one image is required")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createImagePost(context, content, visibility, images, mentions)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    fun createVideoPost(
        content: String?,
        visibility: String = "PUBLIC",
        videoBytes: ByteArray,
        videoFilename: String = "video.mp4",
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createVideoPost(context, content, visibility, videoBytes, videoFilename, mentions)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    fun createLinkPost(
        linkUrl: String,
        content: String?,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (linkUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(createPostError = "Link URL is required")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createLinkPost(context, linkUrl, content, visibility, mentions)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    fun createPollPost(
        pollOptions: List<String>,
        pollDurationHours: Int = 24,
        content: String?,
        visibility: String = "PUBLIC",
        showResultsBeforeVote: Boolean = false,
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (pollOptions.size < 2) {
            _uiState.value = _uiState.value.copy(createPostError = "At least 2 poll options are required")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createPollPost(context, pollOptions, pollDurationHours, content, visibility, showResultsBeforeVote, mentions)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    fun createArticlePost(
        articleTitle: String,
        content: String?,
        visibility: String = "PUBLIC",
        coverImage: Pair<ByteArray, String>? = null,
        articleTags: List<String> = emptyList(),
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        if (articleTitle.isBlank()) {
            _uiState.value = _uiState.value.copy(createPostError = "Article title is required")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createArticlePost(context, articleTitle, content, visibility, coverImage, articleTags, mentions)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    fun createCelebrationPost(
        celebrationType: String,
        content: String?,
        visibility: String = "PUBLIC",
        mentions: List<String> = emptyList(),
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPost = true, createPostError = null)
            
            PostsApiService.createCelebrationPost(context, celebrationType, content, visibility, mentions, null)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        posts = listOf(post) + _uiState.value.posts,
                        isCreatingPost = false,
                        showCreatePostModal = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isCreatingPost = false,
                        createPostError = e.message ?: "Failed to create post"
                    )
                }
        }
    }
    
    // ==================== Edit Post Operations ====================
    
    fun startEditPost(post: FullPost) {
        _uiState.value = _uiState.value.copy(
            postBeingEdited = post,
            isEditingPost = false,
            editPostError = null
        )
    }
    
    fun cancelEditPost() {
        _uiState.value = _uiState.value.copy(
            postBeingEdited = null,
            editPostError = null
        )
    }
    
    fun updatePost(
        postId: String,
        content: String?,
        visibility: String?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEditingPost = true, editPostError = null)
            
            PostsApiService.updatePost(context, postId, content, visibility)
                .onSuccess { updatedPost ->
                    val updatedPosts = _uiState.value.posts.map {
                        if (it.id == postId) updatedPost else it
                    }
                    _uiState.value = _uiState.value.copy(
                        posts = updatedPosts,
                        isEditingPost = false,
                        postBeingEdited = null
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isEditingPost = false,
                        editPostError = e.message ?: "Failed to update post"
                    )
                }
        }
    }
    
    // ==================== Delete Post Operations ====================
    
    fun showDeleteConfirmation(postId: String) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            postIdToDelete = postId
        )
    }
    
    fun hideDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            postIdToDelete = null
        )
    }
    
    fun deletePost(postId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingPost = true)
            
            PostsApiService.deletePost(context, postId)
                .onSuccess {
                    val updatedPosts = _uiState.value.posts.filter { it.id != postId }
                    _uiState.value = _uiState.value.copy(
                        posts = updatedPosts,
                        isDeletingPost = false,
                        showDeleteConfirmation = false,
                        postIdToDelete = null
                    )
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isDeletingPost = false)
                }
        }
    }
    
    // ==================== Engagement Operations ====================
    
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            PostsApiService.toggleLike(context, postId)
                .onSuccess { response ->
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                isLiked = response.liked,
                                likesCount = response.likesCount
                            )
                        } else {
                            post
                        }
                    }
                    _uiState.value = _uiState.value.copy(posts = updatedPosts)
                    
                    // Also update selected post if viewing
                    _uiState.value.selectedPost?.let { selected ->
                        if (selected.id == postId) {
                            _uiState.value = _uiState.value.copy(
                                selectedPost = selected.copy(
                                    isLiked = response.liked,
                                    likesCount = response.likesCount
                                )
                            )
                        }
                    }
                }
        }
    }
    
    fun toggleSave(postId: String) {
        viewModelScope.launch {
            PostsApiService.toggleSave(context, postId)
                .onSuccess { response ->
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                isSaved = response.saved,
                                savesCount = response.savesCount
                            )
                        } else {
                            post
                        }
                    }
                    _uiState.value = _uiState.value.copy(posts = updatedPosts)
                    
                    // Also update selected post if viewing
                    _uiState.value.selectedPost?.let { selected ->
                        if (selected.id == postId) {
                            _uiState.value = _uiState.value.copy(
                                selectedPost = selected.copy(
                                    isSaved = response.saved,
                                    savesCount = response.savesCount
                                )
                            )
                        }
                    }
                }
        }
    }
    
    fun votePoll(postId: String, optionId: String) {
        viewModelScope.launch {
            PostsApiService.votePoll(context, postId, optionId)
                .onSuccess { response ->
                    if (response.success) {
                        val updatedPosts = _uiState.value.posts.map { post ->
                            if (post.id == postId) {
                                post.copy(
                                    pollOptions = response.pollOptions,
                                    userVotedOptionId = optionId
                                )
                            } else {
                                post
                            }
                        }
                        _uiState.value = _uiState.value.copy(posts = updatedPosts)
                    }
                }
        }
    }
    
    // ==================== Likes Modal ====================
    
    fun showLikesModal(postId: String) {
        _uiState.value = _uiState.value.copy(
            showLikesModal = true,
            isLoadingLikes = true,
            likesList = emptyList()
        )
        
        viewModelScope.launch {
            PostsApiService.getLikes(context, postId)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        likesList = response.likes,
                        isLoadingLikes = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingLikes = false)
                }
        }
    }
    
    fun hideLikesModal() {
        _uiState.value = _uiState.value.copy(
            showLikesModal = false,
            likesList = emptyList()
        )
    }
    
    // ==================== Comments Operations ====================
    
    fun showCommentsSheet(postId: String) {
        _uiState.value = _uiState.value.copy(
            showCommentsSheet = true,
            selectedPostIdForComments = postId,
            comments = emptyList(),
            commentsPage = 1,
            hasMoreComments = false
        )
        loadComments(postId)
    }
    
    fun hideCommentsSheet() {
        _uiState.value = _uiState.value.copy(
            showCommentsSheet = false,
            selectedPostIdForComments = null,
            comments = emptyList(),
            commentsError = null
        )
    }
    
    private fun loadComments(postId: String, page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingComments = page == 1,
                isLoadingMoreComments = page > 1,
                commentsError = null
            )
            
            PostsApiService.getComments(context, postId, page = page)
                .onSuccess { response ->
                    val newComments = if (page == 1) {
                        response.comments
                    } else {
                        _uiState.value.comments + response.comments
                    }
                    _uiState.value = _uiState.value.copy(
                        comments = newComments,
                        hasMoreComments = response.hasMore,
                        commentsPage = page,
                        isLoadingComments = false,
                        isLoadingMoreComments = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        commentsError = e.message ?: "Failed to load comments"
                    )
                }
        }
    }
    
    fun loadMoreComments() {
        val postId = _uiState.value.selectedPostIdForComments ?: return
        if (_uiState.value.isLoadingMoreComments || !_uiState.value.hasMoreComments) return
        loadComments(postId, _uiState.value.commentsPage + 1)
    }
    
    fun submitComment(
        content: String,
        parentId: String? = null,
        mentions: List<String>? = null
    ) {
        val postId = _uiState.value.selectedPostIdForComments ?: return
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            
            PostsApiService.createComment(context, postId, content, parentId, mentions)
                .onSuccess { comment ->
                    val updatedComments = listOf(comment) + _uiState.value.comments
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(commentsCount = post.commentsCount + 1)
                        } else {
                            post
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        comments = updatedComments,
                        posts = updatedPosts,
                        isSubmittingComment = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingComment = false,
                        commentsError = "Failed to post comment"
                    )
                }
        }
    }
    
    fun toggleCommentLike(commentId: String) {
        val postId = _uiState.value.selectedPostIdForComments ?: return
        
        viewModelScope.launch {
            PostsApiService.toggleCommentLike(context, postId, commentId)
                .onSuccess { response ->
                    val updatedComments = updateCommentLike(_uiState.value.comments, commentId, response)
                    _uiState.value = _uiState.value.copy(comments = updatedComments)
                }
        }
    }
    
    private fun updateCommentLike(
        comments: List<FullComment>,
        commentId: String,
        response: CommentLikeResponse
    ): List<FullComment> {
        return comments.map { comment ->
            if (comment.id == commentId) {
                comment.copy(
                    isLiked = response.liked,
                    likesCount = response.likesCount
                )
            } else {
                comment.copy(
                    replies = updateCommentLike(comment.replies, commentId, response)
                )
            }
        }
    }
    
    fun deleteComment(commentId: String) {
        val postId = _uiState.value.selectedPostIdForComments ?: return
        
        viewModelScope.launch {
            PostsApiService.deleteComment(context, postId, commentId)
                .onSuccess {
                    val updatedComments = _uiState.value.comments.filter { it.id != commentId }
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(commentsCount = maxOf(0, post.commentsCount - 1))
                        } else {
                            post
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        comments = updatedComments,
                        posts = updatedPosts
                    )
                }
        }
    }
    
    // ==================== Share Operations ====================
    
    fun showShareModal(post: FullPost) {
        _uiState.value = _uiState.value.copy(
            showShareModal = true,
            postToShare = post
        )
    }
    
    fun hideShareModal() {
        _uiState.value = _uiState.value.copy(
            showShareModal = false,
            postToShare = null
        )
    }
    
    fun sharePost(postId: String, activity: Activity) {
        viewModelScope.launch {
            PostsApiService.sharePost(context, postId)
                .onSuccess { response ->
                    // Update share count
                    val updatedPosts = _uiState.value.posts.map { post ->
                        if (post.id == postId) {
                            post.copy(sharesCount = response.sharesCount)
                        } else {
                            post
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        posts = updatedPosts,
                        showShareModal = false
                    )
                    
                    // Launch native share dialog
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out this post on Vormex!")
                        type = "text/plain"
                    }
                    activity.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                }
        }
    }
    
    // ==================== Report Operations ====================
    
    fun showReportModal(postId: String) {
        _uiState.value = _uiState.value.copy(
            showReportModal = true,
            postIdToReport = postId
        )
        
        // Load report reasons if not already loaded
        if (_uiState.value.reportReasons.isEmpty()) {
            viewModelScope.launch {
                PostsApiService.getReportReasons()
                    .onSuccess { response ->
                        _uiState.value = _uiState.value.copy(reportReasons = response.reasons)
                    }
            }
        }
    }
    
    fun hideReportModal() {
        _uiState.value = _uiState.value.copy(
            showReportModal = false,
            postIdToReport = null
        )
    }
    
    fun reportPost(reason: String, description: String? = null, onSuccess: () -> Unit = {}) {
        val postId = _uiState.value.postIdToReport ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReporting = true)
            
            PostsApiService.reportPost(context, postId, reason, description)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isReporting = false,
                        showReportModal = false,
                        postIdToReport = null
                    )
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isReporting = false)
                }
        }
    }
    
    // ==================== Mention Search ====================
    
    fun searchMentions(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(mentionSearchResults = emptyList())
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingMentions = true)
            
            PostsApiService.searchMentions(context, query)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        mentionSearchResults = response.users,
                        isSearchingMentions = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSearchingMentions = false)
                }
        }
    }
    
    fun clearMentionSearch() {
        _uiState.value = _uiState.value.copy(mentionSearchResults = emptyList())
    }
    
    // ==================== Single Post Operations ====================
    
    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPost = true, postError = null)
            
            PostsApiService.getPost(context, postId)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        selectedPost = post,
                        isLoadingPost = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPost = false,
                        postError = e.message ?: "Failed to load post"
                    )
                }
        }
    }
    
    fun clearSelectedPost() {
        _uiState.value = _uiState.value.copy(
            selectedPost = null,
            postError = null
        )
    }
    
    // ==================== Saved Posts ====================
    
    fun loadSavedPosts(refresh: Boolean = false) {
        if (_uiState.value.isLoadingSaved) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingSaved = true,
                savedPosts = if (refresh) emptyList() else _uiState.value.savedPosts
            )
            
            PostsApiService.getSavedPosts(context)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        savedPosts = response.posts,
                        savedNextCursor = response.nextCursor,
                        hasSavedMore = response.hasMore,
                        isLoadingSaved = false
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingSaved = false)
                }
        }
    }
    
    // ==================== Error Handling ====================
    
    fun clearFeedError() {
        _uiState.value = _uiState.value.copy(feedError = null)
    }
    
    fun clearCreatePostError() {
        _uiState.value = _uiState.value.copy(createPostError = null)
    }
    
    fun clearEditPostError() {
        _uiState.value = _uiState.value.copy(editPostError = null)
    }
    
    fun clearCommentsError() {
        _uiState.value = _uiState.value.copy(commentsError = null)
    }
    
    // ==================== Factory ====================
    
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PostsViewModel(context) as T
        }
    }
}
