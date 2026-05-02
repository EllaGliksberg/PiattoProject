package com.example.piattoproject

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.piattoproject.ui.auth.AuthFragment
import com.example.piattoproject.ui.post.AddPostFragment
import com.example.piattoproject.ui.post.FeedFragment
import com.example.piattoproject.ui.profile.ProfileFragment
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var appBarLayout: View

    private val fragmentLifecycleCallbacks =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                if (f.id != R.id.profileFragmentContainer) return
                updateToolbarVisibility(f)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        appBarLayout = findViewById(R.id.mainAppBarLayout)
        findViewById<View>(R.id.mainNavFeed).setOnClickListener { navigateToFeed() }
        findViewById<View>(R.id.mainNavUpload).setOnClickListener { navigateToUpload() }
        findViewById<View>(R.id.mainNavProfile).setOnClickListener { navigateToProfile() }
        findViewById<View>(R.id.mainNavLogout).setOnClickListener { logout() }

        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)

        if (savedInstanceState == null) {
            val startFragment =
                if (FirebaseAuth.getInstance().currentUser == null) {
                    AuthFragment()
                } else {
                    FeedFragment()
                }
            supportFragmentManager.beginTransaction()
                .replace(R.id.profileFragmentContainer, startFragment)
                .commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
        super.onDestroy()
    }

    fun navigateToFeed() {
        clearBackStack()
        supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, FeedFragment())
            .commit()
    }

    fun navigateToProfile() {
        clearBackStack()
        supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, ProfileFragment())
            .commit()
    }

    fun navigateToUpload() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, AddPostFragment())
            .addToBackStack(null)
            .commit()
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        clearBackStack()
        supportFragmentManager.beginTransaction()
            .replace(R.id.profileFragmentContainer, AuthFragment())
            .commit()
    }

    private fun clearBackStack() {
        while (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    private fun updateToolbarVisibility(fragment: Fragment) {
        val show = fragment !is AuthFragment && FirebaseAuth.getInstance().currentUser != null
        appBarLayout.visibility = if (show) View.VISIBLE else View.GONE
    }
}
