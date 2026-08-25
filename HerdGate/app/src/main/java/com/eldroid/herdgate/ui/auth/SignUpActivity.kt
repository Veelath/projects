package com.eldroid.herdgate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.herdgate.R
import com.eldroid.herdgate.databinding.ActivitySignUpBinding
import com.eldroid.herdgate.model.User
import com.eldroid.herdgate.ui.main.MainActivity
import com.eldroid.herdgate.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val roles = arrayOf("Farm Owner", "Field Worker", "System Admin")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleDropdown()
        setupListeners()
        setupTextWatchers()
    }

    private fun setupRoleDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actRole.setAdapter(adapter)
        binding.actRole.setText(roles[0], false) // Default to Farm Owner
    }

    private fun setupListeners() {
        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }

        binding.tvGoToSignIn.setOnClickListener {
            finish() // Return to SignInActivity
        }
    }

    private fun setupTextWatchers() {
        binding.etFullName.addTextChangedListener(createClearErrorWatcher(binding.tilFullName))
        binding.etSignUpEmail.addTextChangedListener(createClearErrorWatcher(binding.tilSignUpEmail))
        binding.actRole.addTextChangedListener(createClearErrorWatcher(binding.tilRole))
        binding.etSignUpPassword.addTextChangedListener(createClearErrorWatcher(binding.tilSignUpPassword))
        binding.etConfirmPassword.addTextChangedListener(createClearErrorWatcher(binding.tilConfirmPassword))
    }

    private fun createClearErrorWatcher(til: com.google.android.material.textfield.TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                til.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun handleSignUp() {
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etSignUpEmail.text?.toString()?.trim().orEmpty()
        val role = binding.actRole.text?.toString()?.trim().orEmpty()
        val password = binding.etSignUpPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        var isValid = true

        // 1. Validate Name
        when (val nameResult = ValidationUtils.validateFullName(fullName)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilFullName.error = nameResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilFullName.error = null
        }

        // 2. Validate Email
        when (val emailResult = ValidationUtils.validateEmail(email)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilSignUpEmail.error = emailResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilSignUpEmail.error = null
        }

        // 3. Validate Role
        when (val roleResult = ValidationUtils.validateRole(role)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilRole.error = roleResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilRole.error = null
        }

        // 4. Validate Password
        when (val passResult = ValidationUtils.validatePassword(password)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilSignUpPassword.error = passResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilSignUpPassword.error = null
        }

        // 5. Validate Password Match
        when (val matchResult = ValidationUtils.validateConfirmPassword(password, confirmPassword)) {
            is ValidationUtils.ValidationResult.Invalid -> {
                binding.tilConfirmPassword.error = matchResult.errorMessage
                isValid = false
            }
            ValidationUtils.ValidationResult.Valid -> binding.tilConfirmPassword.error = null
        }

        if (!isValid) return

        setLoading(true)

        // Create user in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid.orEmpty()

                    // Update Firebase Auth profile display name
                    val profileUpdates = userProfileChangeRequest {
                        displayName = fullName
                    }
                    firebaseUser?.updateProfile(profileUpdates)

                    // Create User object to store in Cloud Firestore
                    val newUser = User(
                        uid = uid,
                        fullName = fullName,
                        email = email,
                        role = role,
                        createdAt = System.currentTimeMillis()
                    )

                    // Save user document into Firestore: users/{uid}
                    firestore.collection("users").document(uid)
                        .set(newUser)
                        .addOnCompleteListener { dbTask ->
                            setLoading(false)
                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Registered, but failed to save profile data: ${dbTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        }
                } else {
                    setLoading(false)
                    val exception = task.exception
                    val errorMessage = if (exception is FirebaseAuthUserCollisionException) {
                        "An account already exists with this email address."
                    } else {
                        exception?.localizedMessage ?: "Registration failed. Please try again."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSignUp.isEnabled = !isLoading
        binding.progressSignUp.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
