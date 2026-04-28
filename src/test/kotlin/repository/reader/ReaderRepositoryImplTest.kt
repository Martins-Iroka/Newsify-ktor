package com.martdev.repository.reader

import com.martdev.domain.NewsArticleData
import com.martdev.domain.Role
import com.martdev.domain.User
import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import com.martdev.repository.connectAndMigrate
import com.martdev.repository.creator_repo.CreatorRepository
import com.martdev.repository.creator_repo.CreatorRepositoryImpl
import com.martdev.repository.postgres
import com.martdev.repository.user_repo.UserRepository
import com.martdev.repository.user_repo.UserRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderRepositoryImplTest {

    private lateinit var readerRepository: ReaderRepository

    private lateinit var userRepository: UserRepository

    private lateinit var creatorRepository: CreatorRepository

    companion object {
        @BeforeClass @JvmStatic
        fun startContainer() {
            postgres.start()
            connectAndMigrate()
        }

        @AfterClass @JvmStatic
        fun stopContainer() {
            postgres.stop()
        }
    }
    @Before
    fun setUp() {
        transaction {
            exec(
                "TRUNCATE TABLE followers, refresh_tokens, users_verification_tracking, news_article, users RESTART IDENTITY CASCADE"
            )
        }
        readerRepository = ReaderRepositoryImpl()
        userRepository = UserRepositoryImpl()
        creatorRepository = CreatorRepositoryImpl()
    }

    @Test
    fun `should get list of valid creators`() = runTest {
      saveCreatorAndUser()
        val creatorsResult = readerRepository.getListOfCreators()
        assertTrue(creatorsResult is DbResult.Success)
        assertEquals(3, creatorsResult.value.size)
    }

    @Test
    fun `should have a reader follow a creator`() = runTest {
        saveCreatorAndUser()
        val savedCID = firstCreatorId()
        val readerId = getReaderID()

        val readerId2 = getReaderID("testing114@email.com")
        val reader1FollowsResult = readerRepository.followCreator(savedCID, readerId)
        assertTrue(reader1FollowsResult is DbResult.Success)

        val (cid, rid) = reader1FollowsResult.value
        assertEquals(savedCID, cid)
        assertEquals(readerId, rid)

        val reader2FollowsResult = readerRepository.followCreator(savedCID, readerId2)
        assertTrue(reader2FollowsResult is DbResult.Success)

        val (cid2, rid2) = reader2FollowsResult.value
        assertEquals(savedCID, cid2)
        assertEquals(readerId2, rid2)
    }

    @Test
    fun `should prevent a creator from following themselves`() = runTest {
        saveCreatorAndUser()
        val savedCID = firstCreatorId()

        val result = readerRepository.followCreator(savedCID, savedCID)
        assertTrue(result is DbResult.Failure)
        println(result.toString())
    }

    @Test
    fun `should get valid list of followers of a creator`() = runTest {
        saveCreatorAndUser()
        val savedCID = firstCreatorId()
        val readerId = getReaderID()

        val readerId2 = getReaderID("testing114@email.com")
        val reader1FollowsResult = readerRepository.followCreator(savedCID, readerId)
        assertTrue(reader1FollowsResult is DbResult.Success)

        val (cid, rid) = reader1FollowsResult.value
        assertEquals(savedCID, cid)
        assertEquals(readerId, rid)

        val reader2FollowsResult = readerRepository.followCreator(savedCID, readerId2)
        assertTrue(reader2FollowsResult is DbResult.Success)
    }

    @Test
    fun `a reader should unfollow a creator`() = runTest {
        saveCreatorAndUser()
        val savedCID = firstCreatorId()
        val readerId = getReaderID()

        val readerId2 = getReaderID("testing114@email.com")
        val reader1FollowsResult = readerRepository.followCreator(savedCID, readerId)
        assertTrue(reader1FollowsResult is DbResult.Success)

        val (cid, rid) = reader1FollowsResult.value
        assertEquals(savedCID, cid)
        assertEquals(readerId, rid)

        val reader2FollowsResult = readerRepository.followCreator(savedCID, readerId2)
        assertTrue(reader2FollowsResult is DbResult.Success)

        val unfollowerResult = readerRepository.unfollowCreator(savedCID, readerId)
        assertTrue(unfollowerResult is DbResult.Success)
    }

    @Test
    fun `unfollow creator should return not found`() = runTest {
        saveCreatorAndUser()
        val savedCID = firstCreatorId()
        val readerID = getReaderID("testing114@email.com")
        val unfollowResult = readerRepository.unfollowCreator(savedCID, readerID)
        assertTrue(unfollowResult is DbResult.Failure)
        assertTrue(unfollowResult.error is DbError.NotFound)
    }

    @Test
    fun `should get all articles by creator id then get news article by creator id and article id`() = runTest {
        saveCreatorAndUser()
        val savedCID = lastCreatorId()
        for (i in 1..2) {
            val newsArticles = NewsArticleData(title = "title$i", content = "content$i", creatorId = savedCID)
            val result = creatorRepository.saveNewsArticle(newsArticles)
            assertTrue(result is DbResult.Success)
        }

        val creatorArticlesResult = readerRepository.getAllArticlesByCreatorId(savedCID)
        assertTrue(creatorArticlesResult is DbResult.Success)
        assertEquals(2, creatorArticlesResult.value.size)

        val articleId = creatorArticlesResult.value.first().id

        val articleResult = readerRepository.getNewsArticleById(savedCID, articleId)
        assertTrue(articleResult is DbResult.Success)
        val article = articleResult.value
        assertEquals("title1", article.title)
        assertEquals("content1", article.content)
    }

    @Test
    fun `add fcm token to an existing reader data`() = runTest {
        saveCreatorAndUser()
        val savedReaderId = getReaderID()
        val result = readerRepository.updateFcmToken(savedReaderId, "fcm_token")
        assertTrue(result is DbResult.Success)
    }

    private suspend fun getReaderID(email: String = "testing112@email.com"): Long {
        val userResult = userRepository.getUserByEmail(email)
        val id = (userResult as DbResult.Success).value.id
        return id
    }

    private suspend fun firstCreatorId(): Long {
        val creatorsResult = readerRepository.getListOfCreators()
        val savedCID = (creatorsResult as DbResult.Success).value.first().id
        return savedCID
    }

    private suspend fun lastCreatorId(): Long {
        val creatorsResult = readerRepository.getListOfCreators()
        val savedCID = (creatorsResult as DbResult.Success).value.last().id
        return savedCID
    }

    private suspend fun saveCreatorAndUser() {
        for (i in 1..5) {
            if (i % 2 != 0) {
                val user = User(
                    email = "test1$i@email.com",
                    username = "username1$i",
                    password = "password$i",
                    role = Role.CREATOR
                )
                userRepository.saveUserAndVerificationToken(user, "token1$i")
            } else {
                val user = User(email = "testing11$i@email.com", username = "username11$i", password = "password$i")
                userRepository.saveUserAndVerificationToken(user, "token1$i")
            }
        }
    }

}