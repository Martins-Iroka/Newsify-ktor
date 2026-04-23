package com.martdev.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsArticleDataDto(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    @SerialName("creator_id")
    val creatorId: Long = 0,
    @SerialName("created_at")
    val createdAt: String = ""
)
