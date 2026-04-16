package com.martdev.plugins

import com.martdev.controller.userRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/v1") {
            userRoutes()
        }
    }
}
