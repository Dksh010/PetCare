package com.petcare.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.petcare.app.databinding.ActivitySplashBinding
import com.petcare.app.ui.dashboard.MainActivity
import com.petcare.app.util.SessionManager
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Staged Animation
        binding.tvSplashMark.alpha = 0f
        binding.tvSplashMark.scaleX = 0.8f
        binding.tvSplashMark.scaleY = 0.8f
        
        binding.tvSplashMark.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Seed demo data if necessary
        lifecycleScope.launch {
            com.petcare.app.data.local.DemoDataSeeder.seedDataIfEmpty(applicationContext)
        }

        // Delay and Route
        Handler(Looper.getMainLooper()).postDelayed({
            routeUser()
        }, 2200)
    }

    private fun routeUser() {
        val nextActivity = if (sessionManager.isLoggedIn()) {
            MainActivity::class.java
        } else {
            EntryActivity::class.java
        }

        startActivity(Intent(this, nextActivity))
        finish()
    }
}