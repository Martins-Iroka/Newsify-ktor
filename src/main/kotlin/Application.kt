package com.martdev

import com.martdev.config.AuthConfig
import com.martdev.config.Configuration
import com.martdev.config.DatabaseConfig
import com.martdev.config.StytchConfig
import com.martdev.plugins.configureHTTP
import com.martdev.plugins.configureRouting
import com.martdev.plugins.configureSecurity
import com.martdev.plugins.configureSerialization
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
    configureHTTP()
    configureSecurity(secret, audience, issuer)
    configureSerialization()
    configureDatabases()
    configureRouting()
}

private fun ApplicationEnvironment.getEnvValue(key: String) = config.property(key).getString()
