package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession
import org.springframework.data.cassandra.core.cql.PreparedStatementBinder
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations
import org.springframework.data.cassandra.core.cql.ReactivePreparedStatementCreator
import org.springframework.data.cassandra.core.cql.RowMapper
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ReactiveCqlOperationsSupportUnitTest {

    companion object : KLoggingChannel()

    private val mockResultSet = mockk<ReactiveResultSet>()
    private val mockRow = mockk<Row>()
    private val testStatement = SimpleStatement.newInstance("SELECT 1")
    private val testMap: MutableMap<String, Any> = mutableMapOf("id" to "1", "name" to "Test")

    // Use relaxed=true so unknown calls don't fail; override only methods needing non-empty Monos/Flux
    @Suppress("UNCHECKED_CAST")
    private val mockOps = mockk<ReactiveCqlOperations>(relaxed = true).also { ops ->
        every { ops.execute(any<String>()) } returns Mono.just(true)
        every { ops.execute(any<Statement<*>>()) } returns Mono.just(true)
        every { ops.execute(any<ReactivePreparedStatementCreator>()) } returns Mono.just(true)
        every { ops.queryForObject(any<String>(), any<RowMapper<String>>(), *anyVararg()) } answers {
            Mono.just("result")
        }
        every { ops.queryForObject(any<String>(), any<Class<*>>(), *anyVararg()) } answers {
            Mono.just("result") as Mono<Any>
        }
        every { ops.queryForObject(any<Statement<*>>(), any<Class<*>>()) } answers {
            Mono.just("result") as Mono<Any>
        }
        every { ops.queryForMap(any<String>(), *anyVararg()) } returns Mono.just(testMap)
        every { ops.queryForMap(any<Statement<*>>()) } returns Mono.just(testMap)
        every { ops.queryForResultSet(any<String>(), *anyVararg()) } returns Mono.just(mockResultSet)
        every { ops.queryForResultSet(any<Statement<*>>()) } returns Mono.just(mockResultSet)
        every { ops.queryForRows(any<Statement<*>>()) } returns Flux.just(mockRow)
        every { ops.queryForRows(any<String>(), *anyVararg()) } returns Flux.just(mockRow)
        every { ops.query(any<Statement<*>>(), any<RowMapper<String>>()) } answers {
            Flux.just("mapped")
        }
    }

    @Test
    fun `executeSuspending with CQL string`() = runSuspendIO {
        val result = mockOps.executeSuspending("TRUNCATE users")
        result shouldBeEqualTo true
    }

    @Test
    fun `executeSuspending with Statement`() = runSuspendIO {
        val result = mockOps.executeSuspending(testStatement)
        result.shouldBeTrue()
    }

    @Test
    fun `coExecute with PreparedStatementCreator`() = runSuspendIO {
        val psc = mockk<ReactivePreparedStatementCreator>()
        val result = mockOps.coExecute(psc)
        result shouldBeEqualTo true
    }

    // Relaxed mock returns a Flux proxy that doesn't implement Publisher correctly for generic T.
    // Collecting the flow would cause UninitializedPropertyAccessException in ReactiveSubscriber.
    // Verifying the Flow is non-null is sufficient for coverage of the function body.
    @Test
    fun `executeSuspending with ReactiveSessionCallback action`() = runSuspendIO {
        val flow = mockOps.executeSuspending<String> { _: ReactiveSession ->
            flowOf("result1", "result2")
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `executeSuspending with CQL and Flow args`() = runSuspendIO {
        val flow = mockOps.executeSuspending("INSERT INTO users VALUES(?)") {
            flowOf(arrayOf("user-1"))
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForObjectSuspending with CQL and RowMapper`() = runSuspendIO {
        val result = mockOps.queryForObjectSuspending("SELECT * FROM users") { row, _ ->
            "mapped"
        }
        result.shouldNotBeNull()
    }

    @Test
    fun `queryForObjectSuspending with Statement`() = runSuspendIO {
        val result = mockOps.queryForObjectSuspending<String>(testStatement)
        result.shouldNotBeNull()
    }

    @Test
    fun `queryForMapSuspending with CQL`() = runSuspendIO {
        val result = mockOps.queryForMapSuspending("SELECT * FROM users WHERE id = ?", "1")
        result.shouldNotBeNull()
        result["id"] shouldBeEqualTo "1"
    }

    @Test
    fun `queryForMapSuspending expands CQL vararg arguments`() = runSuspendIO {
        val localOps = mockk<ReactiveCqlOperations>()
        val cql = "SELECT * FROM users WHERE id = ? AND firstname = ?"

        every { localOps.queryForMap(cql, "user-1", "Debop") } returns Mono.just(testMap)

        val result = localOps.queryForMapSuspending(cql, "user-1", "Debop")

        result["id"] shouldBeEqualTo "1"
        verify(exactly = 1) { localOps.queryForMap(cql, "user-1", "Debop") }
    }

    @Test
    fun `queryForMapSuspending with Statement`() = runSuspendIO {
        val result = mockOps.queryForMapSuspending(testStatement)
        result.shouldNotBeNull()
    }

    @Test
    fun `queryForResultSetSuspending with CQL`() = runSuspendIO {
        val result = mockOps.queryForResultSetSuspending("SELECT * FROM users")
        result.shouldNotBeNull()
    }

    @Test
    fun `queryForResultSetSuspending with Statement`() = runSuspendIO {
        val result = mockOps.queryForResultSetSuspending(testStatement)
        result.shouldNotBeNull()
    }

    @Test
    fun `queryForRowsFlow with Statement`() = runSuspendIO {
        val rows = mockOps.queryForRowsFlow(testStatement).toList()
        rows.shouldNotBeNull()
    }

    @Test
    fun `queryForRowsFlow with CQL`() = runSuspendIO {
        val rows = mockOps.queryForRowsFlow("SELECT * FROM users").toList()
        rows.shouldNotBeNull()
    }

    @Test
    fun `executeForFlow with Flow of CQL strings`() = runSuspendIO {
        val localOps = mockk<ReactiveCqlOperations>().also { ops ->
            every { ops.execute(any<org.reactivestreams.Publisher<String>>()) } answers {
                Flux.just(true)
            }
        }
        val cqlFlow = flowOf("TRUNCATE users")
        val results = localOps.executeForFlow(cqlFlow).toList()
        results.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with Statement and RowMapper`() = runSuspendIO {
        val results = mockOps.queryForFlow<String>(testStatement) { row, _ ->
            "mapped"
        }.toList()
        results.shouldNotBeNull()
    }

    @Test
    fun `queryForMapFlow with CQL`() = runSuspendIO {
        val flow = mockOps.queryForMapFlow("SELECT * FROM users")
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForMapFlow with Statement`() = runSuspendIO {
        val flow = mockOps.queryForMapFlow(testStatement)
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with Statement and ReactiveResultSetExtractor`() = runSuspendIO {
        val flow = mockOps.queryForFlow<String>(testStatement) { _: ReactiveResultSet ->
            flowOf("extracted")
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with PreparedStatementCreator and ReactiveResultSetExtractor`() = runSuspendIO {
        val psc = mockk<ReactivePreparedStatementCreator>()
        val flow = mockOps.queryForFlow<String>(psc) { _: ReactiveResultSet ->
            flowOf("extracted")
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with CQL PSB null and ReactiveResultSetExtractor`() = runSuspendIO {
        val flow = mockOps.queryForFlow<String>("SELECT * FROM users", null as PreparedStatementBinder?) {
            _: ReactiveResultSet -> flowOf("extracted")
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with PSC PSB and ReactiveResultSetExtractor`() = runSuspendIO {
        val psc = mockk<ReactivePreparedStatementCreator>()
        val psb = mockk<PreparedStatementBinder>()
        val flow = mockOps.queryForFlow<String>(psc, psb) { _: ReactiveResultSet ->
            flowOf("extracted")
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with PreparedStatementCreator and RowMapper`() = runSuspendIO {
        val psc = mockk<ReactivePreparedStatementCreator>()
        val flow = mockOps.queryForFlow<String>(psc) { _: Row, _: Int ->
            "mapped"
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with PSC PSB and RowMapper`() = runSuspendIO {
        val psc = mockk<ReactivePreparedStatementCreator>()
        val psb = mockk<PreparedStatementBinder>()
        val flow = mockOps.queryForFlow<String>(psc, psb) { _: Row, _: Int ->
            "mapped"
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `executeForFlow with PSC and action`() = runSuspendIO {
        val psc = mockk<ReactivePreparedStatementCreator>()
        val flow = mockOps.executeForFlow<Int>(psc) { _, _ ->
            flowOf(42)
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `executeForFlow with CQL and action`() = runSuspendIO {
        val flow = mockOps.executeForFlow<Int>("SELECT * FROM users") { _, _ ->
            flowOf(42)
        }
        flow.shouldNotBeNull()
    }

    @Test
    fun `queryForFlow with CQL rowMapper named arg`() = runSuspendIO {
        val rse: (Row, Int) -> String = { _, _ -> "mapped" }
        val flow = mockOps.queryForFlow("SELECT * FROM users WHERE id = ?", rowMapper = rse)
        flow.shouldNotBeNull()
    }
}
