package io.bluetape4k.exposed.r2dbc

import io.r2dbc.spi.Readable
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.ByteBuffer

class ReadableExtensionsTest {
    private class FakeReadable(
        private val valuesByIndex: Map<Int, Any?> = emptyMap(),
        private val valuesByName: Map<String, Any?> = emptyMap(),
    ): Readable {
        override fun <T: Any?> get(
            index: Int,
            type: Class<T>,
        ): T? {
            val value = valuesByIndex[index] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun <T: Any?> get(
            name: String,
            type: Class<T>,
        ): T? {
            val value = valuesByName[name] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun get(index: Int): Any? = valuesByIndex[index]

        override fun get(name: String): Any? = valuesByName[name]
    }

    @Test
    fun `getAs는 인덱스 기반 값을 타입으로 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 123))
        readable.getAs<Int>(0) shouldBeEqualTo 123
    }

    @Test
    fun `getAs는 이름 기반 값을 타입으로 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("name" to "alpha"))
        readable.getAs<String>("name") shouldBeEqualTo "alpha"
    }

    @Test
    fun `getAsOrNull은 null 값을 그대로 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("name" to null))
        readable.getAsOrNull<String>("name").shouldBeNull()
    }

    @Test
    fun `getAs는 null 값이면 상세 메시지로 예외를 던진다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(1 to null))
        val ex =
            assertThrows<IllegalStateException> {
                readable.getAs<String>(1)
            }
        ex.message shouldBeEqualTo "Column[1] is null. Expected type=String."
    }

    @Test
    fun `getExposedBlob은 byte array를 ExposedBlob으로 변환한다`() =
        runTest {
            val bytes = "blob-value".toByteArray()
            val readable = FakeReadable(valuesByName = mapOf("blob" to bytes))

            val blob = readable.getExposedBlob("blob")
            blob.bytes.toList() shouldBeEqualTo bytes.toList()
        }

    @Test
    fun `getExposedBlobOrNull은 byte buffer를 변환하고 원본 position을 보존한다`() =
        runTest {
            val buffer = ByteBuffer.wrap("abcdef".toByteArray()).apply { position(2) }
            val readable = FakeReadable(valuesByName = mapOf("blob" to buffer))

            val blob = readable.getExposedBlobOrNull("blob")
            blob.shouldNotBeNull()
            blob.bytes.toList() shouldBeEqualTo "cdef".toByteArray().toList()
            buffer.position() shouldBeEqualTo 2
        }

    @Test
    fun `getExposedBlobOrNull은 지원하지 않는 타입이면 null을 반환한다`() =
        runTest {
            val readable = FakeReadable(valuesByName = mapOf("blob" to 123))
            readable.getExposedBlobOrNull("blob").shouldBeNull()
        }

    @Test
    fun `getExposedBlob은 null 또는 미지원 타입일 때 예외를 던진다`() =
        runTest {
            val readable = FakeReadable(valuesByName = mapOf("blob" to 123))
            val ex =
                assertThrows<IllegalStateException> {
                    readable.getExposedBlob("blob")
                }
            ex.message shouldBeEqualTo "Column[blob] is null or unsupported blob value type"
        }

    @Test
    fun `getAsOrNull은 인덱스 기반 null 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getAsOrNull<Int>(0).shouldBeNull()
    }

    @Test
    fun `getAs는 이름 기반 null 값이면 상세 메시지로 예외를 던진다`() {
        val readable = FakeReadable(valuesByName = mapOf("col" to null))
        val ex =
            assertThrows<IllegalStateException> {
                readable.getAs<String>("col")
            }
        ex.message shouldBeEqualTo "Column[col] is null. Expected type=String."
    }

    @Test
    fun `getExposedBlobOrNull은 인덱스 기반 byte array를 변환한다`() =
        runTest {
            val bytes = "index-blob".toByteArray()
            val readable = FakeReadable(valuesByIndex = mapOf(0 to bytes))

            val blob = readable.getExposedBlobOrNull(0)
            blob.shouldNotBeNull()
            blob.bytes.toList() shouldBeEqualTo bytes.toList()
        }

    @Test
    fun `getExposedBlob은 인덱스 기반 null일 때 예외를 던진다`() =
        runTest {
            val readable = FakeReadable(valuesByIndex = mapOf(0 to 42))
            val ex =
                assertThrows<IllegalStateException> {
                    readable.getExposedBlob(0)
                }
            ex.message shouldBeEqualTo "Column[0] is null or unsupported blob value type"
        }
}
