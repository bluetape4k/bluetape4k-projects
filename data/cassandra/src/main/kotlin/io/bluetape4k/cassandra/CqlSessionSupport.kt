package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import java.net.InetSocketAddress

/**
 * [CqlSession]을 생성합니다.
 *
 * ```
 * val session = cqlSession {
 *     addContactPoint(InetSocketAddress("localhost", 9042))
 *     withLocalDatacenter("datacenter1")
 *     withKeyspace("keyspace")
 *     withAuthCredentials("username", "password")
 * }
 * ```
 *
 * @param builder [CqlSessionBuilder] 초기화 람다
 */
inline fun cqlSession(
    @BuilderInference builder: CqlSessionBuilder.() -> Unit,
): CqlSession {
    return CqlSessionBuilder().apply(builder).build()
}

/**
 * [CqlSession]을 생성합니다.
 *
 * ```
 * val session = cqlSessionOf(
 *     contactPoint = InetSocketAddress("localhost", 9042),
 *     localDatacenter = "datacenter1",
 *     keyspaceName = "keyspace"
 * )
 * ```
 */
inline fun cqlSessionOf(
    contactPoint: InetSocketAddress = CqlSessionProvider.DEFAULT_CONTACT_POINT,
    localDatacenter: String = CqlSessionProvider.DEFAULT_LOCAL_DATACENTER,
    keyspaceName: String = CqlSessionProvider.DEFAULT_KEYSPACE,
    @BuilderInference builder: CqlSessionBuilder.() -> Unit = {},
): CqlSession = cqlSession {
    addContactPoint(contactPoint)
    withLocalDatacenter(localDatacenter)
    withKeyspace(keyspaceName)

    builder()
}
