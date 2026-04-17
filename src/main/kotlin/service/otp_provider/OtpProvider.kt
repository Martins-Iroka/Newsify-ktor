package com.martdev.service.otp_provider

interface OtpProvider {
    suspend fun sendVerificationCode(email: String): Pair<String, String>
    suspend fun verifyCode(emailID: String, code: String): Pair<Boolean, String>
}