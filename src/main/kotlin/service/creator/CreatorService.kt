package com.martdev.service.creator

import com.martdev.dto.response.NewsArticleDataDto

interface CreatorService {
    suspend fun saveNewsArticle(data: NewsArticleDataDto): Long
    suspend fun getNewsArticleById(creatorId: Long, articleId: Long): NewsArticleDataDto
    suspend fun getAllNewsArticleByCreatorId(creatorId: Long, limit: Int, offset: Long): List<NewsArticleDataDto>
    suspend fun deleteNewsArticle(creatorId: Long, articleId: Long)
    suspend fun updateNewsArticle(newsArticleDataDto: NewsArticleDataDto): NewsArticleDataDto
}