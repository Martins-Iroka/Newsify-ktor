package com.martdev

import com.martdev.config.AuthConfig
import com.martdev.config.DBConfig
import com.martdev.config.StytchConfig
import com.martdev.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ksp.generated.com_martdev_AppModule
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

//todo: test creator repo
//todo: create creator service
//todo: test creator service
//todo: write creator endpoint
//todo: test creator endpoint
fun main(args: Array<String>) {
    dotenv {
        systemProperties = true
    }
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val databaseAddress = environment.getEnvValue("database.address")
    val maxOpenCon = environment.getEnvValue("database.maxOpenConns").toIntOrNull() ?: 10
    val maxIdleCon = environment.getEnvValue("database.maxIdleConns").toIntOrNull() ?: 10
    val maxIdleTime = environment.getEnvValue("database.maxIdleTime").toLongOrNull() ?: 15
    val user = environment.getEnvValue("database.user")
    val password = environment.getEnvValue("database.password")

    val secret = environment.getEnvValue("jwt.secret")
    val issuer = environment.getEnvValue("jwt.issuer")
    val audience = environment.getEnvValue("jwt.audience")
    val exp = environment.getEnvValue("jwt.expirationMinutes").toLongOrNull() ?: 15


    val stytchId = environment.getEnvValue("stytch.stytchID")
    val stytchSecret = environment.getEnvValue("stytch.stytchSecret")

    val authConfig = AuthConfig(secret, exp, issuer, audience)

    val dbConfig = DBConfig(
        databaseAddress, user, password, maxOpenCon, maxIdleCon, maxIdleTime
    )
    val stytchConfig = StytchConfig(stytchId, stytchSecret)

    install(Koin) {
        slf4jLogger()
        val configModule = module {
            single { authConfig }
            single { dbConfig }
            single { stytchConfig }
        }
        modules(com_martdev_AppModule, configModule)
    }
    configureCallLogging()
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureDatabase()
    configureRateLimiter()
    configureStatusPage()
    configureRouting()
    configureBackgroundJobs()
}

private fun ApplicationEnvironment.getEnvValue(key: String) = config.property(key).getString()
