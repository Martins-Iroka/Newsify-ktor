package com.martdev.service.creator

import com.martdev.domain.NewsArticleData
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.dto.response.NewsArticleDataDto
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.creator_repo.CreatorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
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

    private lateinit var service: CreatorService

    @Before
    fun setup() {
        service = CreatorServiceImpl(repository)
    }

    @After
    fun tearDown() {
    }

    private val data = NewsArticleData(
        id = 1,
        title = "title",
        content = "content",
        creatorId = 12
    )
    private val dataDto = NewsArticleDataDto(
        title = "title",
        content = "content",
        creatorId = 12
    )

    @Test
    fun `should save news article returns id`() = runTest {

        val requestSlot = slot<NewsArticleData>()
        coEvery {
            repository.saveNewsArticle(capture(requestSlot))
        } answers {
            val requestCaptured = requestSlot.captured
            assertEquals(data.title, requestCaptured.title)
            assertEquals(data.content, requestCaptured.content)
            assertEquals(data.creatorId, requestCaptured.creatorId)
            DbResult.Success(1)
        }

        val resultId = service.saveNewsArticle(dataDto)
        assertEquals(1, resultId)
    }

    @Test
    fun `should throw bad request exception for invalid news article data`() = runTest {
        val dataWithInvalidTitle = dataDto.copy(
            title = ""
        )

        val badRequestExceptionForEmptyTitle = assertFailsWith<BadRequestException> {
            service.saveNewsArticle(dataWithInvalidTitle)
        }
        assertEquals("title is required", badRequestExceptionForEmptyTitle.error)

        val dataWithInvalidContent = dataDto.copy(
            content = ""
        )

        val badRequestExceptionForEmptyContent = assertFailsWith<BadRequestException> {
            service.saveNewsArticle(dataWithInvalidContent)
        }
        assertEquals("content is required", badRequestExceptionForEmptyContent.error)

        val dataWithInvalidCreatorId = dataDto.copy(
            creatorId = 0
        )

        val badRequestExceptionForEmptyCreatorId = assertFailsWith<BadRequestException> {
            service.saveNewsArticle(dataWithInvalidCreatorId)
        }
        assertEquals("creator id is required", badRequestExceptionForEmptyCreatorId.error)
    }

    @Test
    fun `should throw bad request for db error unique violation`() = runTest {
        coEvery {
            repository.saveNewsArticle(any())
        } returns DbResult.Failure(
            DbError.UniqueViolation
        )

        val exception = assertFailsWith<BadRequestException> {
            service.saveNewsArticle(dataDto)
        }

        assertEquals("duplicate title", exception.error)
    }

    @Test
    fun `should throw internal server`() = runTest {
        coEvery {
            repository.saveNewsArticle(any())
        } returns DbResult.Failure(
            DbError.UnknownError(RuntimeException())
        )

        assertFailsWith<InternalServerException> {
            service.saveNewsArticle(dataDto)
        }
    }

    private val creatorId = 1L
    private val articleId = 11L

    @Test
    fun `should get news article by id successfully`() = runTest {
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
    fun `should throw not found exception for get news article by id`() = runTest {
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
    fun `should throw internal server exception for get news article by id`() = runTest {
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
    fun `should get all news article by creator id`() = runTest {
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
    fun `get all news article returns empty list`() = runTest {
        coEvery {
            repository.getAllNewsArticleByCreatorId(any(), any(), any())
        } returns DbResult.Success(emptyList())

        val response = service.getAllNewsArticleByCreatorId(creatorId, limit, offset)

        assertTrue(response.isEmpty())
    }

    @Test
    fun `should throw internal server error for get all news article`() = runTest {
        coEvery {
            repository.getAllNewsArticleByCreatorId(any(), any(), any())
        } returns DbResult.Failure(DbError.ConnectionError(""))

        assertFailsWith<InternalServerException> {
            service.getAllNewsArticleByCreatorId(creatorId, limit, offset)
        }
    }

    @Test
    fun `should delete news article`() = runTest {
        coEvery {
            repository.deleteNewsArticle(any(), any())
        } returns DbResult.Success(Unit)

        service.deleteNewsArticle(creatorId, articleId)

        coVerify {
            repository.deleteNewsArticle(any(), any())
        }
    }

    @Test
    fun `should delete news article throws internal server exception`() = runTest {
        coEvery {
            repository.deleteNewsArticle(any(), any())
        } returns DbResult.Failure(DbError.ConnectionError(""))

        assertFailsWith<InternalServerException>{ service.deleteNewsArticle(creatorId, articleId) }
    }

    @Test
    fun `should update news article then returns new update`() = runTest {
        val newUpdate = dataDto.copy(title = "title2", content = "content2")

        coEvery {
            repository.updateNewsArticle(any())
        } returns DbResult.Success(
            NewsArticleData(
                title = newUpdate.title,
                content = newUpdate.content
            )
        )

        val response = service.updateNewsArticle(newUpdate)

        assertEquals(newUpdate.title, response.title)
        assertEquals(newUpdate.content, response.content)
    }

    @Test
    fun `should throw bad request exception for update news article containing invalid data`() = runTest {
        val dataWithInvalidTitle = dataDto.copy(
            title = ""
        )

        val badRequestExceptionForEmptyTitle = assertFailsWith<BadRequestException> {
            service.updateNewsArticle(dataWithInvalidTitle)
        }
        assertEquals("title is required", badRequestExceptionForEmptyTitle.error)

        val dataWithInvalidContent = dataDto.copy(
            content = ""
        )

        val badRequestExceptionForEmptyContent = assertFailsWith<BadRequestException> {
            service.updateNewsArticle(dataWithInvalidContent)
        }
        assertEquals("content is required", badRequestExceptionForEmptyContent.error)

        val dataWithInvalidCreatorId = dataDto.copy(
            creatorId = 0
        )

        val badRequestExceptionForEmptyCreatorId = assertFailsWith<BadRequestException> {
            service.updateNewsArticle(dataWithInvalidCreatorId)
        }
        assertEquals("creator id is required", badRequestExceptionForEmptyCreatorId.error)
    }

    @Test
    fun `should throw not found exception for update news article`() = runTest {
        coEvery {
            repository.updateNewsArticle(any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException> {
            service.updateNewsArticle(dataDto)
        }
    }

    @Test
    fun `should throw internal server exception for update news article`() = runTest {
        coEvery {
            repository.updateNewsArticle(any())
        } returns DbResult.Failure(DbError.ConnectionError("error"))

        assertFailsWith<InternalServerException> {
            service.updateNewsArticle(dataDto)
        }
    }
}