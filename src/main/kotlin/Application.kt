package com.martdev

import com.martdev.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

//todo: add request validation plugin
//todo: work on reader repository
//todo: test the reader repository
//todo: work on reader service
//todo: test reader service
//todo: work on reader endpoint
//todo: test reader endpoint
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
    configureBackgroundJobs()
}
