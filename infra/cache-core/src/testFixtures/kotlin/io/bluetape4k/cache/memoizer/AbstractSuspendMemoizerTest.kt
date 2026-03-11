package io.bluetape4k.cache.memoizer

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

abstract class AbstractSuspendMemoizerTest {

    companion object: KLoggingChannel()

    protected abstract val factorial: SuspendFactorialProvider
    protected abstract val fibonacci: SuspendFibonacciProvider

    protected abstract val heavyFunc: suspend (Int) -> Int

    @Test
    fun `run suspend heavy function`() = runSuspendDefault {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        val result = withTimeoutOrNull(10.seconds) {
            heavyFunc(10)
        }
        result shouldBeEqualTo 100
    }

    @Test
    fun `run suspend factorial`() = runSuspendDefault {
        val x1 = factorial.calc(500)

        val result = withTimeoutOrNull(1.seconds) {
            factorial.calc(500)
        }
        result shouldBeEqualTo x1
    }

    @Test
    fun `run suspend fibonacci`() = runSuspendDefault {
        val x1 = fibonacci.calc(500)

        val result = withTimeoutOrNull(1.seconds) {
            fibonacci.calc(500)
        }
        result shouldBeEqualTo x1
    }

    /**
     * 코루틴 환경에서 suspend factorial memoizer가 동일한 결과를 반환하는지 검증합니다.
     * [SuspendedJobTester]를 사용하여 동시 코루틴 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `suspend factorial memoizer는 코루틴 동시 환경에서 동일한 결과를 반환해야 한다`() = runSuspendDefault {
        val expected = factorial.calc(100)

        SuspendedJobTester()
            .workers(16)
            .rounds(4)
            .add {
                factorial.calc(100) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * 코루틴 환경에서 suspend fibonacci memoizer가 동일한 결과를 반환하는지 검증합니다.
     * [SuspendedJobTester]를 사용하여 동시 코루틴 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `suspend fibonacci memoizer는 코루틴 동시 환경에서 동일한 결과를 반환해야 한다`() = runSuspendDefault {
        val expected = fibonacci.calc(100)

        SuspendedJobTester()
            .workers(16)
            .rounds(4)
            .add {
                fibonacci.calc(100) shouldBeEqualTo expected
            }
            .run()
    }
}
