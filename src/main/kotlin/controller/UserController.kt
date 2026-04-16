package com.martdev.controller

import com.martdev.dto.DataResponse
import com.martdev.dto.request.UserRequest
import com.martdev.service.user.UserService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val us by inject<UserService>()
    route("/authentication") {
        /**
         * Tag: authentication
         *
         * Registers a user
         *
         * Path: registers
         *
         * Responses:
         *   - 201 [com.martdev.dto.response.UserResponse] The user was registered successfully.
         *   - 400 [com.martdev.dto.ErrorResponse] bad request.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        post(path = "/register") {
            val userRequest = call.receive<UserRequest>()
            val userResponse = us.registerUser(userRequest)
            val dataResponse = DataResponse(userResponse)
            call.respond(HttpStatusCode.Created, dataResponse)

        }
    }
}