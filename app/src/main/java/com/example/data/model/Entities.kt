package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val password: String,
    val familyCode: String? = null,
    val roleInFamily: String? = null,
    val status: String = "Offline", // Online, Offline, Di Luar, Di Rumah
    val battery: Int = 100,
    val phoneNumber: String = "",
    val emergencyContact: String = "",
    val lastLocationName: String = "Belum Diketahui",
    val lastSeen: String = "Baru saja"
)

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey val code: String, // E.g., FAM-123456
    val name: String
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val familyCode: String,
    val userName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
