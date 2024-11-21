package io.bluetape4k.examples.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.Version
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.CassandraServer
import io.bluetape4k.testcontainers.storage.getCassandraReleaseVersion
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractCassandraTest {

    companion object: KLogging() {
        const val DEFAULT_KEYSPACE = "examples"

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString() =
            Fakers.randomString(1024, 2048)
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
