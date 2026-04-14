package com.martdev.service.user

import com.martdev.dto.request.UserRequest
import com.martdev.dto.response.UserResponse

interface UserService {
    suspend fun registerUser(user: UserRequest): UserResponse
}