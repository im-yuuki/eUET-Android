package me.june8th.euet.core.data.source.studenthub

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Body of `POST api/auth/login`, byte-for-byte the shape the StudentHub SPA sends.
 *
 * [toString] is overridden so the credentials can never leak through a log statement, a crash
 * report, or the `toString()` of anything holding this object.
 */
@Serializable
data class PasswordLoginRequest(
    val userName: String,
    val password: String,
    val captchaId: String,
    val captchaValue: String,
) {
    override fun toString(): String = "PasswordLoginRequest(captchaId=$captchaId)"
}

/** The id + still-encoded image of one captcha challenge, as read off the captcha response. */
data class CaptchaPayload(
    val captchaId: String,
    val imageBase64: String,
)

/** What the login response says happened. Both outcomes arrive as HTTP 200, so this is all we get. */
sealed interface LoginVerdict {
    /** Session established. [token] is null on the cookie-only path, which is the observed one. */
    data class Accepted(val token: String?, val studentCode: String?) : LoginVerdict

    /** The server said no. [captchaProblem] separates a wrong/expired captcha from bad credentials. */
    data class Rejected(val message: String?, val captchaProblem: Boolean) : LoginVerdict

    /** Neither session material nor an error — the caller has to probe an authenticated endpoint. */
    data object Inconclusive : LoginVerdict
}

/**
 * Reads the two `api/auth` responses — the captcha challenge and the login verdict.
 *
 * These are parsed by hand out of a [JsonObject] rather than through typed DTOs because the exact
 * field names were never observed — only the request shapes and the response sizes were. A flat
 * key search over the response tolerates every plausible spelling and any single-level envelope
 * (`{ "data": { … } }`) without a deserialization exception taking the whole sign-in down.
 *
 * Everything here is pure Kotlin (no Android, no I/O) so it is directly unit-testable.
 */
object StudentHubAuthParser {

    /** Key spellings that could hold the captcha id. Checked in order; first non-blank wins. */
    private val CAPTCHA_ID_KEYS = listOf("captchaid", "id", "captchakey", "key", "uuid", "token")

    /** Key spellings that could hold the captcha image payload. */
    private val CAPTCHA_IMAGE_KEYS = listOf(
        "captchaimage", "image", "imagebase64", "base64", "captchabase64",
        "img", "captcha", "data", "content", "value", "picture",
    )

    /** Fields a rejected login puts its reason in. */
    private val MESSAGE_KEYS = listOf(
        "message", "errormessage", "error_message", "error", "msg", "detail",
        "description", "title", "reason",
    )

    /** Fields whose non-blank value proves a bearer token came back. */
    private val TOKEN_KEYS = listOf("token", "accesstoken", "access_token", "jwt", "idtoken", "id_token")

    /**
     * Keys that only appear once a session exists — the SPA's ~472-byte accepted body carries a
     * user payload, while the ~90-byte rejected body cannot.
     */
    private val IDENTITY_KEYS = setOf(
        "user", "userinfo", "profile", "account", "studentcode", "studentid",
        "fullname", "displayname", "roles", "authorities", "permissions",
        "refreshtoken", "refresh_token", "expiresin", "expires_in", "sessionid",
    )

    /** Substrings that mark a failure reason as being about the captcha, in both app languages. */
    private val CAPTCHA_HINTS = listOf(
        "captcha", "mã xác nhận", "ma xac nhan", "mã bảo mật", "ma bao mat", "mã kiểm tra",
    )

    /** Status/`success` values that mean "rejected" whatever else the body contains. */
    private val FAILURE_WORDS = setOf(
        "error", "fail", "failed", "failure", "unauthorized", "forbidden",
        "bad_request", "invalid", "ko", "nok",
    )

    /**
     * Pulls the captcha id and its base64 image out of the response, or null when the body carries
     * neither. The `data:image/png;base64,` prefix a data URI would add is stripped here.
     */
    fun parseCaptcha(json: JsonObject): CaptchaPayload? {
        val flat = json.flatten()
        val id = CAPTCHA_ID_KEYS.firstNotNullOfOrNull { flat.values[it]?.takeIf(String::isNotBlank) }
            ?: return null
        val image = CAPTCHA_IMAGE_KEYS
            .asSequence()
            .mapNotNull { flat.values[it] }
            .filter { it != id }
            .map(::stripDataUriPrefix)
            // A captcha id is a 36-char UUID; anything that short can't be an image payload.
            .firstOrNull { it.length > MIN_IMAGE_LENGTH }
            ?: return null
        return CaptchaPayload(captchaId = id, imageBase64 = image)
    }

