package com.example.piattoproject.ui.postdetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PostDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)
    private val auth = FirebaseAuth.getInstance()

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _isCreator = MutableLiveData<Boolean>(false)
    val isCreator: LiveData<Boolean> = _isCreator

    private val _isSaved = MutableLiveData<Boolean>(false)
    val isSaved: LiveData<Boolean> = _isSaved

    fun loadPost(postId: String) {
        viewModelScope.launch {
            val fetchedPost = repository.getPostById(postId)
            _post.postValue(fetchedPost)
            _isCreator.postValue(fetchedPost?.creatorUid == auth.currentUser?.uid)
            _isSaved.postValue(repository.isPostSaved(postId))
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            val result = repository.toggleSave(postId)
            result.onSuccess { nowSaved ->
                _isSaved.postValue(nowSaved)
                // Reload post to update count
                val fetchedPost = repository.getPostById(postId)
                _post.postValue(fetchedPost)
            }
        }
    }
}
