package com.example.piattoproject.utils

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.example.piattoproject.R
import com.squareup.picasso.Picasso

object ImageUtils {
    fun loadImage(imageView: ImageView, imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }

        if (imageUrl.startsWith("data:image")) {
            try {
                val base64String = imageUrl.substringAfter(",")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(decodedImage)
            } catch (e: Exception) {
                imageView.setImageResource(android.R.drawable.stat_notify_error)
            }
        } else {
            // It's a URL (old posts or external)
            Picasso.get()
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(imageView)
        }
    }
}
