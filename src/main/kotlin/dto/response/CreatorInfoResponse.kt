package com.martdev.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class CreatorInfoResponse(
    val id: Long,
    val username: String
)
