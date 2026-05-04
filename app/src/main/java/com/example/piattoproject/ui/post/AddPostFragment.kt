package com.example.piattoproject.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentAddPostBinding
import com.example.piattoproject.ui.profile.FirebaseUserPostsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        val editPostId = args?.getString(ARG_POST_ID).orEmpty()
        val isEditMode = editPostId.isNotBlank()

        if (isEditMode) {
            binding.editPostTitle.setText(args?.getString(ARG_RECIPE_TITLE).orEmpty())
            binding.editPostDescription.setText(args?.getString(ARG_DESCRIPTION).orEmpty())
            binding.editPostImageUrl.setText(args?.getString(ARG_IMAGE_URL).orEmpty())
            binding.savePostBtn.setText(R.string.add_post_save_changes)
        } else {
            binding.savePostBtn.setText(R.string.add_post_post_recipe)
        }

        binding.savePostBtn.setOnClickListener {
            val title = binding.editPostTitle.text.toString()
            val desc = binding.editPostDescription.text.toString()
            val img = binding.editPostImageUrl.text.toString()

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(context, "You must be signed in to post", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode) {
                val creatorName = args?.getString(ARG_CREATOR_NAME).orEmpty()
                val creatorUid = args?.getString(ARG_CREATOR_UID).orEmpty()
                if (creatorUid != user.uid) {
                    Toast.makeText(context, "You must be signed in to post", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val updatedAt = System.currentTimeMillis()
                val updatedPost = Post(
                    id = editPostId,
                    recipeTitle = title,
                    description = desc,
                    imageUrl = img,
                    creatorName = creatorName,
                    creatorUid = creatorUid,
                    lastUpdated = updatedAt,
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            FirebaseUserPostsRepository().updatePostForCurrentUser(updatedPost)
                            AppLocalDbRepository.getInstance(requireContext()).postDao().insert(updatedPost)
                        }
                    }.onSuccess {
                        Toast.makeText(context, R.string.post_updated_success, Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }.onFailure { e ->
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                return@setOnClickListener
            }

            val displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "User"

            val id = UUID.randomUUID().toString()
            val updatedAt = System.currentTimeMillis()
            val newPost = Post(
                id = id,
                recipeTitle = title,
                description = desc,
                imageUrl = img,
                creatorName = displayName,
                creatorUid = user.uid,
                lastUpdated = updatedAt,
            )

            val firestorePayload = hashMapOf(
                "recipeTitle" to title,
                "description" to desc,
                "imageUrl" to img,
                "creatorName" to displayName,
                "creatorUid" to user.uid,
                "lastUpdated" to updatedAt,
            )

            FirebaseFirestore.getInstance().collection("posts").document(id).set(firestorePayload)
                .addOnSuccessListener {
                    Thread {
                        AppLocalDbRepository.getInstance(requireContext()).postDao().insert(newPost)
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Post added successfully!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                    }.start()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_POST_ID = "edit_post_id"
        const val ARG_RECIPE_TITLE = "edit_recipe_title"
        const val ARG_DESCRIPTION = "edit_description"
        const val ARG_IMAGE_URL = "edit_image_url"
        const val ARG_CREATOR_NAME = "edit_creator_name"
        const val ARG_CREATOR_UID = "edit_creator_uid"
    }
}
