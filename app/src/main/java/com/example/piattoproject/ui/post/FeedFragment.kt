package com.example.piattoproject.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piattoproject.R
import com.example.piattoproject.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PostViewModel
    private lateinit var adapter: PostRecyclerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PostViewModel::class.java]

        binding.addPostBtn.setOnClickListener {
            // Navigate using the action defined in nav_graph.xml
            val action = FeedFragmentDirections.actionFeedToAddPost()
            findNavController().navigate(action)
        }

        binding.postsRecyclerView.layoutManager = LinearLayoutManager(context)
        
        // Initialize adapter with a click listener to navigate to Post Details
        adapter = PostRecyclerAdapter(emptyList()) { post ->
            val action = FeedFragmentDirections.actionFeedToPostDetails(post.id)
            findNavController().navigate(action)
        }
        
        binding.postsRecyclerView.adapter = adapter

        viewModel.posts.observe(viewLifecycleOwner) { updatedPosts ->
            adapter.posts = updatedPosts
            adapter.notifyDataSetChanged()
        }

        viewModel.refreshPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
