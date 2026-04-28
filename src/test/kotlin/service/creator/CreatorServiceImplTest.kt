package com.martdev.service.creator

import com.martdev.domain.NewsArticleData
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.dto.request.CreateNewsArticleRequest
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.creator_repo.CreatorRepository
import com.martdev.service.notification.NotificationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CreatorServiceImplTest {

    @get:Rule
    val mockK = MockKRule(this)

    @MockK
    private lateinit var repository: CreatorRepository

    @MockK
    private lateinit var notificationService: NotificationService

    private lateinit var service: CreatorService

    private val data = NewsArticleData(
        id = 1,
        title = "title",
        content = "content",
        creatorId = 12
    )
    private val request = CreateNewsArticleRequest(
        title = "title",
        content = "content",
    )

    private val creatorId = 1L
    private val articleId = 11L

    @Test
    fun `should save news article returns id`() = performTest {

        val requestSlot = slot<NewsArticleData>()
        coEvery {
            repository.saveNewsArticle(capture(requestSlot))
        } answers {
            val requestCaptured = requestSlot.captured
            assertEquals(data.title, requestCaptured.title)
            assertEquals(data.content, requestCaptured.content)
            assertEquals(creatorId, requestCaptured.creatorId)
            DbResult.Success(1)
        }

        val resultId = service.saveNewsArticle(creatorId, request)
        assertEquals(1, resultId)
    }

    @Test
    fun `should throw bad request for db error unique violation`() = performTest {
        coEvery {
            repository.saveNewsArticle(any())
        } returns DbResult.Failure(
            DbError.UniqueViolation
        )

        val exception = assertFailsWith<BadRequestException> {
            service.saveNewsArticle(creatorId, request)
        }

        assertEquals("duplicate title", exception.error)
    }

    @Test
    fun `should throw internal server`() = performTest {
        coEvery {
            repository.saveNewsArticle(any())
        } returns DbResult.Failure(
            DbError.UnknownError(RuntimeException())
        )

        assertFailsWith<InternalServerException> {
            service.saveNewsArticle(creatorId, request)
        }
    }

    @Test
    fun `should get news article by id successfully`() = performTest {
        val creatorIdSlot = slot<Long>()
        val articleIdSlot = slot<Long>()

        coEvery {
            repository.getNewsArticleById(
                capture(creatorIdSlot),
                capture(articleIdSlot)
            )
        } answers {
            assertEquals(creatorId, creatorIdSlot.captured)
            assertEquals(articleId, articleIdSlot.captured)
            DbResult.Success(
                data
            )
        }

        val response = service.getNewsArticleById(creatorId, articleId)

        assertEquals(data.title, response.title)
    }

    @Test
    fun `should throw not found exception for get news article by id`() = performTest {
        coEvery {
            repository.getNewsArticleById(
                any(), any()
            )
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException> {
            service.getNewsArticleById(creatorId, articleId)
        }
    }

    @Test
    fun `should throw internal server exception for get news article by id`() = performTest {
        coEvery {
            repository.getNewsArticleById(
                any(), any()
            )
        } returns DbResult.Failure(DbError.UnknownError(RuntimeException()))

        assertFailsWith<InternalServerException> {
            service.getNewsArticleById(creatorId, articleId)
        }
    }

    private val limit = 1
    private val offset = 0L
    @Test
    fun `should get all news article by creator id`() = performTest {
        val creatorIdSlot = slot<Long>()
        val limitSlot = slot<Int>()
        val offsetSlot = slot<Long>()

        coEvery {
            repository.getAllNewsArticleByCreatorId(
                capture(creatorIdSlot),
                capture(limitSlot),
                capture(offsetSlot)
            )
        } answers {
            assertEquals(creatorId, creatorIdSlot.captured)
            assertEquals(limit, limitSlot.captured)
            assertEquals(offset, offsetSlot.captured)
            DbResult.Success(
                listOf(data)
            )
        }

        val response = service.getAllNewsArticleByCreatorId(creatorId, limit, offset)

        assertTrue(response.isNotEmpty())
    }

    @Test
    fun `get all news article returns empty list`() = performTest {
        coEvery {
            repository.getAllNewsArticleByCreatorId(any(), any(), any())
        } returns DbResult.Success(emptyList())

        val response = service.getAllNewsArticleByCreatorId(creatorId, limit, offset)

        assertTrue(response.isEmpty())
    }

    @Test
    fun `should throw internal server error for get all news article`() = performTest {
        coEvery {
            repository.getAllNewsArticleByCreatorId(any(), any(), any())
        } returns DbResult.Failure(DbError.ConnectionError(""))

        assertFailsWith<InternalServerException> {
            service.getAllNewsArticleByCreatorId(creatorId, limit, offset)
        }
    }

    @Test
    fun `should delete news article`() = performTest {
        coEvery {
            repository.deleteNewsArticle(any(), any())
        } returns DbResult.Success(Unit)

        service.deleteNewsArticle(creatorId, articleId)

        coVerify {
            repository.deleteNewsArticle(any(), any())
        }
    }

    @Test
    fun `should delete news article throws internal server exception`() = performTest {
        coEvery {
            repository.deleteNewsArticle(any(), any())
        } returns DbResult.Failure(DbError.ConnectionError(""))

        assertFailsWith<InternalServerException>{ service.deleteNewsArticle(creatorId, articleId) }
    }

    @Test
    fun `should update news article then returns new update`() = performTest {
        val newUpdate = request.copy(title = "title2", content = "content2")

        coEvery {
            repository.updateNewsArticle(any())
        } returns DbResult.Success(
            NewsArticleData(
                title = newUpdate.title,
                content = newUpdate.content
            )
        )

        val response = service.updateNewsArticle(creatorId, articleId,newUpdate, )

        assertEquals(newUpdate.title, response.title)
        assertEquals(newUpdate.content, response.content)
    }

    @Test
    fun `should throw not found exception for update news article`() = performTest {
        coEvery {
            repository.updateNewsArticle(any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException> {
            service.updateNewsArticle(creatorId, articleId, request)
        }
    }

    @Test
    fun `should throw internal server exception for update news article`() = performTest {
        coEvery {
            repository.updateNewsArticle(any())
        } returns DbResult.Failure(DbError.ConnectionError("error"))

        assertFailsWith<InternalServerException> {
            service.updateNewsArticle(creatorId, articleId, request)
        }
    }
    
    private inline fun performTest(crossinline block: suspend () -> Unit) = runTest { 
        service = CreatorServiceImpl(repository, notificationService, this)
        block()
    }
}