package me.june8th.euet.core.datastore

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File

/** A cached domain value plus the moment it was written, for optional staleness display. */
data class CachedValue<T>(
    val value: T,
    val savedAtMillis: Long,
)

/** On-disk envelope: the payload wrapped with its write timestamp. */
@Serializable
private data class Snapshot<T>(
    val savedAtMillis: Long,
    val payload: T,
)

/**
 * Offline snapshot store: one JSON file per key under `filesDir/snapshots/`, written with
 * kotlinx.serialization. This is the Android counterpart of the iOS app's SwiftData blob cache
 * (`CacheStore`) — same behaviour (cached screens render instantly, network refreshes overwrite),
 * different tech because no annotation processor (Room) is available in this toolchain.
 *
 * All I/O runs on [Dispatchers.IO]. Reads are corruption-safe: a file that fails to parse (bad
 * write, old schema) is deleted and reported as a miss, never an error.
 */
class SnapshotCache(
    context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {
    private val dir = File(context.filesDir, "snapshots")

    suspend fun <T> save(key: String, serializer: KSerializer<T>, value: T): Unit =
        withContext(Dispatchers.IO) {
            try {
                dir.mkdirs()
                val file = fileFor(key)
                // Write-then-rename so a crash mid-write never corrupts the previous snapshot.
                val tmp = File(dir, file.name + ".tmp")
                tmp.writeText(
                    json.encodeToString(
                        Snapshot.serializer(serializer),
                        Snapshot(System.currentTimeMillis(), value),
                    ),
                )
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            } catch (_: Exception) {
                // Best-effort: the screen already holds the live data this failed to persist.
            }
        }

    suspend fun <T> load(key: String, serializer: KSerializer<T>): CachedValue<T>? =
        withContext(Dispatchers.IO) {
            val file = fileFor(key)
            if (!file.exists()) return@withContext null
            try {
                val snapshot = json.decodeFromString(Snapshot.serializer(serializer), file.readText())
                CachedValue(snapshot.payload, snapshot.savedAtMillis)
            } catch (_: Exception) {
                file.delete() // Corrupt or written by an older schema — drop it and refetch.
                null
            }
        }

    /** Drops a single snapshot, e.g. when its provider is disconnected. */
    suspend fun remove(key: String): Unit = withContext(Dispatchers.IO) {
        fileFor(key).delete()
        Unit
    }

    /** Drops every snapshot. Called on sign-out so no data outlives the session. */
    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        Unit
    }

    private fun fileFor(key: String): File {
        val safe = key.map { if (it.isLetterOrDigit() || it == '.' || it == '-' || it == '_') it else '_' }
            .joinToString("")
        return File(dir, "$safe.json")
    }
}

suspend inline fun <reified T> SnapshotCache.save(key: String, value: T) =
    save(key, serializer(), value)

suspend inline fun <reified T> SnapshotCache.load(key: String): CachedValue<T>? =
    load(key, serializer())
