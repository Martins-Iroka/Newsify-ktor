package com.martdev.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsArticleResponse(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("created_at")
    val createdAt: String = ""
)
