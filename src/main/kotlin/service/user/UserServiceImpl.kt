package com.martdev.service.user

import com.martdev.domain.Role
import com.martdev.domain.User
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.dto.request.LoginUserRequest
import com.martdev.dto.request.UserRequest
import com.martdev.dto.request.VerifyUserRequest
import com.martdev.dto.response.LoginUserResponse
import com.martdev.dto.response.UserResponse
import com.martdev.dto.response.VerifyUserResponse
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.user_repo.UserRepository
import com.martdev.service.auth.Auth
import com.martdev.service.otp_provider.OtpProvider
import com.martdev.util.PasswordHasher
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.exposedLogger
import org.koin.core.annotation.Single
import java.security.MessageDigest
import java.util.*
import kotlin.enums.enumEntries
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@Single
class UserServiceImpl(
    private val repository: UserRepository,
    private val otpProvider: OtpProvider,
    private val auth: Auth
) : UserService {
    override suspend fun registerUser(user: UserRequest): UserResponse {
        validateUserRequest(user)

        val hashedPassword = PasswordHasher.hash(user.password)
        val userModel = User(
            username = user.username,
            email = user.email,
            password = hashedPassword,
            role = Role.valueOf(user.role)
        )
        val token = UUID.randomUUID().toString()

        return when (val result = repository.saveUserAndVerificationToken(userModel, token)) {
            is DbResult.Failure -> when(result.error) {
                is DbError.ConnectionError, is DbError.UnknownError, DbError.ForeignKeyViolation  -> throw InternalServerException()
                is DbError.NotFound -> throw NotFoundException()
                DbError.UniqueViolation -> throw BadRequestException("duplicate email or username")
            }
            is DbResult.Success -> {
                val userModel = result.value
                //todo you have to modify this
                val (emailId, error) = otpProvider.sendVerificationCode(userModel.email)
                if (error.isNotEmpty()) {
                    repository.deleteUserAndVerificationToken(userModel.id)
                    throw InternalServerException("failed to send OTP")
                }
                UserResponse(
                    emailId, token
                )
            }
        }
    }

    override suspend fun verifyUser(request: VerifyUserRequest): VerifyUserResponse {
        validateVerificationRequest(request)

        val (isSuccess, errorMessage) = otpProvider.verifyCode(request.emailId, request.code)
        if (!isSuccess) {
            exposedLogger.error(errorMessage)
            throw InternalServerException("failed to send OTP")
        }
        return when (val result = repository.activateUser(request.token)) {
            is DbResult.Failure -> when (result.error) {
                is DbError.NotFound -> throw NotFoundException("Invalid or expired verification token")
                else -> throw InternalServerException("An error occurred during verification")
            }
            is DbResult.Success -> VerifyUserResponse("verified")
        }
    }

    override suspend fun loginUser(request: LoginUserRequest): LoginUserResponse {
        validateLoginUserRequest(request)

        return when (val result = repository.getUserByEmail(request.email)) {
            is DbResult.Failure -> {
                if (result.error is DbError.NotFound) {
                    throw BadRequestException("Invalid email or password")
                } else throw InternalServerException()
            }

            is DbResult.Success -> {
                val user = result.value
                val isValid = PasswordHasher.verify(request.password, user.password)
                if (!isValid) throw BadRequestException("Invalid email or password")

                if (!user.isVerified) throw BadRequestException("Please verify your email before logging in")

                val accessToken = auth.generateAccessToken(user.id.toString())
                val refreshToken = auth.generateRefreshToken()
                val hash = MessageDigest.getInstance("SHA-256").digest(refreshToken.toByteArray())
                val refreshTokenInHex = HexFormat.of().formatHex(hash)
                val refreshExpiry = Clock.System.now().plus(24.hours).toLocalDateTime(TimeZone.UTC)

                when(val savedResult = repository.saveRefreshToken(user.id,refreshTokenInHex, refreshExpiry)) {
                    is DbResult.Failure -> {
                        if (savedResult.error is DbError.NotFound) throw NotFoundException() else throw InternalServerException()
                    }
                    is DbResult.Success -> LoginUserResponse(accessToken, refreshToken, user.id)
                }
            }
        }
    }

    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+[a-zA-Z]{2,}$")

    private fun validateUserRequest(user: UserRequest) {
        val isValidRole = enumEntries<Role>().any { it.name == user.role.uppercase() }

        when {
            user.email.isEmpty() || !emailPattern.matches(user.email) -> throw BadRequestException("Invalid email format")
            user.password.length < 8 -> throw BadRequestException("Password must be at least 8 characters long")
            user.username.isBlank() -> throw BadRequestException("Username cannot be empty")
            !isValidRole ->
                throw BadRequestException("Invalid role specified. Must be one of: ${enumEntries<Role>().joinToString { it.name }}")
        }
    }

    private fun validateVerificationRequest(request: VerifyUserRequest) {
        when {
            request.code.isEmpty() || request.code.length != 6 -> throw BadRequestException("code is not valid")
            request.emailId.isEmpty() -> throw BadRequestException("email id is needed")
            request.token.isEmpty() -> throw BadRequestException("token is needed")
        }
    }

    private fun validateLoginUserRequest(request: LoginUserRequest) {

        when {
            request.email.isEmpty()
                    || request.email.length > 255
                    || !emailPattern.matches(request.email)
                    || request.password.isEmpty()
                    || request.password.length < 8 -> throw BadRequestException("Invalid email or password")
        }
    }
}