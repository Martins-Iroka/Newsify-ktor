package com.martdev.repository.reader

import com.martdev.domain.NewsArticleData
import com.martdev.domain.Role
import com.martdev.domain.User
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.tables.*
import com.martdev.repository.tables.FollowersTable.creatorID
import com.martdev.repository.tables.FollowersTable.readerID
import com.martdev.repository.util.withTransaction
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.koin.core.annotation.Single

@Single
class ReaderRepositoryImpl : ReaderRepository {
    override suspend fun getListOfCreators(): DbResult<List<User>> {
        return withTransaction {
            val query = UsersTable
                .select(
                    UsersTable.id,
                    UsersTable.username
                ).where {
                    UsersTable.role eq Role.CREATOR.name
                }.orderBy(UsersTable.createdAt, order = SortOrder.DESC)

            val creators = query.map {
                val id = it[UsersTable.id].value
                val username = it[UsersTable.username]
                User(id = id, username = username)
            }

            DbResult.Success(creators)
        }
    }

    override suspend fun followCreator(
        creatorId: Long,
        readerId: Long
    ): DbResult<Pair<Long, Long>> {
        return withTransaction {
            val c = CompositeID {
                it[creatorID] = EntityID(creatorId, UsersTable)
                it[readerID] = EntityID(readerId, UsersTable)
            }
            val entity = FollowersEntity.new(c) {}
            DbResult.Success(Pair(entity.creatorID.value.value, entity.readerID.value.value))
        }
    }

    override suspend fun unfollowCreator(
        creatorId: Long,
        readerId: Long
    ): DbResult<Unit> {
        return withTransaction {
            val deletedRow = FollowersTable.deleteWhere {
                (FollowersTable.creatorID eq EntityID(creatorId, UsersTable)) and (FollowersTable.readerID eq EntityID(readerId,
                    UsersTable))
            }
            if (deletedRow == 0) {
                DbResult.Failure(DbError.NotFound())
            } else
                DbResult.Success(Unit)
        }
    }

    override suspend fun getAllArticlesByCreatorId(creatorId: Long): DbResult<List<NewsArticleData>> {
        return withTransaction {
            val articles = NewsArticlesTable
                .select(NewsArticlesTable.id,
                    NewsArticlesTable.title,
                    NewsArticlesTable.createdAt)
                .where {
                    NewsArticlesTable.creatorId eq creatorId
                }.orderBy(NewsArticlesTable.createdAt, order = SortOrder.DESC).map {
                    val title = it[NewsArticlesTable.title]
                    val createdAt = it[NewsArticlesTable.createdAt]
                    val id = it[NewsArticlesTable.id].value
                    NewsArticleData(id = id, title = title, createdAt = createdAt)
                }

            DbResult.Success(articles)
        }
    }

    override suspend fun getNewsArticleById(
        creatorId: Long,
        articleId: Long
    ): DbResult<NewsArticleData> {
        return withTransaction {
            val article = NewsArticlesTable
                .select(
                    NewsArticlesTable.title,
                    NewsArticlesTable.content,
                    NewsArticlesTable.createdAt
                ).where {
                    (NewsArticlesTable.creatorId eq creatorId) and (NewsArticlesTable.id eq articleId)
                }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            val title = article[NewsArticlesTable.title]
            val content = article[NewsArticlesTable.content]
            val createdAt = article[NewsArticlesTable.createdAt]

            DbResult.Success(NewsArticleData(
                title = title,
                content = content,
                createdAt = createdAt
            ))
        }
    }

    override suspend fun updateFcmToken(readerId: Long, token: String): DbResult<Unit> {
        return withTransaction {
            UserEntity.findByIdAndUpdate(readerId) {
                it.fcmToken = token
            } ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            DbResult.Success(Unit)
        }
    }
}