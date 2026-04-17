package com.martdev.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyUserRequest(
    val code: String,
    @SerialName("email_id")
    val emailId: String,
    val token: String
)
