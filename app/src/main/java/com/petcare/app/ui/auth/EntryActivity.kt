package com.petcare.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.petcare.app.databinding.ActivityEntryBinding

class EntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()

        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun setupAnimations() {
        binding.entryHero.alpha = 0f
        binding.entryHero.translationY = 50f
        
        binding.btnGetStarted.alpha = 0f
        binding.btnGetStarted.translationY = 30f
        
        binding.btnLogin.alpha = 0f
        binding.btnLogin.translationY = 30f

        binding.entryHero.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        binding.btnGetStarted.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(600)
            .setInterpolator(OvershootInterpolator())
            .start()

        binding.btnLogin.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(750)
            .setInterpolator(OvershootInterpolator())
            .start()
    }
}