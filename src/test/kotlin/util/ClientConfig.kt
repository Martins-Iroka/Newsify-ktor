package util

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json

fun ApplicationTestBuilder.clientConfig(token: String = ""): HttpClient = createClient {
    install(ContentNegotiation) {
        json(json = Json {
            ignoreUnknownKeys = true
        })
    }
    defaultRequest {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        bearerAuth(token)
    }
}