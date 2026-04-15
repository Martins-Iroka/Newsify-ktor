package com.martdev.repository.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object RefreshTokensTable : LongIdTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable,
        onDelete = ReferenceOption.CASCADE)
        .index("idx_refresh_tokens_user_id")
    val tokenHash = text("token_hash").uniqueIndex().index("idx_refresh_tokens_token_hash")
    val expiryTime = datetime("expires_at")
    val revoked = bool("revoked").default(false)
}

class RefreshTokenEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<RefreshTokenEntity>(RefreshTokensTable)

    var userEntity by UserEntity referencedOn RefreshTokensTable.userId
    var tokenHash by RefreshTokensTable.tokenHash
    var expiryTime by RefreshTokensTable.expiryTime
    var revoked by RefreshTokensTable.revoked
}