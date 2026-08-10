package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncCqlSessionSupportTest: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private val initialized = atomic(false)
    }

    @BeforeEach
    fun setup() {
        runSuspendIO {
            if (initialized.compareAndSet(expect = false, update = true)) {
                session.executeSuspending("DROP TABLE IF EXISTS user")
                session.executeSuspending("CREATE TABLE IF NOT EXISTS user (id text PRIMARY KEY, username text);")
            }

            session.executeSuspending("TRUNCATE user")
            session.executeSuspending("INSERT INTO user (id, username) VALUES ('WHITE', 'Walter')")
        }
    }

    @Test
    fun `execute by cql in coroutines`() = runSuspendIO {
        session.executeSuspending("SELECT * FROM user").one().shouldNotBeNull()

        session.executeSuspending("DELETE FROM user WHERE id = 'WHITE'").wasApplied().shouldBeTrue()
        session.executeSuspending("SELECT * FROM user").one().shouldBeNull()
    }

    @Test
    fun `execute by cql with params in coroutines`() = runSuspendIO {
        session.executeSuspending("SELECT * FROM user").one().shouldNotBeNull()

        session.executeSuspending("DELETE FROM user WHERE id = ?", "WHITE").wasApplied().shouldBeTrue()
        session.executeSuspending("SELECT * FROM user").one().shouldBeNull()
    }

    @Test
    fun `execute by cql with named params in coroutines`() = runSuspendIO {
        session.executeSuspending("SELECT * FROM user").one().shouldNotBeNull()

        session.executeSuspending("DELETE FROM user WHERE id = :id", mapOf("id" to "WHITE")).wasApplied().shouldBeTrue()
        session.executeSuspending("SELECT * FROM user").one().shouldBeNull()
    }


    @Test
    fun `execute by statement in coroutines`() = runSuspendIO {
        session.executeSuspending(statementOf("SELECT * FROM user")).one().shouldNotBeNull()

        session.executeSuspending(statementOf("DELETE FROM user WHERE id = 'WHITE'")).wasApplied().shouldBeTrue()
        session.executeSuspending(statementOf("SELECT * FROM user")).one().shouldBeNull()
    }

    @Test
    @Suppress("DEPRECATION")
    fun `deprecated async session aliases preserve coroutine execution`() = runSuspendIO {
        val positionalQuery = "SELECT * FROM user WHERE id = ?"
        val namedQuery = "SELECT * FROM user WHERE id = :id"
        val statement = SimpleStatement.newInstance("SELECT * FROM user")

        session.suspendExecute(positionalQuery, "WHITE").one().shouldNotBeNull()
        session.suspendExecute(namedQuery, mapOf("id" to "WHITE")).one().shouldNotBeNull()
        session.suspendExecute(statement).one().shouldNotBeNull()

        session.execute(positionalQuery, "WHITE").one().shouldNotBeNull()
        session.execute(namedQuery, mapOf("id" to "WHITE")).one().shouldNotBeNull()
        session.execute(statement).one().shouldNotBeNull()

        session.suspendPrepare("SELECT * FROM user")
        session.suspendPrepare(statement)
        session.prepare("SELECT * FROM user")
        session.prepare(statement)
        session.prepareSuspending(statement)
    }
}
