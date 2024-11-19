package io.bluetape4k.cassandra.cql

import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncCqlSessionSupportTest: AbstractCassandraTest() {

    companion object: KLogging() {
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
}
