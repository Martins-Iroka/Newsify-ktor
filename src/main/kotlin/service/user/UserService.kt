package com.martdev.service.user

import com.martdev.dto.request.LoginUserRequest
import com.martdev.dto.request.RefreshTokenRequest
import com.martdev.dto.request.UserRequest
import com.martdev.dto.request.VerifyUserRequest
import com.martdev.dto.response.LoginUserResponse
import com.martdev.dto.response.RefreshTokenResponse
import com.martdev.dto.response.UserResponse
import com.martdev.dto.response.VerifyUserResponse

interface UserService {
    suspend fun registerUser(user: UserRequest): UserResponse
    suspend fun verifyUser(request: VerifyUserRequest): VerifyUserResponse
    suspend fun loginUser(request: LoginUserRequest): LoginUserResponse
    suspend fun refreshToken(request: RefreshTokenRequest): RefreshTokenResponse
    suspend fun deleteExpiredRefreshToken()
}