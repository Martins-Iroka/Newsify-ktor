package com.martdev.service.auth

import com.martdev.config.AuthConfig
import org.junit.Assert.*
import org.junit.Test

class JWTAuthImplTest {

    @Test
    fun testJWTAuthImpl() {
        val authConfig = AuthConfig(
            secret = "test",
            exp = 15L,
            iss = "iss",
            audience = "audience"
        )
        val jwtAuth = JWTAuthImpl(authConfig)
        val token = jwtAuth.generateAccessToken("userID")
        assertTrue(token.isNotEmpty())
    }
}