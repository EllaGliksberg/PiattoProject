package com.example.piattoproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val BASE64_IMAGE_PREFIX = "data:image/jpeg;base64,"
    private const val MAX_IMAGE_SIDE = 800
    private const val JPEG_QUALITY = 72

    /**
     * Firebase Storage is unavailable in this project environment, so selected
     * images are resized, compressed, encoded, and shared through Firestore.
     */
    fun encodeImageUriToBase64(context: Context, imageUri: Uri): String {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_IMAGE_SIDE)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decodedBitmap = resolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalArgumentException("Could not decode selected image")

        val scaledBitmap = resizeToMaxSide(decodedBitmap, MAX_IMAGE_SIDE)
        if (scaledBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        if (!scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        return BASE64_IMAGE_PREFIX + Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun loadImage(imageView: ImageView, imageReference: String?) {
        val normalizedReference = imageReference?.trim().orEmpty()
        if (normalizedReference.isBlank()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }

        if (normalizedReference.startsWith("data:image")) {
            try {
                val decodedImage = decodeBase64Image(normalizedReference)
                if (decodedImage == null) {
                    imageView.setImageResource(android.R.drawable.stat_notify_error)
                } else {
                    imageView.setImageBitmap(decodedImage)
                }
            } catch (_: Exception) {
                imageView.setImageResource(android.R.drawable.stat_notify_error)
            }
        } else {
            Picasso.get()
                .load(normalizedReference)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(imageView)
        }
    }

    fun decodeBase64Image(imageReference: String?): Bitmap? {
        val normalizedReference = imageReference?.trim().orEmpty()
        if (!normalizedReference.startsWith("data:image")) {
            return null
        }

        val base64String = normalizedReference.substringAfter(",", missingDelimiterValue = "")
        if (base64String.isBlank()) {
            return null
        }

        return try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height

        while (sampledWidth / 2 >= maxSide || sampledHeight / 2 >= maxSide) {
            sampledWidth /= 2
            sampledHeight /= 2
            sampleSize *= 2
        }

        return sampleSize.coerceAtLeast(1)
    }

    private fun resizeToMaxSide(source: Bitmap, maxSide: Int): Bitmap {
        val longestSide = maxOf(source.width, source.height)
        if (longestSide <= maxSide) {
            return source
        }

        val scale = maxSide.toFloat() / longestSide.toFloat()
        val newWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }
}
