package com.martdev.service.auth

interface Auth {
    fun generateAccessToken(userId: String, role: String): String
    fun generateRefreshToken(): String
}