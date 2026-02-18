package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Row
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.concurrent.failedCompletableFutureOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.test.assertFailsWith

class AsyncResultSetSupportUnitTest: AbstractCassandraTest() {

    companion object: KLoggingChannel()

    private val first: AsyncResultSet = mockk(relaxed = true)
    private val last: AsyncResultSet = mockk(relaxed = true)
    private val row1: Row = mockk(relaxed = true)
    private val row2: Row = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `should iterate first page`() = runTest {
        every { first.remaining() } returns 1
        every { first.currentPage() } returns listOf(row1)
        every { first.hasMorePages() } returns false

        val rows = first.asFlow().toList()
        rows shouldBeEqualTo listOf(row1)
    }

    @Test
    fun `should iterate more page`() = runTest {
        every { first.remaining() } returns 1
        every { first.currentPage() } returns listOf(row1)
        every { first.hasMorePages() } returns true
        every { first.fetchNextPage() } returns CompletableFuture.completedFuture(last)

        every { last.remaining() } returns 1
        every { last.currentPage() } returns listOf(row2)
        every { last.hasMorePages() } returns false

        val rows = first.asFlow().toList()
        rows shouldBeEqualTo listOf(row1, row2)
    }

    @Test
    fun `should propagate exception on iterate`() = runTest {
        every { first.remaining() } returns 1
        every { first.currentPage() } returns listOf(row1)

        val failed = failedCompletableFutureOf<AsyncResultSet>(RuntimeException("BOOM!"))
        every { first.hasMorePages() } returns true
        every { first.fetchNextPage() } returns failed

        assertFailsWith<RuntimeException> {
            first.asFlow().toList()
        }
    }
}
