package com.martdev.plugins

import com.martdev.domain.Role
import com.martdev.dto.request.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import kotlin.enums.enumEntries

fun Application.configureRequestValidation() {
    val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+[a-zA-Z]{2,}$")
    install(RequestValidation) {
        validate<UserRequest> { user ->
            val isValidRole = enumEntries<Role>().any { it.name == user.role.uppercase() }
            when {
                user.email.isEmpty() || !emailPattern.matches(user.email) -> ValidationResult.Invalid("Invalid email format")
                user.password.length < 8 -> ValidationResult.Invalid("Password must be at least 8 characters long")
                user.username.isBlank() -> ValidationResult.Invalid("Username cannot be empty")
                !isValidRole ->
                    ValidationResult.Invalid("Invalid role specified. Must be one of: ${enumEntries<Role>().joinToString { it.name }}")

                else -> ValidationResult.Valid
            }
        }

        validate<UserVerificationRequest> { request ->
            when {
                request.code.isEmpty() || request.code.length != 6 -> ValidationResult.Invalid("code is not valid")
                request.emailId.isEmpty() -> ValidationResult.Invalid("email id is needed")
                request.token.isEmpty() -> ValidationResult.Invalid("token is needed")
                else -> ValidationResult.Valid
            }
        }

        validate<UserLoginRequest> { request ->
            when {
                request.email.isEmpty()
                        || request.email.length > 255
                        || !emailPattern.matches(request.email)
                        || request.password.isEmpty()
                        || request.password.length < 8 -> ValidationResult.Invalid("invalid email or password")

                else -> ValidationResult.Valid
            }
        }

        validate<ResendOTPRequest> { request ->
            if (request.email.isEmpty() || !emailPattern.matches(request.email)) {
                ValidationResult.Invalid("invalid email")
            } else ValidationResult.Valid
        }

        validate<CreateNewsArticleRequest> { dto ->
            when {
                dto.title.isEmpty() -> ValidationResult.Invalid("title is required")
                dto.content.isEmpty() -> ValidationResult.Invalid("content is required")
                else -> ValidationResult.Valid
            }
        }
    }
}
