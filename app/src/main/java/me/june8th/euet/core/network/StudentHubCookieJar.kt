package me.june8th.euet.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Cookie store for the StudentHub client.
 *
 * StudentHub authenticates the SPA with a session cookie, not a bearer token: every authenticated
 * XHR in the captured traffic carries no `Authorization` header at all. So the cookies *are* the
 * session and have to outlive the process, otherwise the app would come back from a restart
 * thinking it is signed in while every request 401s.
 *
 * Renewing the session silently isn't an option here — `api/auth/login` requires a captcha the
 * user has to read — so the cookies are persisted (encrypted, via [load]/[persist], which
 * [me.june8th.euet.core.datastore.SessionManager] backs with AES-GCM encrypted DataStore values)
 * instead of being kept in memory only, the way the daotao client can afford to.
 *
 * Cookies are serialized in `Set-Cookie` form and re-parsed against [url] on restore, so host-only
 * cookies come back bound to the StudentHub host. This jar belongs to a single client and is never
 * shared with the daotao or Canvas clients.
 */
class StudentHubCookieJar(
    private val url: HttpUrl,
    private val load: suspend () -> List<String>,
    private val persist: suspend (List<String>) -> Unit,
) : CookieJar {

    /** Keyed by domain|path|name, the tuple that identifies a cookie per RFC 6265. */
    private val cookies = LinkedHashMap<String, Cookie>()
    private var restored = false

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        restore()
        var changed = false
        cookies.forEach { cookie ->
            val previous = this.cookies.put(key(cookie), cookie)
            if (previous?.value != cookie.value) changed = true
        }
        // Only touch DataStore when the set actually moved; most responses re-send what we hold.
        if (changed) runBlocking { persist(serialize()) }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        restore()
        val now = System.currentTimeMillis()
        return cookies.values.filter { it.matches(url) && it.expiresAt > now }
    }

    /** True when the jar holds at least one live cookie for the StudentHub host. */
    @Synchronized
    fun hasCookies(): Boolean = loadForRequest(url).isNotEmpty()

    /** Drops every cookie, in memory and on disk. Used when the session is signed out. */
    @Synchronized
    fun clear() {
        restore()
        cookies.clear()
        runBlocking { persist(emptyList()) }
    }

    /** Reads the persisted cookies once per process, on the first jar access. */
    private fun restore() {
        if (restored) return
        restored = true
        val stored = runCatching { runBlocking { load() } }.getOrDefault(emptyList())
        stored.forEach { setCookie ->
            Cookie.parse(url, setCookie)?.let { cookies[key(it)] = it }
        }
    }

    private fun serialize(): List<String> = cookies.values.map { it.toString() }

    private fun key(cookie: Cookie): String = "${cookie.domain}|${cookie.path}|${cookie.name}"
}
