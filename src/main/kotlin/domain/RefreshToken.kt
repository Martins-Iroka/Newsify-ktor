package com.martdev.domain

data class RefreshToken(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val createdAt: String,
    val revoked: Boolean
)
