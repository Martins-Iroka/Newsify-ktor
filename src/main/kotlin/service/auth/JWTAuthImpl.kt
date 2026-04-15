package com.martdev.service.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.martdev.config.AuthConfig
import org.koin.core.annotation.Single
import java.util.*
import kotlin.time.Duration.Companion.minutes

@Single
class JWTAuthImpl(
    private val configuration: AuthConfig
) : JWTAuth {
    override fun generateToken(userId: String): String {
        val exp = configuration.exp
        val audience = configuration.audience
        val issuer = configuration.iss
        val secret = configuration.secret
        val expirationDate = Date(System.currentTimeMillis() + exp.minutes.inWholeMilliseconds)

        return JWT.create()
            .withClaim("userId", userId)
            .withAudience(audience)
            .withIssuer(issuer)
            .withIssuedAt(Date(System.currentTimeMillis()))
            .withExpiresAt(expirationDate)
            .withNotBefore(Date(System.currentTimeMillis()))
            .sign(Algorithm.HMAC256(secret))
    }
}