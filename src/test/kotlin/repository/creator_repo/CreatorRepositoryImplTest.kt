package com.martdev.repository.creator_repo

import com.martdev.domain.NewsArticleData
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.connectAndMigrate
import com.martdev.repository.postgres
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CreatorRepositoryImplTest {

    private lateinit var repository: CreatorRepository

    val data = NewsArticleData(
        title = "title",
        content = "content",
        creatorId = 12,
    )

    @Before
    fun setup() {
        postgres.start()
        connectAndMigrate()
        repository = CreatorRepositoryImpl()
    }

    @After
    fun tearDown() {
        postgres.stop()
    }

    @Test
    fun `should save news article data then retrieve by id`() = runTest {
        val newsArticleDataIdResult = repository.saveNewsArticle(data)
        assertTrue(newsArticleDataIdResult is DbResult.Success)

        val dataId = newsArticleDataIdResult.value

        val newsArticleDataResult = repository.getNewsArticleById(
            data.creatorId, dataId
        )
        assertTrue(newsArticleDataResult is DbResult.Success)
        val articleData = newsArticleDataResult.value
        assertEquals(data.title, articleData.title)
        assertEquals(data.content, articleData.content)
    }

    @Test
    fun `should throw not found while retrieving article by id`() = runTest {
        val newsArticleDataResult = repository.getNewsArticleById(
            data.creatorId, 11
        )
        assertTrue(newsArticleDataResult is DbResult.Failure)
        assertTrue(newsArticleDataResult.error is DbError.NotFound)
    }

    @Test
    fun `should save news articles then retrieve a list of articles by creator id`() = runTest {

        for (i in 1..6) {
            if (i % 2 == 0) {
                repository.saveNewsArticle(
                    NewsArticleData(
                        title = "title$i",
                        content = "content$i",
                        creatorId = 12
                    )
                )
            } else {
                repository.saveNewsArticle(
                    NewsArticleData(
                        title = "title$i",
                        content = "content$i",
                        creatorId = 14
                    )
                )
            }
        }

        val articlesByCreatorIdResult = repository.getAllNewsArticleByCreatorId(
            12, 5, 0
        )
        assertTrue(articlesByCreatorIdResult is DbResult.Success)
        val articles = articlesByCreatorIdResult.value
        assertTrue(articles.isNotEmpty())
        assertEquals(3, articles.size)
    }

    @Test
    fun `should get all news article by creator id returns empty list`() = runTest {

        val emptyListOfArticlesResult = repository.getAllNewsArticleByCreatorId(
            20, 5, 0
        )
        assertTrue(emptyListOfArticlesResult is DbResult.Success, emptyListOfArticlesResult.toString())
        assertTrue(emptyListOfArticlesResult.value.isEmpty())
    }

    @Test
    fun `should delete news article`() = runTest {
        val savedArticleIdResult = repository.saveNewsArticle(data)
        assertTrue(savedArticleIdResult is DbResult.Success)

        val id = savedArticleIdResult.value

        var retrievedArticleResult = repository.getNewsArticleById(
            data.creatorId, id
        )
        assertTrue(retrievedArticleResult is DbResult.Success)

        val deletedArticleResult = repository.deleteNewsArticle(
            data.creatorId, id
        )
        assertTrue(deletedArticleResult is DbResult.Success)

        retrievedArticleResult = repository.getNewsArticleById(
            data.creatorId, id
        )

        assertTrue(retrievedArticleResult is DbResult.Failure)
        assertTrue(retrievedArticleResult.error is DbError.NotFound)
    }

    @Test
    fun `should update news article`() = runTest {
        val savedArticleResult = repository.saveNewsArticle(data)
        assertTrue(savedArticleResult is DbResult.Success)

        val id = savedArticleResult.value

        val newsData = NewsArticleData(
            id = id,
            "ik updated this",
            content = "this is the content"
        )

        val updatedResult = repository.updateNewsArticle(newsData)
        assertTrue(updatedResult is DbResult.Success)
        val updatedNewsData = updatedResult.value
        assertEquals(updatedNewsData.id, id)
        assertNotEquals(data.title, updatedNewsData.title)
        assertNotEquals(data.content, updatedNewsData.content)
    }
}