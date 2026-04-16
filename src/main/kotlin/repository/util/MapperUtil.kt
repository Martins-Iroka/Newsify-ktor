package com.martdev.repository.util

import com.martdev.repository.DbError
import com.martdev.repository.DbResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelSuspendTransaction

suspend fun <T> withTransaction(block: suspend JdbcTransaction.() -> DbResult<T>): DbResult<T> =
    withContext(Dispatchers.IO) {
        inTopLevelSuspendTransaction {
            addLogger(StdOutSqlLogger)
            try {
                block()
            } catch (e: ExposedSQLException) {
                val dbError = handleDbException(e)
                DbResult.Failure(dbError)
            } catch (e: Exception) {
                DbResult.Failure(DbError.UnknownError(e))
            }
        }
    }

fun handleDbException(e: ExposedSQLException): DbError {
    return when (e.sqlState) {
        "23505" -> DbError.UniqueViolation
        "23503" -> DbError.ForeignKeyViolation
        else -> DbError.UnknownError(e)
    }
}
