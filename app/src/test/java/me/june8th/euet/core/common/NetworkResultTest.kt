package me.june8th.euet.core.common

import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class NetworkResultTest {

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    private fun errorFrom(throwable: Throwable): NetworkResult.Error = runBlocking {
        safeApiCall<Unit> { throw throwable } as NetworkResult.Error
    }

    @Test
    fun `safeApiCall wraps a successful block`() = runBlocking {
        val result = safeApiCall { 42 }
        assertEquals(NetworkResult.Success(42), result)
    }

    @Test
    fun `401 maps to session expired`() {
        val error = errorFrom(httpException(401))
        assertEquals(ErrorKind.SESSION_EXPIRED, error.kind)
        assertEquals("Your session has expired. Please sign in again.", error.message)
    }

    @Test
    fun `403 maps to forbidden and 404 to not found`() {
        assertEquals(ErrorKind.FORBIDDEN, errorFrom(httpException(403)).kind)
        assertEquals(ErrorKind.NOT_FOUND, errorFrom(httpException(404)).kind)
    }

    @Test
    fun `5xx maps to server trouble`() {
        assertEquals(ErrorKind.SERVER, errorFrom(httpException(500)).kind)
        assertEquals(ErrorKind.SERVER, errorFrom(httpException(503)).kind)
    }

    @Test
    fun `other http codes stay unknown but keep the code in the message`() {
        val error = errorFrom(httpException(418))
        assertEquals(ErrorKind.UNKNOWN, error.kind)
        assertEquals("Request failed (418).", error.message)
    }

    @Test
    fun `io exceptions map to the network kind`() {
        val error = errorFrom(IOException("timeout"))
        assertEquals(ErrorKind.NETWORK, error.kind)
        assertEquals("No connection. Check your network and try again.", error.message)
    }

    @Test
    fun `unexpected exceptions surface their message or a generic fallback`() {
        val withMessage = errorFrom(IllegalStateException("boom"))
        assertEquals(ErrorKind.UNKNOWN, withMessage.kind)
        assertEquals("boom", withMessage.message)

        val without = errorFrom(IllegalStateException())
        assertEquals("Something went wrong.", without.message)
    }

    @Test
    fun `map transforms success and passes errors through unchanged`() {
        val doubled = (NetworkResult.Success(21) as NetworkResult<Int>).map { it * 2 }
        assertEquals(NetworkResult.Success(42), doubled)

        val error: NetworkResult<Int> = NetworkResult.Error("nope", kind = ErrorKind.NETWORK)
        val mapped = error.map { it * 2 }
        assertSame(error, mapped)
        assertTrue(mapped is NetworkResult.Error)
    }
}
