package com.martdev.domain

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.*

object Users : LongIdTable("users") {
    val email = citext("email").uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val password = text("password")
    val isVerified = bool("is_verified").default(false)
    val role = text("role")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users)

    var email by Users.email
    var username by Users.username
    var password by Users.password
    var isVerified by Users.isVerified
    var role by Users.role
    var createdAt by Users.createdAt
}
