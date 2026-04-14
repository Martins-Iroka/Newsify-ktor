package com.martdev.domain

import kotlinx.datetime.LocalDateTime

enum class Role {
    CREATOR, READER
}
data class User(
    val id: Long,
    val email: String,
    val username: String,
    val password: String,
    val isVerified: Boolean,
    val role: Role,
    val createdAt: LocalDateTime
)
