package io.bluetape4k.idgenerators.uuid

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
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
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertFailsWith

class UuidTest {
    companion object : KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val ID_SIZE = 100
        private const val CONCURRENCY_COUNT = 5_000
    }

    @Nested
    inner class V1Test {
        @RepeatedTest(REPEAT_SIZE)
        fun `V1 nextUUID는 유니크한 UUID를 반환한다`() {
            val ids = List(ID_SIZE) { Uuid.V1.nextUUID() }
            ids.distinct() shouldHaveSize ID_SIZE
            log.debug { "V1 sample: ${ids.first()}" }
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `V1 nextBase62는 Base62 문자열을 반환한다`() {
            val s = Uuid.V1.nextBase62()
            s.shouldNotBeNull()
            s.isNotBlank() shouldBeEqualTo true
        }
    }

    @Nested
    inner class V4Test {
        @RepeatedTest(REPEAT_SIZE)
        fun `V4 nextUUID는 유니크한 UUID를 반환한다`() {
            val ids = List(ID_SIZE) { Uuid.V4.nextUUID() }
            ids.distinct() shouldHaveSize ID_SIZE
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `V4 nextBase62는 Base62 문자열을 반환한다`() {
            val s = Uuid.V4.nextBase62()
            s.isNotBlank() shouldBeEqualTo true
        }
    }

    @Nested
    inner class V5Test {
        @RepeatedTest(REPEAT_SIZE)
        fun `V5 nextUUID는 매번 다른 UUID를 반환한다 (비결정론적)`() {
            val ids = List(ID_SIZE) { Uuid.V5.nextUUID() }
            ids.distinct() shouldHaveSize ID_SIZE
        }
    }

    @Nested
    inner class V6Test {
        @RepeatedTest(REPEAT_SIZE)
        fun `V6 nextUUID는 유니크한 UUID를 반환한다`() {
            val ids = List(ID_SIZE) { Uuid.V6.nextUUID() }
            ids.distinct() shouldHaveSize ID_SIZE
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `V6 nextBase62는 Base62 문자열을 반환한다`() {
            val s = Uuid.V6.nextBase62()
            s.isNotBlank() shouldBeEqualTo true
        }
    }

    @Nested
    inner class V7Test {
        @RepeatedTest(REPEAT_SIZE)
        fun `V7 nextUUID는 유니크한 UUID를 반환한다`() {
            val ids = List(ID_SIZE) { Uuid.V7.nextUUID() }
            ids.distinct() shouldHaveSize ID_SIZE
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `V7 nextBase62는 Base62 문자열을 반환한다`() {
            val s = Uuid.V7.nextBase62()
            s.isNotBlank() shouldBeEqualTo true
        }
    }

    @Nested
    inner class SequenceTest {
        @Test
        fun `nextUUIDs는 요청한 크기만큼 UUID 시퀀스를 반환한다`() {
            val ids = Uuid.V7.nextUUIDs(ID_SIZE).toList()
            ids shouldHaveSize ID_SIZE
            ids.distinct() shouldHaveSize ID_SIZE
        }

        @Test
        fun `nextBase62s는 요청한 크기만큼 문자열 시퀀스를 반환한다`() {
            val strs = Uuid.V7.nextBase62s(ID_SIZE).toList()
            strs shouldHaveSize ID_SIZE
            strs.all { it.isNotBlank() } shouldBeEqualTo true
        }

        @Test
        fun `nextBase62s의 size는 1 이상이어야 한다`() {
            assertFailsWith<AssertionError> {
                Uuid.V7.nextBase62s(0).toList()
            }
        }
    }

    @Nested
    inner class CustomRandomTest {
        @Test
        fun `random으로 커스텀 Random을 사용하는 V4 생성기를 만든다`() {
            val gen = Uuid.random(SecureRandom())
            val id = gen.nextUUID()
            id.shouldNotBeNull()
        }

        @Test
        fun `epochRandom으로 커스텀 Random을 사용하는 V7 생성기를 만든다`() {
            val gen = Uuid.epochRandom(SecureRandom())
            val id = gen.nextUUID()
            id.shouldNotBeNull()
        }
    }

    @Nested
    inner class NamebasedTest {
        @Test
        fun `namebased는 동일 name으로 항상 동일한 UUID를 반환한다 (결정론적)`() {
            val gen = Uuid.namebased("fixed-name")
            val id1 = gen.nextId()
            val id2 = gen.nextId()
            id1 shouldBeEqualTo id2
            log.debug { "namebased UUID: $id1" }
        }

        @Test
        fun `namebased는 다른 name으로 다른 UUID를 반환한다`() {
            val id1 = Uuid.namebased("name-a").nextId()
            val id2 = Uuid.namebased("name-b").nextId()
            (id1 == id2) shouldBeEqualTo false
        }
    }

    @Nested
    inner class ConcurrencyTest {
        @RepeatedTest(REPEAT_SIZE)
        fun `V7 멀티스레드 환경에서 중복 없이 UUID를 생성한다`() {
            val idMap = ConcurrentHashMap<String, Int>()

            MultithreadingTester()
                .workers(Runtimex.availableProcessors)
                .rounds(CONCURRENCY_COUNT / Runtimex.availableProcessors)
                .add {
                    val id = Uuid.V7.nextIdAsString()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }

        @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
        @RepeatedTest(REPEAT_SIZE)
        fun `V7 Virtual Thread 환경에서 중복 없이 UUID를 생성한다`() {
            val idMap = ConcurrentHashMap<String, Int>()

            StructuredTaskScopeTester()
                .rounds(CONCURRENCY_COUNT)
                .add {
                    val id = Uuid.V7.nextIdAsString()
                    idMap.putIfAbsent(id, 1).shouldBeNull()
                }.run()
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `V7 Coroutine 환경에서 중복 없이 UUID를 생성한다`() =
            runSuspendDefault {
                val idMap = ConcurrentHashMap<String, Int>()

                SuspendedJobTester()
                    .workers(Runtimex.availableProcessors)
                    .rounds(CONCURRENCY_COUNT)
                    .add {
                        val id = Uuid.V7.nextIdAsString()
                        idMap.putIfAbsent(id, 1).shouldBeNull()
                    }.run()
            }
    }
}
