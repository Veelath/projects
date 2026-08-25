package com.eldroid.herdgate.utils

import android.util.Patterns

object ValidationUtils {

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errorMessage: String) : ValidationResult()
    }

    /**
     * Validates Full Name field
     */
    fun validateFullName(name: String?): ValidationResult {
        val trimmed = name?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Full name is required")
            trimmed.length < 2 -> ValidationResult.Invalid("Name must be at least 2 characters")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates Email format using Android Patterns
     */
    fun validateEmail(email: String?): ValidationResult {
        val trimmed = email?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Email address is required")
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> ValidationResult.Invalid("Enter a valid email address")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates Password strength and length
     */
    fun validatePassword(password: String?): ValidationResult {
        val text = password.orEmpty()
        return when {
            text.isEmpty() -> ValidationResult.Invalid("Password is required")
            text.length < 6 -> ValidationResult.Invalid("Password must be at least 6 characters")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates Confirm Password matches original Password
     */
    fun validateConfirmPassword(password: String?, confirmPassword: String?): ValidationResult {
        val p1 = password.orEmpty()
        val p2 = confirmPassword.orEmpty()
        return when {
            p2.isEmpty() -> ValidationResult.Invalid("Please confirm your password")
            p1 != p2 -> ValidationResult.Invalid("Passwords do not match")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates User Role selection
     */
    fun validateRole(role: String?): ValidationResult {
        val validRoles = listOf("Farm Owner", "Field Worker", "System Admin")
        val trimmed = role?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid("Please select a role")
            !validRoles.contains(trimmed) -> ValidationResult.Invalid("Please select a valid role")
            else -> ValidationResult.Valid
        }
    }
}
