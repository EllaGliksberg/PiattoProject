package com.example.piattoproject.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.piattoproject.databinding.FragmentProfilePostsListBinding
import androidx.appcompat.widget.PopupMenu
import com.example.piattoproject.R
import com.example.piattoproject.ui.post.Post
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.piattoproject.ui.post.PostRecyclerAdapter

class ProfilePostsListFragment : Fragment() {
    private var _binding: FragmentProfilePostsListBinding? = null
    private val binding get() = _binding!!
    private val args: ProfilePostsListFragmentArgs by navArgs()
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var adapter: PostRecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePostsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        
        adapter = PostRecyclerAdapter(
            onPostClick = { post ->
                val action = ProfilePostsListFragmentDirections.actionProfilePostsListToPostDetails(post.id)
                findNavController().navigate(action)
            },
            onMenuClick = { post, anchor ->
                if (args.listType == "my_posts") {
                    showPostActionsMenu(post, anchor)
                }
            }
        )

        binding.postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.postsRecyclerView.adapter = adapter

        observeViewModel()
    }

    private fun observeViewModel() {
        profileViewModel.profileUiState.observe(viewLifecycleOwner) { uiState ->
            val posts = if (args.listType == "my_posts") {
                uiState.myPosts
            } else {
                uiState.savedPosts
            }
            
            adapter.submitList(posts)
            binding.emptyTextView.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
            binding.progressBar.visibility = if (uiState.isLoadingMyPosts || uiState.isLoadingSavedPosts) View.VISIBLE else View.GONE
        }
    }

    private fun showPostActionsMenu(post: Post, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.profile_post_item_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.profile_post_menu_edit -> {
                    openEditPost(post)
                    true
                }
                R.id.profile_post_menu_delete -> {
                    showDeletePostConfirmation(post)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openEditPost(post: Post) {
        val action = ProfilePostsListFragmentDirections.actionProfilePostsListToAddPost(
            postId = post.id,
            isEditMode = true,
        )
        findNavController().navigate(action)
    }

    private fun showDeletePostConfirmation(post: Post) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_post_delete_title)
            .setMessage(R.string.profile_post_delete_message)
            .setNegativeButton(R.string.profile_cancel_button, null)
            .setPositiveButton(R.string.profile_post_delete_confirm) { _, _ ->
                profileViewModel.deletePost(post)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
