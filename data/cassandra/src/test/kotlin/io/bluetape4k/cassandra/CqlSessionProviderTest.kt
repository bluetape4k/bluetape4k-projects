package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlSessionBuilder
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.closeSafe
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.*

class CqlSessionProviderTest: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private const val TEST_KEYSPACE_1 = "testkeyspace_1"
        private const val TEST_KEYSPACE_2 = "testkeyspace_2"
    }

    @Test
    fun `같은 connection context 의 CqlSession 을 재사용한다`() {
        val cqlSessionBuilderSupplier = {
            CqlSessionProvider.newCqlSessionBuilder(
                InetSocketAddress(cassandra4.host, cassandra4.port),
                CqlSessionProvider.DEFAULT_LOCAL_DATACENTER
            )
        }
        val clientId = UUID.randomUUID()
        val sessionIdentity = cqlSessionIdentityOf(
            keyspace = TEST_KEYSPACE_1,
            contextParts = listOf(
                "contactPoint=${cassandra4.host}:${cassandra4.port}",
                "localDatacenter=${CqlSessionProvider.DEFAULT_LOCAL_DATACENTER}",
                "applicationName=provider-test-reuse",
                "clientId=$clientId",
            ),
        )

        val session1 = CqlSessionProvider.getOrCreateSession(sessionIdentity, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-reuse")
            withClientId(clientId)
        }
        val session2 = CqlSessionProvider.getOrCreateSession(sessionIdentity, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-reuse")
            withClientId(clientId)
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

    @Test
    fun `keyspace bootstrap 에 caller builder 설정을 적용한다`() {
        val sessionIdentity = cqlSessionIdentityOf(
            keyspace = "provider_bootstrap_${UUID.randomUUID().toString().take(8)}",
            contextParts = listOf(
                "contactPoint=${cassandra4.host}:${cassandra4.port}",
                "localDatacenter=${CqlSessionProvider.DEFAULT_LOCAL_DATACENTER}",
                "applicationName=provider-test-bootstrap",
            ),
        )
        val bareBuilderSupplier = { CqlSessionBuilder() }

        val session = CqlSessionProvider.getOrCreateSession(sessionIdentity, bareBuilderSupplier) {
            addContactPoint(InetSocketAddress(cassandra4.host, cassandra4.port))
            withLocalDatacenter(CqlSessionProvider.DEFAULT_LOCAL_DATACENTER)
            withApplicationName("provider-test-bootstrap")
        }

        session.keyspace.orElseThrow().asInternal() shouldBeEqualTo sessionIdentity.keyspace

        session.closeSafe()
    }

    @Test
    fun `같은 keyspace 라도 다른 connection context 는 재사용하지 않는다`() {
        val cqlSessionBuilderSupplier = {
            CqlSessionProvider.newCqlSessionBuilder(
                InetSocketAddress(cassandra4.host, cassandra4.port),
                CqlSessionProvider.DEFAULT_LOCAL_DATACENTER
            )
        }

        val session1 = CqlSessionProvider.getOrCreateSession(TEST_KEYSPACE_1, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-context-1")
            withClientId(UUID.randomUUID())
        }
        val session2 = CqlSessionProvider.getOrCreateSession(TEST_KEYSPACE_1, cqlSessionBuilderSupplier) {
            withApplicationName("provider-test-context-2")
            withClientId(UUID.randomUUID())
        }

        session2 shouldNotBeEqualTo session1

        session1.closeSafe()
        session2.closeSafe()
    }

    @Test
    fun `blank keyspace 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            CqlSessionProvider.getOrCreateSession(" ") {
                withApplicationName("provider-test-invalid-keyspace")
            }
        }
    }

    @Test
    fun `blank localDatacenter 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            CqlSessionProvider.newCqlSessionBuilder(localDatacenter = " ")
        }
    }
}
