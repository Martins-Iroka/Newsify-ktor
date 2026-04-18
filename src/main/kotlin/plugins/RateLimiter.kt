package com.martdev.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiter() {
    install(RateLimit) {
        register(name = RateLimitName("resend-otp")) {
            rateLimiter(limit = 1, refillPeriod = 60.seconds)
        }
    }
}
