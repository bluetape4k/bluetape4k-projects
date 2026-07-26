package io.bluetape4k.spring.cassandra

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.data.cassandra.core.ReactiveSelectOperation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.Serializable

class ReactiveSelectOperationSupportTest {

    companion object: KLoggingChannel()

    data class TestEntity(val id: String = "test-id", val name: String = "Test"): Serializable

    private val testEntity = TestEntity()

    @Suppress("UNCHECKED_CAST")
    private val mockProjection = mockk<ReactiveSelectOperation.SelectWithProjection<TestEntity>>().also {
        every { it.`as`(TestEntity::class.java) } returns mockk<ReactiveSelectOperation.SelectWithQuery<TestEntity>>()
    }

    private val mockTerminating = mockk<ReactiveSelectOperation.TerminatingSelect<TestEntity>>().also {
        every { it.count() } returns Mono.just(5L)
        every { it.exists() } returns Mono.just(true)
        every { it.first() } returns Mono.just(testEntity)
        every { it.one() } returns Mono.just(testEntity)
        every { it.all() } returns Flux.just(testEntity)
    }

    @Test
    fun `cast returns SelectWithQuery of projected type`() = runSuspendIO {
        val result = mockProjection.cast<TestEntity>()
        result.shouldNotBeNull()
    }

    @Test
    fun `countSuspending returns count`() = runSuspendIO {
        val count = mockTerminating.countSuspending()
        count shouldBeEqualTo 5L
    }

    @Test
    fun `existsSuspending returns true`() = runSuspendIO {
        val exists = mockTerminating.existsSuspending()
        exists.shouldBeTrue()
    }

    @Test
    fun `firstSuspending returns first entity`() = runSuspendIO {
        val entity = mockTerminating.firstSuspending()
        entity shouldBeEqualTo testEntity
    }

    @Test
    fun `oneSuspending returns entity`() = runSuspendIO {
        val entity = mockTerminating.oneSuspending()
        entity shouldBeEqualTo testEntity
    }

    @Test
    fun `oneSuspending returns null for empty Mono`() = runSuspendIO {
        val emptyTerminating = mockk<ReactiveSelectOperation.TerminatingSelect<TestEntity>>()
        every { emptyTerminating.one() } returns Mono.empty()
        val entity = emptyTerminating.oneSuspending()
        entity.shouldBeNull()
    }

    @Test
    fun `allSuspending returns list of entities`() = runSuspendIO {
        val entities = mockTerminating.allSuspending()
        entities.shouldNotBeNull()
        entities shouldBeEqualTo listOf(testEntity)
    }
}
