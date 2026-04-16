package com.martdev.plugins

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
    routing {
        swaggerUI("/swaggerUI") {
            info = OpenApiInfo(
                title = "Newsify API",
                version = "1.0.0",
                description = "API for Newsify. An application for users to get latest news on a variety of topics",
                termsOfService = "https://swagger.io/terms/")
            source = OpenApiDocSource.Routing(ContentType.Application.Json) {
                routingRoot.descendants()
            }
        }
    }
}
