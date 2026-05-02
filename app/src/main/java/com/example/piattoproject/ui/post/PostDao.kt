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

    @Query("DELETE FROM posts WHERE id = :id")
    fun deleteById(id: String)

    @Query("SELECT * FROM posts WHERE creatorName = :userName")
    fun getPostsByUsername(userName: String): List<Post>

    @Query("SELECT * FROM posts WHERE creatorUid = :creatorUid ORDER BY lastUpdated DESC")
    fun getPostsByCreatorUid(creatorUid: String): List<Post>
}