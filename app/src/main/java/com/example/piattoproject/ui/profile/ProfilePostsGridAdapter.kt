package com.example.piattoproject.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.piattoproject.R
import com.example.piattoproject.ui.post.Post
import com.squareup.picasso.Picasso

class ProfilePostsGridAdapter(
    private var posts: List<Post> = emptyList(),
    private val onPostMenuClick: (Post, View) -> Unit,
) : RecyclerView.Adapter<ProfilePostsGridAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.postThumbnailImageView)
        val menuButton: ImageButton = itemView.findViewById(R.id.postMenuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_post, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val post = posts[position]
        if (post.imageUrl.isNotBlank()) {
            Picasso.get()
                .load(post.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        holder.menuButton.setOnClickListener { anchor ->
            onPostMenuClick(post, anchor)
        }
    }

    override fun getItemCount() = posts.size

    fun submitList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
