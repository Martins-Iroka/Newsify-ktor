package com.martdev.service.user

import com.martdev.domain.User
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.dto.request.UserRequest
import com.martdev.repository.DbResult
import com.martdev.repository.user_repo.UserRepository
import com.martdev.service.auth.Auth
import com.martdev.service.otp_provider.OtpProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UserServiceImplTest {

    @get:Rule
    val mockK = MockKRule(this)

    @MockK
    private lateinit var repository: UserRepository

    @MockK
    private lateinit var otpProvider: OtpProvider

    @MockK
    private lateinit var auth: Auth

    private lateinit var service: UserService

    @Before
    fun setup() {
        service = UserServiceImpl(repository, otpProvider, auth)
    }

    @Test
    fun `should register user successfully`() = runTest {
        val user = UserRequest(
            email = "test@email.com",
            password = "password",
            username = "username",
            role = "READER"
        )

        val userSlot = slot<User>()
        val otpSlot = slot<String>()

        coEvery {
            repository.saveUserAndVerificationToken(capture(userSlot), any())
        } answers {
            assertEquals(user.email, userSlot.captured.email)
            assertNotEquals(user.password, userSlot.captured.password)
            DbResult.Success(
                User(
                    email = "test@email.com",
                    id = 1
                )
            )
        }

        coEvery {
            otpProvider.sendVerificationCode(capture(otpSlot))
        } answers {
            assertEquals(user.email, otpSlot.captured)
            Pair("emailId", "") }

        val result = service.registerUser(user)

        assertEquals("emailId", result.emailId)
        assertTrue(result.token.isNotEmpty())
    }

    @Test
    fun `should throw bad request exception for invalid email address`() = runTest {
        val user = UserRequest(
            email = "invalid email",
            password = "password",
            username = "username",
            role = "READER"
        )

        assertFailsWith<BadRequestException> {
            service.registerUser(user)
        }

        coVerify(atLeast = 0) {
            repository.saveUserAndVerificationToken(any(), any())
            otpProvider.sendVerificationCode(any())
            repository.deleteUserAndVerificationToken(any())
        }
    }

}