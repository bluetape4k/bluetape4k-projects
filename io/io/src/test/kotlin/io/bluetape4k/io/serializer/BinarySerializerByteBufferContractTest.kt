package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.assertFails
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.expectThat
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
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

            expectThat(PAYLOAD.size, name) { written }
            expectThat(start + written, name) { target.position() }
            expectThat(limit, name) { target.limit() }
            expectThat(capacity, name) { target.capacity() }
            expectThat(order, name) { target.order() }
            expectThat(PAYLOAD.toList(), name) {
                target.fullBytes().copyOfRange(start, start + written).toList()
            }
            expectThat(before.copyOfRange(0, start).toList(), name) {
                target.fullBytes().copyOfRange(0, start).toList()
            }
            expectThat(before.copyOfRange(limit, capacity).toList(), name) {
                target.fullBytes().copyOfRange(limit, capacity).toList()
            }
            target.reset()
            expectThat(start, "$name mark") { target.position() }
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

        assertFailsWith<ReadOnlyBufferException> { serializer.serializeTo(null, target) }

        invocations.get() shouldBeEqualTo 0
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `null and empty results write zero bytes`() {
        val serializer = binarySerializer(serialize = { ByteArray(0) })
        val target = configuredTarget(0)
        val before = target.fullBytes()

        serializer.serializeTo(null, target) shouldBeEqualTo 0

        target.position() shouldBeEqualTo 3
        target.fullBytes() shouldContentEqual before
    }

    @Test
    fun `overflow restores position and a larger caller buffer can retry`() {
        val serializer = binarySerializer(serialize = { PAYLOAD })
        val tooSmall = configuredTarget(PAYLOAD.size - 1)
        val start = tooSmall.position()

        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo("value", tooSmall)
        }
        tooSmall.position() shouldBeEqualTo start

        val retry = configuredTarget(PAYLOAD.size)
        serializer.serializeTo("value", retry) shouldBeEqualTo PAYLOAD.size
        retry.fullBytes().copyOfRange(3, 3 + PAYLOAD.size) shouldContentEqual PAYLOAD
    }

    @Test
    fun `ordinary and fatal backend failures restore position and preserve fatal identity`() {
        val ordinary = IllegalStateException("ordinary")
        val fatal = AssertionError("fatal")
        listOf<Throwable>(ordinary, fatal).forEach { expected ->
            val serializer = binarySerializer(serialize = { throw expected })
            val target = configuredTarget(PAYLOAD.size)
            val start = target.position()

            val actual = assertFails {
                serializer.serializeTo("value", target)
            }

            actual::class shouldBeEqualTo expected::class
            actual shouldBeSameInstanceAs expected
            target.position() shouldBeEqualTo start
        }

        val serializer = binarySerializer(serialize = { PAYLOAD })
        val cleanTarget = configuredTarget(PAYLOAD.size)
        serializer.serializeTo("value", cleanTarget) shouldBeEqualTo PAYLOAD.size
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

            expectThat("decoded", name) { serializer.deserializeFrom<String>(source) }
            expectThat(PAYLOAD.toList(), name) { received?.toList() }
            expectThat(start, name) { source.position() }
            expectThat(limit, name) { source.limit() }
            expectThat(order, name) { source.order() }
            source.reset()
            expectThat(start, "$name mark") { source.position() }
        }
    }

    @Test
    fun `deserializeFrom preserves source when the backend throws`() {
        val fatal = AssertionError("fatal")
        sourceBuffers(PAYLOAD).forEach { (name, source) ->
            val start = source.position()
            val limit = source.limit()
            val serializer = binarySerializer(deserialize = { throw fatal })

            val actual = assertFailsWith<AssertionError> {
                serializer.deserializeFrom<String>(source)
            }

            actual shouldBeSameInstanceAs fatal
            expectThat(start, name) { source.position() }
            expectThat(limit, name) { source.limit() }
            source.reset()
            expectThat(start, "$name mark") { source.position() }
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
                serializer.serializeTo(value, target) shouldBeEqualTo expected.size
                target.flip()
                serializer.deserializeFrom<String>(target.asReadOnlyBuffer()) shouldBeEqualTo value
            } else {
                val target = ByteBuffer.allocate(value.encodeToByteArray().size - 1)
                assertFailsWith<BufferOverflowException> {
                    serializer.serializeTo(value, target)
                }
                target.position() shouldBeEqualTo 0
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
