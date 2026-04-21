package com.martdev.controller

import com.martdev.dto.DataResponse
import com.martdev.dto.request.*
import com.martdev.service.user.UserService
import io.ktor.http.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val service by inject<UserService>()
    route("/authentication") {
        /**
         * Tag: authentication
         *
         * Registers a user
         *
         * Responses:
         *   - 201 [com.martdev.dto.response.UserResponse] The user was registered successfully.
         *   - 400 [com.martdev.dto.ErrorResponse] bad request.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        post(path = "/register") {
            val userRequest = call.receive<UserRequest>()
            val userResponse = service.registerUser(userRequest)
            val dataResponse = DataResponse(userResponse)
            call.respond(HttpStatusCode.Created, dataResponse)
        }

        /**
         * Tag: authentication
         *
         * Verifies a user
         *
         * Responses:
         *   - 200 [com.martdev.dto.response.UserVerificationResponse] The user was verified successfully.
         *   - 400 [com.martdev.dto.ErrorResponse] bad request.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        post("/verify-user") {
            val request = call.receive<UserVerificationRequest>()
            val response = service.verifyUser(request)
            val dataResponse = DataResponse(response)
            call.respond(HttpStatusCode.OK, dataResponse)
        }

        /**
         * Tag: authentication
         *
         * Login a user
         *
         * Responses:
         *   - 200 [com.martdev.dto.response.UserLoginResponse] successful login.
         *   - 400 [com.martdev.dto.ErrorResponse] bad request.
         *   - 401 [com.martdev.dto.ErrorResponse] unauthorized
         *   - 404 [com.martdev.dto.ErrorResponse] not found
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        post("/login") {
            val request = call.receive<UserLoginRequest>()
            val response = service.loginUser(request)
            val dataResponse = DataResponse(response)
            call.respond(HttpStatusCode.OK, dataResponse)
        }

        /**
         * Tag: authentication
         *
         * Refresh access token
         *
         * Responses:
         *   - 200 [com.martdev.dto.response.RefreshTokenResponse] token refreshed.
         *   - 401 [com.martdev.dto.ErrorResponse] unauthorized.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        post("/refresh-token") {
            val request = call.receive<RefreshTokenRequest>()
            val response = service.refreshToken(request)
            val dataResponse = DataResponse(response)
            call.respond(HttpStatusCode.OK, dataResponse)
        }

        /**
         * Tag: authentication
         *
         * Resend verification code
         *
         * Responses:
         *   - 200 [com.martdev.dto.response.ResendOTPResponse] OTP sent.
         *   - 400 [com.martdev.dto.ErrorResponse] bad request.
         *   - 429 [com.martdev.dto.ErrorResponse] too many requests.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        rateLimit(RateLimitName("resend-otp")) {
            post("/resend-otp") {
                val request = call.receive<ResendOTPRequest>()
                val response = service.resendOTP(request)
                val dataResponse = DataResponse(response)
                call.respond(HttpStatusCode.OK, dataResponse)
            }
        }
    }
}
