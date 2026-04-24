package com.martdev.controller

import com.martdev.config.AuthConfig
import com.martdev.dto.DataResponse
import com.martdev.dto.request.CreateNewsArticleRequest
import com.martdev.dto.response.NewsArticleResponse
import com.martdev.plugins.*
import com.martdev.service.auth.JWTAuthImpl
import com.martdev.service.creator.CreatorService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CreatorRoutesTest {

    @get:Rule
    val mockK = MockKRule(this)

    @MockK
    private lateinit var service: CreatorService

    private val authConfig = AuthConfig(
        "test-secret", 15, "iss", "audience"
    )

    private val creatorTestModule = module {
        single<CreatorService> { service }
        single {authConfig}
    }

    private val newsArticleDto = NewsArticleResponse(
        id = 1L,
        title = "Test Title",
        content = "Test Content",
        createdAt = "2023-10-27T10:00:00"
    )

    private val createNewsArticleRequest = CreateNewsArticleRequest(
        title = "title",
        content = "content"
    )

    private val token = JWTAuthImpl(authConfig).generateAccessToken("1")

    @Test
    fun testPostCreatorCreateNews_responseWithHttpStatusCode_Created() = testApplication {
        coEvery { service.saveNewsArticle(any(), any()) } returns 1L

        application {
            testConfiguration()
        }
        val client = clientConfig(token)

        val response = client.post("/v1/creator/create-news") {
            setBody(createNewsArticleRequest)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val articleId = response.body<DataResponse<String>>()
        assertContains(articleId.data, "1")
    }

    @Test
    fun testPostCreatorCreateNews_responseWithHttpStatusCode_Unauthorized() = testApplication {
        coEvery { service.saveNewsArticle(any(), any()) } returns 1L

        application {
            testConfiguration()
        }
        val client = clientConfig("invalid token")

        val response = client.post("/v1/creator/create-news") {
            setBody(createNewsArticleRequest)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testDeleteCreatorDeleteNewsArticleCreatorIdArticleId() = testApplication {
        coJustRun { service.deleteNewsArticle(1L, 1L) }

        application {
            testConfiguration()
        }
        val client = clientConfig(token)

        val response = client.delete("/v1/creator/deleteNewsArticle/1")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testGetCreatorGetAllNewsArticleByCreatorIdCreatorId() = testApplication {
        coEvery { service.getAllNewsArticleByCreatorId(1L, any(), any()) } returns listOf(newsArticleDto)

        application {
            testConfiguration()
        }
        val client = clientConfig(token)

        val response = client.get("/v1/creator/getAllNewsArticleByCreatorId")

        assertEquals(HttpStatusCode.OK, response.status)
        val dataResponse = response.body<DataResponse<List<NewsArticleResponse>>>()
        assertEquals(1, dataResponse.data.size)
        assertEquals("Test Title", dataResponse.data[0].title)
    }

    @Test
    fun testGetCreatorGetNewsArticleByIdCreatorIdArticleId() = testApplication {
        coEvery { service.getNewsArticleById(1L, 1L) } returns newsArticleDto

        application {
            testConfiguration()
        }
        val client = clientConfig(token)

        val response = client.get("/v1/creator/getNewsArticleById/1")

        assertEquals(HttpStatusCode.OK, response.status)
        val dataResponse = response.body<DataResponse<NewsArticleResponse>>()
        assertEquals("Test Title", dataResponse.data.title)
    }

    @Test
    fun testPatchCreatorUpdateNewsArticle() = testApplication {
        coEvery { service.updateNewsArticle(any(), any()) } returns newsArticleDto

        application {
            testConfiguration()
        }
        val client = clientConfig(token)

        val response = client.patch("/v1/creator/updateNewsArticle") {
            setBody(createNewsArticleRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val dataResponse = response.body<DataResponse<NewsArticleResponse>>()
        assertEquals("Test Title", dataResponse.data.title)
    }

    private fun Application.testConfiguration() {
        install(Koin) {
            modules(creatorTestModule)
        }
        configureSerialization()
        configureStatusPage()
        configureSecurity()
        configureRateLimiter()
        configureRouting()
    }

    private fun ApplicationTestBuilder.clientConfig(token: String = ""): HttpClient = createClient {
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
}
