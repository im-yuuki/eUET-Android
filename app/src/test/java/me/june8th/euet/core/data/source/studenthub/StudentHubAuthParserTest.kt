package me.june8th.euet.core.data.source.studenthub

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `api/auth` responses were captured by size, not by shape: the field names are unknown and
 * both login outcomes arrive as HTTP 200. These tests pin the leniency that buys — every plausible
 * key spelling for the captcha, and a success/failure call that never looks at a status code.
 *
 * All fixtures are synthetic. No real credential, token or captcha ever appears here.
 */
class StudentHubAuthParserTest {

    /** Mirrors the Json configuration in AppContainer (the app's Retrofit converter). */
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    private fun parse(raw: String): JsonObject = json.decodeFromString(JsonObject.serializer(), raw)

    /** 96 base64 chars — long enough to pass the "this can't be an id" length check. */
    private val image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5E"

    private val challengeId = "3f1a9c22-8b0e-4a6d-9c11-77d0e5b4a201"

    // --- Captcha: key spellings ---

    @Test
    fun `captcha reads the canonical captchaId and captchaImage pair`() {
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"captchaId":"$challengeId","captchaImage":"$image"}"""),
        )
        assertEquals(challengeId, payload?.captchaId)
        assertEquals(image, payload?.imageBase64)
    }

    @Test
    fun `captcha reads a short id key and a short image key`() {
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"id":"$challengeId","image":"$image"}"""),
        )
        assertEquals(challengeId, payload?.captchaId)
        assertEquals(image, payload?.imageBase64)
    }

    @Test
    fun `captcha reads imageBase64 and base64 spellings`() {
        val viaImageBase64 = StudentHubAuthParser.parseCaptcha(
            parse("""{"captchaId":"$challengeId","imageBase64":"$image"}"""),
        )
        val viaBase64 = StudentHubAuthParser.parseCaptcha(
            parse("""{"id":"$challengeId","base64":"$image"}"""),
        )
        assertEquals(image, viaImageBase64?.imageBase64)
        assertEquals(image, viaBase64?.imageBase64)
    }

    @Test
    fun `captcha reads a data field holding the image`() {
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"captchaId":"$challengeId","data":"$image"}"""),
        )
        assertEquals(image, payload?.imageBase64)
    }

    @Test
    fun `captcha reads a captcha field holding the image`() {
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"key":"$challengeId","captcha":"$image"}"""),
        )
        assertEquals(challengeId, payload?.captchaId)
        assertEquals(image, payload?.imageBase64)
    }

    @Test
    fun `captcha unwraps a data envelope`() {
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"status":200,"data":{"captchaId":"$challengeId","captchaImage":"$image"}}"""),
        )
        assertEquals(challengeId, payload?.captchaId)
        assertEquals(image, payload?.imageBase64)
    }

    // --- Captcha: data-uri prefix ---

    @Test
    fun `captcha strips a data uri prefix`() {
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"captchaId":"$challengeId","captchaImage":"data:image/png;base64,$image"}"""),
        )
        assertEquals(image, payload?.imageBase64)
    }

    @Test
    fun `captcha strips a jpeg data uri prefix and wrapping whitespace`() {
        // Escaped newlines, i.e. what a line-wrapped payload looks like inside a JSON string.
        val wrapped = image.chunked(40).joinToString("""\n""")
        val payload = StudentHubAuthParser.parseCaptcha(
            parse("""{"id":"$challengeId","image":"data:image/jpeg;base64,$wrapped"}"""),
        )
        assertEquals(image, payload?.imageBase64)
    }

    @Test
    fun `stripping leaves a bare payload untouched`() {
        assertEquals(image, StudentHubAuthParser.stripDataUriPrefix(image))
    }

    // --- Captcha: refusals ---

    @Test
    fun `captcha without an id is unusable`() {
        assertNull(StudentHubAuthParser.parseCaptcha(parse("""{"captchaImage":"$image"}""")))
    }

    @Test
    fun `captcha without an image is unusable`() {
        assertNull(StudentHubAuthParser.parseCaptcha(parse("""{"captchaId":"$challengeId"}""")))
    }

    @Test
    fun `captcha does not mistake the id for the image`() {
        // Only `id` is present, and a UUID is far too short to be an image payload.
        assertNull(StudentHubAuthParser.parseCaptcha(parse("""{"id":"$challengeId"}""")))
    }

    // --- Login: rejections (HTTP 200, ~90-byte body) ---

    @Test
    fun `a small error body is a rejection despite the 200`() {
        val verdict = StudentHubAuthParser.parseLogin(
            parse("""{"status":400,"message":"Sai tài khoản hoặc mật khẩu"}"""),
        )
        assertTrue(verdict is LoginVerdict.Rejected)
        assertFalse((verdict as LoginVerdict.Rejected).captchaProblem)
    }

    @Test
    fun `a message-only body with no session material is a rejection`() {
        val verdict = StudentHubAuthParser.parseLogin(parse("""{"message":"Đăng nhập thất bại"}"""))
        assertTrue(verdict is LoginVerdict.Rejected)
    }

    @Test
    fun `success false wins over anything else in the body`() {
        val verdict = StudentHubAuthParser.parseLogin(
            parse("""{"success":false,"message":"Không thành công","user":{"userName":"22028123"}}"""),
        )
        assertTrue(verdict is LoginVerdict.Rejected)
    }

    @Test
    fun `a status word marks a failure`() {
        val verdict = StudentHubAuthParser.parseLogin(parse("""{"status":"ERROR","error":"Denied"}"""))
        assertTrue(verdict is LoginVerdict.Rejected)
    }

    @Test
    fun `a captcha complaint is told apart from bad credentials`() {
        val english = StudentHubAuthParser.parseLogin(
            parse("""{"status":400,"message":"Invalid captcha value"}"""),
        )
        val vietnamese = StudentHubAuthParser.parseLogin(
            parse("""{"status":400,"message":"Mã xác nhận không đúng"}"""),
        )
        val byCode = StudentHubAuthParser.parseLogin(
            parse("""{"success":false,"errorCode":"CAPTCHA_EXPIRED"}"""),
        )
        assertTrue((english as LoginVerdict.Rejected).captchaProblem)
        assertTrue((vietnamese as LoginVerdict.Rejected).captchaProblem)
        assertTrue((byCode as LoginVerdict.Rejected).captchaProblem)
    }

    // --- Login: acceptances (HTTP 200, ~472-byte body) ---

    @Test
    fun `a token bearing body is accepted and the token is picked up`() {
        val verdict = StudentHubAuthParser.parseLogin(
            parse("""{"accessToken":"header.payload.signature","expiresIn":3600}"""),
        )
        assertEquals(LoginVerdict.Accepted("header.payload.signature", null), verdict)
    }

    @Test
    fun `a nested data token is picked up`() {
        val verdict = StudentHubAuthParser.parseLogin(
            parse("""{"status":200,"data":{"token":"header.payload.signature","studentCode":"22028123"}}"""),
        )
        assertEquals(LoginVerdict.Accepted("header.payload.signature", "22028123"), verdict)
    }

    @Test
    fun `a cookie only body is accepted on its user payload alone`() {
        // The observed shape: no Authorization header anywhere, so no token comes back either.
        val verdict = StudentHubAuthParser.parseLogin(
            parse(
                """{"user":{"studentCode":"22028123","fullName":"Nguyễn Văn An"},"roles":["STUDENT"]}""",
            ),
        )
        assertEquals(LoginVerdict.Accepted(null, "22028123"), verdict)
    }

    @Test
    fun `a success flag alone is enough`() {
        assertEquals(
            LoginVerdict.Accepted(null, null),
            StudentHubAuthParser.parseLogin(parse("""{"success":true}""")),
        )
    }

    @Test
    fun `a success body keeps its greeting message from being read as an error`() {
        val verdict = StudentHubAuthParser.parseLogin(
            parse("""{"status":200,"message":"Đăng nhập thành công","token":"header.payload.signature"}"""),
        )
        assertEquals(LoginVerdict.Accepted("header.payload.signature", null), verdict)
    }

    @Test
    fun `a short numeric code is not mistaken for a student code`() {
        val verdict = StudentHubAuthParser.parseLogin(
            parse("""{"code":200,"token":"header.payload.signature"}"""),
        )
        assertEquals(LoginVerdict.Accepted("header.payload.signature", null), verdict)
    }

    @Test
    fun `a short value under a token key is a status word, not session material`() {
        // "ok" under `token` must not read as a session; nothing else here settles it either.
        assertEquals(LoginVerdict.Inconclusive, StudentHubAuthParser.parseLogin(parse("""{"token":"ok"}""")))
    }

    // --- Login: nothing to go on ---

    @Test
    fun `an empty body is inconclusive so the caller probes the session`() {
        assertEquals(LoginVerdict.Inconclusive, StudentHubAuthParser.parseLogin(parse("{}")))
    }

    @Test
    fun `a body with only an ok status is inconclusive`() {
        assertEquals(LoginVerdict.Inconclusive, StudentHubAuthParser.parseLogin(parse("""{"status":200}""")))
    }
}
