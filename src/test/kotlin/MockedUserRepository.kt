package com.martdev

import com.martdev.domain.User
import com.martdev.repository.DbResult
import com.martdev.repository.user_repo.UserRepository
import kotlinx.datetime.LocalDateTime

class MockedUserRepository : UserRepository {
    override suspend fun activateUser(token: String): DbResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun saveUserAndVerificationToken(
        user: User,
        token: String
    ): DbResult<User> {
        TODO("Not yet implemented")
    }

    override suspend fun saveRefreshToken(
        userId: Long,
        tokenHash: String,
        time: LocalDateTime
    ): DbResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteExpiredRefreshToken(): DbResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteUserAndVerificationToken(userId: Long): DbResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserByEmail(email: String): DbResult<User> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserById(userId: Long): DbResult<User> {
        TODO("Not yet implemented")
    }

    override suspend fun getUserIdByRefreshToken(tokenHash: String): DbResult<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun revokeRefreshToken(tokenHash: String): DbResult<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAndCreateVerificationToken(
        token: String,
        userId: Long
    ): DbResult<Unit> {
        TODO("Not yet implemented")
    }
}