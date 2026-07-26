package me.june8th.euet.app.common

/** Generic screen state: loading spinner, error with retry, empty state, or loaded data. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Data<T>(val value: T) : UiState<T>
}
