package com.eldroid.herdgate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.herdgate.R
import com.eldroid.herdgate.databinding.ActivitySignInBinding
import com.eldroid.herdgate.ui.main.MainActivity
import com.eldroid.herdgate.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupTextWatchers()
    }

    private fun setupListeners() {
        binding.btnSignIn.setOnClickListener {
            handleSignIn()
        }

        binding.tvGoToSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun setupTextWatchers() {
        // Clear errors in real-time when user begins editing
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun handleSignIn() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        var isValid = true

        // Validate Email
        when (val emailResult = ValidationUtils.validateEmail(email)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilEmail.error = emailResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilEmail.error = null
        }

        // Validate Password
        when (val passResult = ValidationUtils.validatePassword(password)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilPassword.error = passResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilPassword.error = null
        }

        if (!isValid) return

        setLoading(true)

        // Perform Firebase Auth Sign In
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                        else -> exception?.localizedMessage ?: "Authentication failed. Please try again."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Enter your registered email"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(binding.etEmail.text?.toString()?.trim().orEmpty())
        }

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("We will send a password reset link to your email.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val resetEmail = input.text.toString().trim()
                if (ValidationUtils.validateEmail(resetEmail) is ValidationUtils.ValidationResult.Valid) {
                    auth.sendPasswordResetEmail(resetEmail)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Reset link sent to $resetEmail", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSignIn.isEnabled = !isLoading
        binding.progressSignIn.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
