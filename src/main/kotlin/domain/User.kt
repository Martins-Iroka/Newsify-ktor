package com.martdev.domain

enum class Role {
    CREATOR, READER
}
data class User(
    val id: Long = 0,
    val email: String,
    val username: String,
    val password: String,
    val role: Role,
)
