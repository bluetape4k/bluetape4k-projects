package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.CassandraServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
abstract class AbstractCassandraTest {

    companion object: KLoggingChannel() {
        protected const val DEFAULT_KEYSPACE = "examples"

        @JvmStatic
        val faker = Fakers.faker

        @JvmStatic
        val cassandra4 by lazy {
            CassandraServer.Launcher.cassandra4
        }
    }

    // protected val session by lazy { newCqlSession() }
    protected lateinit var session: CqlSession

    protected fun newCqlSession(keyspace: String = DEFAULT_KEYSPACE): CqlSession =
        CassandraServer.Launcher.getOrCreateSession(keyspace)

    protected fun newCqlSessionBuilder(): CqlSessionBuilder = CassandraServer.Launcher.newCqlSessionBuilder()

    @BeforeAll
    fun beforeAll() {
        session = newCqlSession()
    }

    @AfterAll
    fun afterAll() {
        session.close()
    }
}
