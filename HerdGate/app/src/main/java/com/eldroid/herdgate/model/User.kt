package com.eldroid.herdgate.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    @DocumentId
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "Farm Owner", // "Farm Owner", "Field Worker", "System Admin"
    val createdAt: Long = System.currentTimeMillis()
)
