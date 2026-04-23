package com.example.piattoproject.ui.post

import androidx.room.*

@Dao
interface PostDao {
    @Query("SELECT * FROM posts")
    fun getAll(): List<Post>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg posts: Post)

    @Delete
    fun delete(post: Post)

    @Query("SELECT * FROM posts WHERE creatorName = :userName")
    fun getPostsByUsername(userName: String): List<Post>
}