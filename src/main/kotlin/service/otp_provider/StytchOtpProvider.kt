package com.martdev.service.otp_provider

import com.martdev.config.StytchConfig
import com.stytch.java.common.StytchResult
import com.stytch.java.consumer.StytchClient
import com.stytch.java.consumer.models.otp.AuthenticateRequest
import com.stytch.java.consumer.models.otpemail.SendRequest

class StytchOtpProvider(
    stytchConfig: StytchConfig
) : OtpProvider {

    val client = StytchClient(
        stytchConfig.projectId,
        stytchConfig.secret
    )
    override suspend fun sendVerificationCode(email: String): Pair<String, String> {
        val result = client.otps.email.send(
            SendRequest(email)
        )
        return when(result) {
            is StytchResult.Error -> Pair("", result.exception.message?: "Error")
            is StytchResult.Success -> Pair(result.value.emailId, "")
        }
    }

    override suspend fun verifyCode(email: String, code: String): Pair<Int, String> {
        val result = client.otps.authenticate(
            AuthenticateRequest(email, code)
        )
        return when(result) {
            is StytchResult.Error -> Pair(0, result.exception.message?: "Error")
            is StytchResult.Success -> Pair(result.value.statusCode, "")
        }
    }
}