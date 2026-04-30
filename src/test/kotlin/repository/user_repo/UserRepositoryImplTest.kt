package com.martdev.repository.user_repo

import com.martdev.domain.Role
import com.martdev.domain.User
import com.martdev.repository.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes


class UserRepositoryImplTest {

    private lateinit var repository: UserRepository

    val user = User(
        email = "test@gmail.com",
        username = "username",
        password = "hashed_password",
        role = Role.READER
    )

    companion object {
        @BeforeClass
        @JvmStatic
        fun startContainer() {
            postgres.start()
            connectAndMigrate()
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            postgres.stop()
        }
    }

    @Before
    fun setup() {
        resetDbTable()
        repository = UserRepositoryImpl()
    }

    @Test
    fun `should save user and verification token then retrieve user by email`() = runTest {
        val result = repository.saveUserAndVerificationToken(user, "token_value")
        assertIs<DbResult.Success<User>>(result)
        assertEquals("test@gmail.com", result.value.email)

        val retrievedResult = repository.getUserByEmail("test@gmail.com")
        assertIs<DbResult.Success<User>>(retrievedResult)
        assertEquals("username", retrievedResult.value.username)
    }

    @Test
    fun `should save user and verification token then retrieve user by id`() = runTest {
        val result = repository.saveUserAndVerificationToken(user, "token_value")
        assertIs<DbResult.Success<User>>(result)

        val retrievedResult = repository.getUserById(result.value.id)
        assertIs<DbResult.Success<User>>(retrievedResult)
        assertEquals("username", retrievedResult.value.username)
    }

    @Test
    fun `should save user and verification token then activate user`() = runTest {
        val tokenValue = "token_value3"
        val result = repository.saveUserAndVerificationToken(user, tokenValue)
        assertIs<DbResult.Success<User>>(result)

        val result2 = repository.activateUser(tokenValue)
        assertTrue(result2 is DbResult.Success)
    }

    @Test
    fun `should delete user and verification token`() = runTest {
        val result = repository.saveUserAndVerificationToken(user, "token_value4")
        assertIs<DbResult.Success<User>>(result)

        val result2 = repository.deleteUserAndVerificationToken(result.value.id)
        assertTrue(result2 is DbResult.Success)

        val result3 = repository.getUserById(result.value.id)
        assertTrue(result3 is DbResult.Failure)
        assertTrue(result3.error is DbError.NotFound)
    }

    @Test
    fun `should save refresh token then get user by refresh token`() = runTest {
        val refreshTokenHashed = "refresh_token"

        val userResult = repository.saveUserAndVerificationToken(user, "token_hash5")
        assertIs<DbResult.Success<User>>(userResult)

        val ldt = Clock.System.now().plus(15.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        val refreshTokenResult = repository.saveRefreshToken(
            userResult.value.id,
            refreshTokenHashed,
            ldt
        )
        assertIs<DbResult.Success<Unit>>(refreshTokenResult)

        val userResult2 = repository.getUserIdAndRoleByRefreshToken(refreshTokenHashed)
        assertTrue(userResult2 is DbResult.Success, userResult2.toString())
        val u = userResult2.value
        assertEquals(userResult.value.id, u.id)
        assertEquals(userResult.value.role, u.role)
    }

    @Test
    fun `should delete expired refresh token`() = runTest {
        val expiredRefreshToken = "expired_refresh_token"

        val userResult = repository.saveUserAndVerificationToken(user, "token_hash6")
        assertIs<DbResult.Success<User>>(userResult)

        val ldt = Clock.System.now().minus(15.minutes).toLocalDateTime(TimeZone.currentSystemDefault())

        val refreshTokenResult = repository.saveRefreshToken(
            userResult.value.id,
            expiredRefreshToken,
            ldt
        )
        assertIs<DbResult.Success<Unit>>(refreshTokenResult)

        val deletedResult = repository.deleteExpiredRefreshToken()
        assertTrue(deletedResult is DbResult.Success)

        val result = repository.getUserIdAndRoleByRefreshToken(expiredRefreshToken)
        assertTrue(result is DbResult.Failure)
        assertTrue(result.error is DbError.NotFound)
    }

    @Test
    fun `should revoke refresh token`() = runTest {
        val refreshToken = "token_hash"

        val userResult = repository.saveUserAndVerificationToken(user, "token_hash7")
        assertIs<DbResult.Success<User>>(userResult)
        val ldt = Clock.System.now().minus(15.minutes).toLocalDateTime(TimeZone.currentSystemDefault())

        val refreshTokenResult = repository.saveRefreshToken(
            userResult.value.id,
            refreshToken,
            ldt
        )
        assertIs<DbResult.Success<Unit>>(refreshTokenResult)

        val revokedResult = repository.revokeRefreshToken(refreshToken)
        assertTrue(revokedResult is DbResult.Success)

        val result = repository.getUserIdAndRoleByRefreshToken(refreshToken)
        assertTrue(result is DbResult.Failure)
        assertTrue(result.error is DbError.NotFound)
    }
}
