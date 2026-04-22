package com.martdev.repository.creator_repo

import com.martdev.domain.NewsArticleData
import com.martdev.repository.DbResult

interface CreatorRepository {
    suspend fun saveNewsArticle(articleData: NewsArticleData): DbResult<Long>
    suspend fun getNewsArticleById(creatorId: Long, articleId: Long): DbResult<NewsArticleData>
    suspend fun getAllNewsArticleByCreatorId(creatorId: Long, limit: Int, offset: Long): DbResult<List<NewsArticleData>>
    suspend fun deleteNewsArticle(creatorId: Long, articleId: Long): DbResult<Unit>
    suspend fun updateNewsArticle(newsArticleData: NewsArticleData): DbResult<NewsArticleData>
}