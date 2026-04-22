package com.martdev.domain

import kotlinx.datetime.LocalDateTime

data class NewsArticleData(
    val id: Long,
    val title: String,
    val content: String,
    val creatorId: Long,
    val createdAt: LocalDateTime
)
