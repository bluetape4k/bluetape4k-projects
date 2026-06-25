package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.SimpleStatement
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.Serializable

class ReactiveCassandraOperationsCoroutinesUnitTest {

    companion object : KLoggingChannel()

    data class TestEntity(val id: String = "id-1", val name: String = "Test") : Serializable

    private val testEntity = TestEntity()
    private val testSlice: Slice<TestEntity> = SliceImpl(listOf(testEntity))
    private val mockResultSet = mockk<ReactiveResultSet>()
    private val mockWriteResult = mockk<WriteResult>()
    private val mockEntityWriteResult = mockk<EntityWriteResult<TestEntity>>()
    private val testStatement = SimpleStatement.newInstance("SELECT 1")

    @Suppress("UNCHECKED_CAST")
    private val mockOps = mockk<ReactiveCassandraOperations>().also { ops ->
        // select operations
        every { ops.select(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<Class<*>>()) } answers {
            Flux.just(testEntity) as Flux<Any>
        }
        every { ops.select(any<String>(), any<Class<*>>()) } answers { Flux.just(testEntity) as Flux<Any> }
        every { ops.select(any<Query>(), any<Class<*>>()) } answers { Flux.just(testEntity) as Flux<Any> }
        // selectOne operations
        every { ops.selectOne(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<Class<*>>()) } answers {
            Mono.just(testEntity) as Mono<Any>
        }
        every { ops.selectOne(any<String>(), any<Class<*>>()) } answers { Mono.just(testEntity) as Mono<Any> }
        every { ops.selectOne(any<Query>(), any<Class<*>>()) } answers { Mono.just(testEntity) as Mono<Any> }
        // slice
        every { ops.slice(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<Class<*>>()) } answers {
            Mono.just(testSlice) as Mono<Slice<Any>>
        }
        every { ops.slice(any<Query>(), any<Class<*>>()) } answers { Mono.just(testSlice) as Mono<Slice<Any>> }
        // execute
        every { ops.execute(any<com.datastax.oss.driver.api.core.cql.Statement<*>>()) } returns Mono.just(mockResultSet)
        // count
        every { ops.count(any<Class<*>>()) } returns Mono.just(3L)
        every { ops.count(any<Query>(), any<Class<*>>()) } returns Mono.just(3L)
        // exists — split by first-arg type to avoid Query vs Any ambiguity
        every { ops.exists(any<String>(), any<Class<*>>()) } returns Mono.just(true)
        every { ops.exists(any<Query>(), any<Class<*>>()) } returns Mono.just(true)
        // selectOneById
        every { ops.selectOneById(any(), any<Class<*>>()) } answers { Mono.just(testEntity) as Mono<Any> }
        // insert: 1-arg entity, 2-arg entity+InsertOptions
        every { ops.insert(any<TestEntity>()) } returns Mono.just(testEntity)
        every { ops.insert(any<TestEntity>(), any<InsertOptions>()) } returns Mono.just(mockEntityWriteResult)
        // update: 1-arg entity, 2-arg entity+UpdateOptions, 3-arg query+update+class
        every { ops.update(any<TestEntity>()) } returns Mono.just(testEntity)
        every { ops.update(any<TestEntity>(), any<UpdateOptions>()) } returns Mono.just(mockEntityWriteResult)
        every { ops.update(any<Query>(), any(), any<Class<*>>()) } returns Mono.just(true)
        // delete: 1-arg entity, 2-arg entity+QueryOptions, 2-arg entity+DeleteOptions, 2-arg query+class
        every { ops.delete(any<Query>(), any<Class<*>>()) } returns Mono.just(true)
        every { ops.delete(any<TestEntity>()) } returns Mono.just(testEntity)
        every { ops.delete(any<TestEntity>(), any<QueryOptions>()) } returns Mono.just(mockWriteResult)
        every { ops.delete(any<TestEntity>(), any<DeleteOptions>()) } returns Mono.just(mockWriteResult)
        // deleteById
        every { ops.deleteById(any(), any<Class<*>>()) } returns Mono.just(true)
        // truncate
        every { ops.truncate(any<Class<*>>()) } returns Mono.empty()
    }

    @Test
    fun `selectAsFlow with Statement`() = runSuspendIO {
        val flow = mockOps.selectAsFlow<TestEntity>(testStatement)
        flow.shouldNotBeNull()
        flow.toList().shouldNotBeNull()
    }

    @Test
    fun `selectAsFlow with CQL string`() = runSuspendIO {
        val flow = mockOps.selectAsFlow<TestEntity>("SELECT * FROM users")
        flow.shouldNotBeNull()
    }

