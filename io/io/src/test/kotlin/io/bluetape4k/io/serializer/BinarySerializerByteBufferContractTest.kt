package io.bluetape4k.io.serializer

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.concurrent.atomic.AtomicInteger

class BinarySerializerByteBufferContractTest {

    @Test
    fun `default serializeTo preserves target metadata and writes only the bounded range`() {
        writableTargets().forEach { (name, target) ->
            val serializer = binarySerializer(serialize = { PAYLOAD })
            val before = target.fullBytes()
            val start = target.position()
            val limit = target.limit()
            val capacity = target.capacity()
            val order = target.order()

            val written = serializer.serializeTo("value", target)

            assertEquals(PAYLOAD.size, written, name)
            assertEquals(start + written, target.position(), name)
            assertEquals(limit, target.limit(), name)
            assertEquals(capacity, target.capacity(), name)
            assertEquals(order, target.order(), name)
            assertArrayEquals(PAYLOAD, target.fullBytes().copyOfRange(start, start + written), name)
            assertArrayEquals(before.copyOfRange(0, start), target.fullBytes().copyOfRange(0, start), name)
            assertArrayEquals(before.copyOfRange(limit, capacity), target.fullBytes().copyOfRange(limit, capacity), name)
            target.reset()
            assertEquals(start, target.position(), "$name mark")
        }
    }

    @Test
    fun `read-only target is rejected before the ByteArray serializer`() {
        val invocations = AtomicInteger()
        val serializer = binarySerializer(serialize = { graph ->
            invocations.incrementAndGet()
            if (graph == null) ByteArray(0) else PAYLOAD
        })
        val target = ByteBuffer.allocate(16).asReadOnlyBuffer()

        assertThrows(ReadOnlyBufferException::class.java) { serializer.serializeTo(null, target) }

        assertEquals(0, invocations.get())
        assertEquals(0, target.position())
    }

    @Test
    fun `null and empty results write zero bytes`() {
        val serializer = binarySerializer(serialize = { ByteArray(0) })
        val target = configuredTarget(0)
        val before = target.fullBytes()

        assertEquals(0, serializer.serializeTo(null, target))

        assertEquals(3, target.position())
        assertArrayEquals(before, target.fullBytes())
    }

    @Test
    fun `overflow restores position and a larger caller buffer can retry`() {
        val serializer = binarySerializer(serialize = { PAYLOAD })
        val tooSmall = configuredTarget(PAYLOAD.size - 1)
        val start = tooSmall.position()

        assertThrows(BufferOverflowException::class.java) {
            serializer.serializeTo("value", tooSmall)
        }
        assertEquals(start, tooSmall.position())

        val retry = configuredTarget(PAYLOAD.size)
        assertEquals(PAYLOAD.size, serializer.serializeTo("value", retry))
        assertArrayEquals(PAYLOAD, retry.fullBytes().copyOfRange(3, 3 + PAYLOAD.size))
    }

    @Test
    fun `ordinary and fatal backend failures restore position and preserve fatal identity`() {
        val ordinary = IllegalStateException("ordinary")
        val fatal = AssertionError("fatal")
        listOf<Throwable>(ordinary, fatal).forEach { expected ->
            val serializer = binarySerializer(serialize = { throw expected })
            val target = configuredTarget(PAYLOAD.size)
            val start = target.position()

            val actual = assertThrows(expected::class.java) {
                serializer.serializeTo("value", target)
            }

            assertSame(expected, actual)
            assertEquals(start, target.position())
        }

        val serializer = binarySerializer(serialize = { PAYLOAD })
        val cleanTarget = configuredTarget(PAYLOAD.size)
        assertEquals(PAYLOAD.size, serializer.serializeTo("value", cleanTarget))
    }

    @Test
    fun `deserializeFrom reads remaining bytes without changing source state`() {
        sourceBuffers(PAYLOAD).forEach { (name, source) ->
            val start = source.position()
            val limit = source.limit()
            val order = source.order()
            var received: ByteArray? = null
            val serializer = binarySerializer(deserialize = { bytes ->
                received = bytes
                "decoded"
            })

            assertEquals("decoded", serializer.deserializeFrom<String>(source), name)
            assertArrayEquals(PAYLOAD, received, name)
            assertEquals(start, source.position(), name)
            assertEquals(limit, source.limit(), name)
            assertEquals(order, source.order(), name)
            source.reset()
            assertEquals(start, source.position(), "$name mark")
        }
    }

