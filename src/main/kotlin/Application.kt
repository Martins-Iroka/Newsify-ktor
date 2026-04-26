package com.martdev

import com.martdev.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

//todo: work on reader service
//todo: test reader service
//todo: work on reader endpoint
//todo: test reader endpoint
//todo: implement FCM
//todo: rewrite the UserRepositoryImplTest and CreatorRepositoryImplTest class inline with ReaderRepository
fun main(args: Array<String>) {
    dotenv {
        systemProperties = true
    }
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    configureKoin()
    configureCallLogging()
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureDatabase()
    configureRateLimiter()
    configureStatusPage()
    configureRouting()
    configureRequestValidation()
    configureBackgroundJobs()
}
