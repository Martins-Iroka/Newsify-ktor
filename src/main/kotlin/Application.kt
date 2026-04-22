package com.martdev

import com.martdev.plugins.*
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*

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
