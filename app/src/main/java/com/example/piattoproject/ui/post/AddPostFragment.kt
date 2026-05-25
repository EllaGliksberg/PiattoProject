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
import com.example.piattoproject.utils.ImageUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private val args: AddPostFragmentArgs by navArgs()
    private val viewModel: AddPostViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

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

        getCurrentPostLocation { latitude, longitude ->
            viewModel.updatePost(postId, title, desc, latitude, longitude)
        }
    }

    private fun savePost() {
        val title = binding.editPostTitle.text.toString()
        val desc = binding.editPostDescription.text.toString()

        getCurrentPostLocation { latitude, longitude ->
            viewModel.createPost(title, desc, latitude, longitude)
        }
    }

    private fun getCurrentPostLocation(onLocationReady: (Double?, Double?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocationReady(null, null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastKnownLocation ->
                    if (lastKnownLocation != null) {
                        onLocationReady(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    } else {
                        requestCurrentLocation(onLocationReady)
                    }
                }
                .addOnFailureListener {
                    requestCurrentLocation(onLocationReady)
                }
        } catch (_: SecurityException) {
            onLocationReady(null, null)
        }
    }

    private fun requestCurrentLocation(onLocationReady: (Double?, Double?) -> Unit) {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token,
            ).addOnSuccessListener { location ->
                onLocationReady(location?.latitude, location?.longitude)
            }.addOnFailureListener {
                onLocationReady(null, null)
            }
        } catch (_: SecurityException) {
            onLocationReady(null, null)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
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
                ImageUtils.loadImage(binding.postImagePreview, post.imageUrl)
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
