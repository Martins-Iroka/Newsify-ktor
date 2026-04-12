package com.martdev.service.auth

import com.martdev.config.AuthConfig
import com.martdev.config.Configuration
import com.martdev.config.DatabaseConfig
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
        val configuration = Configuration(authConfig = authConfig)
        val jwtAuth = JWTAuthImpl(configuration)
        val token = jwtAuth.generateToken("userID")
        assertTrue(token.isNotEmpty())
    }
}