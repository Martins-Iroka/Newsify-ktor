package com.martdev.service.user

import com.martdev.domain.User
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.request.*
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.user_repo.UserRepository
import com.martdev.service.auth.Auth
import com.martdev.service.otp_provider.OtpProvider
import com.martdev.util.PasswordHasher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

    private val user = UserRequest(
        email = "test@email.com",
        password = "password",
        username = "username",
        role = "READER"
    )

    @Test
    fun `should register user successfully`() = runTest {

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

        val exception = assertFailsWith<BadRequestException> {
            service.registerUser(user)
        }

        assertEquals("invalid email format", exception.error)

        coVerify(atLeast = 0) {
            repository.saveUserAndVerificationToken(any(), any())
            otpProvider.sendVerificationCode(any())
            repository.deleteUserAndVerificationToken(any())
        }
    }

    @Test
    fun `should throw bad request exception for duplicate email or username`() = runTest {

        coEvery {
            repository.saveUserAndVerificationToken(any(), any())
        } returns DbResult.Failure(DbError.UniqueViolation)

        val exception = assertFailsWith<BadRequestException> {
            service.registerUser(user)
        }

        assertEquals("duplicate email or username", exception.error)
    }

    @Test
    fun `should throw internal server exception for unknown db error`() = runTest {
        coEvery {
            repository.saveUserAndVerificationToken(any(), any())
        } returns DbResult.Failure(DbError.UnknownError(RuntimeException("error")))

        val internalServerException = assertFailsWith<InternalServerException> {
            service.registerUser(user)
        }

        assertEquals("the server encountered a problem", internalServerException.error)
    }

    @Test
    fun `should throw internal server exception for otp error`() = runTest {
        coEvery {
            repository.saveUserAndVerificationToken(any(), any())
        } returns DbResult.Success(
            User(
                email = "test@email.com",
                id = 1
            )
        )

        coEvery {
            otpProvider.sendVerificationCode(any())
        } returns Pair("", "error")

        val internalServerException = assertFailsWith<InternalServerException> {
            service.registerUser(user)
        }

        assertEquals("failed to send OTP", internalServerException.error)
    }

    private val request = UserVerificationRequest(
        "123456",
        "emailId",
        token = "token"
    )

    @Test
    fun `should request user verification then returns user verification response`() = runTest {

        val emailIdSlot = slot<String>()
        val codeSlot = slot<String>()
        val tokenSlot = slot<String>()
        coEvery {
            otpProvider.verifyCode(capture(emailIdSlot), capture(codeSlot))
        } answers {
            assertEquals(request.code, codeSlot.captured)
            assertEquals(request.emailId, emailIdSlot.captured)
            Pair(true, "")
        }

        coEvery {
            repository.activateUser(capture(tokenSlot))
        } answers {
            assertEquals(request.token, tokenSlot.captured)
            DbResult.Success(Unit)
        }

        val response = service.verifyUser(request)
        assertEquals("verified", response.status)
    }

    @Test
    fun `should throw bad request exception for invalid verification request`() = runTest {

        val request = UserVerificationRequest(
            "1234",
            "emailId",
            "token"
        )

        val invalidCodeException = assertFailsWith<BadRequestException> {
            service.verifyUser(request)
        }

        assertEquals("code is not valid", invalidCodeException.error)

        val request2 = UserVerificationRequest(
            "123456",
            "",
            "token"
        )

        val invalidEmailIdException = assertFailsWith<BadRequestException> {
            service.verifyUser(request2)
        }

        assertEquals("email id is needed", invalidEmailIdException.error)

        val request3 = UserVerificationRequest(
            "123456",
            "emailId",
            ""
        )

        val invalidTokenException = assertFailsWith<BadRequestException> {
            service.verifyUser(request3)
        }

        assertEquals("token is needed", invalidTokenException.error)
    }

    @Test
    fun `should throw internal server exception for otp verification`() = runTest {

        coEvery {
            otpProvider.verifyCode(any(), any())
        } returns Pair(false, "error")

        val exception = assertFailsWith<InternalServerException> {
            service.verifyUser(request)
        }

        assertEquals("invalid or expired OTP", exception.error)
    }

    @Test
    fun `should throw not found exception when activating user`() = runTest {

        coEvery {
            otpProvider.verifyCode(any(), any())
        } returns Pair(true, "")

        coEvery {
            repository.activateUser(any())
        } returns DbResult.Failure(DbError.NotFound())

        val exception = assertFailsWith<NotFoundException> {
            service.verifyUser(request)
        }

        assertEquals("invalid or expired verification token", exception.error)
    }

    @Test
    fun `should throw internal server exception when activating user`() = runTest {
        coEvery {
            otpProvider.verifyCode(any(), any())
        } returns Pair(true, "")

        coEvery {
            repository.activateUser(any())
        } returns DbResult.Failure(DbError.UnknownError(RuntimeException("error")))

        val exception = assertFailsWith<InternalServerException> {
            service.verifyUser(request)
        }

        assertEquals("an error occurred during verification", exception.error)
    }

    private val loginRequest = LoginUserRequest(
        email = "test@gmail.com",
        password = "12345678"
    )

    private val accessToken = "accessToken"
    private val refreshToken = "refreshToken"

    @Test
    fun `should login user successfully`() = runTest {
        val emailSlot = slot<String>()
        val userIdSlot = slot<String>()

        val hashedPassword = PasswordHasher.hash(loginRequest.password)

        coEvery {
            repository.getUserByEmail(capture(emailSlot))
        } answers {
            assertEquals(loginRequest.email, emailSlot.captured)
            DbResult.Success(
                User(
                    id = 1,
                    password = hashedPassword,
                    isVerified = true
                )
            )
        }

        every {
            auth.generateAccessToken(capture(userIdSlot))
        } answers {
            assertEquals("1", userIdSlot.captured)
            accessToken
        }

        every {
            auth.generateRefreshToken()
        } returns refreshToken

        coEvery {
            repository.saveRefreshToken(any(), any(), any())
        } returns DbResult.Success(Unit)

        val response = service.loginUser(loginRequest)

        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
        assertEquals(1, response.userId)
    }

    @Test
    fun `should throw bad request exception for invalid login request`() = runTest {
        val request = LoginUserRequest(
            email = "",
            password = "123456"
        )

        val exception1 = assertFailsWith<BadRequestException> {
            service.loginUser(request)
        }

        assertEquals("invalid email or password", exception1.error)
    }

    @Test
    fun `should throw bad request exception for get user by email`() = runTest {
        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Failure(DbError.NotFound())

        val exception = assertFailsWith<BadRequestException>{ service.loginUser(loginRequest) }

        assertEquals("invalid email or password", exception.error)

    }

    @Test
    fun `should throw internal server exception for get user by email`() = runTest {
        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Failure(DbError.UnknownError(RuntimeException("error")))

        assertFailsWith<InternalServerException>{ service.loginUser(loginRequest) }
    }

    @Test
    fun `should throw bad request exception for incorrect password`() = runTest {

        val hashedPassword = PasswordHasher.hash("1234567890")
        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
            User(
                password = hashedPassword
            )
        )

        val exception = assertFailsWith<BadRequestException> {
            service.loginUser(loginRequest)
        }

        assertEquals("invalid email or password", exception.error)
    }

    @Test
    fun `should throw unauthorized exception for unverified user`() = runTest {

        val hashedPassword = PasswordHasher.hash(loginRequest.password)
        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
            User(
                password = hashedPassword,
                isVerified = false
            )
        )

        val exception = assertFailsWith<UnauthorizedException> {
            service.loginUser(loginRequest)
        }

        assertEquals("please verify your email before logging in", exception.error)
    }

    @Test
    fun `should throw not found exception when saving refresh token`() = runTest {

        val hashedPassword = PasswordHasher.hash(loginRequest.password)

        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
            User(
                id = 1,
                password = hashedPassword,
                isVerified = true
            )
        )

        every {
            auth.generateAccessToken(any())
        } returns accessToken

        every {
            auth.generateRefreshToken()
        } returns refreshToken

        coEvery {
            repository.saveRefreshToken(any(), any(), any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException> {
            service.loginUser(loginRequest)
        }
    }

    @Test
    fun `should throw internal server exception when saving refresh token`() = runTest {

        val hashedPassword = PasswordHasher.hash(loginRequest.password)

        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
            User(
                id = 1,
                password = hashedPassword,
                isVerified = true
            )
        )

        every {
            auth.generateAccessToken(any())
        } returns accessToken

        every {
            auth.generateRefreshToken()
        } returns refreshToken

        coEvery {
            repository.saveRefreshToken(any(), any(), any())
        } returns DbResult.Failure(DbError.ConnectionError("error"))

        assertFailsWith<InternalServerException> {
            service.loginUser(loginRequest)
        }
    }

    private val refreshTokenRequest = RefreshTokenRequest(
        "refresh_token"
    )

    @Test
    fun `should refresh token successfully`() = runTest {
        coEvery {
            repository.getUserIdByRefreshToken(any())
        } returns DbResult.Success(1)

        coEvery {
            repository.revokeRefreshToken(any())
        } returns DbResult.Success(Unit)

        every {
            auth.generateAccessToken(any())
        } returns accessToken

        every {
            auth.generateRefreshToken()
        } returns refreshToken

        coEvery {
            repository.saveRefreshToken(any(), any(), any())
        } returns DbResult.Success(Unit)

        val response = service.refreshToken(refreshTokenRequest)

        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
    }

    @Test
    fun `should throw bad request exception for invalid refresh token`() = runTest {

        val request = refreshTokenRequest.copy(refreshToken = "")

        assertFailsWith<BadRequestException> {
            service.refreshToken(request)
        }
    }

    @Test
    fun `should throw unauthorized exception when getting user id by refresh token`() = runTest {

        coEvery {
            repository.getUserIdByRefreshToken(any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<UnauthorizedException> {
            service.refreshToken(refreshTokenRequest)
        }
    }

    @Test
    fun `should delete expired refresh token`() = runTest {
        coEvery {
            repository.deleteExpiredRefreshToken()
        } returns DbResult.Success(Unit)

        repository.deleteExpiredRefreshToken()

        coVerify {
            repository.deleteExpiredRefreshToken()
        }
    }

    val resendOTPRequest = ResendOTPRequest(
        email = "test@example.com"
    )

    @Test
    fun `should resend otp successfully`() = runTest {

        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
            User(
                id = 1,
                email = resendOTPRequest.email,
                isVerified = false
            )
        )

        coEvery {
            otpProvider.sendVerificationCode(any())
        } returns Pair("emailId", "")

        coEvery {
            repository.deleteAndCreateVerificationToken(any(), any())
        } returns DbResult.Success(Unit)

        val response = service.resendOTP(resendOTPRequest)

        assertEquals("emailId", response.emailId)
        assertTrue(response.verificationToken.isNotEmpty())
    }

    @Test
    fun `should throw bad request exception for invalid request otp`() = runTest {
        val request = resendOTPRequest.copy(email = "invalid email")

        val exception = assertFailsWith<BadRequestException> {
            service.resendOTP(request)
        }

        assertEquals("invalid email", exception.error)
    }

    @Test
    fun `should return na parameter for db failed request to get user by email`() = runTest {

        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Failure(DbError.ConnectionError("error"))

        val response = service.resendOTP(resendOTPRequest)

        assertEquals("n/a", response.emailId)
        assertEquals("n/a", response.verificationToken)
    }

    @Test
    fun `should throw bad request exception when a verified user request for otp`() = runTest {
        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
        User(
            isVerified = true
        ))

        val exception = assertFailsWith<BadRequestException> {
            service.resendOTP(resendOTPRequest)
        }

        assertEquals("user is already verified", exception.error)
    }

    @Test
    fun `should throw internal server exception when otp provider fails to send code`() = runTest {
        coEvery {
            repository.getUserByEmail(any())
        } returns DbResult.Success(
            User(
                isVerified = false
            ))

        coEvery {
            otpProvider.sendVerificationCode(any())
        } returns Pair("", "error")

        val exception = assertFailsWith<InternalServerException> {
            service.resendOTP(resendOTPRequest)
        }

        assertEquals("failed to resend OTP", exception.error)
    }
}