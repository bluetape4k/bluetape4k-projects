package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.cql.Statement
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.ReactiveSession
import reactor.core.publisher.Mono

class ReactiveSessionCoroutinesTest {

    companion object : KLoggingChannel()

    private val mockResultSet = mockk<ReactiveResultSet>()
    private val mockPreparedStatement = mockk<PreparedStatement>()
    private val mockSession = mockk<ReactiveSession>().also {
        every { it.execute(any<Statement<*>>()) } returns Mono.just(mockResultSet)
        every { it.prepare(any<String>()) } returns Mono.just(mockPreparedStatement)
        every { it.prepare(any<SimpleStatement>()) } returns Mono.just(mockPreparedStatement)
    }

    @Test
    fun `executeSuspending with CQL string`() = runSuspendIO {
        val result = mockSession.executeSuspending("SELECT 1")
        result.shouldNotBeNull()
    }

    @Test
    fun `executeSuspending with CQL string and positional args`() = runSuspendIO {
        val result = mockSession.executeSuspending("SELECT * FROM users WHERE id = ?", "user-1")
        result.shouldNotBeNull()
    }

    @Test
    fun `executeSuspending with CQL string and named map args`() = runSuspendIO {
        val result = mockSession.executeSuspending(
            "SELECT * FROM users WHERE id = :id",
            mapOf("id" to "user-1")
        )
        result.shouldNotBeNull()
    }

    @Test
    fun `executeSuspending with Statement`() = runSuspendIO {
        val statement = SimpleStatement.newInstance("SELECT 1")
        val result = mockSession.executeSuspending(statement)
        result.shouldNotBeNull()
    }

    @Test
    fun `prepareSuspending with CQL string`() = runSuspendIO {
        val result = mockSession.prepareSuspending("SELECT * FROM users WHERE id = ?")
        result.shouldNotBeNull()
    }

    @Test
    fun `prepareSuspending with SimpleStatement`() = runSuspendIO {
        val stmt = SimpleStatement.newInstance("SELECT * FROM users WHERE id = ?")
        val result = mockSession.prepareSuspending(stmt)
        result.shouldNotBeNull()
    }
}
