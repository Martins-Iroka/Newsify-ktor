package com.martdev.repository.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object FollowersTable : CompositeIdTable("followers") {
    val creatorID = reference("creator_id", UsersTable, onDelete = ReferenceOption.CASCADE).entityId()
    val readerID = reference("reader_id", UsersTable, onDelete = ReferenceOption.CASCADE).entityId()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey: PrimaryKey = PrimaryKey(creatorID, readerID)
}

class FollowersEntity(id: EntityID<CompositeID>) : CompositeEntity(id) {
    companion object : CompositeEntityClass<FollowersEntity>(FollowersTable)

    var creatorID by FollowersTable.creatorID
    var readerID by FollowersTable.readerID
}