package controller

import com.martdev.config.AuthConfig
import com.martdev.controller.readerRoutes
import com.martdev.domain.Role
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.dto.request.FcmTokenRequest
import com.martdev.dto.response.CreatorInfoResponse
import com.martdev.dto.response.NewsArticleResponse
import com.martdev.plugins.configureSecurity
import com.martdev.plugins.configureSerialization
import com.martdev.plugins.configureStatusPage
import com.martdev.service.auth.JWTAuthImpl
import com.martdev.service.reader.ReaderService
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import org.junit.Rule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import util.clientConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderRoutesTest {

    @get:Rule
    val mockK = MockKRule(this)

    @MockK
    private lateinit var service: ReaderService

    private val authConfig = AuthConfig(
        "test-secret", 15, "iss", "audience"
    )

    private val readerTestModule = module {
        single<ReaderService> { service }
        single { authConfig }
    }

    private val readerToken = JWTAuthImpl(authConfig).generateAccessToken("2", Role.READER.name)


    @Test
    fun `test get news article by creator id and article id responds with HttpStatusCodeOk`() = testApplication {
        coEvery {
            service.getNewsArticleById(any(), any())
        } returns NewsArticleResponse()
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.get("/reader/1/get-news-article-by-id/11").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `test get news article by creator id and article id responds with HttpStatusCodeUnauthorized`() = testApplication {
        application {
            readerConfiguration()
        }
        val client = clientConfig("invalid reader token")
        client.get("/reader/1/get-news-article-by-id/11").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test get news article by creator id and article id responds with HttpStatusCodeBadRequest for invalid creator id`() = testApplication {
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.get("/reader/invalid creator id/get-news-article-by-id/11").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test get news article by creator id and article id responds with HttpStatusCodeBadRequest for invalid article id`() = testApplication {
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.get("/reader/1/get-news-article-by-id/invalid article id").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test reader should follow creator then respond with HttpStatusCodeOk`() = testApplication {
        coJustRun {
            service.followCreator(any(), any())
        }
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)

        client.post("/reader/follow-creator/1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `test reader should follow creator then respond with HttpStatusCodeBadRequest for invalid creator id`() = testApplication {
        coJustRun {
            service.followCreator(any(), any())
        }
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)

        client.post("/reader/follow-creator/invalid creator id").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test get articles respond with HttpStatusCodeOk`() = testApplication {
        coEvery {
            service.getAllArticlesByCreators(any(), any(), any())
        } returns listOf(
            NewsArticleResponse()
        )
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.get("/reader/get-articles?creatorId=1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `test get articles respond with HttpStatusCodeBadRequest for no creator id query`() = testApplication {
        coEvery {
            service.getAllArticlesByCreators(any(), any(), any())
        } returns listOf(
            NewsArticleResponse()
        )
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.get("/reader/get-articles").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `test get creators respond with HttpStatusCodeOk`() = testApplication {
        coEvery {
            service.getListOfCreators()
        } returns listOf(
            CreatorInfoResponse(
                id = 1, username = "username"
            )
        )
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.get("/reader/get-creators").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `test reader should unfollow creator respond with HttpStatusCodeOk`() = testApplication {
        coJustRun {
            service.unfollowCreator(any(), any())
        }
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.post("/reader/unfollow-creator/1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `add fcm token should respond with HttpStatusCodeOk`() = testApplication {
        coJustRun {
            service.updateFcmToken(any(), any())
        }
        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.patch("/reader/addFcmToken") {
            setBody(FcmTokenRequest("token"))
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `add fcm token should respond with HttpStatusCodeNotFound`() = testApplication {
        coEvery {
            service.updateFcmToken(any(), any())
        } throws NotFoundException()

        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.patch("/reader/addFcmToken") {
            setBody(FcmTokenRequest("token"))
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `add fcm token should respond with HttpStatusCodeInternalServer`() = testApplication {
        coEvery {
            service.updateFcmToken(any(), any())
        } throws NotFoundException()

        application {
            readerConfiguration()
        }
        val client = clientConfig(readerToken)
        client.patch("/reader/addFcmToken") {
            setBody(FcmTokenRequest("token"))
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    private fun Application.readerConfiguration() {
        install(Koin) {
            modules(readerTestModule)
        }
        configureSerialization()
        configureStatusPage()
        configureSecurity()
        routing {
            readerRoutes()
        }
    }
}