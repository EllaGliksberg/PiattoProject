package com.example.piattoproject

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.piattoproject.ui.profile.ProfileFragment
import com.example.piattoproject.ui.post.AppLocalDbRepository
import com.example.piattoproject.ui.post.Post

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val db = AppLocalDbRepository.getInstance(this)

        val testPost = Post(
            id = "1",
            recipeTitle = "Pasta Pomodoro",
            description = "Grandma's secret Italian recipe",
            imageUrl = "https://example.com/pasta.jpg",
            creatorName = "Noa",
            latitude = 32.0853,
            longitude = 34.7818
        )

        Thread {
            db.postDao().insert(testPost)

            val allPosts = db.postDao().getAll()
            println("✅ Room Test: Found ${allPosts.size} posts in database")
        }.start()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.profileFragmentContainer, ProfileFragment())
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}