package com.martdev.repository.creator_repo

import com.martdev.domain.NewsArticleData
import com.martdev.domain.User
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.tables.FollowersTable
import com.martdev.repository.tables.FollowersTable.creatorID
import com.martdev.repository.tables.FollowersTable.readerID
import com.martdev.repository.tables.NewsArticleEntity
import com.martdev.repository.tables.NewsArticlesTable
import com.martdev.repository.tables.UsersTable
import com.martdev.repository.util.withTransaction
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.koin.core.annotation.Single

@Single
class CreatorRepositoryImpl : CreatorRepository {
    override suspend fun saveNewsArticle(articleData: NewsArticleData): DbResult<Long> {
        return withTransaction {
            val id = NewsArticleEntity.new {
                title = articleData.title
                content = articleData.content
                creatorId = articleData.creatorId
            }.id.value
            DbResult.Success(id)
        }
    }

    override suspend fun getNewsArticleById(
        creatorId: Long,
        articleId: Long
    ): DbResult<NewsArticleData> {
        return withTransaction {
            val rowQuery = NewsArticlesTable
                .select(
                    NewsArticlesTable.title,
                    NewsArticlesTable.content,
                    NewsArticlesTable.createdAt
                ).where {
                    (NewsArticlesTable.creatorId eq creatorId) and (NewsArticlesTable.id eq articleId)
                }.orderBy(NewsArticlesTable.createdAt, order = SortOrder.DESC).firstOrNull() ?: return@withTransaction DbResult.Failure(
                DbError.NotFound())

            val title = rowQuery[NewsArticlesTable.title]
            val content = rowQuery[NewsArticlesTable.content]
            val date = rowQuery[NewsArticlesTable.createdAt]
            val data = NewsArticleData(
                title = title,
                content = content,
                createdAt = date
            )
            DbResult.Success(data)
        }
    }

    override suspend fun getAllNewsArticleByCreatorId(
        creatorId: Long,
        limit: Int,
        offset: Long
    ): DbResult<List<NewsArticleData>> {
        return withTransaction {
            val entities = NewsArticlesTable
                .select(NewsArticlesTable.id,
                    NewsArticlesTable.title,
                    NewsArticlesTable.createdAt
                ).where(NewsArticlesTable.creatorId eq creatorId)
                .orderBy(NewsArticlesTable.createdAt, SortOrder.DESC)
                .limit(limit).offset(offset)

            val newsArticleDataList = entities.map {
                val id = it[NewsArticlesTable.id].value
                val title = it[NewsArticlesTable.title]
                val createdAt = it[NewsArticlesTable.createdAt]
                NewsArticleData(id = id, title = title, createdAt = createdAt)
            }
            DbResult.Success(newsArticleDataList)
        }
    }

    override suspend fun deleteNewsArticle(
        creatorId: Long,
        articleId: Long
    ): DbResult<Unit> {
        return withTransaction {
            val deletedRow = NewsArticlesTable.deleteWhere {
                (NewsArticlesTable.creatorId eq creatorId) and (NewsArticlesTable.id eq articleId)
            }
            if (deletedRow <= 0) {
                DbResult.Failure(DbError.UnknownError(Exception("failed to delete news article")))
            } else DbResult.Success(Unit)
        }
    }

    override suspend fun updateNewsArticle(newsArticleData: NewsArticleData): DbResult<NewsArticleData> {
        return withTransaction {

            val entity = NewsArticleEntity.findByIdAndUpdate(
                newsArticleData.id
            ) {
                it.title = newsArticleData.title
                it.content = newsArticleData.content
            } ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            DbResult.Success(
                NewsArticleData(
                    id = entity.id.value,
                    title = entity.title,
                    content = entity.content

            ))
        }
    }

    override suspend fun getFollowersByCreatorId(creatorId: Long): DbResult<List<User>> {
        return withTransaction {
            val followers2 = FollowersTable
                .join(
                    otherTable = UsersTable,
                    joinType = JoinType.INNER,
                    onColumn = readerID,
                    otherColumn = UsersTable.id
                )
                .select(UsersTable.id, UsersTable.username)
                .where { creatorID eq EntityID(creatorId, UsersTable) }
                .map { row ->
                    User(
                        id = row[UsersTable.id].value,
                        username = row[UsersTable.username],
                    )
                }
            DbResult.Success(followers2)
        }
    }
}