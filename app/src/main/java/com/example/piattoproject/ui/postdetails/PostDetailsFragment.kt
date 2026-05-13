package com.example.piattoproject.ui.postdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.piattoproject.databinding.FragmentPostDetailsBinding

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val args: PostDetailsFragmentArgs by navArgs()

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

        val postId = args.postId
        // In a real app, we would fetch the post details using this ID
        binding.detailsTitle.text = "Post ID: $postId"
        binding.detailsDescription.text = "Detailed information for post $postId will appear here."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
