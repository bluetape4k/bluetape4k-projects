package io.bluetape4k.mongodb

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.mongodb.MongoClientProvider.DEFAULT_CONNECTION_STRING
import io.bluetape4k.support.closeSafe
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches and manages coroutine [MongoClient] instances by [MongoClientSettings].
 *
 * Calls that resolve to equal [MongoClientSettings] share the same provider-managed
 * [MongoClient]. Callers must not close returned clients directly because the same
 * cached instance may be used elsewhere. Use [close] or [closeAll] to remove and
 * close provider-managed entries. Registered clients are also closed from the JVM
 * shutdown queue.
 *
 * ```kotlin
 * val client = MongoClientProvider.getOrCreate("mongodb://localhost:27017")
 * MongoClientProvider.close("mongodb://localhost:27017")
 * ```
 */
object MongoClientProvider: KLogging() {

    /** Default MongoDB connection string. */
    const val DEFAULT_CONNECTION_STRING = "mongodb://localhost:27017"

    /** Default database name. */
    const val DEFAULT_DATABASE_NAME = "test"

    private val settingsClientCache = ConcurrentHashMap<MongoClientSettings, MongoClient>()

    /**
     * Returns a coroutine [MongoClient] for [connectionString].
     *
     * The connection string is first converted to [MongoClientSettings], then the
     * settings-based cache is used. The returned client is provider-managed and
     * must be closed through [close] or [closeAll].
     *
     * ```kotlin
     * val client1 = MongoClientProvider.getOrCreate("mongodb://localhost:27017")
     * val client2 = MongoClientProvider.getOrCreate("mongodb://localhost:27017")
     * // client1 === client2
     * ```
     *
     * @param connectionString MongoDB connection string. Defaults to [DEFAULT_CONNECTION_STRING].
     * @return cached or newly created coroutine [MongoClient]
     */
    fun getOrCreate(connectionString: String = DEFAULT_CONNECTION_STRING): MongoClient {
        return getOrCreate(mongoClientSettingsOf(connectionString))
    }

    /**
     * Returns a coroutine [MongoClient] for [connectionString] plus custom settings.
     *
     * The final [MongoClientSettings] produced after applying [builder] is the cache
     * key. Therefore the same URL can return different clients when timeout, TLS,
     * application name, credentials, or other effective settings differ.
     *
     * ```kotlin
     * val client = MongoClientProvider.getOrCreate("mongodb://localhost:27017") {
     *     applyToSocketSettings { it.connectTimeout(5, TimeUnit.SECONDS) }
     * }
     * ```
     *
     * @param connectionString MongoDB connection string. Defaults to [DEFAULT_CONNECTION_STRING].
     * @param builder additional [MongoClientSettings.Builder] configuration
     * @return cached or newly created coroutine [MongoClient]
     */
    fun getOrCreate(
        connectionString: String = DEFAULT_CONNECTION_STRING,
        builder: MongoClientSettings.Builder.() -> Unit,
    ): MongoClient {
        return getOrCreate(mongoClientSettingsOf(connectionString, builder))
    }

    /**
     * Returns a coroutine [MongoClient] for [settings].
     *
     * [MongoClientSettings] is used directly as the cache key. The returned client is
     * provider-managed and must be closed through [close] or [closeAll].
     *
     * ```kotlin
     * val settings = MongoClientSettings.builder()
     *     .applyConnectionString(ConnectionString("mongodb://localhost:27017"))
     *     .build()
     * val client = MongoClientProvider.getOrCreate(settings)
     * ```
     *
     * @param settings effective MongoDB client settings
     * @return cached or newly created coroutine [MongoClient]
     */
    fun getOrCreate(settings: MongoClientSettings): MongoClient {
        return settingsClientCache.computeIfAbsent(settings) {
            log.info { "Creating new MongoClient with MongoClientSettings" }
            MongoClient.create(settings).also {
                ShutdownQueue.register(it)
            }
        }
    }

    /**
     * Removes and closes the provider-managed [MongoClient] for [connectionString].
     *
     * @return `true` when a cached client was found and closed; otherwise `false`
     */
    fun close(connectionString: String = DEFAULT_CONNECTION_STRING): Boolean {
        return close(mongoClientSettingsOf(connectionString))
    }

    /**
     * Removes and closes the provider-managed [MongoClient] for [connectionString]
     * plus custom settings.
     *
     * This computes the same settings key as [getOrCreate], so it can explicitly
     * manage a shared client created by the builder overload.
     *
     * @return `true` when a cached client was found and closed; otherwise `false`
     */
    fun close(
        connectionString: String = DEFAULT_CONNECTION_STRING,
        builder: MongoClientSettings.Builder.() -> Unit,
    ): Boolean {
        return close(mongoClientSettingsOf(connectionString, builder))
    }

    /**
     * Removes and closes the provider-managed [MongoClient] for [settings].
     *
     * @return `true` when a cached client was found and closed; otherwise `false`
     */
    fun close(settings: MongoClientSettings): Boolean {
        return settingsClientCache.remove(settings)
            ?.also { it.closeSafe() }
            ?.let { true }
            ?: false
    }

    /**
     * Closes every provider-managed [MongoClient] and clears the cache.
     */
    fun closeAll() {
        settingsClientCache.forEach { (settings, client) ->
            if (settingsClientCache.remove(settings, client)) {
                client.closeSafe()
            }
        }
    }

    private fun mongoClientSettingsOf(
        connectionString: String,
        builder: MongoClientSettings.Builder.() -> Unit = {},
    ): MongoClientSettings {
        val url = connectionString.requireNotBlank("connectionString")

        return MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(url))
            .apply(builder)
            .build()
    }

}
