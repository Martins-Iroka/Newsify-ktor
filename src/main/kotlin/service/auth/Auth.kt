package com.martdev.service.auth

interface Auth {
    fun generateAccessToken(userId: String): String
    fun generateRefreshToken(): String
}