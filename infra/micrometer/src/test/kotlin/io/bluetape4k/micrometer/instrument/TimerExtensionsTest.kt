package io.bluetape4k.micrometer.instrument

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TimerExtensionsTest: AbstractMicrometerTest() {

    companion object: KLoggingChannel() {
        private const val DELAY_TIME = 100L
    }

    private lateinit var registry: SimpleMeterRegistry

    @BeforeEach
    fun beforeEach() {
        registry = SimpleMeterRegistry()
    }

    @AfterEach
    fun afterEach() {
        registry.close()
    }

    // NOTE: 실제 시간을 측정하기 위해서는 `runTest` 대신 `runSuspendIO`를 사용합니다.
    // NOTE: `runTest` 는 실행 시간을 emulation 해서 테스트를 빠르게 하기 위함입니다.
    @Test
    fun `measure time for suspend function`() = runSuspendIO {
        val timer = registry.timer("suspend.timer")

        repeat(5) {
            val result = timer.recordSuspend {
                delay(DELAY_TIME)
                "result"
            }
            result shouldBeEqualTo "result"

            log.debug { "timer max  =${timer.max(TimeUnit.MILLISECONDS)}" }
            log.debug { "timer total=${timer.totalTime(TimeUnit.MILLISECONDS)}" }
            timer.max(TimeUnit.MILLISECONDS).toLong() shouldBeGreaterThan DELAY_TIME
        }
    }

    @Test
    fun `measure time for jobs`() = runSuspendIO {
        val timer = registry.timer("job.timer")

        repeat(5) {
            timer.recordSuspend {
                val jobs = List(10) {
                    launch {
                        delay(DELAY_TIME)
                        log.debug { "Complete Job $it" }
                    }
                }
                jobs.joinAll()
            }

            log.debug { "timer max  =${timer.max(TimeUnit.MILLISECONDS)}" }
            log.debug { "timer total=${timer.totalTime(TimeUnit.MILLISECONDS)}" }
            timer.max(TimeUnit.MILLISECONDS).toLong() shouldBeGreaterThan DELAY_TIME
        }
    }

    @Test
    fun `measure time for flow`() = runSuspendIO {
        val timer = registry.timer("flow.timer")

        repeat(5) {
            val flow = flow {
                repeat(10) {
                    delay(DELAY_TIME)
                    emit(it)
                }
            }

            val list = flow
                .buffer(4)
                .withTimer(timer)
                .onEach {
                    log.debug { "collect $it" }
                }
                .toFastList()

            list shouldHaveSize 10

            log.debug { "timer max  =${timer.max(TimeUnit.MILLISECONDS)}" }
            log.debug { "timer total=${timer.totalTime(TimeUnit.MILLISECONDS)}" }
            timer.totalTime(TimeUnit.MILLISECONDS).toLong() shouldBeGreaterThan DELAY_TIME
        }
    }
}
