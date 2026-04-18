package com.martdev.service.user

import com.martdev.dto.request.*
import com.martdev.dto.response.*

interface UserService {
    suspend fun registerUser(user: UserRequest): UserResponse
    suspend fun verifyUser(request: VerifyUserRequest): VerifyUserResponse
    suspend fun loginUser(request: LoginUserRequest): LoginUserResponse
    suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse
    suspend fun deleteExpiredRefreshToken()
    suspend fun resendOTP(request: ResendOTPRequest): ResendOTPResponse
}