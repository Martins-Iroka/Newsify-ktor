package com.martdev.config

import java.util.Date


data class Configuration(
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
    val authConfig: AuthConfig = AuthConfig(),
    val stytchConfig: StytchConfig = StytchConfig()
)

data class DatabaseConfig(
    val address: String = "",
    val maxOpenCon: Int = 0,
    val maxIdleCon: Int = 0,
    val maxIdleTime: String = ""
)

data class AuthConfig(
    val secret: String = "",
    val exp: Long = 0,
    val iss: String = "",
    val audience: String = ""
)

data class StytchConfig(
    val projectId: String = "",
    val secret: String = ""
)