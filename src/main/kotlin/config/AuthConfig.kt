package com.martdev.config

data class AuthConfig(
    val secret: String = "",
    val exp: Long = 0,
    val iss: String = "",
    val audience: String = ""
)