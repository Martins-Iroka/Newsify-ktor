package com.martdev.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class UserVerificationResponse(
    val status: String
)
