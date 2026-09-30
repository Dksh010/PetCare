package com.petcare.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.model.UserEntity
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.databinding.ActivitySignupBinding
import com.petcare.app.ui.dashboard.PetViewModel
import com.petcare.app.ui.dashboard.PetViewModelFactory
import com.petcare.app.util.SecurityUtils
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private var currentStep = 1

    private val viewModel: PetViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PetRepository(
            database.petDao(),
            database.taskDao(),
            database.userDao(),
            database.reminderDao(),
            database.appointmentDao(),
            database.activityLogDao(),
            database.vaccinationDao()
        )
        PetViewModelFactory(repository)
    }

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
        binding.etPassword.doAfterTextChanged { text ->
            binding.tilPassword.error = null
            validatePasswordStrength(text?.toString() ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.btnNextStep.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            if (name.isEmpty()) {
                binding.tilFullName.error = getString(R.string.error_name_empty)
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = getString(R.string.error_valid_email)
                return@setOnClickListener
            }

            // Move to Step 2
            currentStep = 2
            binding.tvStepIndicator.text = "02 / 02"
            binding.layoutStep1.visibility = View.GONE
            binding.layoutStep2.visibility = View.VISIBLE
            binding.tvSignIn.visibility = View.GONE
        }

        binding.btnSignUp.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            lifecycleScope.launch {
                val existingUser = viewModel.getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@SignUpActivity, "Email already registered", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val passwordHash = SecurityUtils.hashPassword(password)
                val newUser = UserEntity(name = name, email = email, passwordHash = passwordHash)
                viewModel.registerUser(newUser)
                
                Toast.makeText(this@SignUpActivity, getString(R.string.toast_account_created), Toast.LENGTH_SHORT).show()
                // Finish and return to LoginActivity explicitly (MANDATORY CONSTRAINT: NO AUTO-LOGIN)
                finish()
            }
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun validatePasswordStrength(pass: String) {
        val hasLen = pass.length >= 8
        val hasNum = pass.any { it.isDigit() }
        val hasUpper = pass.any { it.isUpperCase() }
        val hasLower = pass.any { it.isLowerCase() }
        val hasSpecial = pass.any { !it.isLetterOrDigit() }

        // Update requirements live text colors
        binding.reqLength.setTextColor(getColor(if (hasLen) R.color.success else R.color.muted_text))
        binding.reqNumber.setTextColor(getColor(if (hasNum) R.color.success else R.color.muted_text))
        binding.reqUpper.setTextColor(getColor(if (hasUpper) R.color.success else R.color.muted_text))
        binding.reqLower.setTextColor(getColor(if (hasLower) R.color.success else R.color.muted_text))
        binding.reqSpecial.setTextColor(getColor(if (hasSpecial) R.color.success else R.color.muted_text))

        var passedCount = 0
        if (hasLen) passedCount++
        if (hasNum) passedCount++
        if (hasUpper) passedCount++
        if (hasLower) passedCount++
        if (hasSpecial) passedCount++

        // Update bars
        binding.strengthBar1.setBackgroundColor(getColor(if (passedCount >= 1) R.color.error else R.color.elevated_surface))
        binding.strengthBar2.setBackgroundColor(getColor(if (passedCount >= 3) R.color.amber else R.color.elevated_surface))
        binding.strengthBar3.setBackgroundColor(getColor(if (passedCount == 5) R.color.success else R.color.elevated_surface))

        when {
            passedCount == 5 -> {
                binding.tvStrengthLabel.text = "Strength: Strong"
                binding.tvStrengthLabel.setTextColor(getColor(R.color.success))
                binding.btnSignUp.isEnabled = true
            }
            passedCount >= 3 -> {
                binding.tvStrengthLabel.text = "Strength: Medium"
                binding.tvStrengthLabel.setTextColor(getColor(R.color.amber))
                binding.btnSignUp.isEnabled = false
            }
            else -> {
                binding.tvStrengthLabel.text = "Strength: Weak"
                binding.tvStrengthLabel.setTextColor(getColor(R.color.error))
                binding.btnSignUp.isEnabled = false
            }
        }
    }
}