package com.martdev.controller

import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.DataResponse
import com.martdev.dto.ErrorResponse
import com.martdev.dto.request.*
import com.martdev.dto.response.*
import com.martdev.plugins.configureRateLimiter
import com.martdev.plugins.configureRequestValidation
import com.martdev.plugins.configureSerialization
import com.martdev.plugins.configureStatusPage
import com.martdev.service.user.UserService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

class UserRoutesTest {

    @get:Rule
    val mockK = MockKRule(this)

    @MockK
    private lateinit var service: UserService

    private val userRequest = UserRequest(
        "test@gmail.com",
        "12345678",
        "username",
        "creator"
    )

    private val userTestModule = module {
        single<UserService> { service }
    }

    private val registerPath = "/v1/authentication/register"
    private val verifyUserPath = "/v1/authentication/verify-user"
    private val loginUserPath = "/v1/authentication/login"
    private val refreshTokenPath = "/v1/authentication/refresh-token"
    private val resendOTPPath = "/v1/authentication/resend-otp"

    @Test
    fun `post register user returns 201`() = testApplication {

        coEvery {
            service.registerUser(any())
        } returns UserResponse("emailId", "token")

        application {
            testConfiguration()
        }
        val client = clientConfig()

        val response = client.post(registerPath) {
            setBody(userRequest)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val userResponse = response.body<DataResponse<UserResponse>>()
        assertEquals("emailId", userResponse.data.emailId)
        assertEquals("token", userResponse.data.token)
    }

    @Test
    fun `post register user returns 400`() = testApplication {

        val invalidUserRequest = userRequest.copy(email = "invalid email")

        application {
            testConfiguration()
        }
        val client = clientConfig()
        val response = client.post(registerPath) {
            setBody(invalidUserRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val errorMessage = response.body<ErrorResponse>().error
        assertEquals("Invalid email format", errorMessage)
    }

    @Test
    fun `post register user returns 500`() = testApplication {

        coEvery {
            service.registerUser(any())
        } throws InternalServerException()

        application {
            testConfiguration()
        }
        val client = clientConfig()

        val response = client.post(registerPath) {
            setBody(userRequest)
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    private val verifyRequest = UserVerificationRequest(
        "123456", "test@email.com", "token"
    )

    @Test
    fun `verify user returns 200`() = testApplication {

        coEvery {
            service.verifyUser(verifyRequest)
        } returns UserVerificationResponse("verified")

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(verifyUserPath) {
            setBody(verifyRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `verify user returns 400`() = testApplication {

        val invalidVerificationRequest = verifyRequest.copy(code = "12345")
        coEvery {
            service.verifyUser(verifyRequest)
        } throws BadRequestException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(verifyUserPath) {
            setBody(invalidVerificationRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `verify user returns 500`() = testApplication {

        coEvery {
            service.verifyUser(verifyRequest)
        } throws InternalServerException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(verifyUserPath) {
            setBody(verifyRequest)
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    private val loginRequest = UserLoginRequest(
        email = "test@email.com",
        password = "password"
    )

    @Test
    fun `login user returns 200`() = testApplication {

        coEvery {
            service.loginUser(any())
        } returns UserLoginResponse(
            "accessToken", "refreshToken", 1
        )

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(loginUserPath) {
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login user returns 400`() = testApplication {

        val invalidLoginRequest = loginRequest.copy(email = "invalid email")

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(loginUserPath) {
            setBody(invalidLoginRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `login user returns 401`() = testApplication {

        coEvery {
            service.loginUser(any())
        } throws UnauthorizedException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(loginUserPath) {
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login user returns 404`() = testApplication {

        coEvery {
            service.loginUser(any())
        } throws NotFoundException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(loginUserPath) {
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `login user returns 500`() = testApplication {

        coEvery {
            service.loginUser(any())
        } throws InternalServerException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(loginUserPath) {
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    private val refreshTokenRequest = RefreshTokenRequest("refreshToken")
    @Test
    fun `post refresh token returns 200`() = testApplication {
        coEvery {
            service.refreshToken(any())
        } returns RefreshTokenResponse(
            "access_token", "refresh_token"
        )

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(refreshTokenPath) {
            setBody(refreshTokenRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `post refresh token returns 401`() = testApplication {
        coEvery {
            service.refreshToken(any())
        } throws UnauthorizedException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(refreshTokenPath) {
            setBody(refreshTokenRequest)
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `post refresh token returns 500`() = testApplication {
        coEvery {
            service.refreshToken(any())
        } throws InternalServerException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(refreshTokenPath) {
            setBody(refreshTokenRequest)
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    private val resendOTPRequest = ResendOTPRequest("test@email.com")

    @Test
    fun `post resend otp returns 200`() = testApplication {
        coEvery {
            service.resendOTP(any())
        } returns ResendOTPResponse("emailId", "verification_token")

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(resendOTPPath) {
            setBody(resendOTPRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `post resend otp returns 400`() = testApplication {
        val invalidResendOTPRequest = resendOTPRequest.copy(email = "invalid email")

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(resendOTPPath) {
            setBody(invalidResendOTPRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `post resend otp returns 429`() = testApplication {
        coEvery {
            service.resendOTP(any())
        } returns ResendOTPResponse("emailId", "verification_token")

        application {
            testConfiguration()
        }

        val client = clientConfig()

        client.post(resendOTPPath) {
            setBody(resendOTPRequest)
        }

        val response2 = client.post(resendOTPPath) {
            setBody(resendOTPRequest)
        }


        assertEquals(HttpStatusCode.TooManyRequests, response2.status)
    }

    @Test
    fun `post resend otp returns 500`() = testApplication {
        coEvery {
            service.resendOTP(any())
        } throws InternalServerException()

        application {
            testConfiguration()
        }

        val client = clientConfig()

        val response = client.post(resendOTPPath) {
            setBody(resendOTPRequest)
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }


    private fun Application.testConfiguration() {
        install(Koin) {
            modules(userTestModule)
        }
        configureSerialization()
        configureStatusPage()
        configureRateLimiter()
        configureRequestValidation()
        routing {
            route("/v1") {
                userRoutes()
            }
        }
    }

    private fun ApplicationTestBuilder.clientConfig(): HttpClient = createClient {
        install(ContentNegotiation) {
            json(json = Json {
                ignoreUnknownKeys = true
            })
        }
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
    }
}