    @Test
    fun `selectOneSuspending with Statement`() = runSuspendIO {
        val result = mockOps.selectOneSuspending<TestEntity>(testStatement)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectOneOrNullSuspending with Statement`() = runSuspendIO {
        val result = mockOps.selectOneOrNullSuspending<TestEntity>(testStatement)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectOneOrNullSuspending returns null for empty Mono`() = runSuspendIO {
        val emptyOps = mockk<ReactiveCassandraOperations>()
        every { emptyOps.selectOne(any<com.datastax.oss.driver.api.core.cql.Statement<*>>(), any<Class<*>>()) } returns
            Mono.empty<Any>()
        val result = emptyOps.selectOneOrNullSuspending<TestEntity>(testStatement)
        result.shouldBeNull()
    }

    @Test
    fun `selectOneSuspending with CQL string`() = runSuspendIO {
        val result = mockOps.selectOneSuspending<TestEntity>("SELECT * FROM users LIMIT 1")
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectOneOrNullSuspending with CQL string`() = runSuspendIO {
        val result = mockOps.selectOneOrNullSuspending<TestEntity>("SELECT * FROM users LIMIT 1")
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectOneSuspending with Query`() = runSuspendIO {
        val result = mockOps.selectOneSuspending<TestEntity>(Query.empty())
        result shouldBeEqualTo testEntity
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
    fun `sliceSuspending with Query`() = runSuspendIO {
        val result = mockOps.sliceSuspending<TestEntity>(Query.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `executeSuspending with Statement`() = runSuspendIO {
        val result = mockOps.executeSuspending(testStatement)
        result.shouldNotBeNull()
    }

    @Test
    fun `countSuspending with Query`() = runSuspendIO {
        val count = mockOps.countSuspending<TestEntity>(Query.empty())
        count shouldBeEqualTo 3L
    }

    @Test
    fun `existsSuspending with Query`() = runSuspendIO {
        val exists = mockOps.existsSuspending<TestEntity>(Query.empty())
        exists.shouldBeTrue()
    }

    @Test
    fun `existsSuspending with id`() = runSuspendIO {
        val exists = mockOps.existsSuspending<TestEntity>("id-1")
        exists.shouldBeTrue()
    }

    @Test
    fun `selectOneByIdSuspending`() = runSuspendIO {
        val result = mockOps.selectOneByIdSuspending<TestEntity>("id-1")
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `selectOneOrNullByIdSuspending`() = runSuspendIO {
        val result = mockOps.selectOneOrNullByIdSuspending<TestEntity>("id-1")
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `insertSuspending entity no options`() = runSuspendIO {
        val result = mockOps.insertSuspending(testEntity)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `insertSuspending with options`() = runSuspendIO {
        val result = mockOps.insertSuspending(testEntity, InsertOptions.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `updateSuspending entity`() = runSuspendIO {
        val result = mockOps.updateSuspending(testEntity)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `updateSuspending entity with UpdateOptions`() = runSuspendIO {
        val result = mockOps.updateSuspending(testEntity, UpdateOptions.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `updateSuspending query and update`() = runSuspendIO {
        val result = mockOps.updateSuspending<TestEntity>(Query.empty(), Update.empty())
        result.shouldBeTrue()
    }

    @Test
    fun `deleteSuspending query`() = runSuspendIO {
        val result = mockOps.deleteSuspending<TestEntity>(Query.empty())
        result.shouldBeTrue()
    }

    @Test
    fun `deleteSuspending entity`() = runSuspendIO {
        val result = mockOps.deleteSuspending(testEntity)
        result shouldBeEqualTo testEntity
    }

    @Test
    fun `deleteSuspending entity with QueryOptions`() = runSuspendIO {
        val result = mockOps.deleteSuspending(testEntity, mockk<QueryOptions>())
        result.shouldNotBeNull()
    }

    @Test
    fun `deleteSuspending entity with DeleteOptions`() = runSuspendIO {
        val result = mockOps.deleteSuspending(testEntity, DeleteOptions.empty())
        result.shouldNotBeNull()
    }

    @Test
    fun `deleteByIdSuspending`() = runSuspendIO {
        val result = mockOps.deleteByIdSuspending<TestEntity>("id-1")
        result.shouldBeTrue()
    }

    @Test
    fun `countSuspending no query`() = runSuspendIO {
        val count = mockOps.countSuspending<TestEntity>()
        count shouldBeEqualTo 3L
    }

    @Test
    fun `truncateSuspending`() = runSuspendIO {
        mockOps.truncateSuspending<TestEntity>()
    }
}
