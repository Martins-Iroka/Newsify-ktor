package com.martdev.repository.tables

import com.martdev.domain.NewsArticleData
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object NewsArticlesTable : LongIdTable("news_article") {
    val title = text("title").uniqueIndex()
    val content = text("content")
    val creatorId = long("creator_id")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class NewsArticleEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<NewsArticleEntity>(NewsArticlesTable)

    var title by NewsArticlesTable.title
    var content by NewsArticlesTable.content
    var creatorId by NewsArticlesTable.creatorId
    var createdAt by NewsArticlesTable.createdAt
}

fun NewsArticleEntity.toNewsArticleData() = NewsArticleData(
    id = id.value,
    title,
    content,
    creatorId,
    createdAt = createdAt
)