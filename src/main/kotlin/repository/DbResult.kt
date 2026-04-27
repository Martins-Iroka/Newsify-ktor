package com.martdev.repository

sealed class DbResult<out T> {
    data class Success<T>(val value: T) : DbResult<T>()
    data class Failure(val error: DbError) : DbResult<Nothing>()
}

sealed interface DbError {
    data class NotFound(val message: String = "Not found") : DbError
    data class ConnectionError(val message: String) : DbError
    data class UnknownError(val cause: Throwable = Exception()) : DbError
    data object UniqueViolation : DbError
    data object ForeignKeyViolation : DbError
}

