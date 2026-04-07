package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.idgenerators.IdGenerator
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.ConcurrentHashMap

class KsuidGeneratorTest {
    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val ID_SIZE = 100
        private const val CONCURRENCY_COUNT = 5_000
    }

    @Test
    fun `기본 생성자는 Seconds 전략을 사용한다`() {
        val gen = KsuidGenerator()
        val id = gen.nextId()
        id.shouldNotBeNull()
        id.length shouldBeEqualTo Ksuid.Seconds.MAX_ENCODED_LEN
    }

    @Test
    fun `Millis 전략을 주입할 수 있다`() {
        val gen = KsuidGenerator(Ksuid.Millis)
        val id = gen.nextId()
        id.shouldNotBeNull()
        id.length shouldBeEqualTo Ksuid.Millis.MAX_ENCODED_LEN
    }

    @Test
    fun `IdGenerator 인터페이스로 사용할 수 있다`() {
        val gen: IdGenerator<String> = KsuidGenerator()
        val id = gen.nextId()
        id.shouldNotBeNull()
    }

    @Test
    fun `nextIds는 요청한 크기만큼 시퀀스를 반환한다`() {
        val gen = KsuidGenerator()
        val ids = gen.nextIds(ID_SIZE).toList()
        ids shouldHaveSize ID_SIZE
        ids.distinct() shouldHaveSize ID_SIZE
    }

    @Test
    fun `nextIdsAsString은 요청한 크기만큼 문자열 시퀀스를 반환한다`() {
        val gen = KsuidGenerator()
        val strs = gen.nextIdsAsString(ID_SIZE).toList()
        strs shouldHaveSize ID_SIZE
        strs.all { it.length == Ksuid.Seconds.MAX_ENCODED_LEN } shouldBeEqualTo true
    }

    @Nested
    inner class ConcurrencyTest {
        @RepeatedTest(REPEAT_SIZE)
        fun `멀티스레드 환경에서 중복 없이 KSUID를 생성한다 (Seconds)`() {
            val gen = KsuidGenerator()
            val idMap = ConcurrentHashMap<String, Int>()

            MultithreadingTester()
                .workers(Runtimex.availableProcessors)
                .rounds(CONCURRENCY_COUNT / Runtimex.availableProcessors)
                .add {
                    val id = gen.nextId()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `멀티스레드 환경에서 중복 없이 KSUID를 생성한다 (Millis)`() {
            val gen = KsuidGenerator(Ksuid.Millis)
            val idMap = ConcurrentHashMap<String, Int>()

            MultithreadingTester()
                .workers(Runtimex.availableProcessors)
                .rounds(CONCURRENCY_COUNT / Runtimex.availableProcessors)
                .add {
                    val id = gen.nextId()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }

        @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
        @RepeatedTest(REPEAT_SIZE)
        fun `Virtual Thread 환경에서 중복 없이 KSUID를 생성한다`() {
            val gen = KsuidGenerator()
            val idMap = ConcurrentHashMap<String, Int>()

            StructuredTaskScopeTester()
                .rounds(CONCURRENCY_COUNT)
                .add {
                    val id = gen.nextId()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `Coroutine 환경에서 중복 없이 KSUID를 생성한다`() =
            runSuspendDefault {
                val gen = KsuidGenerator()
                val idMap = ConcurrentHashMap<String, Int>()

                SuspendedJobTester()
                    .workers(Runtimex.availableProcessors)
                    .rounds(CONCURRENCY_COUNT)
                    .add {
                        val id = gen.nextId()
                        idMap.putIfAbsent(id, 1).shouldBeNull()
                    }.run()
            }
    }
}
