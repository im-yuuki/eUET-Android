package me.june8th.euet.core.common

import retrofit2.HttpException
import java.io.IOException

/**
 * Broad classification of an error. Core keeps emitting stable English [NetworkResult.Error]
 * messages, but tags the common cases with a kind so the UI layer can localize them at display
 * time; [UNKNOWN] means "show the raw message as-is".
 */
enum class ErrorKind {
    NETWORK,
    SESSION_EXPIRED,
    FORBIDDEN,
    NOT_FOUND,
    SERVER,
    BAD_CREDENTIALS,

    /** The captcha answer was wrong, or its id had already been spent. Retry with a fresh one. */
    CAPTCHA_REJECTED,
    CANVAS_TOKEN_REJECTED,
    SIGN_IN_DAOTAO,
    SIGN_IN_STUDENTHUB,
    CONNECT_CANVAS,
    UNKNOWN,
}

/** Outcome of a single network/data operation. */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val kind: ErrorKind = ErrorKind.UNKNOWN,
    ) : NetworkResult<Nothing>
}

/** Wraps a suspend API call, mapping exceptions to a friendly [NetworkResult.Error]. */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): NetworkResult<T> =
    try {
        NetworkResult.Success(block())
    } catch (e: HttpException) {
        val (msg, kind) = when (e.code()) {
            401 -> "Your session has expired. Please sign in again." to ErrorKind.SESSION_EXPIRED
            403 -> "You don't have access to this." to ErrorKind.FORBIDDEN
            404 -> "Not found." to ErrorKind.NOT_FOUND
            in 500..599 -> "The server is having trouble. Try again later." to ErrorKind.SERVER
            else -> "Request failed (${e.code()})." to ErrorKind.UNKNOWN
        }
        NetworkResult.Error(msg, e, kind)
    } catch (e: IOException) {
        NetworkResult.Error("No connection. Check your network and try again.", e, ErrorKind.NETWORK)
    } catch (e: Exception) {
        NetworkResult.Error(e.message ?: "Something went wrong.", e)
    }

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
}
