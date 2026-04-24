package com.martdev.service.creator

import com.martdev.domain.NewsArticleData
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.dto.request.CreateNewsArticleRequest
import com.martdev.dto.response.NewsArticleResponse
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.creator_repo.CreatorRepository
import org.koin.core.annotation.Single

@Single
class CreatorServiceImpl(
    private val repository: CreatorRepository
) : CreatorService{
    override suspend fun saveNewsArticle(creatorId: Long, data: CreateNewsArticleRequest): Long {
        validateNewsArticleData(data)
        val newsArticleData = NewsArticleData(
            title = data.title,
            content = data.content,
            creatorId = creatorId
        )
        return when(val result = repository.saveNewsArticle(newsArticleData)) {
            is DbResult.Failure -> {
                when(result.error) {
                    DbError.UniqueViolation -> throw BadRequestException("duplicate title")
                    else -> throw InternalServerException()
                }
            }
            is DbResult.Success -> result.value
        }
    }

    override suspend fun getNewsArticleById(
        creatorId: Long,
        articleId: Long
    ): NewsArticleResponse {
        return when(val result = repository.getNewsArticleById(creatorId, articleId)) {
            is DbResult.Failure -> {
                if (result.error is DbError.NotFound) {
                    throw NotFoundException()
                } else throw InternalServerException()
            }
            is DbResult.Success -> {
                val entity = result.value
                NewsArticleResponse(
                    title = entity.title,
                    content = entity.content,
                    createdAt = entity.createdAt.toString()
                )
            }
        }
    }

    override suspend fun getAllNewsArticleByCreatorId(
        creatorId: Long,
        limit: Int,
        offset: Long
    ): List<NewsArticleResponse> {
        return when(val result = repository.getAllNewsArticleByCreatorId(
            creatorId, limit, offset
        )) {
            is DbResult.Failure -> throw InternalServerException()
            is DbResult.Success -> {
                result.value.map {
                    NewsArticleResponse(
                        id = it.id,
                        title = it.title,
                        createdAt = it.createdAt.toString()
                    )
                }
            }
        }
    }

    override suspend fun deleteNewsArticle(creatorId: Long, articleId: Long) {
        when(repository.deleteNewsArticle(creatorId, articleId)) {
            is DbResult.Failure -> throw InternalServerException()
            is DbResult.Success -> Unit
        }
    }

    override suspend fun updateNewsArticle(
        creatorId: Long,
        newsArticleDataDto: CreateNewsArticleRequest
    ): NewsArticleResponse {
        validateNewsArticleData(newsArticleDataDto)
        val data = NewsArticleData(
            title = newsArticleDataDto.title,
            content = newsArticleDataDto.content
        )
        return when(val result = repository.updateNewsArticle(data)) {
            is DbResult.Failure -> {
                if (result.error is DbError.NotFound) {
                    throw NotFoundException()
                } else throw InternalServerException()
            }
            is DbResult.Success -> {
                val updatedNewsArticle = result.value
                NewsArticleResponse(
                    updatedNewsArticle.id,
                    updatedNewsArticle.title,
                    updatedNewsArticle.content
                )
            }
        }
    }

    private fun validateNewsArticleData(dto: CreateNewsArticleRequest) {

        when {
            dto.title.isEmpty() -> throw BadRequestException("title is required")
            dto.content.isEmpty() -> throw BadRequestException("content is required")
        }
    }
}