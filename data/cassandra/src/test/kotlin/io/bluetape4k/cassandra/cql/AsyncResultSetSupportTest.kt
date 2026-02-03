package io.bluetape4k.cassandra.cql

import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.concurrent.sequence
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger

class AsyncResultSetSupportTest: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private const val SIZE = 6000
    }

    @BeforeAll
    fun setup() {
        runSuspendIO {
            session.suspendExecute("DROP TABLE IF EXISTS bulks")
            session.suspendExecute("CREATE TABLE IF NOT EXISTS bulks (id text PRIMARY KEY, name text);")
            session.suspendExecute("TRUNCATE bulks")

            val ps = session.suspendPrepare("INSERT INTO bulks(id, name) VALUES(?, ?)")
            val futures = fastList(SIZE) {
                val id = it.toString()
                val name = faker.credentials().username()

                session.executeAsync(ps.bind(id, name))
            }
            futures.sequence().await()
        }
    }

    @Test
    fun `load as flow`() = runSuspendIO {
        log.debug { "Load all bulks" }
        val counter = AtomicInteger(0)

        val flow = session.suspendExecute("SELECT * FROM bulks").asFlow()
        flow
            .buffer()
            .onEach { row ->
                counter.incrementAndGet()
                val id = row.getStringOrEmpty(0)
                val name = row.getStringOrEmpty(1)

                id.shouldNotBeEmpty()
                name.shouldNotBeEmpty()
            }
            .collect()

        log.debug { "Loaded record count=${counter.get()}" }
        counter.get() shouldBeEqualTo SIZE
    }

    data class Bulk(val id: String, val name: String): Serializable

    @Test
    fun `load as flow with row mapper`() = runSuspendIO {
        log.debug { "Load all bulks" }
        val counter = AtomicInteger(0)

        val flow = session
            .suspendExecute("SELECT * FROM bulks")
            .asFlow { row -> Bulk(row.getStringOrEmpty(0), row.getStringOrEmpty(1)) }

        flow.buffer()
            .onEach { bulk ->
                counter.incrementAndGet()
                bulk.id.shouldNotBeEmpty()
                bulk.name.shouldNotBeEmpty()
            }
            .collect()

        log.debug { "Loaded record count=${counter.get()}" }
        counter.get() shouldBeEqualTo SIZE
    }
}
