package com.example.piattoproject.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.piattoproject.databinding.PostListRowBinding
import com.example.piattoproject.utils.ImageUtils

class PostRecyclerAdapter(
    private val onPostClick: (Post) -> Unit,
) : ListAdapter<Post, PostRecyclerAdapter.PostViewHolder>(PostDiffCallback()) {

    class PostViewHolder(val binding: PostListRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.binding.postTitle.text = post.recipeTitle
        holder.binding.postDescription.text = post.description
        holder.binding.postAuthor.text = "By ${post.creatorName}"
        ImageUtils.loadImage(holder.binding.postImage, post.imageUrl)
        holder.itemView.setOnClickListener {
            onPostClick(post)
        }
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}
