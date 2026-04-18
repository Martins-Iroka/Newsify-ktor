package com.martdev.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ResendOTPRequest(
    val email: String
)