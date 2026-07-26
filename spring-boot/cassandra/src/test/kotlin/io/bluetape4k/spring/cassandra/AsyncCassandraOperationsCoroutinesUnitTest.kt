package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.AsyncCassandraOperations
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import java.io.Serializable
import java.util.concurrent.CompletableFuture

class AsyncCassandraOperationsCoroutinesUnitTest {

    companion object: KLoggingChannel()

    data class TestEntity(val id: String = "id-1", val name: String = "Test"): Serializable

    private val testEntity = TestEntity()
    private val testSlice: Slice<TestEntity> = SliceImpl(listOf(testEntity))
    private val mockAsyncResultSet = mockk<AsyncResultSet>()
    private val mockWriteResult = mockk<WriteResult>()
    private val mockEntityWriteResult = mockk<EntityWriteResult<TestEntity>>()
    private val testStatement = SimpleStatement.newInstance("SELECT 1")

    @Suppress("UNCHECKED_CAST")
    private val mockOps = mockk<AsyncCassandraOperations>().also { ops ->
        // execute
        every { ops.execute(any<com.datastax.oss.driver.api.core.cql.Statement<*>>()) } returns
                CompletableFuture.completedFuture(mockAsyncResultSet)
        // select by Statement and Query (split to avoid overload ambiguity)
        every { ops.select(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<Class<TestEntity>>()) } returns
                CompletableFuture.completedFuture(mutableListOf(testEntity))
        every { ops.select(any<Query>(), any<Class<TestEntity>>()) } returns
                CompletableFuture.completedFuture(mutableListOf(testEntity))
        // selectOne by Statement and Query
        every {
            ops.selectOne(
                any<com.datastax.oss.driver.api.core.cql.Statement<*>>(),
                any<Class<TestEntity>>()
            )
        } returns
                CompletableFuture.completedFuture(testEntity)
        every { ops.selectOne(any<Query>(), any<Class<TestEntity>>()) } returns
                CompletableFuture.completedFuture(testEntity)
        // slice by Statement and Query
        every { ops.slice(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<Class<TestEntity>>()) } returns
                CompletableFuture.completedFuture(testSlice)
        every { ops.slice(any<Query>(), any<Class<TestEntity>>()) } returns
                CompletableFuture.completedFuture(testSlice)
        // count
        every { ops.count(any<Class<*>>()) } returns CompletableFuture.completedFuture(3L)
        every { ops.count(any<Query>(), any<Class<*>>()) } returns CompletableFuture.completedFuture(3L)
        // exists — split by first-arg type to avoid Query vs Any ambiguity
        every { ops.exists(any<Query>(), any<Class<*>>()) } returns CompletableFuture.completedFuture(true)
        every { ops.exists(any<String>(), any<Class<*>>()) } returns CompletableFuture.completedFuture(true)
        // selectOneById — use Class<TestEntity> to pick the right overload
        every { ops.selectOneById(any(), any<Class<TestEntity>>()) } returns
                CompletableFuture.completedFuture(testEntity)
        // insert — use TestEntity to avoid matching fluent-API Class<T> overload
        every { ops.insert(any<TestEntity>()) } returns CompletableFuture.completedFuture(testEntity)
        every { ops.insert(any<TestEntity>(), any<InsertOptions>()) } returns
                CompletableFuture.completedFuture(mockEntityWriteResult)
        // update entity — use TestEntity to avoid matching fluent-API Class<T> overload
        every { ops.update(any<TestEntity>()) } returns CompletableFuture.completedFuture(testEntity)
        every { ops.update(any<TestEntity>(), any<UpdateOptions>()) } returns
                CompletableFuture.completedFuture(mockEntityWriteResult)
        // update by Query
        every { ops.update(any<Query>(), any<Update>(), any<Class<*>>()) } returns
                CompletableFuture.completedFuture(true)
        // delete entity — use TestEntity to avoid matching fluent-API Class<T> overload
        every { ops.delete(any<TestEntity>()) } returns CompletableFuture.completedFuture(testEntity)
        // delete by Query
        every { ops.delete(any<Query>(), any<Class<*>>()) } returns CompletableFuture.completedFuture(true)
        // delete with options
        every { ops.delete(any<TestEntity>(), any<DeleteOptions>()) } returns
                CompletableFuture.completedFuture(mockWriteResult)
        // deleteById
        every { ops.deleteById(any(), any<Class<*>>()) } returns CompletableFuture.completedFuture(true)
        // truncate
        every { ops.truncate(any<Class<*>>()) } returns CompletableFuture.completedFuture(null)
    }

    @Suppress("UNCHECKED_CAST")
    private val emptyOps = mockk<AsyncCassandraOperations>().also { ops ->
        every {
            ops.selectOne(
                any<com.datastax.oss.driver.api.core.cql.Statement<*>>(),
                any<Class<TestEntity>>()
            )
        } answers {
            CompletableFuture.completedFuture(null) as CompletableFuture<TestEntity?>
        }
        every { ops.selectOne(any<Query>(), any<Class<TestEntity>>()) } answers {
            CompletableFuture.completedFuture(null) as CompletableFuture<TestEntity?>
        }
        every { ops.select(any<Query>(), any<Class<TestEntity>>()) } answers {
            CompletableFuture.completedFuture(null) as CompletableFuture<MutableList<TestEntity>>
        }
        every {
            ops.slice(
                any<com.datastax.oss.driver.api.core.cql.Statement<*>>(),
                any<Class<TestEntity>>()
            )
        } answers {
            CompletableFuture.completedFuture(null) as CompletableFuture<Slice<TestEntity>>
        }
        every { ops.slice(any<Query>(), any<Class<TestEntity>>()) } answers {
            CompletableFuture.completedFuture(null) as CompletableFuture<Slice<TestEntity>>
        }
    }

