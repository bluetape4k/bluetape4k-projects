package io.bluetape4k.exposed.r2dbc

import io.r2dbc.spi.Readable
import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReadableExtensionsTest {

    private class FakeReadable(
        private val valuesByIndex: Map<Int, Any?> = emptyMap(),
        private val valuesByName: Map<String, Any?> = emptyMap(),
    ): Readable {
        override fun <T: Any?> get(index: Int, type: Class<T>): T? {
            val value = valuesByIndex[index] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun <T: Any?> get(name: String, type: Class<T>): T? {
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
        assertEquals(123, readable.getAs<Int>(0))
    }

    @Test
    fun `getAs는 이름 기반 값을 타입으로 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("name" to "alpha"))
        assertEquals("alpha", readable.getAs<String>("name"))
    }

    @Test
    fun `getAsOrNull은 null 값을 그대로 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("name" to null))
        assertNull(readable.getAsOrNull<String>("name"))
    }

    @Test
    fun `getAs는 null 값이면 상세 메시지로 예외를 던진다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(1 to null))
        val ex = assertFailsWith<IllegalStateException> {
            readable.getAs<String>(1)
        }
        assertEquals("Column[1] is null. Expected type=String.", ex.message)
    }

    @Test
    fun `getExposedBlob은 byte array를 ExposedBlob으로 변환한다`() = runTest {
        val bytes = "blob-value".toByteArray()
        val readable = FakeReadable(valuesByName = mapOf("blob" to bytes))

        val blob = readable.getExposedBlob("blob")
        assertEquals(bytes.toList(), blob.bytes.toList())
    }

    @Test
    fun `getExposedBlobOrNull은 byte buffer를 변환하고 원본 position을 보존한다`() = runTest {
        val buffer = ByteBuffer.wrap("abcdef".toByteArray()).apply { position(2) }
        val readable = FakeReadable(valuesByName = mapOf("blob" to buffer))

        val blob = readable.getExposedBlobOrNull("blob")
        assertEquals("cdef".toByteArray().toList(), blob?.bytes?.toList())
        assertEquals(2, buffer.position())
    }

    @Test
    fun `getExposedBlobOrNull은 지원하지 않는 타입이면 null을 반환한다`() = runTest {
        val readable = FakeReadable(valuesByName = mapOf("blob" to 123))
        assertNull(readable.getExposedBlobOrNull("blob"))
    }

    @Test
    fun `getExposedBlob은 null 또는 미지원 타입일 때 예외를 던진다`() = runTest {
        val readable = FakeReadable(valuesByName = mapOf("blob" to 123))
        val ex = assertFailsWith<IllegalStateException> {
            readable.getExposedBlob("blob")
        }
        assertEquals("Column[blob] is null or unsupported blob value type", ex.message)
    }
}
