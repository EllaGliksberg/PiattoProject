package com.example.piattoproject.ui.post

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import java.util.Locale
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
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationServices
import android.os.Looper
import com.google.android.gms.location.Priority

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

    private fun fetchAndDisplayCurrentLocation() {
        if (!hasLocationPermission()) return
        
        try {
            viewModel.setLocationStatus("Detecting location...")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && !viewModel.hasValidCoordinates()) {
                    processLocation(location)
                } else if (location == null) {
                    // אם אין מיקום אחרון שמור, נבקש עדכון מיקום טרי
                    requestFreshLocation()
                }
            }.addOnFailureListener {
                requestFreshLocation()
            }
        } catch (e: SecurityException) {
            viewModel.setLocationStatus("Permission error")
        }
    }

    private fun requestFreshLocation() {
        if (viewModel.hasValidCoordinates()) return
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let { 
                        if (!viewModel.hasValidCoordinates()) {
                            processLocation(it)
                        }
                    }
                }
            }, Looper.getMainLooper())
        } catch (e: SecurityException) {
            viewModel.setLocationStatus("Failed to get location")
        }
    }

    private fun processLocation(location: android.location.Location) {
        viewModel.setLocationCoords(location.latitude, location.longitude, "Fetching address...")
        
        // נשתמש ב-Thread נפרד ל-Geocoder כדי לא לתקוע את ה-UI
        Thread {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addressText = if (addresses != null && addresses.isNotEmpty()) {
                    addresses[0].getAddressLine(0)
                } else {
                    "${location.latitude}, ${location.longitude}"
                }
                activity?.runOnUiThread {
                    viewModel.setLocationCoords(location.latitude, location.longitude, addressText)
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    viewModel.setLocationCoords(location.latitude, location.longitude, "${location.latitude}, ${location.longitude}")
                }
            }
        }.start()
    }

    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            fetchAndDisplayCurrentLocation()
        } else {
            viewModel.setLocationEnabled(false)
            Toast.makeText(context, "Location permission is required to attach location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // סנכרון מצב ה-Switch עם ה-ViewModel (שימוש ב-setOnClickListener למניעת לופים)
        binding.switchLocation.setOnClickListener {
            val isChecked = binding.switchLocation.isChecked
            viewModel.setLocationEnabled(isChecked)
            if (isChecked) {
                if (!hasLocationPermission()) {
                    requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    fetchAndDisplayCurrentLocation()
                }
            }
        }

        // האזנה למצב המיקום ב-ViewModel
        viewModel.isLocationEnabled.observe(viewLifecycleOwner) { isEnabled ->
            if (binding.switchLocation.isChecked != isEnabled) {
                binding.switchLocation.isChecked = isEnabled
            }
        }

        // האזנה לטקסט הכתובת ב-ViewModel
        viewModel.locationStatusText.observe(viewLifecycleOwner) { status ->
            binding.tvLocationStatus.text = status
        }

        binding.savePostBtn.setOnClickListener {
            val title = binding.editPostTitle.text.toString()
            val desc = binding.editPostDescription.text.toString()
            if (args.isEditMode && args.postId != null) {
                viewModel.updatePost(args.postId!!, title, desc)
            } else {
                viewModel.createPost(title, desc)
            }
        }

        if (args.isEditMode && args.postId != null) {
            binding.tvAddPostTitle.text = "Edit Recipe"
            binding.savePostBtn.text = "Update Post"
            viewModel.loadPostForEdit(args.postId!!)
        }

        observeUiState()
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
                if (binding.editPostTitle.text.isNullOrEmpty()) {
                    binding.editPostTitle.setText(post.recipeTitle)
                    binding.editPostDescription.setText(post.description)
                    ImageUtils.loadImage(binding.postImagePreview, post.imageUrl)
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

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
