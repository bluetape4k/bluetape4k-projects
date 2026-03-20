package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.ConcurrentLinkedQueue

class ULIDConcurrencyTest : AbstractULIDTest() {
    companion object : KLogging() {
        private const val NUM_WORKERS = 16
        private const val ROUNDS = 100
    }

    @Nested
    inner class FactoryConcurrency {
        @RepeatedTest(REPEAT_SIZE)
        fun `nextULID - 멀티스레드에서 모든 ULID가 고유하다`() {
            val ulids = ConcurrentLinkedQueue<ULID>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add { ulids += ULID.nextULID() }
                .run()

            val expected = NUM_WORKERS * ROUNDS
            ulids.size shouldBeEqualTo expected
            ulids.distinct().size shouldBeEqualTo expected
            log.debug { "Factory: $expected unique ULIDs generated" }
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `randomULID - 멀티스레드에서 모든 문자열이 고유하다`() {
            val ulidStrings = ConcurrentLinkedQueue<String>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add { ulidStrings += ULID.randomULID() }
                .run()

            val expected = NUM_WORKERS * ROUNDS
            ulidStrings.size shouldBeEqualTo expected
            ulidStrings.distinct().size shouldBeEqualTo expected
        }
    }

    @Nested
    inner class MonotonicConcurrency {
        @RepeatedTest(REPEAT_SIZE)
        fun `nextULID - 멀티스레드에서 동일 previous 기반 생성이 안전하다`() {
            val monotonic = ULID.monotonic()
            val previous = ULID.nextULID(timestamp = 1000L)
            val ulids = ConcurrentLinkedQueue<ULID>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add { ulids += monotonic.nextULID(previous) }
                .run()

            val expected = NUM_WORKERS * ROUNDS
            ulids.size shouldBeEqualTo expected
        }
    }

    @Nested
    inner class StatefulMonotonicConcurrency {
        @RepeatedTest(REPEAT_SIZE)
        fun `nextULID - 멀티스레드에서 모든 ULID가 고유하다`() {
            val generator = ULID.statefulMonotonic()
            val ulids = ConcurrentLinkedQueue<ULID>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add { ulids += generator.nextULID() }
                .run()

            val expected = NUM_WORKERS * ROUNDS
            ulids.size shouldBeEqualTo expected
            ulids.distinct().size shouldBeEqualTo expected
            log.debug { "StatefulMonotonic: $expected unique ULIDs generated" }
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `nextULID - 동일 timestamp로 멀티스레드 생성 시 모든 ULID가 고유하다`() {
            val generator = ULID.statefulMonotonic()
            val fixedTimestamp = System.currentTimeMillis()
            val ulids = ConcurrentLinkedQueue<ULID>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add { ulids += generator.nextULID(fixedTimestamp) }
                .run()

            val expected = NUM_WORKERS * ROUNDS
            ulids.size shouldBeEqualTo expected
            ulids.distinct().size shouldBeEqualTo expected

            // 동일 timestamp이므로 모든 ULID의 timestamp가 같아야 한다
            ulids.forEach { it.timestamp shouldBeEqualTo fixedTimestamp }

            // 단조증가 검증
            val sorted = ulids.sorted()
            (1 until sorted.size).forEach { i ->
                (sorted[i] > sorted[i - 1]).shouldBeTrue()
            }
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `nextULIDStrict - 멀티스레드에서 null이 아닌 결과는 모두 고유하다`() {
            val generator = ULID.statefulMonotonic()
            val ulids = ConcurrentLinkedQueue<ULID>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add {
                    generator.nextULIDStrict()?.let { ulids += it }
                }.run()

            // strict 모드에서는 CAS 경쟁으로 일부 null 반환 가능
            ulids.size shouldBeGreaterThan 0
            ulids.distinct().size shouldBeEqualTo ulids.size
        }

        @RepeatedTest(REPEAT_SIZE)
        fun `randomULID - 멀티스레드에서 Factory 위임 메서드도 안전하다`() {
            val generator = ULID.statefulMonotonic()
            val ulidStrings = ConcurrentLinkedQueue<String>()

            MultithreadingTester()
                .workers(NUM_WORKERS)
                .rounds(ROUNDS)
                .add { ulidStrings += generator.randomULID() }
                .run()

            val expected = NUM_WORKERS * ROUNDS
            ulidStrings.size shouldBeEqualTo expected
            ulidStrings.distinct().size shouldBeEqualTo expected
        }
    }
}
