package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.Version
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.CassandraServer
import io.bluetape4k.testcontainers.storage.getCassandraReleaseVersion
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractCassandraTest {

    companion object: KLoggingChannel() {
        const val DEFAULT_KEYSPACE = "examples"
        val faker = Fakers.faker
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
