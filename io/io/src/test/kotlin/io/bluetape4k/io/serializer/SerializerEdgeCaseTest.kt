package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.test.assertFailsWith

/**
 * [BinarySerializer] 구현체들의 edge case 테스트입니다.
 *
 * 단일 바이트, 중첩 컬렉션, nullable 필드, 멀티스레드 안전성 등을 검증합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializerEdgeCaseTest {

    companion object : KLogging() {
        private const val THREAD_COUNT = 8

        @JvmStatic
        fun allSerializers(): Stream<BinarySerializer> = Stream.of(
            JdkBinarySerializer(),
            KryoBinarySerializer(),
            ForyBinarySerializer(),
        )

        @JvmStatic
        fun kryoAndFory(): Stream<BinarySerializer> = Stream.of(
            KryoBinarySerializer(),
            ForyBinarySerializer(),
        )
    }

    data class NestedData(
        val id: Long,
        val name: String,
        val children: List<ChildData>,
        val metadata: Map<String, String>,
    ) : Serializable

    data class ChildData(
        val value: Int,
        val label: String?,
    ) : Serializable

    data class WithNullableFields(
        val id: Long,
        val optionalName: String? = null,
        val optionalValue: Int? = null,
    ) : Serializable

    data class WithJavaTimes(
        val instant: Instant,
        val localDate: LocalDate,
        val localDateTime: LocalDateTime,
    ) : Serializable

    @ParameterizedTest(name = "null 직렬화는 emptyByteArray 를 반환한다: {0}")
    @MethodSource("allSerializers")
    fun `null 직렬화는 emptyByteArray 를 반환한다`(serializer: BinarySerializer) {
        val bytes = serializer.serialize(null)
        bytes shouldBeEqualTo byteArrayOf()
    }

    @ParameterizedTest(name = "null/empty 역직렬화는 null 을 반환한다: {0}")
    @MethodSource("allSerializers")
    fun `null 또는 empty 역직렬화는 null 을 반환한다`(serializer: BinarySerializer) {
        serializer.deserialize<String>(null).shouldBeNull()
        serializer.deserialize<String>(byteArrayOf()).shouldBeNull()
    }

    @ParameterizedTest(name = "빈 문자열 직렬화/역직렬화: {0}")
    @MethodSource("allSerializers")
    fun `빈 문자열을 직렬화하고 역직렬화한다`(serializer: BinarySerializer) {
        val input = ""
        val bytes = serializer.serialize(input)
        bytes.shouldNotBeEmpty()

        val result = serializer.deserialize<String>(bytes)
        result.shouldNotBeNull() shouldBeEqualTo input
    }

    @ParameterizedTest(name = "중첩 데이터 클래스 직렬화/역직렬화: {0}")
    @MethodSource("allSerializers")
    fun `중첩 데이터 클래스를 직렬화하고 역직렬화한다`(serializer: BinarySerializer) {
        val input = NestedData(
            id = 42L,
            name = "Test Nested",
            children = listOf(
                ChildData(1, "first"),
                ChildData(2, null),
                ChildData(3, "third"),
            ),
            metadata = mapOf("key1" to "value1", "key2" to "value2"),
        )

        val bytes = serializer.serialize(input)
        bytes.shouldNotBeEmpty()

        val result = serializer.deserialize<NestedData>(bytes)
        result.shouldNotBeNull() shouldBeEqualTo input
    }

    @ParameterizedTest(name = "nullable 필드를 가진 데이터 클래스 직렬화/역직렬화: {0}")
    @MethodSource("allSerializers")
    fun `nullable 필드가 있는 데이터 클래스를 직렬화하고 역직렬화한다`(serializer: BinarySerializer) {
        val withNull = WithNullableFields(id = 1L, optionalName = null, optionalValue = null)
        val withValues = WithNullableFields(id = 2L, optionalName = "hello", optionalValue = 42)

        for (input in listOf(withNull, withValues)) {
            val bytes = serializer.serialize(input)
            bytes.shouldNotBeEmpty()
            val result = serializer.deserialize<WithNullableFields>(bytes)
            result.shouldNotBeNull() shouldBeEqualTo input
        }
    }

    @ParameterizedTest(name = "Java Time 타입 직렬화/역직렬화: {0}")
    @MethodSource("kryoAndFory")
    fun `Java Time 타입을 직렬화하고 역직렬화한다`(serializer: BinarySerializer) {
        val input = WithJavaTimes(
            instant = Instant.parse("2025-01-15T10:30:00Z"),
            localDate = LocalDate.of(2025, 1, 15),
            localDateTime = LocalDateTime.of(2025, 1, 15, 10, 30, 0),
        )

        val bytes = serializer.serialize(input)
        bytes.shouldNotBeEmpty()

        val result = serializer.deserialize<WithJavaTimes>(bytes)
        result.shouldNotBeNull() shouldBeEqualTo input
    }

    @ParameterizedTest(name = "빈 컬렉션 직렬화/역직렬화: {0}")
    @MethodSource("allSerializers")
    fun `빈 리스트와 맵을 직렬화하고 역직렬화한다`(serializer: BinarySerializer) {
        val emptyList = emptyList<String>()
        val emptyMap = emptyMap<String, Int>()

        val listBytes = serializer.serialize(emptyList)
        val mapBytes = serializer.serialize(emptyMap)

        listBytes.shouldNotBeEmpty()
        mapBytes.shouldNotBeEmpty()

        val resultList = serializer.deserialize<List<String>>(listBytes)
        val resultMap = serializer.deserialize<Map<String, Int>>(mapBytes)

        resultList.shouldNotBeNull() shouldBeEqualTo emptyList
        resultMap.shouldNotBeNull() shouldBeEqualTo emptyMap
    }

    @ParameterizedTest(name = "대용량 컬렉션 직렬화/역직렬화: {0}")
    @MethodSource("allSerializers")
    fun `대용량 리스트를 직렬화하고 역직렬화한다`(serializer: BinarySerializer) {
        val input = (1..10_000).map { "item-$it" }
        val bytes = serializer.serialize(input)
        bytes.shouldNotBeEmpty()

        val result = serializer.deserialize<List<String>>(bytes)
        result.shouldNotBeNull() shouldBeEqualTo input

        log.debug { "${serializer.javaClass.simpleName} 10k items: ${bytes.size} bytes" }
    }

    @ParameterizedTest(name = "멀티스레드 안전성: {0}")
    @MethodSource("allSerializers")
    fun `멀티스레드 환경에서 동시 직렬화가 안전하게 동작한다`(serializer: BinarySerializer) {
        data class Item(val id: Int, val name: String) : Serializable

        val executor = Executors.newFixedThreadPool(THREAD_COUNT)
        val latch = CountDownLatch(THREAD_COUNT)
        val errorCount = java.util.concurrent.atomic.AtomicInteger(0)
        val results = java.util.concurrent.ConcurrentHashMap<Int, Item>()

        repeat(THREAD_COUNT) { idx ->
            val item = Item(idx, "thread-$idx")
            executor.submit {
                try {
                    val bytes = serializer.serialize(item)
                    val deserialized = serializer.deserialize<Item>(bytes)
                    if (deserialized != null) results[idx] = deserialized
                } catch (e: Throwable) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        errorCount.get() shouldBeEqualTo 0
        results.size shouldBeEqualTo THREAD_COUNT
        repeat(THREAD_COUNT) { idx ->
            results[idx].shouldNotBeNull()
            results[idx]!!.id shouldBeEqualTo idx
        }
    }

    @Test
    fun `KryoBinarySerializer 보안 모드는 미등록 클래스 직렬화 시 예외를 던진다`() {
        data class Registered(val value: String) : Serializable
        data class Unregistered(val value: String) : Serializable

        val secureSerializer = KryoBinarySerializer.secure(Registered::class.java)

        // 등록된 클래스는 직렬화 가능
        val bytes = secureSerializer.serialize(Registered("hello"))
        bytes.shouldNotBeEmpty()
        val result = secureSerializer.deserialize<Registered>(bytes)
        result.shouldNotBeNull()

        // 미등록 클래스 직렬화 시 예외 발생
        assertFailsWith<BinarySerializationException> {
            secureSerializer.serialize(Unregistered("fail"))
        }
    }

    @Test
    fun `JdkBinarySerializer bufferSize 가 0 이하이면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> {
            JdkBinarySerializer(bufferSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            JdkBinarySerializer(bufferSize = -1)
        }
    }

    @Test
    fun `KryoBinarySerializer bufferSize 1 로도 정상 직렬화된다`() {
        val serializer = KryoBinarySerializer(bufferSize = 1)
        val input = "small-buffer-test"
        val bytes = serializer.serialize(input)
        serializer.deserialize<String>(bytes) shouldBeEqualTo input
    }

    @Test
    fun `KryoProvider 는 Pool 을 통한 Kryo 인스턴스를 안전하게 대여하고 반납한다`() {
        // 대여 후 반납이 올바르게 동작하는지 확인
        val kryo = KryoProvider.obtainKryo()
        kryo.shouldNotBeNull()
        KryoProvider.releaseKryo(kryo)

        val input = KryoProvider.obtainInput()
        input.shouldNotBeNull()
        KryoProvider.releaseInput(input)

        val output = KryoProvider.obtainOutput()
        output.shouldNotBeNull()
        KryoProvider.releaseOutput(output)
    }

    @Test
    fun `KryoProvider 멀티스레드 환경에서 안전하게 동작한다`() {
        val executor = Executors.newFixedThreadPool(THREAD_COUNT)
        val latch = CountDownLatch(THREAD_COUNT)
        val errorCount = java.util.concurrent.atomic.AtomicInteger(0)

        repeat(THREAD_COUNT) { idx ->
            executor.submit {
                try {
                    val kryo = KryoProvider.obtainKryo()
                    try {
                        // Kryo로 간단한 직렬화 수행
                        val output = KryoProvider.obtainOutput()
                        try {
                            kryo.writeObject(output, "thread-safe-test-$idx")
                        } finally {
                            KryoProvider.releaseOutput(output)
                        }
                    } finally {
                        KryoProvider.releaseKryo(kryo)
                    }
                } catch (e: Throwable) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()
        errorCount.get() shouldBeEqualTo 0
    }
}
