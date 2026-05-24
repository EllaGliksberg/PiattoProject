package com.example.piattoproject.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val postDao = AppLocalDbRepository.getInstance(application).postDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val loadedPosts = mutableListOf<Post>()
    private var lastDocument: DocumentSnapshot? = null
    private var hasMore = true
    private var isLoadingNextPage = false

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadInitialFeed() {
        viewModelScope.launch {
            _errorMessage.value = null
            if (loadedPosts.isNotEmpty()) {
                _posts.value = loadedPosts.toList()
                return@launch
            }
            val cached = withContext(Dispatchers.IO) {
                postDao.getFeedOrdered(PAGE_SIZE)
            }
            if (cached.isNotEmpty()) {
                _posts.value = cached
            }
            fetchFirstPage()
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _errorMessage.value = null
            lastDocument = null
            hasMore = true
            loadedPosts.clear()
            fetchFirstPage()
        }
    }

    fun loadNextPage() {
        if (!hasMore || isLoadingNextPage || _isLoading.value == true || lastDocument == null) {
            return
        }
        viewModelScope.launch {
            isLoadingNextPage = true
            _isLoadingMore.value = true
            try {
                val snapshot = baseQuery()
                    .startAfter(lastDocument!!)
                    .limit(PAGE_SIZE.toLong())
                    .get()
                    .await()
                val newPosts = snapshot.documents.map { it.toPost() }
                withContext(Dispatchers.IO) {
                    newPosts.forEach { postDao.insert(it) }
                }
                lastDocument = snapshot.documents.lastOrNull()
                hasMore = snapshot.size() == PAGE_SIZE
                appendDeduped(newPosts)
                _posts.value = loadedPosts.toList()
            } catch (_: Exception) {
                _errorMessage.value = "Failed to load more posts"
            } finally {
                isLoadingNextPage = false
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun fetchFirstPage() {
        _isLoading.value = true
        try {
            val snapshot = baseQuery()
                .limit(PAGE_SIZE.toLong())
                .get()
                .await()
            val newPosts = snapshot.documents.map { it.toPost() }
            withContext(Dispatchers.IO) {
                newPosts.forEach { postDao.insert(it) }
            }
            lastDocument = snapshot.documents.lastOrNull()
            hasMore = snapshot.size() == PAGE_SIZE
            loadedPosts.clear()
            loadedPosts.addAll(newPosts)
            _posts.value = loadedPosts.toList()
        } catch (_: Exception) {
            _errorMessage.value = "Failed to refresh posts"
        } finally {
            _isLoading.value = false
        }
    }

    private fun baseQuery(): Query = firestore.collection(POSTS_COLLECTION)
        .orderBy("lastUpdated", Query.Direction.DESCENDING)

    private fun appendDeduped(newPosts: List<Post>) {
        val existingIds = loadedPosts.map { it.id }.toSet()
        newPosts.filter { it.id !in existingIds }.forEach { loadedPosts.add(it) }
    }

    private companion object {
        private const val PAGE_SIZE = 10
        private const val POSTS_COLLECTION = "posts"
    }
}
