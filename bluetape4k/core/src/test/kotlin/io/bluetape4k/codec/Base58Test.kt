package io.bluetape4k.codec

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Runtimex
import net.datafaker.Faker
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.*

@RandomizedTest
class Base58Test {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
        private val faker = Fakers.faker
        private val fakerKr = Faker(Locale.KOREAN)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Base58 랜덤 문자열을 생성하면 고유한 문자열을 생성한다`() {
        val size = 100
        val strs = fastList(size) { Base58.randomString(12) }
        strs.distinct().size shouldBeEqualTo size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `짧은 문자열을 Base58로 인코딩 후 디코딩하면 원래 값과 같아야 한다`() {
        val expected: String = faker.lorem().characters()

        val encoded: String = Base58.encode(expected)
        val decoded: String = Base58.decodeAsString(encoded)

        decoded shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `긴 문자열을 Base58로 인코딩, 디코딩하면 원본과 같다`() {
        val expected = faker.lorem().paragraph()

        val encoded = Base58.encode(expected)
        val decoded = Base58.decodeAsString(encoded)

        decoded shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `한국어를 Base58로 인코딩, 디코딩하면 원본과 같다`() {
        val expected = fakerKr.lorem().paragraph()

        val encoded = Base58.encode(expected)
        val decoded = Base58.decodeAsString(encoded)

        decoded shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `UUID 를 Base58로 인코딩, 디코딩하면 원본과 같다`() {
        val uuid = UUID.randomUUID()
        val encoded = Base58.encode(uuid.toString())
        val decoded = Base58.decodeAsString(encoded)
        UUID.fromString(decoded) shouldBeEqualTo uuid
    }

    @Test
    fun `멀티 스레드 환경에서 Base58 인코딩, 디코딩하기`() {
        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(4)
            .add {
                val expected = faker.lorem().characters()
                val encoded = Base58.encode(expected)
                val decoded = Base58.decodeAsString(encoded)
                decoded shouldBeEqualTo expected
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `Virtual Threads 환경에서 Base58 인코딩, 디코딩하기`() {
        StructuredTaskScopeTester()
            .roundsPerTask(8 * Runtimex.availableProcessors)
            .add {
                val expected = faker.lorem().characters()
                val encoded = Base58.encode(expected)
                val decoded = Base58.decodeAsString(encoded)
                decoded shouldBeEqualTo expected
            }
            .run()
    }

    @Test
    fun `코루틴 환경에서 Base58 인코딩, 디코딩하기`() = runSuspendDefault {
        SuspendedJobTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerJob(8 * Runtimex.availableProcessors)
            .add {
                val expected = faker.lorem().characters()
                val encoded = Base58.encode(expected)
                val decoded = Base58.decodeAsString(encoded)
                decoded shouldBeEqualTo expected
            }
            .run()
    }
}
