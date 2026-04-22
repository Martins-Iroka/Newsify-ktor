package com.martdev.repository

import org.testcontainers.postgresql.PostgreSQLContainer

val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine").apply {
    withDatabaseName("newsify_test")
    withUsername("test")
    withPassword("test")
}