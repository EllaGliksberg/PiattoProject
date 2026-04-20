package com.example.piattoproject.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.piattoproject.databinding.FragmentAddPostBinding
import com.google.firebase.firestore.FirebaseFirestore
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

        binding.savePostBtn.setOnClickListener {
            val title = binding.editPostTitle.text.toString()
            val desc = binding.editPostDescription.text.toString()
            val img = binding.editPostImageUrl.text.toString()

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val id = UUID.randomUUID().toString()
            val newPost = Post(
                id = id,
                recipeTitle = title,
                description = desc,
                imageUrl = img,
                creatorName = "Noa",
                lastUpdated = System.currentTimeMillis()
            )

            // שמירה ל-Firebase
            FirebaseFirestore.getInstance().collection("posts").document(id).set(newPost)
                .addOnSuccessListener {
                    // שמירה ל-Room המקומי
                    Thread {
                        AppLocalDbRepository.getInstance(requireContext()).postDao().insert(newPost)
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Post added successfully!", Toast.LENGTH_SHORT).show()
                            // פקודת החזרה למסך הקודם (הפיד)
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
}