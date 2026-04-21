package com.martdev.service.user

import com.martdev.domain.Role
import com.martdev.domain.User
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.request.*
import com.martdev.dto.response.*
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
            role = Role.valueOf(user.role.uppercase())
        )
        val token = UUID.randomUUID().toString()

        return when (val result = repository.saveUserAndVerificationToken(userModel, token)) {
            is DbResult.Failure -> when(result.error) {
                DbError.UniqueViolation -> throw BadRequestException("duplicate email or username")
                else -> throw InternalServerException()
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

    override suspend fun verifyUser(request: UserVerificationRequest): UserVerificationResponse {
        validateVerificationRequest(request)

        val (isSuccess, errorMessage) = otpProvider.verifyCode(request.emailId, request.code)
        if (!isSuccess) {
            exposedLogger.error(errorMessage)
            throw InternalServerException("invalid or expired OTP")
        }
        return when (val result = repository.activateUser(request.token)) {
            is DbResult.Failure -> when (result.error) {
                is DbError.NotFound -> throw NotFoundException("invalid or expired verification token")
                else -> throw InternalServerException("an error occurred during verification")
            }
            is DbResult.Success -> UserVerificationResponse("verified")
        }
    }

    override suspend fun loginUser(request: UserLoginRequest): UserLoginResponse {
        validateLoginUserRequest(request)

        return when (val result = repository.getUserByEmail(request.email)) {
            is DbResult.Failure -> {
                if (result.error is DbError.NotFound) {
                    throw BadRequestException("invalid email or password")
                } else throw InternalServerException()
            }

            is DbResult.Success -> {
                val user = result.value
                val isValid = PasswordHasher.verify(request.password, user.password)
                if (!isValid) throw BadRequestException("invalid email or password")

                if (!user.isVerified) throw UnauthorizedException("please verify your email before logging in")

                val accessToken = auth.generateAccessToken(user.id.toString())
                val refreshToken = auth.generateRefreshToken()
                val refreshTokenInHex = generateHexValueFromToken(refreshToken)
                val refreshExpiry = Clock.System.now().plus(24.hours).toLocalDateTime(TimeZone.UTC)

                when(val savedResult = repository.saveRefreshToken(user.id,refreshTokenInHex, refreshExpiry)) {
                    is DbResult.Failure -> {
                        if (savedResult.error is DbError.NotFound) throw NotFoundException() else throw InternalServerException()
                    }
                    is DbResult.Success -> UserLoginResponse(accessToken, refreshToken, user.id)
                }
            }
        }
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse {

        if (request.refreshToken.isEmpty()) throw BadRequestException("invalid refresh token")

        val tokenInHex = generateHexValueFromToken(request.refreshToken)

        return when (val result = repository.getUserIdByRefreshToken(tokenInHex)) {
            is DbResult.Failure -> when (result.error) {
                is DbError.NotFound -> throw UnauthorizedException()
                else -> throw InternalServerException()
            }
            is DbResult.Success -> {
                repository.revokeRefreshToken(tokenInHex)
                val newAccessToken = auth.generateAccessToken(result.value.toString())
                val newRefreshToken = auth.generateRefreshToken()
                val newRefreshTokenInHex = generateHexValueFromToken(newRefreshToken)
                val refreshExpiry = Clock.System.now().plus(24.hours).toLocalDateTime(TimeZone.UTC)
                repository.saveRefreshToken(userId = result.value, newRefreshTokenInHex, refreshExpiry)
                RefreshTokenResponse(newAccessToken, newRefreshToken)
            }
        }
    }

    override suspend fun deleteExpiredRefreshToken() {
        repository.deleteExpiredRefreshToken()
    }

    override suspend fun resendOTP(request: ResendOTPRequest): ResendOTPResponse {
        if (request.email.isEmpty() || !emailPattern.matches(request.email)) throw BadRequestException("invalid email")

        return when(val userResult = repository.getUserByEmail(request.email)) {
            is DbResult.Failure -> {
                /*if (userResult.error is DbError.NotFound) {
                    throw NotFoundException()
                } else throw InternalServerException()*/
                ResendOTPResponse("n/a", "n/a")
            }
            is DbResult.Success -> {
                val user = userResult.value
                if (user.isVerified) throw BadRequestException("user is already verified")

                val (emailId, error) = otpProvider.sendVerificationCode(user.email)
                if (error.isNotEmpty()) throw InternalServerException("failed to resend OTP")

                val token = UUID.randomUUID().toString()
                repository.deleteAndCreateVerificationToken(token, user.id)

                ResendOTPResponse(emailId, token)
            }
        }
    }

    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+[a-zA-Z]{2,}$")

    private fun validateUserRequest(user: UserRequest) {
        val isValidRole = enumEntries<Role>().any { it.name == user.role.uppercase() }

        when {
            user.email.isEmpty() || !emailPattern.matches(user.email) -> throw BadRequestException("invalid email format")
            user.password.length < 8 -> throw BadRequestException("password must be at least 8 characters long")
            user.username.isBlank() -> throw BadRequestException("username cannot be empty")
            !isValidRole ->
                throw BadRequestException("Invalid role specified. Must be one of: ${enumEntries<Role>().joinToString { it.name }}")
        }
    }

    private fun validateVerificationRequest(request: UserVerificationRequest) {
        when {
            request.code.isEmpty() || request.code.length != 6 -> throw BadRequestException("code is not valid")
            request.emailId.isEmpty() -> throw BadRequestException("email id is needed")
            request.token.isEmpty() -> throw BadRequestException("token is needed")
        }
    }

    private fun validateLoginUserRequest(request: UserLoginRequest) {
        when {
            request.email.isEmpty()
                    || request.email.length > 255
                    || !emailPattern.matches(request.email)
                    || request.password.isEmpty()
                    || request.password.length < 8 -> throw BadRequestException("invalid email or password")
        }
    }

    private fun generateHexValueFromToken(token: String): String {
        val hashedToken = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return HexFormat.of().formatHex(hashedToken)
    }
}