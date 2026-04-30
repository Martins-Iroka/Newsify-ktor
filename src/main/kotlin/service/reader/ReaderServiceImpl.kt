package com.martdev.service.reader

import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.dto.response.CreatorInfoResponse
import com.martdev.dto.response.NewsArticleResponse
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.reader.ReaderRepository
import org.koin.core.annotation.Single

@Single
class ReaderServiceImpl(
    private val readerRepository: ReaderRepository
) : ReaderService {
    override suspend fun getListOfCreators(): List<CreatorInfoResponse> {
        return when (val result = readerRepository.getListOfCreators()) {
            is DbResult.Failure -> throw InternalServerException()
            is DbResult.Success -> {
                result.value.map {
                    CreatorInfoResponse(id = it.id, username = it.username)
                }
            }
        }
    }

    override suspend fun followCreator(creatorId: Long, readerId: Long) {
        val result = readerRepository.followCreator(creatorId, readerId)
        if (result is DbResult.Failure) {
            if (result.error is DbError.UniqueViolation) {
                throw BadRequestException("You can't follow yourself!")
            }
            throw InternalServerException()
        }
    }

    override suspend fun unfollowCreator(creatorId: Long, readerId: Long) {
        val result = readerRepository.unfollowCreator(creatorId, readerId)
        if (result is DbResult.Failure) {
            if (result.error is DbError.NotFound) {
                throw NotFoundException()
            }
            throw InternalServerException()
        }
    }

    override suspend fun getAllArticlesByCreators(
        creatorIds: List<Long>,
        limit: Int,
        offset: Long
    ): List<NewsArticleResponse> {
        return when(val result = readerRepository.getAllArticlesByCreators(creatorIds, limit, offset)) {
            is DbResult.Failure -> throw InternalServerException()
            is DbResult.Success -> {
                result.value.map {
                    NewsArticleResponse(id = it.id, title = it.title, createdAt = it.createdAt.toString())
                }
            }
        }
    }

    override suspend fun getNewsArticleById(
        creatorId: Long,
        articleId: Long
    ): NewsArticleResponse {
        return when(val result = readerRepository.getNewsArticleById(creatorId, articleId)) {
            is DbResult.Failure -> {
                if (result.error is DbError.NotFound) {
                    throw NotFoundException()
                }
                throw InternalServerException()
            }
            is DbResult.Success -> {
                val article = result.value
                NewsArticleResponse(
                    title = article.title,
                    content = article.content,
                    createdAt = article.createdAt.toString()
                )
            }
        }
    }

    override suspend fun updateFcmToken(readerId: Long, token: String) {
        val result = readerRepository.updateFcmToken(readerId, token)
        if (result is DbResult.Failure) {
            if (result.error is DbError.NotFound) {
                throw NotFoundException()
            }
            throw InternalServerException()
        }
    }
}