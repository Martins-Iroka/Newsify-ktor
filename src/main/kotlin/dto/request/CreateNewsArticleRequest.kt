package com.martdev.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateNewsArticleRequest(
    val title: String,
    val content: String,
)
