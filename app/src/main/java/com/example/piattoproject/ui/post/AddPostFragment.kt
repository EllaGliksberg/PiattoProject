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

import androidx.lifecycle.lifecycleScope

import androidx.navigation.fragment.navArgs

import com.example.piattoproject.R

import com.example.piattoproject.databinding.FragmentAddPostBinding

import com.example.piattoproject.ui.profile.FirebaseUserPostsRepository

import com.google.android.gms.location.FusedLocationProviderClient

import com.google.android.gms.location.LocationServices

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.auth.FirebaseUser

import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.util.UUID



class AddPostFragment : Fragment() {



    private var _binding: FragmentAddPostBinding? = null

    private val binding get() = _binding!!

    private val args: AddPostFragmentArgs by navArgs()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

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



        val editPostId = args.editPostId

        val isEditMode = editPostId.isNotBlank()



        if (isEditMode) {

            binding.editPostTitle.setText(args.editRecipeTitle)

            binding.editPostDescription.setText(args.editDescription)

            binding.editPostImageUrl.setText(args.editImageUrl)

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

                val creatorName = args.editCreatorName

                val creatorUid = args.editCreatorUid

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



            createPostWithLocation(
                user = user,
                title = title,
                description = desc,
                imageUrl = img,
            )

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

        user: FirebaseUser,

        title: String,

        description: String,

        imageUrl: String,

    ) {

        if (!hasLocationPermission()) {

            saveNewPost(user, title, description, imageUrl, latitude = null, longitude = null)

            return

        }

        try {

            fusedLocationClient.lastLocation

                .addOnSuccessListener { location ->

                    saveNewPost(

                        user = user,

                        title = title,

                        description = description,

                        imageUrl = imageUrl,

                        latitude = location?.latitude,

                        longitude = location?.longitude,

                    )

                }

                .addOnFailureListener {

                    saveNewPost(user, title, description, imageUrl, latitude = null, longitude = null)

                }

        } catch (_: SecurityException) {

            saveNewPost(user, title, description, imageUrl, latitude = null, longitude = null)

        }

    }

    private fun saveNewPost(

        user: FirebaseUser,

        title: String,

        description: String,

        imageUrl: String,

        latitude: Double?,

        longitude: Double?,

    ) {

        val displayName = user.displayName?.takeIf { it.isNotBlank() }

            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }

            ?: "User"



        val id = UUID.randomUUID().toString()

        val updatedAt = System.currentTimeMillis()

        val newPost = Post(

            id = id,

            recipeTitle = title,

            description = description,

            imageUrl = imageUrl,

            creatorName = displayName,

            creatorUid = user.uid,

            latitude = latitude,

            longitude = longitude,

            lastUpdated = updatedAt,

        )



        val firestorePayload = hashMapOf<String, Any>(

            "recipeTitle" to title,

            "description" to description,

            "imageUrl" to imageUrl,

            "creatorName" to displayName,

            "creatorUid" to user.uid,

            "lastUpdated" to updatedAt,

        )

        if (latitude != null && longitude != null) {

            firestorePayload["latitude"] = latitude

            firestorePayload["longitude"] = longitude

        }



        FirebaseFirestore.getInstance().collection("posts").document(id).set(firestorePayload)

            .addOnSuccessListener {

                viewLifecycleOwner.lifecycleScope.launch {

                    withContext(Dispatchers.IO) {

                        AppLocalDbRepository.getInstance(requireContext()).postDao().insert(newPost)

                    }

                    Toast.makeText(context, "Post added successfully!", Toast.LENGTH_SHORT).show()

                    parentFragmentManager.popBackStack()

                }

            }

            .addOnFailureListener { e ->

                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()

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

