package com.martdev.repository.user_repo

import com.martdev.domain.User
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.tables.RefreshTokenEntity
import com.martdev.repository.tables.RefreshTokensTable
import com.martdev.repository.tables.UserEntity
import com.martdev.repository.tables.UserVerificationEntity
import com.martdev.repository.tables.UsersTable
import com.martdev.repository.tables.UsersVerificationTable
import com.martdev.repository.tables.UsersVerificationTable.token
import com.martdev.repository.util.withTransaction
import com.stytch.java.consumer.api.users.Users
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select

class UserRepositoryImpl : UserRepository {
    override suspend fun activateUser(token: String): DbResult<Unit> {
        return withTransaction {
            try {
                val query = getUserByVerificationToken(token)
                val userId = query?.get(UsersTable.id) ?: return@withTransaction DbResult.Failure(DbError.NotFound())

                updateUser(userId.value) ?: return@withTransaction DbResult.Failure(DbError.NotFound())

                val deletedRow = deleteUserVerificationToken(userId.value)
                return@withTransaction if (deletedRow > 0) {
                    DbResult.Success(Unit)
                } else DbResult.Failure(DbError.UnknownError(Exception("")))
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.UniqueViolation("${e.sqlState}: ${e.message}"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun saveUserAndVerificationToken(
        user: User,
        token: String
    ): DbResult<Unit> {
        return withTransaction {
            try {
                val uid = createUser(user).value
                createUserVerificationToken(token, uid)
                DbResult.Success(Unit)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.UniqueViolation("${e.sqlState}: ${e.message}"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun saveRefreshToken(userId: Long, tokenHash: String, time: LocalDateTime): DbResult<Unit> {
        return withTransaction {
            try {
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
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.UniqueViolation("${e.sqlState}: ${e.message}"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun deleteExpiredRefreshToken(): DbResult<Unit> {
        return withTransaction {
            try {
                RefreshTokensTable.deleteWhere {
                    expiryTime less CurrentDateTime
                }
                DbResult.Success(Unit)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.ConnectionError(e.message ?: "Database error while deleting token"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun deleteUserAndVerificationToken(userId: Long): DbResult<Unit> {
        return withTransaction {
            try {
                val deletedRow = deleteUser(userId)
                if (deletedRow == 0) {
                    return@withTransaction DbResult.Failure(DbError.NotFound())
                }
                val deletedVT = deleteUserVerificationToken(userId)
                if (deletedVT == 0) {
                    return@withTransaction DbResult.Failure(DbError.NotFound())
                }
                DbResult.Success(Unit)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.ConnectionError(e.message ?: "Database error found while deleting user and verification token"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun getUserByEmail(email: String): DbResult<UserEntity> {
        return withTransaction {
            try {
                val entity = UserEntity.find {
                    UsersTable.email eq email
                }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

                DbResult.Success(entity)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.ConnectionError(e.message ?: "Database error found while getting user by email"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun getUserById(userId: Long): DbResult<UserEntity> {
        return withTransaction {
            try {
                val entity = UserEntity.find {
                    UsersTable.id eq userId
                }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

                DbResult.Success(entity)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.ConnectionError(e.message ?: "Database error found while getting user by id"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun getUserByRefreshToken(tokenHash: String): DbResult<UserEntity> {
        return withTransaction {
            try {
                val row = UsersTable.join(
                    otherTable = RefreshTokensTable,
                    joinType = JoinType.INNER,
                    onColumn = UsersTable.id,
                    otherColumn = RefreshTokensTable.userId
                ).select(UsersTable.id).where {
                    RefreshTokensTable.tokenHash eq tokenHash and RefreshTokensTable.expiryTime.greater(CurrentDateTime) and RefreshTokensTable.revoked.eq(
                        false
                    )
                }.firstOrNull() ?: return@withTransaction DbResult.Failure(DbError.NotFound())

                val entity = UserEntity.wrapRow(row)

                DbResult.Success(entity)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.ConnectionError(e.message ?: "Database error found while getting user by refresh token"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

    override suspend fun revokeRefreshToken(tokenHash: String): DbResult<Unit> {
        return withTransaction {
            try {
                RefreshTokenEntity.findSingleByAndUpdate(RefreshTokensTable.tokenHash.eq(tokenHash)) {
                    it.revoked = true
                } ?: return@withTransaction DbResult.Failure(DbError.NotFound())

                DbResult.Success(Unit)
            } catch (e: ExposedSQLException) {
                DbResult.Failure(DbError.ConnectionError(e.message ?: "Database error found while revoking refresh token"))
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
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
        role = user.role.name.lowercase()
    }.id

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