package com.martdev.service.otp_provider

import com.martdev.config.StytchConfig
import com.stytch.java.common.StytchResult
import com.stytch.java.consumer.StytchClient
import com.stytch.java.consumer.models.otp.AuthenticateRequest
import com.stytch.java.consumer.models.otpemail.SendRequest
import io.ktor.http.*
import org.koin.core.annotation.Single

@Single
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

    override suspend fun verifyCode(emailID: String, code: String): Pair<Boolean, String> {
        val result = client.otps.authenticate(
            AuthenticateRequest(emailID, code)
        )
        return when(result) {
            is StytchResult.Error -> Pair(false, result.exception.message?: "Error")
            is StytchResult.Success -> verifyCode(result.value.statusCode)
        }
    }

    private fun verifyCode(code: Int) = when(code) {
       in 400..507 -> Pair(false, HttpStatusCode.fromValue(code).description)
        else -> Pair(true, "")
    }
}