package com.martdev.service.reader

import com.martdev.domain.NewsArticleData
import com.martdev.domain.User
import com.martdev.domain.exceptions.BadRequestException
import com.martdev.domain.exceptions.InternalServerException
import com.martdev.domain.exceptions.NotFoundException
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.reader.ReaderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReaderServiceImplTest {

    @get:Rule
    val mockK = MockKRule(this)

    @MockK
    private lateinit var repository: ReaderRepository

    private lateinit var service: ReaderService
    @Before
    fun setUp() {
        service = ReaderServiceImpl(repository)
    }

    @Test
    fun `get list of creators returns non empty list`() = runTest {
        val creators = listOf(
            User(id = 1, username = "username"),
            User(id = 2, username = "username2"),
            User(id = 3, username = "username3"),
        )

        coEvery {
            repository.getListOfCreators()
        } returns DbResult.Success(creators)

        val result = service.getListOfCreators()

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `get list of creators returns empty list`() = runTest {
        coEvery {
            repository.getListOfCreators()
        } returns DbResult.Success(emptyList())
    }

    @Test
    fun `get list of creators throws internal server exception`() = runTest {
        coEvery {
            repository.getListOfCreators()
        } returns DbResult.Failure(DbError.UnknownError(Exception()))

        assertFailsWith<InternalServerException> {
            service.getListOfCreators()
        }
    }

    @Test
    fun `a reader should follow creator`() = runTest {
        coEvery {
            repository.followCreator(any(), any())
        } returns DbResult.Success(Pair(1, 2))

        service.followCreator(1, 2)

        coVerify {
            repository.followCreator(any(), any())
        }
    }

    @Test
    fun `follow creator should throw bad request exception`() = runTest {
        coEvery {
            repository.followCreator(any(), any())
        } returns DbResult.Failure(DbError.UniqueViolation)

        val exception = assertFailsWith<BadRequestException> {
            service.followCreator(1, 2)
        }

        assertEquals("You can't follow yourself!", exception.error)
    }

    @Test
    fun `follow creator should throw internal server exception`() = runTest {
        coEvery {
            repository.followCreator(any(), any())
        } returns DbResult.Failure(DbError.UnknownError(Exception()))

        assertFailsWith<InternalServerException> {
            service.followCreator(1, 2)
        }
    }

    @Test
    fun `a reader should unfollow a creator`() = runTest {
        coEvery {
            repository.unfollowCreator(any(), any())
        } returns DbResult.Success(Unit)

        service.unfollowCreator(1, 2)

        coVerify {
            repository.unfollowCreator(any(), any())
        }
    }

    @Test
    fun `unfollow a creator should throw not found exception`() = runTest {
        coEvery {
            repository.unfollowCreator(any(), any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException> {
            service.unfollowCreator(1, 2)
        }
    }

    @Test
    fun `unfollow a creator should throw internal server exception`() = runTest {
        coEvery {
            repository.unfollowCreator(any(), any())
        } returns DbResult.Failure(DbError.UnknownError())

        assertFailsWith<InternalServerException> {
            service.unfollowCreator(1, 2)
        }
    }

    @Test
    fun `get all articles by creator returns valid list`() = runTest {
        val articles = listOf(
            NewsArticleData(),
            NewsArticleData(),
            NewsArticleData()
        )
        coEvery {
            repository.getAllArticlesByCreatorId(any())
        } returns DbResult.Success(articles)

        val result = service.getAllArticlesByCreatorId(1)
        assertTrue(result.isNotEmpty())
        assertEquals(3, result.size)
    }

    @Test
    fun `get all articles by creator returns empty list`() = runTest {
        coEvery {
            repository.getAllArticlesByCreatorId(any())
        } returns DbResult.Success(emptyList())

        val result = service.getAllArticlesByCreatorId(1)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `get all articles by creator throws internal server exception`() = runTest {
        coEvery {
            repository.getAllArticlesByCreatorId(any())
        } returns DbResult.Failure(DbError.UnknownError())

        assertFailsWith<InternalServerException>{ service.getAllArticlesByCreatorId(1) }
    }

    @Test
    fun `get news article by id returns valid data`() = runTest {
        val article = NewsArticleData(
            id = 1,
            title = "title",
            content = "content"
        )
        coEvery {
            repository.getNewsArticleById(any(), any())
        } returns DbResult.Success(
            article
        )

        val result = service.getNewsArticleById(1, 2)

        assertEquals(article.title, result.title)
        assertEquals(article.content, result.content)
    }

    @Test
    fun `get news article by id throws not found exception`() = runTest {
        coEvery {
            repository.getNewsArticleById(any(), any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException> {
            service.getNewsArticleById(1, 2)
        }
    }

    @Test
    fun `get news article by id throws internal server exception`() = runTest {
        coEvery {
            repository.getNewsArticleById(any(), any())
        } returns DbResult.Failure(DbError.UnknownError())

        assertFailsWith<InternalServerException> {
            service.getNewsArticleById(1, 2)
        }
    }

    @Test
    fun `add fcm token to an existing reader data`() = runTest {
        coEvery {
            repository.updateFcmToken(any(), any())
        } returns DbResult.Success(Unit)

        service.updateFcmToken(1, "token")

        coVerify {
            repository.updateFcmToken(any(), any())
        }
    }

    @Test
    fun `add fcm token to an existing reader data throws not found exception`() = runTest {
        coEvery {
            repository.updateFcmToken(any(), any())
        } returns DbResult.Failure(DbError.NotFound())

        assertFailsWith<NotFoundException>{ service.updateFcmToken(1, "token") }
    }

    @Test
    fun `add fcm token to an existing reader data throws internal server exception`() = runTest {
        coEvery {
            repository.updateFcmToken(any(), any())
        } returns DbResult.Failure(DbError.UnknownError())

        assertFailsWith<InternalServerException>{ service.updateFcmToken(1, "token") }
    }
}