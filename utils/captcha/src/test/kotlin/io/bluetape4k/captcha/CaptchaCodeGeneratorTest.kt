package io.bluetape4k.captcha

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CaptchaCodeGeneratorTest: AbstractCaptchaTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `특정 길이의 랜덤 Digit 문자열을 생성합니다`() {
        val codeGenerator = CaptchaCodeGenerator(digitOnly = true)
        val codeLength = Random.nextInt(4, 10)

        val code = codeGenerator.next(codeLength)
        log.debug { "code=`$code`" }
        code.length shouldBeEqualTo codeLength

        code.all { it.isDigit() }.shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `특정 길이의 램덤 Alpha Digit 문자열을 생성합니다`() {
        val codeGenerator = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.ALPHA_DIGITS)
        val codeLength = Random.nextInt(4, 10)

        val code = codeGenerator.next(codeLength)
        log.debug { "code=`$code`" }
        code.length shouldBeEqualTo codeLength

        code.all { it.isLetterOrDigit() }.shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `특정 길이의 램덤 Upper Digit 문자열을 생성합니다`() {
        val codeGenerator = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER_DIGITS)
        val codeLength = Random.nextInt(4, 10)

        val code = codeGenerator.next(codeLength)
        log.debug { "code=`$code`" }
        code.length shouldBeEqualTo codeLength

        code.all { it.isUpperCase() || it.isDigit() }.shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `특정 길이의 랜덤 Upper 문자열을 생성합니다`() {
        val codeGenerator = CaptchaCodeGenerator(symbols = CaptchaCodeGenerator.UPPER)
        val codeLength = Random.nextInt(4, 10)

        val code = codeGenerator.next(codeLength)
        log.debug { "code=`$code`" }
        code.length shouldBeEqualTo codeLength

        code.all { it.isUpperCase() }.shouldBeTrue()
    }

    @Test
    fun `멀티 스레딩 환경에서 안전하게 동작합니다`() {
        val codeGenerator = CaptchaCodeGenerator.DEFAULT

        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors * 4)
            .roundsPerThread(4)
            .add {
                val codeLength = Random.nextInt(4, 10)
                val code = codeGenerator.next(codeLength)
                code.all { it.isDigit() || it.isUpperCase() }.shouldBeTrue()
            }
            .run()
    }

    @Test
    fun `버추얼 스레딩 환경에서 안전하게 동작합니다`() {
        val codeGenerator = CaptchaCodeGenerator.DEFAULT

        StructuredTaskScopeTester()
            .roundsPerTask(Runtimex.availableProcessors * 4 * 4)
            .add {
                val codeLength = Random.nextInt(4, 10)
                val code = codeGenerator.next(codeLength)
                code.all { it.isDigit() || it.isUpperCase() }.shouldBeTrue()
            }
            .run()
    }

    @Test
    fun `코루틴 환경에서 안전하게 동작합니다`() = runSuspendIO {
        val codeGenerator = CaptchaCodeGenerator.DEFAULT

        SuspendedJobTester()
            .numThreads(Runtimex.availableProcessors * 4)
            .roundsPerJob(Runtimex.availableProcessors * 4 * 4)
            .add {
                val codeLength = Random.nextInt(4, 10)
                val code = codeGenerator.next(codeLength)
                code.all { it.isDigit() || it.isUpperCase() }.shouldBeTrue()
            }
            .run()
    }
}
