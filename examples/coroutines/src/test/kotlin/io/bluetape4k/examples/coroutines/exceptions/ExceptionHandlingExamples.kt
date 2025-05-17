package io.bluetape4k.examples.coroutines.exceptions

import io.bluetape4k.coroutines.support.coLogging
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

class ExceptionHandlingExamples {

    companion object: KLoggingChannel()

    private var hasException = false

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        hasException = true
        log.error(exception) { "예외가 발생했습니다." }
    }

    @BeforeEach
    fun beforeEach() {
        hasException = false
    }

    @Disabled("예외가 발생하는 launch 구역은 try/catch를 추가해야 합니다.")
    @Test
    fun `예외처리가 없는 경우에 복수의 Job을 실행하는 방식`() = runTest {
        coroutineScope {
            launch {
                launch {
                    delay(100L)
                    throw RuntimeException("Boom!")
                }
                launch {
                    delay(200L)
                    fail("이 코드까지 실행되면 안됩니다.")
                }
                launch {
                    delay(50L)
                    log.info { "이 코드는 실행되어야 합니다." }
                }
            }

            launch {
                delay(200L)
                fail("이 코드까지 실행되면 안됩니다.")
            }
        }
    }

    @Disabled("외부에 try catch를 적용해도 예외를 잡지 못합니다")
    @Test
    fun `외부에 try catch를 적용해도 예외를 잡지 못합니다`() = runTest {
        try {
            launch {
                delay(100L)
                throw RuntimeException("Boom!")
            }
        } catch (e: Throwable) {
            log.warn(e) { "예외를 catch하지 못했습니다" }
        }

        launch {
            delay(200L)
            fail("이 코드는 실행되면 안됩니다")
        }
    }

    @Test
    fun `내부에 try catch를 적용해야 예외를 잡아냅니다`() = runTest {
        var capturedException: Throwable? = null

        coroutineScope {
            launch(exceptionHandler) {
                try {
                    delay(100L)
                    throw RuntimeException("Boom!")
                } catch (e: Throwable) {
                    coLogging { "에외를 잡았습니다." }
                    e shouldBeInstanceOf RuntimeException::class
                    capturedException = e
                }
            }.log("#1")

            launch(exceptionHandler) {
                delay(200L)
            }.log("#2")
        }

        capturedException.shouldNotBeNull() shouldBeInstanceOf RuntimeException::class
    }

    /**
     * Supervisor job을 사용하면, 다른 child job이 예외를 발생시키더라도 다른 child job은 계속 실행됩니다.
     */
    @Test
    fun `SupervisorJob을 활용하여 예외처리하기`() = runTest {
        val job = SupervisorJob()
        val scope = CoroutineScope(job)
        val run2 = atomic(false)

        scope.launch(exceptionHandler) {
            delay(100L)
            coLogging { "예외가 발생합니다 ..." }
            throw RuntimeException("Boom!")
        }.log("#1")

        scope.launch(exceptionHandler) {
            delay(200L)
            run2.value = true
            coLogging { "이 코드는 실행되어야 합니다" }
        }.log("#2")

        yield()

        job.complete().shouldBeTrue()
        job.join()
        run2.value.shouldBeTrue()

        scope.cancel()
    }

    /**
     * SupervisorJob 은 child job 중 하나가 예외를 일으켜도, 다른 child job 은 실행시킵니다.
     * 이 예제는 SupervisorJob 을 잘못 적용한 예입니다.
     */
    @Test
    fun `SupervisorJob을 잘 못 사용하는 예`() = runTest {
        val secondJob = atomic(false)
        // 이렇게 주입해버리면, parent job 으로서 complete(), join() 을 못하므로 문제가 발생합니다.
        // 자식 Job 에게는 SupervisorJob 이 적용되지 않는다
        val job = launch(SupervisorJob() + exceptionHandler) {
            // 예외를 전파시켜 버립니다.
            launch {
                delay(100)
                throw RuntimeException("Boom!")
            }.log("#1")

            launch {
                // 위의 Job
                delay(200)
                secondJob.value = true
                log.error { "출력되지 않습니다." }
            }.log("#2")
        }.log("Parent")

        job.join()
        // secondJob 이 실행되지 않습니다. - SupervisorJob 잘 못 적용한 사례
        secondJob.value.shouldBeFalse()
    }

    @Test
    fun `SupervisorJob을 이용하여 예외 시에도 다른 모든 Child를 실행하기`() = runTest {
        var job2Executed = false

        val job = SupervisorJob()
        // 예외를 전파시키지 않습니다.
        launch(job + exceptionHandler) {
            delay(100)
            throw RuntimeException("Boom!")
        }.log("#1")

        launch(job + exceptionHandler) {
            delay(200)
            job2Executed = true
            log.info { "Job2는 실행됩니다." }
        }.log("#2")

        job.complete()
        job.join()
        job2Executed.shouldBeTrue()
    }

    @Test
    fun `supervisorScope 를 이용하여 예외 전파를 래핑하기`() = runTest {
        var job2Executed = false

        // supervisorScope 를 이용하면 child 간의 예외에 대한 영향을 받지 않습니다.
        val job = supervisorScope {
            // 예외가 발생하지만 전파시키지 않습니다.
            launch(exceptionHandler) {
                delay(100)
                throw RuntimeException("Boom!")
            }.log("#1")

            launch(exceptionHandler) {
                delay(200)
                job2Executed = true
                log.info { "Job2는 실행됩니다." }
            }.log("#2")
        }
        job.join()
        log.info { "Done" }
        job2Executed.shouldBeTrue()
    }

    @Test
    fun `suspervisorScope와 await 함수에서 예외 전파를 막기`() = runTest {
        // supervisorScope 환경 하에서 children 을 독립적으로 실행할 수 있습니다.
        supervisorScope {
            val str1 = async<String> {
                delay(100)
                throw RuntimeException("Boom!")
            }.log("#1")

            val str2 = async {
                delay(200)
                "Text2"
            }.log("#2")

            // supervisorScope 덕분에 str1 용 coroutines 에서 예외가 발생하더라도 str2용 async는 예외 전파없이 실행 된다.
            try {
                str1.await()
            } catch (e: Throwable) {
                log.info { "예외를 잡았습니다" }
            }
            log.info { "str2=${str2.await()}" }
            str2.await() shouldBeEqualTo "Text2"
        }
    }

    /**
     * [CancellationException]은 부모에게 전파되지 않습니다.
     */
    object MyNonPropagatingException: CancellationException()

    @Test
    fun `자식이 취소된다고 부모가 취소되지는 않습니다`() = runTest {
        var job2Executed = false

        // 자식 1
        launch {
            // 손자 1
            launch {
                delay(100)
                fail("취소되었기 때문에 실행되면 안됩니다.")
            }.log("#11")
            throw MyNonPropagatingException
        }.log("#1")

        yield()

        // 자식 2 - 자식 1과 상관없이 실행된다.
        launch {
            delay(200)
            job2Executed = true
            log.info { "Child2 는 Child1 과 상관없이 실행됩니다." }
        }.log("#2").join()

        job2Executed.shouldBeTrue()
    }

    @Test
    fun `Coroutine exception handler를 사용하여 작업하기`() = runTest {
        var hasException = false
        var job2Executed = false
        val handler = CoroutineExceptionHandler { _, exception ->
            hasException = true
            log.error(exception) { "예외가 발생했습니다." }
        }

        val job = SupervisorJob()
        val scope = CoroutineScope(job + handler)

        // 예외 발생 시 exception handler 가 처리합니다.
        scope.launch {
            delay(100)
            throw RuntimeException("Boom!")
        }.log("#1")

        scope.launch {
            delay(200)
            job2Executed = true
            coLogging { "이 코드는 출력되어야 합니다." }
        }.log("#2")

        job.complete()
        job.join()
        hasException.shouldBeTrue()
        job2Executed.shouldBeTrue()
    }
}
