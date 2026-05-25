package com.example.piattoproject.ui.post

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val recipeTitle: String,
    val description: String,
    val imageUrl: String,
    val creatorName: String,
    val creatorUid: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val savesCount: Int = 0,
)