    @Test
    fun `deserializeFrom preserves source when the backend throws`() {
        val fatal = AssertionError("fatal")
        sourceBuffers(PAYLOAD).forEach { (name, source) ->
            val start = source.position()
            val limit = source.limit()
            val serializer = binarySerializer(deserialize = { throw fatal })

            val actual = assertThrows(AssertionError::class.java) {
                serializer.deserializeFrom<String>(source)
            }

            assertSame(fatal, actual, name)
            assertEquals(start, source.position(), name)
            assertEquals(limit, source.limit(), name)
            source.reset()
            assertEquals(start, source.position(), "$name mark")
        }
    }

    @Test
    fun `reusable serializer supports bounded concurrent valid and invalid caller buffers`() {
        val serializer = binarySerializer(
            serialize = { graph -> graph.toString().encodeToByteArray() },
            deserialize = { bytes -> bytes?.decodeToString() },
        )

        verifySerializerBufferConcurrency { worker, repetition ->
            val value = "$worker:$repetition"
            if (repetition % 2 == 0) {
                val expected = value.encodeToByteArray()
                val target = ByteBuffer.allocate(expected.size)
                assertEquals(expected.size, serializer.serializeTo(value, target))
                target.flip()
                assertEquals(value, serializer.deserializeFrom<String>(target.asReadOnlyBuffer()))
            } else {
                val target = ByteBuffer.allocate(value.encodeToByteArray().size - 1)
                assertThrows(BufferOverflowException::class.java) {
                    serializer.serializeTo(value, target)
                }
                assertEquals(0, target.position())
            }
        }
    }

    private fun binarySerializer(
        serialize: (Any?) -> ByteArray = { PAYLOAD },
        deserialize: (ByteArray?) -> Any? = { null },
    ): BinarySerializer =
        object: BinarySerializer {
            override fun serialize(graph: Any?): ByteArray = serialize.invoke(graph)

            @Suppress("UNCHECKED_CAST")
            override fun <T: Any> deserialize(bytes: ByteArray?): T? = deserialize.invoke(bytes) as T?
        }

    private fun writableTargets(): List<Pair<String, ByteBuffer>> = listOf(
        "heap" to configuredTarget(7),
        "direct" to configuredTarget(7, direct = true),
        "slice" to configuredSliceTarget(7),
    )

    private fun configuredTarget(remaining: Int, direct: Boolean = false): ByteBuffer {
        val target = if (direct) ByteBuffer.allocateDirect(16) else ByteBuffer.allocate(16)
        repeat(target.capacity()) { target.put(FILL) }
        target.clear()
        target.order(ByteOrder.LITTLE_ENDIAN)
        target.position(3)
        target.mark()
        target.limit(3 + remaining)
        return target
    }

    private fun configuredSliceTarget(remaining: Int): ByteBuffer {
        val parent = ByteBuffer.allocate(20)
        repeat(parent.capacity()) { parent.put(FILL) }
        parent.position(2)
        parent.limit(18)
        return parent.slice().apply {
            order(ByteOrder.LITTLE_ENDIAN)
            position(3)
            mark()
            limit(3 + remaining)
        }
    }

    private fun sourceBuffers(payload: ByteArray): List<Pair<String, ByteBuffer>> = listOf(
        "heap" to configuredSource(payload, direct = false),
        "direct" to configuredSource(payload, direct = true),
        "slice" to configuredSliceSource(payload),
        "read-only" to configuredSource(payload, direct = false).asReadOnlyBuffer().apply {
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        },
    )

    private fun configuredSource(payload: ByteArray, direct: Boolean): ByteBuffer {
        val source = if (direct) ByteBuffer.allocateDirect(payload.size + 5) else ByteBuffer.allocate(payload.size + 5)
        source.put(byteArrayOf(9, 8, 7))
        source.put(payload)
        source.put(byteArrayOf(6, 5))
        source.position(3)
        source.limit(3 + payload.size)
        source.order(ByteOrder.LITTLE_ENDIAN)
        source.mark()
        return source
    }

    private fun configuredSliceSource(payload: ByteArray): ByteBuffer {
        val parent = ByteBuffer.allocate(payload.size + 7)
        parent.put(byteArrayOf(1, 2))
        parent.put(byteArrayOf(9, 8, 7))
        parent.put(payload)
        parent.put(byteArrayOf(6, 5))
        parent.flip()
        parent.position(2)
        val source = parent.slice()
        source.position(3)
        source.limit(3 + payload.size)
        source.order(ByteOrder.LITTLE_ENDIAN)
        source.mark()
        return source
    }

    private fun ByteBuffer.fullBytes(): ByteArray =
        duplicate().clear().let { view -> ByteArray(view.remaining()).also(view::get) }

    private companion object {
        val PAYLOAD = byteArrayOf(1, 2, 3, 4)
        const val FILL: Byte = 0x55
    }
}