    /**
     * Classifies a login response. HTTP 200 covers both outcomes, so the decision walks the body:
     * an explicit failure flag first, then session material, then — failing both — an error field.
     * A body with neither is [LoginVerdict.Inconclusive].
     *
     * This verdict is advisory. A cookie-only success can legitimately answer with nothing useful,
     * so the caller settles the question by probing an authenticated endpoint and uses this only
     * to pick up a token and to choose the error copy. See
     * [me.june8th.euet.core.data.repository.AuthRepository.loginWithPassword].
     */
    fun parseLogin(json: JsonObject): LoginVerdict {
        val flat = json.flatten()
        val message = MESSAGE_KEYS.firstNotNullOfOrNull { flat.values[it]?.takeIf(String::isNotBlank) }
        // Length guard: a real token is long, so a short value under one of these keys ("ok",
        // "none") is a status word, not session material. Nested `data.token` is covered too,
        // because the flattening walks into envelopes.
        val token = TOKEN_KEYS.firstNotNullOfOrNull { flat.values[it]?.takeIf { v -> v.length >= MIN_TOKEN_LENGTH } }
        val explicitSuccess = listOf("success", "issuccess", "succeeded", "ok")
            .firstNotNullOfOrNull { flat.values[it] }
            ?.lowercase()

        if (isFailure(flat, explicitSuccess)) {
            return LoginVerdict.Rejected(message, isCaptchaProblem(flat, message))
        }
        if (token != null) return LoginVerdict.Accepted(token, studentCode(flat))
        if (explicitSuccess == "true") return LoginVerdict.Accepted(null, studentCode(flat))
        if (flat.keys.any { it in IDENTITY_KEYS }) return LoginVerdict.Accepted(null, studentCode(flat))
        if (message != null) return LoginVerdict.Rejected(message, isCaptchaProblem(flat, message))
        return LoginVerdict.Inconclusive
    }

    /** Removes a `data:image/png;base64,` style prefix and any whitespace a wrapped payload has. */
    fun stripDataUriPrefix(value: String): String {
        val trimmed = value.trim()
        val payload = if (trimmed.startsWith("data:", ignoreCase = true)) {
            trimmed.substringAfter(",", missingDelimiterValue = "")
        } else {
            trimmed
        }
        return payload.filterNot { it == '\n' || it == '\r' || it == ' ' }
    }

    /** True when the body flags a failure outright, regardless of what else it carries. */
    private fun isFailure(flat: FlatJson, explicitSuccess: String?): Boolean {
        if (explicitSuccess == "false") return true

        val statusText = flat.values["status"]?.lowercase()
        if (statusText != null && statusText in FAILURE_WORDS) return true

        val codes = listOf("status", "statuscode", "code", "errorcode", "httpstatus")
            .mapNotNull { flat.values[it]?.toIntOrNull() }
        // 0 is used as "no error" by some backends, so only real HTTP-ish codes count.
        return codes.any { it >= 100 && it !in 200..299 }
    }

    private fun isCaptchaProblem(flat: FlatJson, message: String?): Boolean {
        val haystack = listOfNotNull(
            message,
            flat.values["errorcode"],
            flat.values["code"],
            flat.values["field"],
        ).joinToString(" ").lowercase()
        return CAPTCHA_HINTS.any { it in haystack }
    }

    /**
     * The student code, when the response volunteers one. `code` is only trusted at student-code
     * length — short values there are status codes, not identifiers.
     */
    private fun studentCode(flat: FlatJson): String? =
        flat.values["studentcode"]?.takeIf(String::isNotBlank)
            ?: flat.values["studentid"]?.takeIf(String::isNotBlank)
            ?: flat.values["username"]?.takeIf(String::isNotBlank)
            ?: flat.values["code"]?.takeIf { it.length >= MIN_STUDENT_CODE_LENGTH }

    private const val MIN_IMAGE_LENGTH = 64
    private const val MIN_STUDENT_CODE_LENGTH = 6
    private const val MIN_TOKEN_LENGTH = 16
}

/**
 * A JSON object reduced to "leaf key → primitive value" plus the set of every key seen, so a
 * response can be inspected without knowing whether it is flat or wrapped in an envelope.
 */
internal data class FlatJson(
    /** Lower-cased leaf key → primitive content. The shallowest occurrence of a key wins. */
    val values: Map<String, String>,
    /** Every lower-cased key in the tree, including the object- and array-valued ones. */
    val keys: Set<String>,
)

/**
 * Flattens the object breadth-first (so top-level fields beat nested ones with the same name),
 * capping the depth so a pathological response can't cost anything.
 */
internal fun JsonObject.flatten(maxDepth: Int = 3): FlatJson {
    val values = LinkedHashMap<String, String>()
    val keys = LinkedHashSet<String>()
    var level = listOf(this)
    var depth = 0

    while (level.isNotEmpty() && depth < maxDepth) {
        val next = mutableListOf<JsonObject>()
        level.forEach { obj ->
            obj.forEach { (rawKey, element) ->
                val key = rawKey.lowercase()
                keys += key
                when (element) {
                    is JsonPrimitive ->
                        if (element.isString || element.content != "null") {
                            values.putIfAbsent(key, element.content)
                        }
                    is JsonObject -> next += element
                    is JsonArray -> next += element.filterIsInstance<JsonObject>()
                }
            }
        }
        level = next
        depth++
    }
    return FlatJson(values = values, keys = keys)
}
