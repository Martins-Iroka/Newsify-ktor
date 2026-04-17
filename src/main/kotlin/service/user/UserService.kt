package com.martdev.service.user

import com.martdev.dto.request.UserRequest
import com.martdev.dto.request.VerifyUserRequest
import com.martdev.dto.response.UserResponse
import com.martdev.dto.response.VerifyUserResponse

interface UserService {
    suspend fun registerUser(user: UserRequest): UserResponse
    suspend fun verifyUser(request: VerifyUserRequest): VerifyUserResponse
}