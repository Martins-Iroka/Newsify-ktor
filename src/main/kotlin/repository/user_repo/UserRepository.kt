package com.martdev.repository.user_repo

import com.martdev.domain.User
import com.martdev.repository.DbResult
import kotlinx.datetime.LocalDateTime

interface UserRepository {
    suspend fun activateUser(token: String): DbResult<Unit>
    suspend fun saveUserAndVerificationToken(user: User, token: String): DbResult<User>
    suspend fun saveRefreshToken(userId: Long, tokenHash: String, time: LocalDateTime): DbResult<Unit>
    suspend fun deleteExpiredRefreshToken(): DbResult<Unit>
    suspend fun deleteUserAndVerificationToken(userId: Long): DbResult<Unit>
    suspend fun getUserByEmail(email: String): DbResult<User>
    suspend fun getUserById(userId: Long): DbResult<User>
    suspend fun getUserIdAndRoleByRefreshToken(tokenHash: String): DbResult<User>
    suspend fun revokeRefreshToken(tokenHash: String): DbResult<Unit>
    suspend fun deleteAndCreateVerificationToken(token: String, userId: Long): DbResult<Unit>
}