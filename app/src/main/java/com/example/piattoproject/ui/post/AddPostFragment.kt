package com.example.piattoproject.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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

    // Launcher לבחירת תמונה מהגלריה
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.postImagePreview.setImageURI(it)
            viewModel.setSelectedImageUri(it)
        }
    }

    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        requestLocationPermissionIfNeeded()

        // כפתור בחירת תמונה
        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.savePostBtn.setOnClickListener {
            if (args.isEditMode && args.postId != null) {
                updatePost(args.postId!!)
            } else {
                savePost()
            }
        }

        if (args.isEditMode && args.postId != null) {
            binding.tvAddPostTitle.text = "Edit Recipe"
            binding.savePostBtn.text = "Update Post"
            viewModel.loadPostForEdit(args.postId!!)
        }

        observeUiState()
    }

    private fun updatePost(postId: String) {
        val title = binding.editPostTitle.text.toString()
        val desc = binding.editPostDescription.text.toString()

        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    viewModel.updatePost(postId, title, desc, location?.latitude, location?.longitude)
                }.addOnFailureListener {
                    viewModel.updatePost(postId, title, desc, null, null)
                }
            } catch (e: SecurityException) {
                viewModel.updatePost(postId, title, desc, null, null)
            }
        } else {
            viewModel.updatePost(postId, title, desc, null, null)
        }
    }

    private fun savePost() {
        val title = binding.editPostTitle.text.toString()
        val desc = binding.editPostDescription.text.toString()

        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    viewModel.createPost(title, desc, location?.latitude, location?.longitude)
                }.addOnFailureListener {
                    viewModel.createPost(title, desc, null, null)
                }
            } catch (e: SecurityException) {
                viewModel.createPost(title, desc, null, null)
            }
        } else {
            viewModel.createPost(title, desc, null, null)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // ניהול תצוגת טעינה
            binding.postProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.savePostBtn.isEnabled = !state.isLoading
            binding.btnPickImage.isEnabled = !state.isLoading

            state.errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }

            state.editingPost?.let { post ->
                binding.editPostTitle.setText(post.recipeTitle)
                binding.editPostDescription.setText(post.description)
                if (post.imageUrl.isNotEmpty()) {
                    com.squareup.picasso.Picasso.get()
                        .load(post.imageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(binding.postImagePreview)
                }
            }

            if (state.isSuccess) {
                val message = if (args.isEditMode) "Post updated!" else "Post added!"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                findNavController().popBackStack()
            }
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        if (!hasLocationPermission()) {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
