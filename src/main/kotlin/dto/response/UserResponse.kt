package com.martdev.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val emailId: String,
    val token: String
)
