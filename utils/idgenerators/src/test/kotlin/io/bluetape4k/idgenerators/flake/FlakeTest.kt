package io.bluetape4k.idgenerators.flake

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FlakeTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 10
        private const val ID_SIZE = 100
        private const val TEST_COUNT = Short.MAX_VALUE * 4
    }

    private val flake = Flake()

    @RepeatedTest(REPEAT_SIZE)
    fun `generate flake id`() {
        val ids = List(3) { flake.nextId() }

        ids.toSet() shouldHaveSize 3

        ids.forEach {
            log.debug { "id=$it, ${Flake.asComponentString(it)}" }
        }
        ids.forEach {
            log.debug { "id as Hex=${it.encodeHexString()}" }
        }
        ids.forEach {
            log.debug { "id as Base62=${Flake.asBase62String(it)}" }
        }
    }

    @Test
    fun `sequence increment`() {
        val nodeIdentifier: () -> Long = { 123456789L }
        val clock = Clock.tick(Clock.systemUTC(), Duration.ofMinutes(1))
        val customFlake = Flake(nodeIdentifier, clock)

        val ids = List(ID_SIZE) {
            customFlake.nextIdAsString()
        }
        ids.forEachIndexed { index, id ->
            log.trace { "id[$index]=$id" }
        }

        ids shouldHaveSize ID_SIZE
        ids.distinct() shouldHaveSize ID_SIZE
        ids.sorted() shouldBeEqualTo ids
    }

    @Test
    fun `component string uses timestamp node and sequence byte layout`() {
        val timestamp = 1_700_000_000_123L
        val nodeId = 0x010203040506L
        val clock = Clock.fixed(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)
        val customFlake = Flake({ nodeId }, clock)

        val id = customFlake.nextId()

        Flake.asComponentString(id) shouldBeEqualTo "$timestamp-$nodeId-1"
    }

    @Test
    fun `generate flake more max sequence`() {
        repeat(TEST_COUNT) {
            flake.nextId()
        }
    }

    @Test
    fun `1 msec 이 지나면 sequence는 리셋되어야 합니다`() {
        val initialMillis = 1_700_000_000_000L
        val clock = MutableClock(initialMillis)
        val customFlake = Flake({ 123456789L }, clock)

        val first = customFlake.nextId()
        clock.advanceBy(Duration.ofMillis(1))
        val second = customFlake.nextId()

        Flake.asComponentString(first) shouldBeEqualTo "$initialMillis-123456789-1"
        Flake.asComponentString(second) shouldBeEqualTo "${initialMillis + 1}-123456789-0"
    }

    @Test
    fun `generate id in multi-threading`() {
        val flake = Flake()
        val idMaps = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(100)
            .add {
                val id = flake.nextIdAsString()
                idMaps.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @Test
    fun `generate id in virtual threading`() {
        val flake = Flake()
        val idMaps = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .rounds(100 * 2 * Runtimex.availableProcessors)
            .add {
                val id = flake.nextIdAsString()
                idMaps.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `generate flake id in coroutines`() = runSuspendDefault {
        val tasks = List(ID_SIZE) {
            async {
                flake.nextId()
            }
        }
        val ids = tasks.awaitAll()
        ids shouldHaveSize ID_SIZE
        ids.distinct() shouldHaveSize ID_SIZE
    }

    @Test
    fun `generate id in multi jobs`() = runSuspendDefault {
        val flake = Flake()
        val idMaps = ConcurrentHashMap<String, Int>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(100 * 2 * Runtimex.availableProcessors)
            .add {
                val id = flake.nextIdAsString()
                idMaps.putIfAbsent(id, 1).shouldBeNull()
            }
            .run()
    }

    private class MutableClock(
        private val currentMillis: AtomicLong,
        private val zone: ZoneId,
    ): Clock() {

        constructor(initialMillis: Long): this(AtomicLong(initialMillis), ZoneOffset.UTC)

        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(currentMillis, zone)

        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis.get())

        fun advanceBy(duration: Duration) {
            currentMillis.addAndGet(duration.toMillis())
        }
    }
}
