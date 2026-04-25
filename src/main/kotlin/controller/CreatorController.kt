package com.martdev.controller

import com.martdev.domain.Role
import com.martdev.domain.exceptions.ForbiddenException
import com.martdev.domain.exceptions.UnauthorizedException
import com.martdev.dto.DataResponse
import com.martdev.dto.ErrorResponse
import com.martdev.dto.request.CreateNewsArticleRequest
import com.martdev.service.creator.CreatorService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.creatorRoutes() {
    val service by inject<CreatorService>()
    val authJWT = "auth-jwt"

    authenticate(authJWT) {
        route("/creator") {
            /**
             * Tag: creator
             *
             * Create news article
             *
             * Responses:
             *   - 201 [com.martdev.dto.response.NewsArticleResponse] news article created successfully.
             *   - 400 [com.martdev.dto.ErrorResponse] bad request.
             *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
             */
            post("/create-news") {
                val creatorId = verifyCreatorAndGetId()
                val newsArticleRequest = call.receive<CreateNewsArticleRequest>()
                val articleId = service.saveNewsArticle(creatorId, newsArticleRequest)
                val dataResponse = DataResponse(
                    "news article with id $articleId has been created"
                )
                call.respond(HttpStatusCode.Created, dataResponse)
            }

            /**
             * Tag: creator
             *
             * Get news article by creator id and articleId
             *
             * Path: articleId [Long]
             *
             * Responses:
             *   - 200 [com.martdev.dto.response.NewsArticleResponse] news article info retrieved.
             *   - 404 [com.martdev.dto.ErrorResponse] not found.
             *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
             */
            get("/getNewsArticleById/{articleId}") {
                verifyPaths { creatorId, articleId ->
                    val newsArticleData = service.getNewsArticleById(creatorId, articleId)
                    val dataResponse = DataResponse(newsArticleData)
                    call.respond(HttpStatusCode.OK, dataResponse)
                }
            }

            /**
             * Tag: creator
             *
             * Get all news article by creator id
             *
             * Responses:
             *   - 200 [kotlin.collections.List<com.martdev.dto.response.NewsArticleDataDto>] A list of news articles.
             *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
             */
            get("/getAllNewsArticleByCreatorId") {
                val creatorId = verifyCreatorAndGetId()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L

                if (limit <= 0 || offset < 0) {
                    val errorResponse = ErrorResponse("'limit' must be positive and 'offset' must be non-negative.")
                    call.respond(HttpStatusCode.BadRequest, errorResponse)
                    return@get
                }

                val newsArticleList = service.getAllNewsArticleByCreatorId(creatorId, limit, offset)
                val dataResponse = DataResponse(newsArticleList)
                call.respond(HttpStatusCode.OK, dataResponse)
            }

            /**
             * Tag: creator
             *
             * Delete news article by id
             *
             * Path: articleId [Long]
             *
             * Responses:
             *   - 200 article deleted successfully.
             *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
             */
            delete("/deleteNewsArticle/{articleId}") {
                verifyPaths { creatorId, articleId ->
                    service.deleteNewsArticle(creatorId, articleId)
                    call.respond(HttpStatusCode.OK)
                }
            }

            /**
             * Tag: creator
             *
             * Update news article by creator id and articleId
             *
             * Path: articleId [Long]
             *
             * Responses:
             *   - 200 [com.martdev.dto.response.NewsArticleResponse] news article info updated.
             *   - 404 [com.martdev.dto.ErrorResponse] not found.
             *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
             */
            patch("/updateNewsArticle/{articleId}") {
                verifyPaths { creatorId, articleId ->
                    val request = call.receive<CreateNewsArticleRequest>()
                    val response = service.updateNewsArticle(creatorId, articleId,request)
                    val dataResponse = DataResponse(response)
                    call.respond(HttpStatusCode.OK, dataResponse)
                }
            }
        }
    }
}

private suspend inline fun RoutingContext.verifyPaths(block: (Long, Long) -> Unit) {
    val creatorId = verifyCreatorAndGetId()
    val articleId = call.parameters["articleId"]?.toLongOrNull()
    if (articleId == null) {
        val errorResponse = ErrorResponse(
            "invalid articleId"
        )
        call.respond(HttpStatusCode.BadRequest, errorResponse)
        return
    }
    block(creatorId, articleId)
}

private fun RoutingContext.verifyCreatorAndGetId(): Long {
    val principal = call.principal<JWTPrincipal>()
    if (principal?.payload?.getClaim("role")?.asString() != Role.CREATOR.name) {
        throw ForbiddenException("You do not have the necessary permissions to perform this action.")
    }
    return principal.payload.getClaim("userId")?.asString()?.toLongOrNull() ?: throw UnauthorizedException("Missing or invalid userId claim in token")
}