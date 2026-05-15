package io.bluetape4k.spring.cassandra

import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.cassandra.cql.writeOptions
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.ReactiveCassandraBatchOperations

class ReactiveCassandraBatchOperationsCoroutinesTest {

    companion object : KLoggingChannel()

    private val mockBatchOps = mockk<ReactiveCassandraBatchOperations>(relaxed = true)

    @Test
    fun `insertFlow without options`() {
        val entities = flowOf("entity1", "entity2")
        val result = mockBatchOps.insertFlow(entities)
        result.shouldNotBeNull()
    }

    @Test
    fun `insertFlow with WriteOptions`() {
        val entities = flowOf("entity1")
        val options = writeOptions { }
        val result = mockBatchOps.insertFlow(entities, options)
        result.shouldNotBeNull()
    }

    @Test
    fun `updateFlow without options`() {
        val entities = flowOf("entity1")
        val result = mockBatchOps.updateFlow(entities)
        result.shouldNotBeNull()
    }

    @Test
    fun `updateFlow with WriteOptions`() {
        val entities = flowOf("entity1")
        val options = writeOptions { }
        val result = mockBatchOps.updateFlow(entities, options)
        result.shouldNotBeNull()
    }

    @Test
    fun `deleteFlow without options`() {
        val entities = flowOf("entity1")
        val result = mockBatchOps.deleteFlow(entities)
        result.shouldNotBeNull()
    }

    @Test
    fun `deleteFlow with WriteOptions`() {
        val entities = flowOf("entity1")
        val options = writeOptions { }
        val result = mockBatchOps.deleteFlow(entities, options)
        result.shouldNotBeNull()
    }
}
