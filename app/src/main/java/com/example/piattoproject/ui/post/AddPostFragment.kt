package com.example.piattoproject.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentAddPostBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private val args: AddPostFragmentArgs by navArgs()
    private val viewModel: AddPostViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var boundEditPostId: String? = null

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        requestLocationPermissionIfNeeded()

        val editPostId = args.postId.orEmpty()
        val isEditMode = args.isEditMode && editPostId.isNotBlank()

        if (isEditMode) {
            binding.savePostBtn.setText(R.string.add_post_save_changes)
            viewModel.loadPostForEdit(editPostId)
        } else {
            binding.savePostBtn.setText(R.string.add_post_post_recipe)
        }

        observeUiState()

        binding.savePostBtn.setOnClickListener {
            val title = binding.editPostTitle.text.toString()
            val desc = binding.editPostDescription.text.toString()
            val img = binding.editPostImageUrl.text.toString()

            if (isEditMode) {
                viewModel.updatePost(
                    postId = editPostId,
                    title = title,
                    description = desc,
                    imageUrl = img,
                )
            } else {
                createPostWithLocation(
                    title = title,
                    description = desc,
                    imageUrl = img,
                )
            }
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        if (hasLocationPermission()) {
            return
        }
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocationGranted || coarseLocationGranted
    }

    private fun createPostWithLocation(
        title: String,
        description: String,
        imageUrl: String,
    ) {
        if (!hasLocationPermission()) {
            viewModel.createPost(title, description, imageUrl, latitude = null, longitude = null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    viewModel.createPost(
                        title = title,
                        description = description,
                        imageUrl = imageUrl,
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                    )
                }
                .addOnFailureListener {
                    viewModel.createPost(title, description, imageUrl, latitude = null, longitude = null)
                }
        } catch (_: SecurityException) {
            viewModel.createPost(title, description, imageUrl, latitude = null, longitude = null)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.savePostBtn.isEnabled = !state.isLoading
            bindEditingPost(state.editingPost)

            val error = state.errorMessage
            if (!error.isNullOrBlank()) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }

            if (state.isSuccess) {
                val message = if (args.isEditMode) R.string.post_updated_success else null
                if (message != null) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Post added successfully!", Toast.LENGTH_SHORT).show()
                }
                viewModel.resetState()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun bindEditingPost(post: Post?) {
        if (post == null || boundEditPostId == post.id) {
            return
        }
        boundEditPostId = post.id
        binding.editPostTitle.setText(post.recipeTitle)
        binding.editPostDescription.setText(post.description)
        binding.editPostImageUrl.setText(post.imageUrl)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
