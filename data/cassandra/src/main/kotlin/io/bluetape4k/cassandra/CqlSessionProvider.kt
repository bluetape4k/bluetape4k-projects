package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import java.io.Serializable
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * Stable cache identity for a Cassandra session.
 *
 * Use this identity to make the session cache boundary explicit when a keyspace can be reached
 * through multiple contact points, datacenters, credentials, clients, or tenant contexts.
 */
data class CqlSessionIdentity(
    val keyspace: String,
    val context: String,
): Serializable {
    init {
        keyspace.requireNotBlank("keyspace")
        context.requireNotBlank("context")
    }

    companion object {
        private const val serialVersionUID = 1L

        /**
         * Builds a deterministic identity from normalized context parts.
         */
        @Deprecated(
            message = "Use the Kotlin package function cqlSessionIdentityOf().",
            replaceWith = ReplaceWith("cqlSessionIdentityOf(keyspace, contextParts)")
        )
        fun of(
            keyspace: String,
            contextParts: Iterable<String>,
        ): CqlSessionIdentity = cqlSessionIdentityOf(keyspace, contextParts)
    }
}

/**
 * Builds a deterministic Cassandra session cache identity from normalized context parts.
 */
fun cqlSessionIdentityOf(
    keyspace: String,
    contextParts: Iterable<String>,
): CqlSessionIdentity {
    keyspace.requireNotBlank("keyspace")
    val context = contextParts
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .sorted()
        .joinToString(separator = "|")
        .ifBlank { "default" }

    return CqlSessionIdentity(keyspace, context)
}

object CqlSessionProvider: KLogging() {

    private val sessionCache = ConcurrentHashMap<CqlSessionIdentity, CqlSession>()

    val DEFAULT_CONTACT_POINT = InetSocketAddress("localhost", 9042)
    const val DEFAULT_LOCAL_DATACENTER = "datacenter1"
    const val DEFAULT_KEYSPACE = "general"

    /**
     * Creates a new [CqlSessionBuilder] with the provided contact point and local datacenter.
     *
     * ```
     * val builder = CqlSessionProvider.newCqlSessionBuilder(InetSocketAddress("localhost", 9042), "datacenter1")
     * ```
     *
     * @param contactPoint Cassandra server address.
     * @param localDatacenter Local datacenter name.
     * @return A configured [CqlSessionBuilder].
     */
    fun newCqlSessionBuilder(
        contactPoint: InetSocketAddress = DEFAULT_CONTACT_POINT,
        localDatacenter: String = DEFAULT_LOCAL_DATACENTER,
    ): CqlSessionBuilder {
        localDatacenter.requireNotBlank("localDatacenter")
        return CqlSessionBuilder()
            .addContactPoint(contactPoint)
            .withLocalDatacenter(localDatacenter)
    }

    /**
     * Creates or reuses a [CqlSession] for the resolved connection identity.
     *
     * The compatibility overload no longer caches by keyspace alone. It derives a conservative
     * per-call identity from the keyspace, builder supplier, and builder lambda to avoid silently
     * reusing a session across different builder blocks. Use the [CqlSessionIdentity] overload when
     * same-context reuse must remain stable across call sites.
     *
     * ```
     * val session = CqlSessionProvider.getOrCreateSession("keyspace") {
     *   withLocalDatacenter("datacenter1")
     *   withAuthCredentials("username", "password")
     * }
     * ```
     *
     * [builder] is applied to both the bootstrap admin session and the final keyspace-bound
     * session, so secured or custom driver options are honored while creating the keyspace.
     * Do not set the keyspace inside [builder]; this provider binds [keyspace] only after
     * bootstrap. Use the overload with explicit `bootstrapBuilder` and `sessionBuilder` when
     * the final session needs additional keyspace-specific settings.
     *
     * @param keyspace Keyspace name to bootstrap and bind.
     * @param builderSupplier Supplier that creates a fresh [CqlSessionBuilder].
     * @param builder Shared builder customization for bootstrap and final sessions.
     * @return A cached [CqlSession] bound to [keyspace].
     */
    fun getOrCreateSession(
        keyspace: String = DEFAULT_KEYSPACE,
        builderSupplier: () -> CqlSessionBuilder = { newCqlSessionBuilder() },
        builder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        keyspace.requireNotBlank("keyspace")

        val sessionIdentity = toSessionIdentity(keyspace, builderSupplier, builder)

        return resolveSession(
            identity = sessionIdentity,
            builderSupplier = builderSupplier,
            bootstrapBuilder = builder,
            sessionBuilder = builder,
        )
    }

