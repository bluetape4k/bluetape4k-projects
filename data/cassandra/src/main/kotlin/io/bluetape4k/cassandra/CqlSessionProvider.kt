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
        fun of(
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
    }
}

object CqlSessionProvider: KLogging() {

    private val sessionCache = ConcurrentHashMap<CqlSessionIdentity, CqlSession>()

    val DEFAULT_CONTACT_POINT = InetSocketAddress("localhost", 9042)
    const val DEFAULT_LOCAL_DATACENTER = "datacenter1"
    const val DEFAULT_KEYSPACE = "general"

    /**
     * 새로운 [CqlSessionBuilder] 를 생성합니다.
     *
     * ```
     * val builder = CqlSessionProvider.newCqlSessionBuilder(InetSocketAddress("localhost", 9042), "datacenter1")
     * ```
     *
     * @param contactPoint    Cassandra 서버 주소
     * @param localDatacenter LocalDataCenter 이름
     * @return [CqlSessionBuilder] 인스턴스
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
     *   withKeyspace("keyspace")
     *   withAuthCredentials("username", "password")
     * }
     * ```
     *
     * @param keyspace  keyspace 명, null 이면 cql 에 keyspace 를 지정해주어야 합니다.
     * @param builderSupplier [CqlSessionBuilder]를 제공하는 Supplier
     * @param builder [CqlSessionBuilder]를 이용하여 설정하는 함수
     * @return `keyspace` 전용의 [CqlSession] 인스턴스
     */
    fun getOrCreateSession(
        keyspace: String = DEFAULT_KEYSPACE,
        builderSupplier: () -> CqlSessionBuilder = { newCqlSessionBuilder() },
        builder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        keyspace.requireNotBlank("keyspace")

        val sessionIdentity = toSessionIdentity(keyspace, builderSupplier, builder)

        return resolveSession(sessionIdentity, builderSupplier, builder)
    }

    /**
     * Creates or reuses a [CqlSession] with a caller-provided cache [identity].
     *
     * Use this overload when the builder is created outside [CqlSessionProvider] or when the caller
     * needs to include additional tenant, credential, or routing state in the cache boundary.
     */
    fun getOrCreateSession(
        identity: CqlSessionIdentity,
        builderSupplier: () -> CqlSessionBuilder = { newCqlSessionBuilder() },
        builder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        return resolveSession(identity, builderSupplier, builder)
    }

    private fun resolveSession(
        identity: CqlSessionIdentity,
        builderSupplier: () -> CqlSessionBuilder,
        builder: CqlSessionBuilder.() -> Unit,
    ): CqlSession {
        // Cache may still contain closed sessions from previous runs.
        // Drop them before resolving the requested session identity.
        val closedSessions = sessionCache.filterValues { it.isClosed }
        closedSessions.forEach {
            sessionCache.remove(it.key)
        }

        return sessionCache.computeIfAbsent(identity) {
            log.info { "Creating new CqlSession for ${identity.keyspace} (${identity.context})" }

            // keyspace가 없을 수 있으므로, adminSession으로 신규 keyspace를 생성하도록 합니다.
            builderSupplier().build().use { adminSession ->
                CassandraAdmin.createKeyspace(adminSession, identity.keyspace)
            }

            builderSupplier()
                .withKeyspace(identity.keyspace)
                .apply(builder)
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
    return CqlSessionIdentity.of(
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
