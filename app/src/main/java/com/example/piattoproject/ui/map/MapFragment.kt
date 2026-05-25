package com.example.piattoproject.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import com.example.piattoproject.utils.ImageUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapViewModel: MapViewModel
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
        mapViewModel = ViewModelProvider(this)[MapViewModel::class.java]
        mapViewModel.posts.observe(viewLifecycleOwner) { updatedPosts ->
            posts = updatedPosts
            renderPostMarkers()
        }
        mapViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer)
            as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        mapViewModel.loadMapPosts()
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
                    .icon(createPostMarkerIcon(post))
                    .anchor(0.5f, 1f)
            )
            marker?.tag = post.id
        }
    }

    private fun createPostMarkerIcon(post: Post): BitmapDescriptor {
        val postImage = ImageUtils.decodeBase64Image(post.imageUrl)
        val markerBitmap = if (postImage != null) {
            createPhotoMarkerBitmap(postImage)
        } else {
            createFoodMarkerBitmap()
        }
        return BitmapDescriptorFactory.fromBitmap(markerBitmap)
    }

    private fun createPhotoMarkerBitmap(source: Bitmap): Bitmap {
        val marker = Bitmap.createBitmap(MARKER_WIDTH, MARKER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(marker)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawMarkerBase(canvas, paint, Color.WHITE)

        val imageBounds = RectF(
            MARKER_PADDING,
            MARKER_PADDING,
            MARKER_WIDTH - MARKER_PADDING,
            MARKER_WIDTH - MARKER_PADDING,
        )
        val path = Path().apply {
            addOval(imageBounds, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(centerCrop(source, imageBounds.width().toInt(), imageBounds.height().toInt()), null, imageBounds, paint)
        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.WHITE
        canvas.drawOval(imageBounds, paint)

        return marker
    }

    private fun createFoodMarkerBitmap(): Bitmap {
        val marker = Bitmap.createBitmap(MARKER_WIDTH, MARKER_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(marker)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawMarkerBase(canvas, paint, Color.rgb(230, 92, 54))

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(MARKER_WIDTH / 2f, MARKER_WIDTH / 2f, 24f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(230, 92, 54)
        canvas.drawCircle(MARKER_WIDTH / 2f, MARKER_WIDTH / 2f, 13f, paint)
        canvas.drawLine(30f, 25f, 30f, 65f, paint)
        canvas.drawLine(76f, 25f, 76f, 65f, paint)
        canvas.drawLine(84f, 25f, 84f, 65f, paint)

        return marker
    }

    private fun drawMarkerBase(canvas: Canvas, paint: Paint, color: Int) {
        val circleRadius = MARKER_WIDTH / 2f
        val centerX = MARKER_WIDTH / 2f
        val centerY = MARKER_WIDTH / 2f

        paint.style = Paint.Style.FILL
        paint.color = Color.argb(80, 0, 0, 0)
        canvas.drawOval(RectF(18f, MARKER_HEIGHT - 14f, MARKER_WIDTH - 18f, MARKER_HEIGHT - 2f), paint)

        paint.color = color
        canvas.drawCircle(centerX, centerY, circleRadius, paint)

        val pointer = Path().apply {
            moveTo(centerX - 18f, centerY + 38f)
            lineTo(centerX + 18f, centerY + 38f)
            lineTo(centerX, MARKER_HEIGHT - 10f)
            close()
        }
        canvas.drawPath(pointer, paint)
    }

    private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = maxOf(
            targetWidth.toFloat() / source.width.toFloat(),
            targetHeight.toFloat() / source.height.toFloat(),
        )
        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val left = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
        val top = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(scaled, left, top, targetWidth, targetHeight)
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
        private const val MARKER_WIDTH = 96
        private const val MARKER_HEIGHT = 124
        private const val MARKER_PADDING = 10f
    }
}
