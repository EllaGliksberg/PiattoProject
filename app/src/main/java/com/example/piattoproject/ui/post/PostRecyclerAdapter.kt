package com.example.piattoproject.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.piattoproject.databinding.PostListRowBinding

import com.example.piattoproject.R
import com.example.piattoproject.utils.ImageUtils

class PostRecyclerAdapter(var posts: List<Post>, private val onPostClick: (Post) -> Unit) :
    RecyclerView.Adapter<PostRecyclerAdapter.PostViewHolder>() {

    class PostViewHolder(val binding: PostListRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.binding.postTitle.text = post.recipeTitle
        holder.binding.postDescription.text = post.description
        holder.binding.postAuthor.text = "By ${post.creatorName}"

        ImageUtils.loadImage(holder.binding.postImage, post.imageUrl)
        
        holder.itemView.setOnClickListener {
            onPostClick(post)
        }
    }

    override fun getItemCount() = posts.size
}
