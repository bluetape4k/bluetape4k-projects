package io.bluetape4k.spring.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.cql.AsyncCqlOperations
import org.springframework.data.cassandra.core.cql.AsyncResultSetExtractor
import org.springframework.data.cassandra.core.cql.RowMapper

class AsyncCqlOperationsCoroutinesUnitTest {

    companion object: KLoggingChannel()

    private val testStatement = SimpleStatement.newInstance("SELECT 1")

    @Test
    fun `querySuspending with CQL string and ResultSet extractor`() = runSuspendIO {
        @Suppress("UNCHECKED_CAST")
        val localOps = mockk<AsyncCqlOperations>().also { ops ->
            every {
                ops.query(any<String>(), any<AsyncResultSetExtractor<String>>(), *anyVararg())
            } answers {
                completableFutureOf("extracted")
            }
        }
        val result = localOps.querySuspending<String>("SELECT 1") { _: AsyncResultSet ->
            completableFutureOf("extracted")
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
                completableFutureOf(mutableListOf("mapped"))
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
                ops.query(
                    any<com.datastax.oss.driver.api.core.cql.Statement<*>>(),
                    any<AsyncResultSetExtractor<String>>()
                )
            } answers {
                completableFutureOf("extracted")
            }
        }
        val result = localOps.querySuspending<String>(testStatement) { _: AsyncResultSet ->
            completableFutureOf("extracted")
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
                completableFutureOf(mutableListOf("mapped"))
            }
        }
        val result = localOps.querySuspending<String>(testStatement) { _: Row, _: Int ->
            "mapped"
        }
        result.shouldNotBeNull()
    }
}
