package com.martdev.controller

import com.martdev.dto.DataResponse
import com.martdev.dto.ErrorResponse
import com.martdev.dto.response.NewsArticleDataDto
import com.martdev.service.creator.CreatorService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.creatorRoutes() {
    val service by inject<CreatorService>()

    route("/creator") {
        /**
         * Tag: creator
         *
         * Create news article
         *
         * Responses:
         *   - 201 [com.martdev.dto.response.NewsArticleDataDto] news article created successfully.
         *   - 400 [com.martdev.dto.ErrorResponse] bad request.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        post("/create-news") {
            val newsArticleRequest = call.receive<NewsArticleDataDto>()
            service.saveNewsArticle(newsArticleRequest)
            call.respond(HttpStatusCode.Created)
        }

        /**
         * Tag: creator
         *
         * Get news article by creator id and articleId
         *
         * Path: creatorId [Long]
         *
         * Path: articleId [Long]
         *
         * Responses:
         *   - 200 [com.martdev.dto.response.NewsArticleDataDto] news article info retrieved.
         *   - 404 [com.martdev.dto.ErrorResponse] not found.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        get("/getNewsArticleById/{creatorId}/{articleId}") {
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
         * Path: creatorId [Long]
         *
         * Query: limit [Int]
         *
         * Query: offset [Int]
         *
         * Responses:
         *   - 200 [kotlin.collections.List<com.martdev.dto.response.NewsArticleDataDto>] A list of news articles.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        get("/getAllNewsArticleByCreatorId/{creatorId}") {
            val creatorId = call.parameters["creatorId"]?.toLongOrNull()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L

            if (creatorId == null) {
                val errorResponse = ErrorResponse(
                    "Invalid or missing 'creatorId'. It must be a number."
                )
                call.respond(HttpStatusCode.BadRequest, errorResponse)
                return@get
            }

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
         * Path: creatorId [Long]
         *
         * Path: articleId [Long]
         *
         * Responses:
         *   - 200 article deleted successfully.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        delete("/deleteNewsArticle/{creatorId}/{articleId}") {
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
         * Responses:
         *   - 200 [com.martdev.dto.response.NewsArticleDataDto] news article info updated.
         *   - 404 [com.martdev.dto.ErrorResponse] not found.
         *   - 500 [com.martdev.dto.ErrorResponse] internal server error.
         */
        patch("/updateNewsArticle") {
            val request = call.receive<NewsArticleDataDto>()
            val response = service.updateNewsArticle(request)
            val dataResponse = DataResponse(response)
            call.respond(HttpStatusCode.OK, dataResponse)
        }
    }
}

private suspend inline fun RoutingContext.verifyPaths(block: (Long, Long) -> Unit) {
    val creatorId = call.parameters["creatorId"]?.toLongOrNull()
    val articleId = call.parameters["articleId"]?.toLongOrNull()
    if (creatorId == null || articleId == null) {
        val errorResponse = ErrorResponse(
            "invalid creatorId or articleId. Both must be numbers"
        )
        call.respond(HttpStatusCode.BadRequest, errorResponse)
        return
    }
    block(creatorId, articleId)
}