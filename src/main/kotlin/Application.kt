package com.martdev

import com.martdev.config.AuthConfig
import com.martdev.config.Configuration
import com.martdev.config.DatabaseConfig
import com.martdev.config.StytchConfig
import com.martdev.plugins.configureHTTP
import com.martdev.plugins.configureRouting
import com.martdev.plugins.configureSecurity
import com.martdev.plugins.configureSerialization
import com.martdev.plugins.configureStatusPage
import com.martdev.repository.user_repo.UserRepositoryImpl
import com.martdev.service.otp_provider.StytchOtpProvider
import com.martdev.service.user.UserServiceImpl
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val databaseAddress = environment.getEnvValue("database.address")
    val maxOpenCon = environment.getEnvValue("database.maxOpenConns")
    val maxIdleCon = environment.getEnvValue("database.maxIdleConns")
    val maxIdleTime = environment.getEnvValue("database.maxIdleTime")
    val databaseConfig = DatabaseConfig(
        databaseAddress, maxOpenCon.toInt(), maxIdleCon.toInt(), maxIdleTime
    )

    val secret = environment.getEnvValue("jwt.secret")
    val issuer = environment.getEnvValue("jwt.issuer")
    val audience = environment.getEnvValue("audience")
    val exp = environment.getEnvValue("expirationMinutes")
    val authConfig = AuthConfig(secret, exp.toLong(), issuer, audience)

    val stytchId = environment.getEnvValue("stytchID")
    val stytchSecret = environment.getEnvValue("stytchSecret")
    val stytchConfig = StytchConfig(stytchId, stytchSecret)

    val configuration = Configuration(databaseConfig, authConfig, stytchConfig)
    val userRepository = UserRepositoryImpl()
    val otpProvider = StytchOtpProvider(stytchConfig)
    val userService = UserServiceImpl(userRepository, otpProvider)
    configureHTTP()
    configureSecurity(secret, audience, issuer)
    configureSerialization()
    configureDatabases()
    configureRouting(userService)
    configureStatusPage()
}

private fun ApplicationEnvironment.getEnvValue(key: String) = config.property(key).getString()
