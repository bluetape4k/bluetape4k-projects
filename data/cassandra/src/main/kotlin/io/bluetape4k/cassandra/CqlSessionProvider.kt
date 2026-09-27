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
 * Cassandra session을 안정적으로 캐시하기 위한 identity입니다.
 *
 * 하나의 keyspace가 여러 contact point, datacenter, credential, client, tenant context를 통해
 * 접근될 수 있을 때 이 identity로 session cache 경계를 명시합니다.
 *
 * @property keyspace session이 bootstrap하고 bind할 Cassandra keyspace 이름입니다.
 * @property context 같은 keyspace 안에서 접속 경계를 구분하는 정규화된 context 값입니다.
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
         * 정규화된 context 조각으로 결정적 identity를 만듭니다.
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
 * 정규화된 context 조각으로 결정적 Cassandra session cache identity를 만듭니다.
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
     * 지정한 contact point와 local datacenter로 새 [CqlSessionBuilder]를 생성합니다.
     *
     * ```
     * val builder = CqlSessionProvider.newCqlSessionBuilder(InetSocketAddress("localhost", 9042), "datacenter1")
     * ```
     *
     * @param contactPoint Cassandra server 주소입니다.
     * @param localDatacenter local datacenter 이름입니다.
     * @return 기본 접속 정보가 적용된 [CqlSessionBuilder]입니다.
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
     * 해석된 connection identity에 해당하는 [CqlSession]을 생성하거나 재사용합니다.
     *
     * 호환성 overload는 더 이상 keyspace만으로 캐시하지 않습니다. 서로 다른 builder block 사이에서 session이
     * 조용히 재사용되는 일을 피하기 위해 keyspace, builder supplier, builder lambda에서 보수적인 호출별
     * identity를 만듭니다. 같은 context의 재사용이 call site 전반에서 안정적으로 유지되어야 할 때는
     * [CqlSessionIdentity] overload를 사용합니다.
     *
     * ```
     * val session = CqlSessionProvider.getOrCreateSession("keyspace") {
     *   withLocalDatacenter("datacenter1")
     *   withAuthCredentials("username", "password")
     * }
     * ```
     *
     * [builder]는 bootstrap admin session과 최종 keyspace-bound session에 모두 적용되므로,
     * keyspace를 만들 때 보안 또는 사용자 지정 driver option이 유지됩니다. [builder] 안에서 keyspace를
     * 설정하지 마십시오. 이 provider는 bootstrap 이후에만 [keyspace]를 bind합니다. 최종 session에
     * keyspace별 추가 설정이 필요하면 명시적 `bootstrapBuilder`와 `sessionBuilder`를 받는 overload를 사용합니다.
     *
     * @param keyspace bootstrap하고 bind할 keyspace 이름입니다.
     * @param builderSupplier 새 [CqlSessionBuilder]를 생성하는 supplier입니다.
     * @param builder bootstrap과 최종 session에 공통 적용할 builder customization입니다.
     * @return [keyspace]에 bind된 cached [CqlSession]입니다.
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
     * 호출자가 제공한 cache [identity]로 [CqlSession]을 생성하거나 재사용합니다.
     *
     * builder를 [CqlSessionProvider] 밖에서 만들거나, caller가 tenant, credential, routing 상태를
     * cache 경계에 추가로 포함해야 할 때 이 overload를 사용합니다.
     *
     * [builder]는 bootstrap admin session과 최종 keyspace-bound session에 모두 적용됩니다.
     * [builder] 안에서 keyspace를 설정하지 마십시오. 이 provider는 bootstrap 이후에만 [identity.keyspace]를
     * bind합니다. 최종 session에 keyspace별 추가 설정이 필요하면 명시적 `bootstrapBuilder`와
     * `sessionBuilder`를 받는 overload를 사용합니다.
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
     * bootstrap과 최종 session 설정을 분리해 [CqlSession]을 생성하거나 재사용합니다.
     *
     * [identity.keyspace]를 만들기 위해 필요한 connection, credential, TLS, driver, application 설정은
     * [bootstrapBuilder]에 둡니다. 같은 공유 option과 keyspace가 존재한 뒤에만 유효한 설정은
     * [sessionBuilder]에 둡니다.
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
     * bootstrap과 최종 session 설정을 분리해 [CqlSession]을 생성하거나 재사용합니다.
     *
     * 이 overload는 [keyspace], [builderSupplier], [sessionBuilder]에서 보수적인 cache identity를 만듭니다.
     * call site 전반에서 안정적인 cache 재사용이 필요하면 [CqlSessionIdentity] overload를 우선 사용합니다.
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
