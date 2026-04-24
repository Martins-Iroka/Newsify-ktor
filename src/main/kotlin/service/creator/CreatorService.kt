package com.martdev.service.creator

import com.martdev.dto.request.CreateNewsArticleRequest
import com.martdev.dto.response.NewsArticleResponse

interface CreatorService {
    suspend fun saveNewsArticle(creatorId: Long, data: CreateNewsArticleRequest): Long
    suspend fun getNewsArticleById(creatorId: Long, articleId: Long): NewsArticleResponse
    suspend fun getAllNewsArticleByCreatorId(creatorId: Long, limit: Int, offset: Long): List<NewsArticleResponse>
    suspend fun deleteNewsArticle(creatorId: Long, articleId: Long)
    suspend fun updateNewsArticle(creatorId: Long, articleId: Long, newsArticleDataDto: CreateNewsArticleRequest): NewsArticleResponse
}