package com.martdev.repository.user_repo

import com.martdev.domain.User
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.tables.*
import com.martdev.repository.tables.UsersVerificationTable.token
import com.martdev.repository.util.withTransaction
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.koin.core.annotation.Single

@Single
class UserRepositoryImpl : UserRepository {
    override suspend fun activateUser(token: String): DbResult<Unit> {
        return withTransaction {
            val resultRow = getUserByVerificationToken(token)
            val userId = resultRow?.get(UsersTable.id) ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            updateUser(userId.value) ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            val deletedRow = deleteUserVerificationToken(userId.value)
            return@withTransaction if (deletedRow > 0) {
                DbResult.Success(Unit)
            } else DbResult.Failure(DbError.UnknownError(Exception("")))
        }
    }

    override suspend fun saveUserAndVerificationToken(
        user: User,
        token: String
    ): DbResult<User> {
        return withTransaction {
            val userEntity = createUser(user)
            createUserVerificationToken(token, userEntity.id.value)
            DbResult.Success(userEntity.toUserModel())
        }
    }

    override suspend fun saveRefreshToken(userId: Long, tokenHash: String, time: LocalDateTime): DbResult<Unit> {
        return withTransaction {
            val entity =
                UserEntity.findById(userId) ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            val id = RefreshTokenEntity.new {
                userEntity = entity
                this.tokenHash = tokenHash
                expiryTime = time
            }.id
            if (id.value > 0) {
                DbResult.Success(Unit)
            } else DbResult.Failure(DbError.UnknownError(Exception("failed to save token")))
        }
    }

    override suspend fun deleteExpiredRefreshToken(): DbResult<Unit> {
        return withTransaction {
            RefreshTokensTable.deleteWhere {
                expiryTime less CurrentDateTime
            }
            DbResult.Success(Unit)
        }
    }

    override suspend fun deleteUserAndVerificationToken(userId: Long): DbResult<Unit> {
        return withTransaction {
            val deletedRow = deleteUser(userId)
            if (deletedRow == 0) {
                return@withTransaction DbResult.Failure(DbError.NotFound())
            }
            val deletedVT = deleteUserVerificationToken(userId)
            if (deletedVT == 0) {
                return@withTransaction DbResult.Failure(DbError.NotFound())
            }
            DbResult.Success(Unit)
        }
    }

    override suspend fun getUserByEmail(email: String): DbResult<User> {
        return withTransaction {
            val entity = UserEntity.find {
                UsersTable.email eq email
            }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            DbResult.Success(entity.toUserModel())
        }
    }

    override suspend fun getUserById(userId: Long): DbResult<User> {
        return withTransaction {
            val entity = UserEntity.find {
                UsersTable.id eq userId
            }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            DbResult.Success(entity.toUserModel())
        }
    }

    override suspend fun getUserByRefreshToken(tokenHash: String): DbResult<Long> {
        return withTransaction {
            val row = UsersTable.join(
                otherTable = RefreshTokensTable,
                joinType = JoinType.INNER,
                onColumn = UsersTable.id,
                otherColumn = RefreshTokensTable.userId
            ).select(UsersTable.id).where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                        (RefreshTokensTable.expiryTime.greater(
                    CurrentDateTime
                )) and (RefreshTokensTable.revoked.eq(
                    false
                ))
            }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            val userId = row[UsersTable.id].value

            DbResult.Success(userId)
        }
    }

    override suspend fun revokeRefreshToken(tokenHash: String): DbResult<Unit> {
        return withTransaction {
            RefreshTokenEntity.findSingleByAndUpdate(RefreshTokensTable.tokenHash.eq(tokenHash)) {
                it.revoked = true
            } ?: return@withTransaction DbResult.Failure(DbError.NotFound())

            DbResult.Success(Unit)
        }
    }

    private fun getUserByVerificationToken(token: String) = UsersTable.join(
        otherTable = UsersVerificationTable,
        joinType = JoinType.INNER,
        onColumn = UsersTable.id,
        otherColumn = UsersVerificationTable.userId
    ).select(UsersTable.id).where {
        UsersVerificationTable.token eq token
    }.firstOrNull()

    private fun updateUser(userId: Long) = UserEntity.findByIdAndUpdate(userId) {
        it.isVerified = true
    }

    private fun deleteUserVerificationToken(userId: Long) = UsersVerificationTable.deleteWhere {
        UsersVerificationTable.userId eq userId
    }

    private fun createUser(user: User) = UserEntity.new {
        email = user.email
        username = user.username
        password = user.password
        role = user.role.name
    }

    private fun createUserVerificationToken(tokenParam: String, uid: Long) {
        val userVerificationId = CompositeID {
            it[token] = tokenParam
        }
        UserVerificationEntity.new(userVerificationId) {
            userId = uid
        }
    }

    private fun deleteUser(userId: Long) = UsersTable.deleteWhere {
        UsersTable.id eq userId
    }
}