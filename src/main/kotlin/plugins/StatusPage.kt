package com.martdev.plugins

import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

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

        exception<Exception> { call, cause ->
            val errorResponse = ErrorResponse("Internal server error")
            call.respond(status = HttpStatusCode.InternalServerError, errorResponse)
        }
    }
}