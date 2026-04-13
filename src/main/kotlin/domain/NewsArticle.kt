package com.martdev.domain

data class NewsArticle(
    val id: Long,
    val title: String,
    val content: String,
    val creatorId: Long,
    val createdAt: String
)
