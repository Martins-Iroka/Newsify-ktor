package com.martdev.domain

data class UserVerification(
    val token: String,
    val userId: Long
)
