package com.martdev.repository.tables

import com.martdev.domain.Role
import com.martdev.domain.User
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object UsersTable : LongIdTable("users") {
    val email = citext("email").uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val password = text("password")
    val isVerified = bool("is_verified").default(false)
    val role = text("role").default("reader")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class UserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(UsersTable)

    var email by UsersTable.email
    var username by UsersTable.username
    var password by UsersTable.password
    var isVerified by UsersTable.isVerified
    var role by UsersTable.role
}

fun UserEntity.toUserModel() = User(
    id.value,
    email,
    username,
    password,
    isVerified = isVerified,
    Role.valueOf(role)
)
