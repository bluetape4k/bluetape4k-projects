package io.bluetape4k.idgenerators.flake

import io.bluetape4k.codec.encodeHexString
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
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
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class FlakeTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 10
        private const val ID_SIZE = 100
        private const val TEST_COUNT = Short.MAX_VALUE * 4
    }

    private val flake = Flake()

    @RepeatedTest(REPEAT_SIZE)
    fun `generate flake id`() {
        val ids = fastList(3) { flake.nextId() }

        ids.toUnifiedSet() shouldHaveSize 3

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

        val ids = fastList(ID_SIZE) {
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
    fun `generate flake more max sequence`() {
        repeat(TEST_COUNT) {
            flake.nextId()
        }
    }

    @Test
    fun `1 msec 이 지나면 sequence는 리셋되어야 합니다`() {
        val seq1 = flake.nextId().copyOfRange(14, 15)
        Thread.sleep(10)
        val seq2 = flake.nextId().copyOfRange(14, 15)

        // 1 msec 이 지나면 sequence가 리셋되어야 합니다.
        seq2 shouldBeEqualTo seq1
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

    @EnabledOnJre(JRE.JAVA_21)
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
        val tasks = fastList(ID_SIZE) {
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
}
