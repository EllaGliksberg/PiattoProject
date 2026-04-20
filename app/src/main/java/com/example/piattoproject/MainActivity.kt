package com.example.piattoproject

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.piattoproject.ui.post.AppLocalDbRepository
import com.example.piattoproject.ui.post.Post
import com.example.piattoproject.ui.post.FeedFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val db = AppLocalDbRepository.getInstance(this)

        val testPost = Post(
            id = "1",
            recipeTitle = "Classic Pasta Pomodoro",
            description = "Fresh basil, tomatoes, and extra virgin olive oil. A true Italian classic.",
            imageUrl = "https://www.haaretz.co.il/magazine/the-edge/2019-11-20/ty-article-magazine/.premium/0000017f-e156-d7b2-a77f-e357fe330000",
            creatorName = "Noa"
        )

        Thread {
            db.postDao().insert(testPost)
        }.start()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.profileFragmentContainer, FeedFragment())
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}