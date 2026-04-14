package com.martdev.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val email: String,
    val password: String,
    val username: String,
    val role: String
)