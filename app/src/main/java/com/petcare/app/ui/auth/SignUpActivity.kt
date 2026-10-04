package com.petcare.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.data.model.UserEntity
import com.petcare.app.databinding.ActivitySignupBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.Constants
import com.petcare.app.util.SecurityUtils
import kotlinx.coroutines.launch

/**
 * Single-screen registration: name, email, password and confirmation.
 */
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    private val viewModel: PetViewModel by viewModels { PetViewModelFactory.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTextChangeListeners()
        setupClickListeners()
    }

    private fun setupTextChangeListeners() {
        binding.etFullName.doAfterTextChanged { binding.tilFullName.error = null }
        binding.etEmail.doAfterTextChanged { binding.tilEmail.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
        binding.etConfirmPassword.doAfterTextChanged { binding.tilConfirmPassword.error = null }
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener { register() }

        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun register() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim().lowercase()
        val password = binding.etPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        // Check every field so all problems are shown at once.
        var valid = true
        if (name.isEmpty()) {
            binding.tilFullName.error = getString(R.string.error_name_empty)
            valid = false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_valid_email)
            valid = false
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            binding.tilPassword.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
            valid = false
        }
        if (confirm != password) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            valid = false
        }
        if (!valid) return

        binding.btnSignUp.isEnabled = false
        lifecycleScope.launch {
            if (viewModel.getUserByEmail(email) != null) {
                binding.tilEmail.error = "This email is already registered"
                binding.btnSignUp.isEnabled = true
                return@launch
            }

            viewModel.registerUser(
                UserEntity(name = name, email = email, passwordHash = SecurityUtils.hashPassword(password))
            )
            Toast.makeText(this@SignUpActivity, getString(R.string.toast_account_created), Toast.LENGTH_SHORT).show()
            // No auto-login: send the user to sign in with their new account.
            startActivity(Intent(this@SignUpActivity, LoginActivity::class.java).putExtra(Constants.EXTRA_EMAIL, email))
            finish()
        }
    }


    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
