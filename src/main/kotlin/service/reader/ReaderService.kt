package com.martdev.service.reader

import com.martdev.dto.response.CreatorInfoResponse
import com.martdev.dto.response.NewsArticleResponse

interface ReaderService {
    suspend fun getListOfCreators(): List<CreatorInfoResponse>
    suspend fun followCreator(creatorId: Long, readerId: Long)
    suspend fun unfollowCreator(creatorId: Long, readerId: Long)
    suspend fun getAllArticlesByCreatorId(creatorId: Long): List<NewsArticleResponse>
    suspend fun getNewsArticleById(creatorId: Long, articleId: Long): NewsArticleResponse
    suspend fun updateFcmToken(readerId: Long, token: String)
}