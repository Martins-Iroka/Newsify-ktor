package com.martdev.plugins

import com.martdev.dto.ErrorResponse
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPage() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            val errorResponse = ErrorResponse(cause.message ?: "Bad Request")
            call.respond(status = HttpStatusCode.BadRequest, errorResponse)
        }

        exception<NotFoundException> { call, cause ->
            val errorResponse = ErrorResponse(cause.message ?: "Not found")
            call.respond(status = HttpStatusCode.NotFound, errorResponse)
        }

        exception<InternalServerException> { call, cause ->
            val errorResponse = ErrorResponse(cause.message ?: "Internal server error")
            call.respond(status = HttpStatusCode.InternalServerError, errorResponse)
        }

        exception<UnauthorizedException> { call, cause ->
            val errorResponse = ErrorResponse(cause.message ?: "Unauthorized")
            call.respond(status = HttpStatusCode.Unauthorized, errorResponse)
        }
    }
}