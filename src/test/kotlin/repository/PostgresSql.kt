package com.martdev.repository

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.postgresql.PostgreSQLContainer

val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine").apply {
    withDatabaseName("newsify_test")
    withUsername("test")
    withPassword("test")
}

fun connectAndMigrate(): Database {
    val flyway = Flyway.configure()
        .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        .load()

    flyway.migrate()

    return Database.connect(
        url = postgres.jdbcUrl,
        driver = "org.postgresql.Driver",
        user = postgres.username,
        password = postgres.password
    )
}

fun resetDbTable() {
    transaction {
        exec(
            "TRUNCATE TABLE followers, refresh_tokens, users_verification_tracking, news_article, users RESTART IDENTITY CASCADE"
        )
    }
}