package me.june8th.euet.app.common

import me.june8th.euet.core.common.ErrorKind

/** Generic screen state: loading spinner, error with retry, empty state, or loaded data. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(
        val message: String,
        val kind: ErrorKind = ErrorKind.UNKNOWN,
    ) : UiState<Nothing>
    data class Data<T>(val value: T) : UiState<T>
}