    /**
     * Creates or reuses a [CqlSession] with a caller-provided cache [identity].
     *
     * Use this overload when the builder is created outside [CqlSessionProvider] or when the caller
     * needs to include additional tenant, credential, or routing state in the cache boundary.
     *
     * [builder] is applied to both the bootstrap admin session and the final keyspace-bound session.
     * Do not set the keyspace inside [builder]; this provider binds [identity.keyspace] only after
     * bootstrap. Use the overload with explicit `bootstrapBuilder` and `sessionBuilder` when the
     * final session needs additional keyspace-specific settings.
     */
    fun getOrCreateSession(
        identity: CqlSessionIdentity,
        builderSupplier: () -> CqlSessionBuilder = { newCqlSessionBuilder() },
        builder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        return resolveSession(
            identity = identity,
            builderSupplier = builderSupplier,
            bootstrapBuilder = builder,
            sessionBuilder = builder,
        )
    }

    /**
     * Creates or reuses a [CqlSession] with separate bootstrap and final-session configuration.
     *
     * Use [bootstrapBuilder] for connection, credential, TLS, driver, and application settings
     * required to create [identity.keyspace]. Use [sessionBuilder] for the same shared options plus
     * any settings that are valid only after the keyspace exists.
     */
    fun getOrCreateSession(
        identity: CqlSessionIdentity,
        builderSupplier: () -> CqlSessionBuilder = { newCqlSessionBuilder() },
        bootstrapBuilder: CqlSessionBuilder.() -> Unit,
        sessionBuilder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        return resolveSession(
            identity = identity,
            builderSupplier = builderSupplier,
            bootstrapBuilder = bootstrapBuilder,
            sessionBuilder = sessionBuilder,
        )
    }

    /**
     * Creates or reuses a [CqlSession] with separate bootstrap and final-session configuration.
     *
     * This overload derives a conservative cache identity from [keyspace], [builderSupplier], and
     * [sessionBuilder]. Prefer the [CqlSessionIdentity] overload when stable cache reuse across call
     * sites is required.
     */
    fun getOrCreateSession(
        keyspace: String = DEFAULT_KEYSPACE,
        builderSupplier: () -> CqlSessionBuilder = { newCqlSessionBuilder() },
        bootstrapBuilder: CqlSessionBuilder.() -> Unit,
        sessionBuilder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        keyspace.requireNotBlank("keyspace")

        val sessionIdentity = toSessionIdentity(keyspace, builderSupplier, sessionBuilder)

        return resolveSession(
            identity = sessionIdentity,
            builderSupplier = builderSupplier,
            bootstrapBuilder = bootstrapBuilder,
            sessionBuilder = sessionBuilder,
        )
    }

    private fun resolveSession(
        identity: CqlSessionIdentity,
        builderSupplier: () -> CqlSessionBuilder,
        bootstrapBuilder: CqlSessionBuilder.() -> Unit,
        sessionBuilder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        // Cache may still contain closed sessions from previous runs.
        // Drop them before resolving the requested session identity.
        val closedSessions = sessionCache.filterValues { it.isClosed }
        closedSessions.forEach {
            sessionCache.remove(it.key)
        }

        return sessionCache.computeIfAbsent(identity) {
            log.info { "Creating new CqlSession for ${identity.keyspace} (${identity.context})" }

            // The keyspace may not exist yet, so bootstrap with an admin session before binding it.
            builderSupplier()
                .apply(bootstrapBuilder)
                .build()
                .use { adminSession ->
                    CassandraAdmin.createKeyspace(adminSession, identity.keyspace)
                }

            builderSupplier()
                .apply(sessionBuilder)
                .withKeyspace(identity.keyspace)
                .build()
                .also {
                    ShutdownQueue.register(it)
                }
        }
    }

}

private fun toSessionIdentity(
    keyspace: String,
    builderSupplier: () -> CqlSessionBuilder,
    builder: CqlSessionBuilder.() -> Unit,
): CqlSessionIdentity {
    return cqlSessionIdentityOf(
        keyspace = keyspace,
        contextParts = listOf(
            "builderSupplier=${builderSupplier.cachePart()}",
            "builder=${builder.cachePart()}",
        ),
    )
}

private fun Any.cachePart(): String {
    return "${javaClass.name}@${System.identityHashCode(this)}"
}
