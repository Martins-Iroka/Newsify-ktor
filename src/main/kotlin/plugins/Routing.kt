package com.martdev.plugins

import com.martdev.controller.userRoutes
import com.martdev.service.user.UserService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(userService: UserService) {
    routing {
        route("/v1") {
            userRoutes(userService)
        }
    }
}
