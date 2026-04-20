package com.example.piattoproject.ui.post

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String,
    var recipeTitle: String,
    var description: String,
    var imageUrl: String,
    var creatorName: String,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var lastUpdated: Long = 0L
)