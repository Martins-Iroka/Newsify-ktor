package com.martdev.service.reader

import com.martdev.domain.NewsArticleData
import com.martdev.domain.User
import com.martdev.repository.DbResult

interface ReaderRepository {
    suspend fun getListOfCreators(): DbResult<List<User>>
    suspend fun followCreator(creatorId: Long, readerId: Long): DbResult<Pair<Long, Long>>
    suspend fun unfollowCreator(creatorId: Long, readerId: Long): DbResult<Unit>
    suspend fun getAllArticlesByCreatorId(creatorId: Long): DbResult<List<NewsArticleData>>
    suspend fun getNewsArticleById(creatorId: Long, articleId: Long): DbResult<NewsArticleData>
}