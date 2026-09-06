package io.bluetape4k.spring.task

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNullOrEmpty
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.MDC
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
class MdcTaskDecoratorTest {

    private lateinit var executor: ExecutorService
    private val decorator = MdcTaskDecorator()

    @BeforeEach
    fun setUp() {
        MDC.clear()
        executor = Executors.newSingleThreadExecutor()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
        executor.shutdownNow()
        executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
    }

    @Test
    fun `서로 다른 caller context와 task local key는 재사용 worker 사이에 새지 않는다`() {
        seedWorkerContext(mapOf("worker" to "seed"))

        MDC.setContextMap(mapOf("request" to "one"))
        val first = decorator.decorate(Runnable {
            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("request" to "one")
            MDC.put("taskOnly", "first")
        })
        runOnWorker(first)
        workerContext() shouldBeEqualTo mapOf("worker" to "seed")

        MDC.setContextMap(mapOf("request" to "two", "tenant" to "blue"))
        val second = decorator.decorate(Runnable {
            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("request" to "two", "tenant" to "blue")
            MDC.get("taskOnly").shouldBeNullOrEmpty()
        })
        runOnWorker(second)
        workerContext() shouldBeEqualTo mapOf("worker" to "seed")
    }

    @Test
    fun `빈 caller context는 worker context를 숨기고 종료 후 복원한다`() {
        seedWorkerContext(mapOf("worker" to "seed", "stale" to "value"))
        MDC.clear()

        val decorated = decorator.decorate(Runnable {
            MDC.getCopyOfContextMap().orEmpty() shouldBeEqualTo emptyMap<String, String>()
            MDC.put("taskOnly", "empty-caller")
        })
        runOnWorker(decorated)

        workerContext() shouldBeEqualTo mapOf("worker" to "seed", "stale" to "value")
    }

    @Test
    fun `예외 task도 worker context를 복원한다`() {
        seedWorkerContext(mapOf("worker" to "seed", "stale" to "value"))
        MDC.setContextMap(mapOf("request" to "failure"))

        val decorated = decorator.decorate(Runnable {
            MDC.get("request") shouldBeEqualTo "failure"
            MDC.put("taskOnly", "failure")
            error("boom")
        })
        val failure = assertFailsWith<ExecutionException> {
            executor.submit(decorated).get(5, TimeUnit.SECONDS)
        }

        failure.cause shouldBeInstanceOf IllegalStateException::class
        failure.cause?.message shouldBeEqualTo "boom"
        workerContext() shouldBeEqualTo mapOf("worker" to "seed", "stale" to "value")
    }

    @Test
    fun `nested decorator는 outer와 seed context를 순서대로 복원한다`() {
        seedWorkerContext(mapOf("worker" to "seed"))
        MDC.setContextMap(mapOf("request" to "outer"))

        val decorated = decorator.decorate(Runnable {
            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("request" to "outer")
            MDC.put("outerOnly", "value")

            val nested = decorator.decorate(Runnable {
                MDC.getCopyOfContextMap() shouldBeEqualTo mapOf(
                    "request" to "outer",
                    "outerOnly" to "value",
                )
                MDC.put("innerOnly", "value")
            })
            nested.run()

            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf(
                "request" to "outer",
                "outerOnly" to "value",
            )
        })
        runOnWorker(decorated)

        workerContext() shouldBeEqualTo mapOf("worker" to "seed")
    }

    @Test
    fun `decorate 이후 caller 변경은 task snapshot에 반영되지 않는다`() {
        seedWorkerContext(mapOf("worker" to "seed"))
        MDC.setContextMap(mapOf("request" to "before"))
        val decorated = decorator.decorate(Runnable {
            MDC.getCopyOfContextMap() shouldBeEqualTo mapOf("request" to "before")
        })

        MDC.setContextMap(mapOf("request" to "after", "late" to "value"))
        runOnWorker(decorated)

        workerContext() shouldBeEqualTo mapOf("worker" to "seed")
    }

    private fun seedWorkerContext(context: Map<String, String>) {
        runOnWorker(Runnable { MDC.setContextMap(context) })
    }

    private fun workerContext(): Map<String, String> =
        executor.submit(Callable { MDC.getCopyOfContextMap().orEmpty() })
            .get(5, TimeUnit.SECONDS)

    private fun runOnWorker(task: Runnable) {
        executor.submit(task).get(5, TimeUnit.SECONDS)
    }
}
