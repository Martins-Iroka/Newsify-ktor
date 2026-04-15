package com.martdev.plugins

import com.martdev.config.AuthConfig
import com.martdev.config.DatabaseConfig
import com.martdev.config.StytchConfig
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ksp.generated.com_martdev_AppModule
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    val databaseAddress = environment.getEnvValue("database.address")
    val maxOpenCon = environment.getEnvValue("database.maxOpenConns").toIntOrNull() ?: 10
    val maxIdleCon = environment.getEnvValue("database.maxIdleConns").toIntOrNull() ?: 10
    val maxIdleTime = environment.getEnvValue("database.maxIdleTime")

    val secret = environment.getEnvValue("jwt.secret")
    val issuer = environment.getEnvValue("jwt.issuer")
    val audience = environment.getEnvValue("audience")
    val exp = environment.getEnvValue("expirationMinutes").toLongOrNull() ?: 15


    val stytchId = environment.getEnvValue("stytchID")
    val stytchSecret = environment.getEnvValue("stytchSecret")

    val authConfig = AuthConfig(secret, exp, issuer, audience)

    val databaseConfig = DatabaseConfig(
        databaseAddress, maxOpenCon, maxIdleCon, maxIdleTime
    )
    val stytchConfig = StytchConfig(stytchId, stytchSecret)

    install(Koin) {
        slf4jLogger()
        val configModule = module {
            single { authConfig }
            single { databaseConfig }
            single { stytchConfig }
        }
        modules(com_martdev_AppModule, configModule)
    }
}

private fun ApplicationEnvironment.getEnvValue(key: String) = config.property(key).getString()