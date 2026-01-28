package io.bluetape4k.junit5.concurrency

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import kotlin.system.measureTimeMillis
import kotlin.test.assertFailsWith

class StructuredTaskScopeTesterTest {
    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `예외를 발생시키는 코드는 실패한다`() {
        val block = { throw RuntimeException("BAM!") }

        assertFailsWith<RuntimeException> {
            StructuredTaskScopeTester()
                .roundsPerTask(Runtime.getRuntime().availableProcessors())
                .add(block)
                .run()
        }
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `thread 수가 복수이면 실행시간은 테스트 코드의 실행 시간의 총합보다 작아야 한다`() {
        val time = measureTimeMillis {
            StructuredTaskScopeTester()
                .roundsPerTask(4)
                .add { Thread.sleep(100) }
                .add { Thread.sleep(100) }
                .run()
        }
        time shouldBeLessOrEqualTo 200
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `하나의 코드블럭을 여러번 수행 시 수행 횟수는 같아야 한다`() {
        val block = CountingTask()

        StructuredTaskScopeTester()
            .roundsPerTask(10)
            .add(block)
            .run()

        block.count shouldBeEqualTo 10
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `두 개의 코드 블럭을 병렬로 실행`() {
        val block1 = CountingTask()
        val block2 = CountingTask()

        StructuredTaskScopeTester()
            .roundsPerTask(4)
            .addAll(block1, block2)
            .run()

        block1.count shouldBeEqualTo 4
        block2.count shouldBeEqualTo 4
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `실행할 코드블럭을 등록하지 않으면 예외가 발생한다`() {
        assertFailsWith<IllegalStateException> {
            StructuredTaskScopeTester().run()
        }
    }


    private class CountingTask: () -> Unit {
        private val counter = atomic(0)
        val count by counter

        override fun invoke() {
            Thread.sleep(1)
            counter.incrementAndGet()
            log.trace { "Execution count: $count" }
        }
    }
}
