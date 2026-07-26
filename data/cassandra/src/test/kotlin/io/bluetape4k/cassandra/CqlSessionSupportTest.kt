package io.bluetape4k.cassandra

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.closeSafe
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress

/**
 * [cqlSession] 및 [cqlSessionOf] DSL 팩토리 함수를 검증합니다.
 */
class CqlSessionSupportTest: AbstractCassandraTest() {

    companion object: KLoggingChannel()

    @Test
    fun `cqlSession DSL 로 세션을 생성한다`() {
        val cqlSession = cqlSession {
            addContactPoint(InetSocketAddress(cassandra4.host, cassandra4.port))
            withLocalDatacenter(CqlSessionProvider.DEFAULT_LOCAL_DATACENTER)
        }
        try {
            cqlSession.shouldNotBeNull()
            cqlSession.isClosed.shouldBeFalse()
        } finally {
            cqlSession.closeSafe()
        }
    }

    @Test
    fun `cqlSessionOf 로 keyspace 지정 세션을 생성한다`() {
        val cqlSession = cqlSessionOf(
            contactPoint = InetSocketAddress(cassandra4.host, cassandra4.port),
            localDatacenter = CqlSessionProvider.DEFAULT_LOCAL_DATACENTER,
            keyspaceName = DEFAULT_KEYSPACE,
        )
        try {
            cqlSession.shouldNotBeNull()
            cqlSession.isClosed.shouldBeFalse()
        } finally {
            cqlSession.closeSafe()
        }
    }
}
