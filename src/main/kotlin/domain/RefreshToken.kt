package com.martdev.domain

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object RefreshTokens : LongIdTable("refresh_tokens") {
    val userId = reference("user_id", Users.id,
        onDelete = ReferenceOption.CASCADE)
        .index("idx_refresh_tokens_user_id")
    val tokenHash = text("token_hash").uniqueIndex().index("idx_refresh_tokens_token_hash")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val revoked = bool("revoked").default(false)
}

class RefreshToken(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<RefreshToken>(RefreshTokens)

    val userId by RefreshTokens.userId
    val tokenHash by RefreshTokens.tokenHash
    val createdAt by RefreshTokens.createdAt
    val revoked by RefreshTokens.revoked
}