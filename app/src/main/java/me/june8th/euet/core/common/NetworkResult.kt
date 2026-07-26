package me.june8th.euet.core.common

import retrofit2.HttpException
import java.io.IOException

/** Outcome of a single network/data operation. */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>
}

/** Wraps a suspend API call, mapping exceptions to a friendly [NetworkResult.Error]. */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: HttpException) {
        val msg = when (e.code()) {
            401 -> "Your session has expired. Please sign in again."
            403 -> "You don't have access to this."
            404 -> "Not found."
            in 500..599 -> "The server is having trouble. Try again later."
            else -> "Request failed (${e.code()})."
        }
        NetworkResult.Error(msg, e)
    } catch (e: IOException) {
        NetworkResult.Error("No connection. Check your network and try again.", e)
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Something went wrong.", e)
    }

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
}
