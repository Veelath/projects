package com.eldroid.herdgate.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.herdgate.databinding.ActivitySplashBinding
import com.eldroid.herdgate.ui.auth.SignInActivity
import com.eldroid.herdgate.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Delay briefly for branding presentation then check auth state
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 1200)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already signed in
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not signed in
            startActivity(Intent(this, SignInActivity::class.java))
        }
        finish()
    }
}
