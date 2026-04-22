package com.martdev.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class NewsArticleData(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val creatorId: Long = 0,
    val createdAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
)
