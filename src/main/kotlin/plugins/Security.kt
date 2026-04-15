package com.martdev.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.martdev.config.AuthConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject

fun Application.configureSecurity() {
    val authConfig by inject<AuthConfig>()
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT
                .require(Algorithm.HMAC256(authConfig.secret))
                .withAudience(authConfig.audience)
                .withIssuer(authConfig.iss)
                .build())
            validate {credential ->
                if (credential.payload.getClaim("userId").asString().isNotEmpty()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "unauthorized")
            }
        }
    }
}
