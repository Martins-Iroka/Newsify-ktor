package com.martdev.controller

import com.martdev.domain.Role
import com.martdev.domain.exceptions.ForbiddenException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.DataResponse
import com.martdev.dto.ErrorResponse
import com.martdev.dto.request.FcmTokenRequest
import com.martdev.service.reader.ReaderService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.readerRoutes() {
    val service by inject<ReaderService>()
    val authJWT = "auth-jwt"

    authenticate(authJWT) {
        route("/reader") {
            /**
             * Tag: reader
             *
             * Get all creators
             *
             * Responses:
             *      - 200 [com.martdev.dto.response.CreatorInfoResponse] list of all creators
             *      - 500 [com.martdev.dto.ErrorResponse] internal server error
             */
            get("/get-creators") {
                verifyReaderRoleAndGetId()
                val creators = service.getListOfCreators()
                val response = DataResponse(creators)
                call.respond(HttpStatusCode.OK, response)
            }

            /**
             * Tag: reader
             *
             * Reader can follow a creator
             *
             * Responses:
             *      - 200 [String] successfully followed creator
             *      - 400 [com.martdev.dto.ErrorResponse] bad request
             *      - 500 [com.martdev.dto.ErrorResponse] internal server error
             */
            post("/follow-creator/{creatorId}") {
                verifyReaderAndCreatorId { readerId, creatorId ->
                    service.followCreator(creatorId, readerId)
                    val response = DataResponse("Successfully followed creator")
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            /**
             * Tag: reader
             *
             * Reader can unfollow a creator
             *
             * Responses:
             *      - 200 [String] successfully unfollowed creator
             *      - 404 [com.martdev.dto.ErrorResponse] not found
             *      - 500 [com.martdev.dto.ErrorResponse] internal server error
             */
            post("/unfollow-creator/{creatorId}") {
                verifyReaderAndCreatorId { readerId, creatorId ->
                    service.unfollowCreator(creatorId, readerId)
                    val response = DataResponse("Successfully unfollowed creator")
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            /**
             * Tag: reader
             *
             * Get News articles by a creator
             *
             * Responses:
             *      - 200 [List<NewsArticleResponse>] list of news article by a creator
             *      - 500 [com.martdev.dto.ErrorResponse] internal server error
             */
            get("/get-articles-by-creators") {
                verifyReaderRoleAndGetId()
                val creatorIds = call.request.queryParameters.getAll("creatorId")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L
                if (creatorIds.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("At least one creatorId is required"))
                    return@get
                }
                val result = service.getAllArticlesByCreators(creatorIds, limit, offset)
                val response = DataResponse(result)
                call.respond(HttpStatusCode.OK, response)
            }

            /**
             * Tag: reader
             *
             * Get news article by article id using creator id
             *
             * Responses:
             *      - 200 [com.martdev.dto.response.NewsArticleResponse] returned news article details
             *      - 404 [com.martdev.dto.ErrorResponse] not found
             *      - 500 [com.martdev.dto.ErrorResponse] internal server error
             */
            get("/{creatorId}/get-news-article-by-id/{articleId}") {
                verifyReaderAndCreatorId { _, creatorId ->
                    val articleId = call.parameters["articleId"]?.toLongOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest, ErrorResponse("Invalid or missing articleId"))

                    val result = service.getNewsArticleById(creatorId, articleId)
                    val response = DataResponse(result)
                    call.respond(HttpStatusCode.OK, response)
                }
            }

            /**
             * Tag: reader
             *
             * add fcm token for notification
             *
             * Responses:
             *      - 200 token was successfully added
             *      - 404 [com.martdev.dto.ErrorResponse] reader not found
             *      - 500 [com.martdev.dto.ErrorResponse] internal server error
             */
            patch("/addFcmToken") {
                val readerId = verifyReaderRoleAndGetId()
                val token = call.receive<FcmTokenRequest>()
                service.updateFcmToken(readerId, token.fcmToken)
                call.respond(HttpStatusCode.OK)
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