package com.example.piattoproject.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.piattoproject.databinding.FragmentFeedBinding
import com.google.android.material.snackbar.Snackbar

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

        viewModel = ViewModelProvider(requireActivity())[PostViewModel::class.java]

        binding.addPostBtn.setOnClickListener {
            val action = FeedFragmentDirections.actionFeedToAddPost()
            findNavController().navigate(action)
        }

        val layoutManager = LinearLayoutManager(context)
        binding.postsRecyclerView.layoutManager = layoutManager

        adapter = PostRecyclerAdapter { post ->
            val action = FeedFragmentDirections.actionFeedToPostDetails(post.id)
            findNavController().navigate(action)
        }

        binding.postsRecyclerView.adapter = adapter

        binding.postsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = layoutManager.itemCount
                if (lastVisible >= total - 3) {
                    viewModel.loadNextPage()
                }
            }
        })

        binding.feedSwipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        viewModel.posts.observe(viewLifecycleOwner) { updatedPosts ->
            adapter.submitList(updatedPosts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.feedSwipeRefresh.isRefreshing = isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.loadInitialFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
