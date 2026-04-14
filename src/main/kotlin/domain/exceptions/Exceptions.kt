package com.martdev.domain.exceptions

data class BadRequestException(val error: String = "bad request") : Exception(error)

data class InternalServerException(val error: String = "the server encountered a problem") : Exception(error)

data class NotFoundException(val error: String = "not found") : Exception(error)

data class UnauthorizedException(val error: String = "unauthorized") : Exception(error)