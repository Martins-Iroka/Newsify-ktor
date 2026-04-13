package com.martdev.repository.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable.id,
        onDelete = ReferenceOption.CASCADE)
        .index("idx_refresh_tokens_user_id")
    val tokenHash = text("token_hash").uniqueIndex().index("idx_refresh_tokens_token_hash")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val revoked = bool("revoked").default(false)
}

class RefreshTokenEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<RefreshTokenEntity>(RefreshTokensTable)

    val userId by RefreshTokensTable.userId
    val tokenHash by RefreshTokensTable.tokenHash
    val createdAt by RefreshTokensTable.createdAt
    val revoked by RefreshTokensTable.revoked
}