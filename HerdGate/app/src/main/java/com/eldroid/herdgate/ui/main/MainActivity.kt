package com.eldroid.herdgate.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eldroid.herdgate.R
import com.eldroid.herdgate.databinding.ActivityMainBinding
import com.eldroid.herdgate.model.User
import com.eldroid.herdgate.ui.auth.SignInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verifyAuthAndLoadUserData()
        setupListeners()
        listenToLiveHerdCount()
    }

    private fun verifyAuthAndLoadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            redirectToSignIn()
            return
        }

        binding.tvUserGreeting.text = "Welcome, ${currentUser.displayName ?: "Farmer"}!"
        binding.tvUserEmail.text = currentUser.email.orEmpty()

        // Fetch user role and detailed profile from Cloud Firestore: users/{uid}
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    if (user != null) {
                        binding.tvUserGreeting.text = "Welcome, ${user.fullName}!"
                        binding.tvUserRoleBadge.text = "Role: ${user.role}"
                    }
                }
            }
            .addOnFailureListener {
                // Fallback to Auth display name if network fails
            }
    }

    private fun setupListeners() {
        binding.btnSignOut.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            redirectToSignIn()
        }
    }

    private fun listenToLiveHerdCount() {
        // Real-time synchronization from Firestore as per HerdGate FR-04: gates/{gate_id} or system/stats
        firestore.collection("system_metrics").document("herd_gate_status")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val count = snapshot.getLong("live_herd_count") ?: 0L
                    val status = snapshot.getString("capacity_status") ?: "Normal"
                    
                    binding.tvLiveHerdCount.text = count.toString()
                    binding.tvThresholdStatus.text = "Capacity Status: $status"
                }
            }
    }

    private fun redirectToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
