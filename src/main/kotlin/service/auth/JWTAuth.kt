package com.martdev.service.auth

interface JWTAuth {
    fun generateToken(userId: String): String
}