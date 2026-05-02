package com.example.piattoproject.ui.post

import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toPost(): Post = Post(
    id = id,
    recipeTitle = getString("recipeTitle").orEmpty(),
    description = getString("description").orEmpty(),
    imageUrl = getString("imageUrl").orEmpty(),
    creatorName = getString("creatorName").orEmpty(),
    creatorUid = getString("creatorUid").orEmpty(),
    lastUpdated = getLong("lastUpdated") ?: 0L,
)
