package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.cql.AsyncCqlOperations
import org.springframework.data.cassandra.core.cql.AsyncResultSetExtractor
import org.springframework.data.cassandra.core.cql.RowMapper
import java.util.concurrent.CompletableFuture

class AsyncCqlOperationsCoroutinesUnitTest {

    companion object : KLoggingChannel()

    private val testStatement = SimpleStatement.newInstance("SELECT 1")

    @Test
    fun `querySuspending with CQL string and ResultSet extractor`() = runSuspendIO {
        @Suppress("UNCHECKED_CAST")
        val localOps = mockk<AsyncCqlOperations>().also { ops ->
            every {
                ops.query(any<String>(), any<AsyncResultSetExtractor<String>>(), *anyVararg())
            } answers {
                CompletableFuture.completedFuture("extracted") as CompletableFuture<String>
            }
        }
        val result = localOps.querySuspending<String>("SELECT 1") { _: AsyncResultSet ->
            CompletableFuture.completedFuture("extracted")
        }
        result.shouldNotBeNull()
    }

    @Test
    fun `querySuspending with CQL string and RowMapper`() = runSuspendIO {
        @Suppress("UNCHECKED_CAST")
        val localOps = mockk<AsyncCqlOperations>().also { ops ->
            every {
                ops.query(any<String>(), any<RowMapper<String>>(), *anyVararg())
            } answers {
                CompletableFuture.completedFuture(mutableListOf("mapped")) as CompletableFuture<MutableList<String>>
            }
        }
        val result = localOps.querySuspending<String>("SELECT 1") { _: Row, _: Int ->
            "mapped"
        }
        result.shouldNotBeNull()
    }

    @Test
    fun `querySuspending with Statement and ResultSet extractor`() = runSuspendIO {
        @Suppress("UNCHECKED_CAST")
        val localOps = mockk<AsyncCqlOperations>().also { ops ->
            every {
                ops.query(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<AsyncResultSetExtractor<String>>())
            } answers {
                CompletableFuture.completedFuture("extracted") as CompletableFuture<String>
            }
        }
        val result = localOps.querySuspending<String>(testStatement) { _: AsyncResultSet ->
            CompletableFuture.completedFuture("extracted")
        }
        result.shouldNotBeNull()
    }

    @Test
    fun `querySuspending with Statement and RowMapper`() = runSuspendIO {
        @Suppress("UNCHECKED_CAST")
        val localOps = mockk<AsyncCqlOperations>().also { ops ->
            every {
                ops.query(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<RowMapper<String>>())
            } answers {
                CompletableFuture.completedFuture(mutableListOf("mapped")) as CompletableFuture<MutableList<String>>
            }
        }
        val result = localOps.querySuspending<String>(testStatement) { _: Row, _: Int ->
            "mapped"
        }
        result.shouldNotBeNull()
    }
}
