package com.martdev.domain

import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

object UsersVerification : CompositeIdTable("users_verification_tracking") {
    val token = varchar("token", 255).entityId()
    val userId = long("user_id")

    override val primaryKey: PrimaryKey = PrimaryKey(token)
}

class UserVerification(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<UserVerification>(UsersVerification)

    var token by UsersVerification.token
    var userId by UsersVerification.userId
}