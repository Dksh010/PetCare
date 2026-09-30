package com.petcare.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.databinding.ActivityLoginBinding
import com.petcare.app.ui.dashboard.MainActivity
import com.petcare.app.util.SecurityUtils
import com.petcare.app.util.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            navigateToDashboard()
            return
        }

        setupTextChangeListeners()
        setupClickListeners()
    }

    private fun setupTextChangeListeners() {
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInputs(email, password)) {
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val user = db.userDao().getUserByEmail(email)

                    if (user != null && SecurityUtils.verifyPassword(password, user.passwordHash)) {
                        sessionManager.createSession(userId = user.id, email = email)
                        Toast.makeText(this@LoginActivity, "Good to see you again.", Toast.LENGTH_SHORT).show()
                        navigateToDashboard()
                    } else {
                        Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            Toast.makeText(this, "Google Sign-In integration ready for future setup", Toast.LENGTH_SHORT).show()
        }

        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInputs(email: String, pass: String): Boolean {
        var isValid = true

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_valid_email)
            isValid = false
        }

        if (pass.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_length)
            isValid = false
        }

        return isValid
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}