package io.bluetape4k.codec

import com.fasterxml.uuid.impl.TimeBasedEpochGenerator
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@RandomizedTest
class Base62Test {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        private val uuidGenerator = TimeBasedEpochGenerator(java.util.Random())
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Long 타입 값을 Base62 인코딩 디코딩하기`(
        @RandomValue(type = BigInteger::class, size = 20) expectes: List<BigInteger>,
    ) {
        expectes.forEach { expected ->
            Base62.decode(Base62.encode(expected)) shouldBeEqualTo expected
        }
    }

    @Test
    fun `숫자를 나타내는 다양한 문자열을 Base62로 인코딩, 디코딩하기`() {
        "00001".decodeBase62().encodeBase62() shouldBeEqualTo "1"
        "01001".decodeBase62().encodeBase62() shouldBeEqualTo "1001"
        "00abcd".decodeBase62().encodeBase62() shouldBeEqualTo "abcd"
    }

    @Test
    fun `Base62는 128bit 를 초과할 수 없습니다`() {
        assertFailsWith<IllegalArgumentException> {
            Base62.decode("1Vkp6axDWu5pI3q1xQO3oO0")
        }
    }

    @Test
    fun `잘못된 문자를 포함한 Base62 문자열은 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            Base62.decode("Foo Bar")
        }
    }

    @Test
    fun `빈 문자열은 Base62 디코딩 시 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            Base62.decode("")
        }

        assertFailsWith<IllegalArgumentException> {
            Base62.decode(" \t ")
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Long 타입의 값을 Base62 인코딩하기`() {
        val expected = Random.nextLong(0, 1000000000L)
        expected.encodeBase62().decodeBase62().toLong() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode base 62 for UUID`() {
        val expected = UUID.randomUUID()

        expected.encodeBase62().decodeBase62AsUuid() shouldBeEqualTo expected
        val encoded = Url62.encode(expected)
        Url62.decode(encoded) shouldBeEqualTo expected
    }

    @Test
    fun `멀티 스레드 환경에서 UUID 값을 Base62 인코딩, 디코딩하기`() {
        val uuids = ConcurrentHashMap<UUID, Int>()
        val base62s = ConcurrentHashMap<String, Int>()

        MultithreadingTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(64)
            .add {
                val uuid = uuidGenerator.generate()
                uuids.put(uuid, 1).shouldBeNull()
                val encoded = uuid.encodeBase62()
                base62s.put(encoded, 1).shouldBeNull()
                encoded.decodeBase62AsUuid() shouldBeEqualTo uuid
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `Virtual Threads 환경에서 UUID 값을 Base62 인코딩, 디코딩하기`() {
        val uuids = ConcurrentHashMap<UUID, Int>()
        val base62s = ConcurrentHashMap<String, Int>()

        StructuredTaskScopeTester()
            .rounds(128 * Runtimex.availableProcessors)
            .add {
                val uuid = uuidGenerator.generate()
                uuids.put(uuid, 1).shouldBeNull()
                val encoded = uuid.encodeBase62()
                base62s.put(encoded, 1).shouldBeNull()
                encoded.decodeBase62AsUuid() shouldBeEqualTo uuid
            }
            .run()
    }

    @Test
    fun `코루틴 환경에서 UUID 값을 Base62 인코딩, 디코딩하기`() = runSuspendDefault {
        val uuids = ConcurrentHashMap<UUID, Int>()
        val base62s = ConcurrentHashMap<String, Int>()

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(128 * Runtimex.availableProcessors)
            .add {
                val uuid = uuidGenerator.generate()
                uuids.put(uuid, 1).shouldBeNull()
                val encoded = uuid.encodeBase62()
                base62s.put(encoded, 1).shouldBeNull()
                encoded.decodeBase62AsUuid() shouldBeEqualTo uuid
            }
            .run()
    }
}