    @Test
    fun `executeSuspending with Statement`() = runSuspendIO {
        val result = mockOps.executeSuspending(testStatement)
        result.shouldNotBeNull()
    }

    @Test
    fun `selectSuspending with Statement`() = runSuspendIO {
        val result = mockOps.selectSuspending<TestEntity>(testStatement)
        result.shouldNotBeEmpty()
        result.first() shouldBeEqualTo testEntity
    }

    @Test
    fun `selectSuspending with CQL string`() = runSuspendIO {
        val result = mockOps.selectSuspending<TestEntity>("SELECT * FROM users")
        result.shouldNotBeEmpty()
    }

    @Test
    fun `selectSuspending returns empty list when null`() = runSuspendIO {
        val result = emptyOps.selectSuspending<TestEntity>(Query.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `selectOneOrNullSuspending with Statement`() = runSuspendIO {
        val result = mockOps.selectOneOrNullSuspending<TestEntity>(testStatement)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectOneOrNullSuspending returns null`() = runSuspendIO {
        val result = emptyOps.selectOneOrNullSuspending<TestEntity>(testStatement)
        result.shouldBeNull()
    }

    @Test
    fun `selectOneOrNullSuspending with CQL string`() = runSuspendIO {
        val result = mockOps.selectOneOrNullSuspending<TestEntity>("SELECT * FROM users LIMIT 1")
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectSuspending with Query`() = runSuspendIO {
        val result = mockOps.selectSuspending<TestEntity>(Query.empty())
        result.shouldNotBeEmpty()
    }

    @Test
    fun `selectOneOrNullSuspending with Query`() = runSuspendIO {
        val result = mockOps.selectOneOrNullSuspending<TestEntity>(Query.empty())
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `sliceSuspending with Statement`() = runSuspendIO {
        val result = mockOps.sliceSuspending<TestEntity>(testStatement)
        result.shouldNotBeNull()
        result.content shouldBeEqualTo listOf(testEntity)
    }

    @Test
    fun `sliceSuspending returns empty when null`() = runSuspendIO {
        val result = emptyOps.sliceSuspending<TestEntity>(testStatement)
        result.shouldNotBeNull()
        result.content.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `sliceSuspending with Query`() = runSuspendIO {
        val result = mockOps.sliceSuspending<TestEntity>(Query.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `sliceSuspending with Query returns empty when null`() = runSuspendIO {
        val result = emptyOps.sliceSuspending<TestEntity>(Query.empty())
        result.content.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `countSuspending all`() = runSuspendIO {
        val count = mockOps.countSuspending<TestEntity>()
        count shouldBeEqualTo 3L
    }

    @Test
    fun `countSuspending with Query`() = runSuspendIO {
        val count = mockOps.countSuspending<TestEntity>(Query.empty())
        count shouldBeEqualTo 3L
    }

    @Test
    fun `existsSuspending with id`() = runSuspendIO {
        val exists = mockOps.existsSuspending<TestEntity>("id-1")
        exists.shouldBeTrue()
    }

    @Test
    fun `existsSuspending with Query`() = runSuspendIO {
        val exists = mockOps.existsSuspending<TestEntity>(Query.empty())
        exists.shouldBeTrue()
    }

    @Test
    fun `selectOneByIdSuspending`() = runSuspendIO {
        val result = mockOps.selectOneByIdSuspending<TestEntity>("id-1")
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `insertSuspending entity`() = runSuspendIO {
        val result = mockOps.insertSuspending(testEntity)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `insertSuspending entity with options`() = runSuspendIO {
        val result = mockOps.insertSuspending(testEntity, InsertOptions.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `updateSuspending entity`() = runSuspendIO {
        val result = mockOps.updateSuspending(testEntity)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `updateSuspending entity with options`() = runSuspendIO {
        val result = mockOps.updateSuspending(testEntity, mockk<UpdateOptions>())
        result.shouldNotBeNull()
    }

    @Test
    fun `updateSuspending with Query and Update`() = runSuspendIO {
        val result = mockOps.updateSuspending<TestEntity>(Query.empty(), Update.empty())
        result shouldBeEqualTo true
    }

    @Test
    fun `deleteSuspending entity`() = runSuspendIO {
        val result = mockOps.deleteSuspending(testEntity)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `deleteSuspending with Query`() = runSuspendIO {
        val result = mockOps.deleteSuspending<TestEntity>(Query.empty())
        result shouldBeEqualTo true
    }

    @Test
    fun `deleteSuspending entity with DeleteOptions`() = runSuspendIO {
        val result = mockOps.deleteSuspending(testEntity, mockk<DeleteOptions>())
        result.shouldNotBeNull()
    }

    @Test
    fun `deleteByIdSuspending`() = runSuspendIO {
        val result = mockOps.deleteByIdSuspending<TestEntity>("id-1")
        result.shouldBeTrue()
    }

    @Test
    fun `truncateSuspending`() = runSuspendIO {
        mockOps.truncateSuspending<TestEntity>()
        // no exception = success
    }
}
