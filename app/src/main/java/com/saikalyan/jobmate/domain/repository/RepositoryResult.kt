package com.saikalyan.jobmate.domain.repository

/** A repository operation that either returns its value or a classified domain-safe failure. */
sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Failure(val error: RepositoryError) : RepositoryResult<Nothing>
}

sealed interface RepositoryError {
    data class InvalidInput(val field: String, val reasonCode: String) : RepositoryError
    data object NotFound : RepositoryError
    data object Conflict : RepositoryError
    data class CorruptData(val field: String) : RepositoryError
    data object Storage : RepositoryError
    data object Unexpected : RepositoryError
}