val exposed_version: String by project
val h2_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val postgres_version: String by project
val flyway_version: String by project
val koin_version: String by project
val koin_annotation_version: String by project
val test_container_version: String by project

plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.4.2"
    id("com.google.devtools.ksp") version "2.3.6"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
    id("org.flywaydb.flyway") version "10.15.0"
}

/*flyway {
    url = "jdbc:postgresql://localhost:5433/newsify"
    user = "your_user"
    password = "your_password"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}*/

ktor {
    openApi {
        enabled = true
        codeInferenceEnabled = true
        onlyCommented = false
    }
}

group = "com.martdev"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-swagger")
    implementation("io.ktor:ktor-server-routing-openapi")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("com.h2database:h2:$h2_version")
    implementation("org.postgresql:postgresql:${postgres_version}")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("com.stytch.java:sdk:6.0.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposed_version")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-host-common")
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")
    implementation("io.insert-koin:koin-ktor:${koin_version}")
    implementation("io.insert-koin:koin-logger-slf4j:${koin_version}")
    implementation("io.insert-koin:koin-annotations:$koin_annotation_version")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.5.1")
    implementation("io.ktor:ktor-server-rate-limit")
    implementation("io.ktor:ktor-server-call-logging")
    ksp("io.insert-koin:koin-ksp-compiler:$koin_annotation_version")
    testImplementation("org.testcontainers:testcontainers:$test_container_version")
    testImplementation("org.testcontainers:testcontainers-postgresql:$test_container_version")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-content-negotiation")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("io.insert-koin:koin-test:${koin_version}")
    testImplementation("io.insert-koin:koin-test-junit4:${koin_version}")
}
