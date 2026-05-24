package com.example.piattoproject.ui.postdetails

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentPostDetailsBinding
import com.example.piattoproject.utils.ImageUtils

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: PostDetailsFragmentArgs by navArgs()
    private val viewModel: PostDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadPost(args.postId)

        observeViewModel()

        binding.editPostBtn.setOnClickListener {
            val action = PostDetailsFragmentDirections.actionPostDetailsToAddPost(
                postId = args.postId,
                isEditMode = true
            )
            findNavController().navigate(action)
        }

        binding.detailsSaveBtn.setOnClickListener {
            viewModel.toggleSave(args.postId)
        }
    }

    private fun observeViewModel() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            if (post != null) {
                binding.detailsTitle.text = post.recipeTitle
                binding.detailsDescription.text = post.description
                binding.detailsAuthor.text = "By ${post.creatorName}"
                binding.detailsSavesCount.text = post.savesCount.toString()

                ImageUtils.loadImage(binding.detailsImage, post.imageUrl)

                if (post.latitude != null && post.longitude != null) {
                    binding.locationContainer.visibility = View.VISIBLE
                    
                    // Display human-readable address
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val addresses = geocoder.getFromLocation(post.latitude, post.longitude, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val addressText = address.getAddressLine(0) ?: "${post.latitude}, ${post.longitude}"
                            binding.tvLocationAddress.text = addressText
                        } else {
                            binding.tvLocationAddress.text = "Location: ${post.latitude}, ${post.longitude}"
                        }
                    } catch (e: Exception) {
                        binding.tvLocationAddress.text = "Location: ${post.latitude}, ${post.longitude}"
                    }

                    binding.tvViewOnMap.setOnClickListener {
                        val gmmIntentUri = Uri.parse("geo:${post.latitude},${post.longitude}?q=${post.latitude},${post.longitude}(${post.recipeTitle})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        startActivity(mapIntent)
                    }
                } else {
                    binding.locationContainer.visibility = View.GONE
                }
            }
        }

        viewModel.isSaved.observe(viewLifecycleOwner) { isSaved ->
            val iconRes = if (isSaved) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.ic_menu_save
            }
            binding.detailsSaveIcon.setImageResource(iconRes)
        }

        viewModel.isCreator.observe(viewLifecycleOwner) { isCreator ->
            binding.editPostBtn.visibility = if (isCreator) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
