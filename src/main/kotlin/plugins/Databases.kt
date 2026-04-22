package com.martdev.plugins

import com.martdev.config.DBConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.ktor.ext.inject

fun Application.configureDatabase() {
    val dBConfig by inject<DBConfig>()

    val flyway = Flyway.configure()
        .dataSource(
            dBConfig.address,
            dBConfig.user,
            dBConfig.password
        ).baselineOnMigrate(true).load()

    flyway.migrate()

    val config = HikariConfig().apply {
        jdbcUrl = dBConfig.address
        driverClassName = "org.postgresql.Driver"
        username = dBConfig.user
        password = dBConfig.password
        maximumPoolSize = dBConfig.maxOpenCon
        minimumIdle = dBConfig.maxIdleCon
        idleTimeout = dBConfig.maxIdleTime
        connectionTimeout = 10000L
    }
    val dataSource = HikariDataSource(config)

    Database.connect(
        datasource = dataSource
    )
}
