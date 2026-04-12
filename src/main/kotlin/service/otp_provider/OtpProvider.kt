package com.martdev.service.otp_provider

interface OtpProvider {
    suspend fun sendVerificationCode(email: String): Pair<String, String>
    suspend fun verifyCode(email: String, code: String): Pair<Int, String>
}