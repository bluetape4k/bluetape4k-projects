package io.bluetape4k.codec

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import kotlin.random.Random

@RandomizedTest
abstract class AbstractStringEncoderTest {


    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    protected abstract val encoder: StringEncoder

    @Test
    fun `null 또는 빈 byte array 를 인코딩하면 빈문자열을 반환한다`() {
        encoder.encode(null).shouldBeEmpty()
        encoder.encode(ByteArray(0)).shouldBeEmpty()
    }

    @Test
    fun `null,빈 문자열, 블랭크 문자열을 디코딩하면 빈문자열을 반환한다`() {
        encoder.decode(null).shouldBeEmpty()
        encoder.decode("").shouldBeEmpty()
        encoder.decode(" \t ").shouldBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `문자열을 인코딩, 디코딩 하면 원본 문자열과 같아야 한다`(@RandomValue expected: String) {

        val encoded = encoder.encode(expected.toUtf8Bytes())
        val decoded = encoder.decode(encoded)

        decoded.toUtf8String() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `랜덤 바이트 배열을 인코딩,디코딩 하면 원본과 같아야 한다`(@RandomValue bytes: ByteArray) {

        val encoded = encoder.encode(bytes)
        val decoded = encoder.decode(encoded)

        decoded shouldContainSame bytes
    }

    @Test
    fun `멀티 스레드 환경에서 인코딩, 디코딩 하면 원본과 같아야 한다`() {
        val bytes = Random.nextBytes(4096)

        MultithreadingTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(4)
            .add {
                val converted = encoder.decode(encoder.encode(bytes))
                converted shouldContainSame bytes
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `Virtual Thread 환경에서 인코딩, 디코딩 하면 원본과 같아야 한다`() {
        val bytes = Random.nextBytes(4096)

        StructuredTaskScopeTester()
            .rounds(8 * Runtimex.availableProcessors)
            .add {
                val converted = encoder.decode(encoder.encode(bytes))
                converted shouldContainSame bytes
            }
            .run()
    }

    @Test
    fun `코루틴 환경에서 인코딩, 디코딩 하면 원본과 같아야 한다`() = runSuspendDefault {
        val bytes = Random.nextBytes(4096)

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(8 * Runtimex.availableProcessors)
            .add {
                val converted = encoder.decode(encoder.encode(bytes))
                converted shouldContainSame bytes
            }
            .run()
    }
}
