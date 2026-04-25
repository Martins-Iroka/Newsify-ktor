package com.martdev.repository.user_repo

import com.martdev.domain.Role
import com.martdev.domain.User
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.connectAndMigrate
import com.martdev.repository.postgres
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
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
        username = "testUsername",
        password = "hashed_password",
        role = Role.READER
    )

    @Before
    fun setup() {
        postgres.start()
        connectAndMigrate()
        repository = UserRepositoryImpl()
    }

    @After
    fun tearDown() {
        postgres.stop()
    }


    @Test
    fun `should save user and verification token then retrieve user by email`() = runTest {

        val result = repository.saveUserAndVerificationToken(user, "token_value")
        assertIs<DbResult.Success<User>>(result)
        assertEquals("test@gmail.com", result.value.email)

        val retrievedResult = repository.getUserByEmail("test@gmail.com")
        assertIs<DbResult.Success<User>>(retrievedResult)
        assertEquals("testUsername", retrievedResult.value.username)
    }

    @Test
    fun `should save user and verification token then retrieve user by id`() = runTest {

        val result = repository.saveUserAndVerificationToken(user, "token_value")
        assertIs<DbResult.Success<User>>(result)
        assertEquals("test@gmail.com", result.value.email)

        val retrievedResult = repository.getUserById(result.value.id)
        assertIs<DbResult.Success<User>>(retrievedResult)
        assertEquals("testUsername", retrievedResult.value.username)
    }

    @Test
    fun `should save user and verification token then activate user`() = runTest {
        val tokenValue = "token_value"
        val result = repository.saveUserAndVerificationToken(user, tokenValue)
        assertIs<DbResult.Success<User>>(result)

        val result2 = repository.activateUser(tokenValue)
        assertTrue(result2 is DbResult.Success)
    }

    @Test
    fun `should delete user and verification token`() = runTest {
        val result = repository.saveUserAndVerificationToken(user, "token_value")
        assertIs<DbResult.Success<User>>(result)
        assertEquals("test@gmail.com", result.value.email)

        val result2 = repository.deleteUserAndVerificationToken(result.value.id)
        assertTrue(result2 is DbResult.Success)

        val result3 = repository.getUserById(result.value.id)
        assertTrue(result3 is DbResult.Failure)
        assertTrue(result3.error is DbError.NotFound)
    }

    @Test
    fun `should save refresh token then get user by refresh token`() = runTest {
        val tokenHash = "token_hash"

        val userResult = repository.saveUserAndVerificationToken(user, tokenHash)
        assertIs<DbResult.Success<User>>(userResult)
        val ldt = Clock.System.now().plus(15.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        val refreshTokenResult = repository.saveRefreshToken(
            userResult.value.id,
            "token_hash",
            ldt
        )
        assertIs<DbResult.Success<Unit>>(refreshTokenResult)

        val userResult2 = repository.getUserIdAndRoleByRefreshToken(tokenHash)
        assertTrue(userResult2 is DbResult.Success, userResult2.toString())
        val u = userResult2.value
        assertEquals(userResult.value.id, u.id)
        assertEquals(userResult.value.role, u.role)
    }

    @Test
    fun `should delete expired refresh token`() = runTest {
        val tokenHash = "token_hash"

        val userResult = repository.saveUserAndVerificationToken(user, tokenHash)
        assertIs<DbResult.Success<User>>(userResult)
        val ldt = Clock.System.now().minus(15.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        println(ldt)
        val refreshTokenResult = repository.saveRefreshToken(
            userResult.value.id,
            "token_hash",
            ldt
        )
        assertIs<DbResult.Success<Unit>>(refreshTokenResult)

        val deletedResult = repository.deleteExpiredRefreshToken()
        assertTrue(deletedResult is DbResult.Success)

        val result = repository.getUserIdAndRoleByRefreshToken(tokenHash)
        assertTrue(result is DbResult.Failure)
        assertTrue(result.error is DbError.NotFound)
    }

    @Test
    fun `should revoke refresh token`() = runTest {
        val tokenHash = "token_hash"

        val userResult = repository.saveUserAndVerificationToken(user, tokenHash)
        assertIs<DbResult.Success<User>>(userResult)
        val ldt = Clock.System.now().minus(15.minutes).toLocalDateTime(TimeZone.currentSystemDefault())
        println(ldt)
        val refreshTokenResult = repository.saveRefreshToken(
            userResult.value.id,
            tokenHash,
            ldt
        )
        assertIs<DbResult.Success<Unit>>(refreshTokenResult)

        val revokedResult = repository.revokeRefreshToken(tokenHash)
        assertTrue(revokedResult is DbResult.Success)

        val result = repository.getUserIdAndRoleByRefreshToken(tokenHash)
        assertTrue(result is DbResult.Failure)
        assertTrue(result.error is DbError.NotFound)
    }
}
