package com.martdev.controller

import com.martdev.domain.Role
import com.martdev.domain.exceptions.ForbiddenException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.DataResponse
import com.martdev.dto.ErrorResponse
import com.martdev.service.reader.ReaderService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.readerRoutes() {
    val service by inject<ReaderService>()
    val authJWT = "auth-jwt"

    authenticate(authJWT) {
        route("/reader") {
            get("/get-creators") {
                verifyReaderRoleAndGetId()
                val creators = service.getListOfCreators()
                val response = DataResponse(creators)
                call.respond(HttpStatusCode.OK, response)
            }

            post("/follow-creator/{creatorId}") {
                verifyReaderAndCreatorId { readerId, creatorId ->
                    service.followCreator(creatorId, readerId)
                    val response = DataResponse("Successfully followed creator")
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            post("/unfollow-creator/{creatorId}") {
                verifyReaderAndCreatorId { readerId, creatorId ->
                    service.unfollowCreator(creatorId, readerId)
                    val response = DataResponse("Successfully unfollowed creator")
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            get("/get-articles-by-creatorId/{creatorId}") {
                verifyReaderAndCreatorId { _, creatorId ->
                    val result = service.getAllArticlesByCreatorId(creatorId)
                    val response = DataResponse(result)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            get("/{creatorId}/get-news-article-by-id/{articleId}") {
                verifyReaderAndCreatorId { _, creatorId ->
                    val articleId = call.parameters["articleId"]?.toLongOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, ErrorResponse("Invalid or missing articleId"))

                    val result = service.getNewsArticleById(creatorId, articleId)
                    val response = DataResponse(result)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
        }
    }
}

private suspend inline fun RoutingContext.verifyReaderAndCreatorId(block: (Long, Long) -> Unit) {
    val readerId = verifyReaderRoleAndGetId()
    val creatorId = call.parameters["creatorId"]?.toLongOrNull()
    if (creatorId == null) {
        val errorResponse = ErrorResponse(
            "Invalid or missing creatorId"
        )
        call.respond(HttpStatusCode.BadRequest, errorResponse)
        return
    }
    block(readerId, creatorId)
}

private fun RoutingContext.verifyReaderRoleAndGetId(): Long {
    val principal = call.principal<JWTPrincipal>()
    if (principal?.payload?.getClaim("role")?.asString() != Role.READER.name) {
        throw ForbiddenException("You do not have the necessary permissions to perform this action.")
    }
    return principal.payload.getClaim("userId")?.asString()?.toLongOrNull() ?: throw UnauthorizedException("Missing or invalid userId claim in token")
}