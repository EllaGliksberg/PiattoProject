package com.example.piattoproject.ui.post

import androidx.room.*

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY lastUpdated DESC")
    fun getAll(): androidx.lifecycle.LiveData<List<Post>>

    @Query("SELECT * FROM posts ORDER BY lastUpdated DESC LIMIT :limit")
    fun getFeedOrdered(limit: Int): List<Post>

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

    @Query("SELECT * FROM posts WHERE id = :id")
    fun getPostById(id: String): Post?

    @Query("SELECT * FROM posts WHERE id IN (:ids)")
    fun getPostsByIds(ids: List<String>): List<Post>
}