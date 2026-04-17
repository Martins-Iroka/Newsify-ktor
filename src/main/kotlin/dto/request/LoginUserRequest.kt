package com.martdev.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginUserRequest(
    val email: String,
    val password: String
)
