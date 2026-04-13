package com.martdev.domain

enum class Role {
    CREATOR, READER
}
data class User(
    val id: Long,
    val username: String,
    val email: String,
    val password: String,
    val isVerified: Boolean,
    val role: Role,
    val createdAt: String
)
