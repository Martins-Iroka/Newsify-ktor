package com.martdev.plugins

import io.ktor.server.application.*

fun Application.configureKoin() {

}

private fun ApplicationEnvironment.getEnvValue(key: String) = config.property(key).getString()