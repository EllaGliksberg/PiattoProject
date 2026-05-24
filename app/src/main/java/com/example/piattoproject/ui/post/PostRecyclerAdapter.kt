package com.example.piattoproject.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.piattoproject.databinding.PostListRowBinding

import com.squareup.picasso.Picasso
import com.example.piattoproject.R

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

        if (post.imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(post.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // תמונה זמנית בזמן טעינה
                .error(android.R.drawable.stat_notify_error)      // תמונה במקרה של שגיאה
                .into(holder.binding.postImage)
        } else {
            holder.binding.postImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        
        holder.itemView.setOnClickListener {
            onPostClick(post)
        }
    }

    override fun getItemCount() = posts.size
}
