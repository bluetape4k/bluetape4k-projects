package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.ConcurrentHashMap

class UlidGeneratorConcurrencyTest {
    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val TEST_COUNT = 10_000
    }

    private val generator = UlidGenerator()

    @RepeatedTest(REPEAT_SIZE)
    fun `멀티스레드 환경에서 중복 없이 ULID를 생성한다`() {
        val idMap = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .workers(Runtimex.availableProcessors)
            .rounds(TEST_COUNT / Runtimex.availableProcessors)
            .add {
                val id = generator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }.run()
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread 환경에서 중복 없이 ULID를 생성한다`() {
        val idMap = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .rounds(TEST_COUNT)
            .add {
                val id = generator.nextId()
                idMap.putIfAbsent(id, 1).shouldBeNull()
            }.run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Coroutine 환경에서 중복 없이 ULID를 생성한다`() =
        runSuspendDefault {
            val idMap = ConcurrentHashMap<String, Int>()

            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(TEST_COUNT)
                .add {
                    val id = generator.nextId()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }

    @RepeatedTest(REPEAT_SIZE)
    fun `멀티스레드 환경에서 ULID 값 객체도 중복 없이 생성한다`() {
        val idMap = ConcurrentHashMap<ULID, Int>()

        MultithreadingTester()
            .workers(Runtimex.availableProcessors)
            .rounds(TEST_COUNT / Runtimex.availableProcessors)
            .add {
                val ulid = generator.nextULID()
                idMap.putIfAbsent(ulid, 1).shouldBeNull()
            }.run()
    }
}
