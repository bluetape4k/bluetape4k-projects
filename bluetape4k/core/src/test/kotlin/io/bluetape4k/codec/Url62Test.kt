package io.bluetape4k.codec

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.*
import kotlin.test.assertFailsWith

@RandomizedTest
class Url62Test {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `UUID를 Base62로 인코딩,디코딩을 수행한다`(@RandomValue(type = UUID::class, size = 20) uuids: List<UUID>) {
        uuids.forEach { uuid ->
            val encoded = uuid.encodeUrl62()
            log.trace { "uuid=$uuid, encoded=$encoded" }
            encoded.decodeUrl62() shouldBeEqualTo uuid
        }
    }

    @Test
    fun `잘못된 문자열을 Url62 디코딩을 하면 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            Url62.decode("Foo Bar")
        }
    }

    @Test
    fun `빈 문자열을 디코딩하면 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            Url62.decode("")
        }

        assertFailsWith<IllegalArgumentException> {
            Url62.decode(" \t ")
        }
    }

    @Test
    fun `128 bit 이상의 문자열은 디코딩할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            Url62.decode("7NLCAyd6sKR7kDHxgAWFPas")
        }
    }

    @Test
    fun `멀티 스레드 환경에서 인코딩, 디코딩을 한다`() {
        MultithreadingTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(4)
            .add {
                val url = UUID.randomUUID()
                val converted = url.encodeUrl62().decodeUrl62()
                converted shouldBeEqualTo url
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `Virtual Threads 환경에서 인코딩, 디코딩을 한다`() {
        StructuredTaskScopeTester()
            .rounds(8 * Runtimex.availableProcessors)
            .add {
                val url = UUID.randomUUID()
                val converted = url.encodeUrl62().decodeUrl62()
                converted shouldBeEqualTo url
            }
            .run()
    }

    @Test
    fun `코루틴 환경에서 인코딩, 디코딩을 한다`() = runSuspendDefault {
        SuspendedJobTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(8 * Runtimex.availableProcessors)
            .add {
                val url = UUID.randomUUID()
                val converted = url.encodeUrl62().decodeUrl62()
                converted shouldBeEqualTo url
            }
            .run()
    }
}
