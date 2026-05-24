package com.example.piattoproject.ui.map

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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentMapBinding
import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.PostViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var postViewModel: PostViewModel
    private var posts: List<Post> = emptyList()

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
                moveCameraToDeviceLocation()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.map_location_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
                moveCameraToDefaultLocation()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        postViewModel.posts.observe(viewLifecycleOwner) { updatedPosts ->
            posts = updatedPosts
            renderPostMarkers()
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer)
            as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        postViewModel.refreshPosts()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setOnMarkerClickListener { marker ->
            val postId = marker.tag as? String
            if (postId != null) {
                val action = MapFragmentDirections.actionMapToPostDetails(postId)
                findNavController().navigate(action)
            }
            true
        }

        if (hasFineLocationPermission()) {
            enableUserLocation()
            moveCameraToDeviceLocation()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        renderPostMarkers()
    }

    private fun renderPostMarkers() {
        val map = googleMap ?: return
        map.clear()
        posts.forEach { post ->
            val latitude = post.latitude
            val longitude = post.longitude
            if (latitude == null || longitude == null) {
                return@forEach
            }
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(latitude, longitude))
                    .title(post.recipeTitle)
            )
            marker?.tag = post.id
        }
    }

    private fun enableUserLocation() {
        if (!hasFineLocationPermission()) {
            return
        }

        try {
            googleMap?.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            moveCameraToDefaultLocation()
        }
    }

    private fun moveCameraToDeviceLocation() {
        if (!hasFineLocationPermission()) {
            moveCameraToDefaultLocation()
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (!isAdded) {
                        return@addOnSuccessListener
                    }

                    if (location != null) {
                        val currentLocation = LatLng(location.latitude, location.longitude)
                        moveCamera(currentLocation)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.map_location_unavailable),
                            Toast.LENGTH_SHORT
                        ).show()
                        moveCameraToDefaultLocation()
                    }
                }
                .addOnFailureListener {
                    if (isAdded) {
                        moveCameraToDefaultLocation()
                    }
                }
        } catch (_: SecurityException) {
            moveCameraToDefaultLocation()
        }
    }

    private fun moveCameraToDefaultLocation() {
        moveCamera(DEFAULT_LOCATION)
    }

    private fun moveCamera(location: LatLng) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        googleMap = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        private val DEFAULT_LOCATION = LatLng(32.0853, 34.7818)
        private const val DEFAULT_ZOOM = 14f
    }
}
