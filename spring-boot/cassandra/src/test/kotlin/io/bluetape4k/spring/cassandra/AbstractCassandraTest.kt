package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.Version
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.storage.CassandraServer
import io.bluetape4k.testcontainers.storage.getCassandraReleaseVersion
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractCassandraTest {

    companion object: KLoggingChannel() {
        const val DEFAULT_KEYSPACE = "examples"
        val faker = Fakers.faker

        private val initializedKeyspaces = ConcurrentHashMap.newKeySet<String>()

        /**
         * 동일 테스트 JVM 안에서 지정한 keyspace를 한 번만 재생성합니다.
         *
         * 여러 Spring Cassandra 테스트 컨텍스트가 같은 keyspace를 공유하므로,
         * 최초 컨텍스트에서만 초기화하고 이후 컨텍스트는 기존 스키마를 재사용합니다.
         */
        fun recreateKeyspaceOnce(keyspace: String = DEFAULT_KEYSPACE) {
            keyspace.requireNotBlank("keyspace")

            if (initializedKeyspaces.add(keyspace)) {
                try {
                    CassandraServer.Launcher.recreateKeyspace(keyspace)
                } catch (e: Throwable) {
                    initializedKeyspaces.remove(keyspace)
                    throw e
                }
            } else {
                log.debug { "Keyspace already initialized. keyspace=[$keyspace]" }
            }
        }
    }

    @Autowired
    protected lateinit var session: CqlSession

    protected fun createKeyspace(keyspace: String) {
        CassandraServer.Launcher.createKeyspace(session, keyspace)
    }

    protected fun dropKeyspace(keyspace: String) {
        CassandraServer.Launcher.dropKeyspace(session, keyspace)
    }

    protected fun getCassandraVersion(session: CqlSession): Version? {
        return session.getCassandraReleaseVersion()
    }
}
