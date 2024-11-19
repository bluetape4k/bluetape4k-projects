package io.bluetape4k.cassandra

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.closeSafe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.*

class CqlSessionProviderTest: AbstractCassandraTest() {

    companion object: KLogging() {
        private const val TEST_KEYSPACE_1 = "testkeyspace_1"
        private const val TEST_KEYSPACE_2 = "testkeyspace_2"
    }

    @Test
    fun `새로운 CqlSession 을 생성하고 캐싱한다`() {
        val cqlSessionBuilderSupplier = {
            CqlSessionProvider.newCqlSessionBuilder(
                InetSocketAddress(cassandra4.host, cassandra4.port),
                CqlSessionProvider.DEFAULT_LOCAL_DATACENTER
            )
        }

        val session1 = CqlSessionProvider.getOrCreateSession(TEST_KEYSPACE_1, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-1")
            withClientId(UUID.randomUUID())
        }
        val session2 = CqlSessionProvider.getOrCreateSession(TEST_KEYSPACE_1, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-2")
            withClientId(UUID.randomUUID())
        }

        session2 shouldBeEqualTo session1

        val session3 = CqlSessionProvider.getOrCreateSession(TEST_KEYSPACE_2, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-3")
            withClientId(UUID.randomUUID())
        }

        session3 shouldNotBeEqualTo session1

        session1.closeSafe()
        session2.closeSafe()
        session3.closeSafe()
    }
}
