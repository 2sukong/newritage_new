package com.newritage.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val salt: String,
    val email: String,
    val phone: String,
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0L
)
