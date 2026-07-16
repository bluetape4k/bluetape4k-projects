package io.bluetape4k.json

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class JsonSerializerByteBufferContractTest {

    @Test
    fun `default serializeTo writes from position without changing target bounds or order`() {
        writableTargets().forEach { (name, target) ->
            val serializer = jsonSerializer(serialize = { PAYLOAD })
            val before = target.fullBytes()
            val start = target.position()
            val limit = target.limit()
            val capacity = target.capacity()
            val order = target.order()

            assertEquals(PAYLOAD.size, serializer.serializeTo("value", target), name)

            assertEquals(start + PAYLOAD.size, target.position(), name)
            assertEquals(limit, target.limit(), name)
            assertEquals(capacity, target.capacity(), name)
            assertEquals(order, target.order(), name)
            assertArrayEquals(PAYLOAD, target.fullBytes().copyOfRange(start, start + PAYLOAD.size), name)
            assertArrayEquals(before.copyOfRange(0, start), target.fullBytes().copyOfRange(0, start), name)
            assertArrayEquals(before.copyOfRange(limit, capacity), target.fullBytes().copyOfRange(limit, capacity), name)
            target.reset()
            assertEquals(start, target.position(), "$name mark")
        }
    }

    @Test
    fun `read-only validation precedes null serialization`() {
        val invocations = AtomicInteger()
        val serializer = jsonSerializer(serialize = {
            invocations.incrementAndGet()
            ByteArray(0)
        })
        val target = ByteBuffer.allocate(8).asReadOnlyBuffer()

        assertThrows(ReadOnlyBufferException::class.java) {
            serializer.serializeTo(null, target)
        }

        assertEquals(0, invocations.get())
        assertEquals(0, target.position())
    }

    @Test
    fun `empty result returns zero without changing content or position`() {
        val serializer = jsonSerializer(serialize = { ByteArray(0) })
        val target = configuredTarget(0)
        val before = target.fullBytes()

        assertEquals(0, serializer.serializeTo(null, target))

        assertEquals(3, target.position())
        assertArrayEquals(before, target.fullBytes())
    }

    @Test
    fun `overflow and backend failures restore position while Error identity is retained`() {
        val overflowTarget = configuredTarget(PAYLOAD.size - 1)
        val serializer = jsonSerializer(serialize = { PAYLOAD })
        assertThrows(BufferOverflowException::class.java) {
            serializer.serializeTo("value", overflowTarget)
        }
        assertEquals(3, overflowTarget.position())

        val fatal = AssertionError("fatal")
        val fatalTarget = configuredTarget(PAYLOAD.size)
        val fatalSerializer = jsonSerializer(serialize = { throw fatal })
        val actual = assertThrows(AssertionError::class.java) {
            fatalSerializer.serializeTo("value", fatalTarget)
        }
        assertSame(fatal, actual)
        assertEquals(3, fatalTarget.position())

        val retry = configuredTarget(PAYLOAD.size)
        assertEquals(PAYLOAD.size, serializer.serializeTo("value", retry))
    }

    @Test
    fun `deserializeFrom and Kotlin facades preserve every source shape`() {
        sourceBuffers(PAYLOAD).forEach { (name, source) ->
            val start = source.position()
            val limit = source.limit()
            val order = source.order()
            var received: ByteArray? = null
            val serializer = jsonSerializer(deserialize = { bytes, _ ->
                received = bytes
                "decoded"
            })

            assertEquals("decoded", serializer.deserializeFrom(source, String::class.java), name)
            assertEquals("decoded", serializer.deserialize<String>(source), "$name reified")
            assertArrayEquals(PAYLOAD, received, name)
            assertEquals(start, source.position(), name)
            assertEquals(limit, source.limit(), name)
            assertEquals(order, source.order(), name)
            source.reset()
            assertEquals(start, source.position(), "$name mark")
        }
    }

    @Test
    fun `deserialize failure preserves source and fatal identity`() {
        val fatal = AssertionError("fatal")
        sourceBuffers(PAYLOAD).forEach { (name, source) ->
            val start = source.position()
            val limit = source.limit()
            val serializer = jsonSerializer(deserialize = { _, _ -> throw fatal })

            val actual = assertThrows(AssertionError::class.java) {
                serializer.deserializeFrom(source, String::class.java)
            }

            assertSame(fatal, actual, name)
            assertEquals(start, source.position(), name)
            assertEquals(limit, source.limit(), name)
            source.reset()
            assertEquals(start, source.position(), "$name mark")
        }
    }

    @Test
    fun `empty and malformed sources preserve state while JSON exceptions propagate`() {
        sourcePolicyCases().forEach { (name, expectedBytes, source) ->
            val start = source.position()
            val limit = source.limit()
            val order = source.order()
            var received: ByteArray? = null
            val serializer = jsonSerializer(deserialize = { bytes, _ ->
                received = bytes
                val kind = if (bytes == null || bytes.isEmpty()) "empty" else "malformed"
                throw JsonSerializationException("$kind JSON source")
            })

            val failure = assertThrows(JsonSerializationException::class.java, {
                serializer.deserializeFrom(source, String::class.java)
            }, name)

            assertEquals("${if (expectedBytes.isEmpty()) "empty" else "malformed"} JSON source", failure.message, name)
            assertArrayEquals(expectedBytes, received, name)
            assertEquals(start, source.position(), name)
            assertEquals(limit, source.limit(), name)
            assertEquals(order, source.order(), name)
            source.reset()
            assertEquals(start, source.position(), "$name mark")
        }
    }

    @Test
    fun `reusable JSON serializer supports bounded concurrent valid and invalid calls`() {
        val serializer = jsonSerializer(
            serialize = { graph -> graph.toString().encodeToByteArray() },
            deserialize = { bytes, _ ->
                if (bytes.contentEquals(MALFORMED)) throw JsonSerializationException("malformed JSON source")
                bytes?.decodeToString()
            },
        )

        verifyJsonBufferConcurrency { worker, repetition ->
            val value = "$worker:$repetition"
            if (repetition % 2 == 0) {
                val expected = value.encodeToByteArray()
                val target = ByteBuffer.allocate(expected.size)
                assertEquals(expected.size, serializer.serializeTo(value, target))
                target.flip()
                assertEquals(value, serializer.deserializeFrom(target.asReadOnlyBuffer(), String::class.java))
            } else {
                val target = ByteBuffer.allocate(value.encodeToByteArray().size - 1)
                assertThrows(BufferOverflowException::class.java) { serializer.serializeTo(value, target) }
                assertEquals(0, target.position())

                val source = ByteBuffer.wrap(MALFORMED.copyOf()).asReadOnlyBuffer()
                assertThrows(JsonSerializationException::class.java) {
                    serializer.deserializeFrom(source, String::class.java)
                }
                assertEquals(0, source.position())
            }
        }
    }

    private fun jsonSerializer(
        serialize: (Any?) -> ByteArray = { PAYLOAD },
        deserialize: (ByteArray?, Class<*>) -> Any? = { _, _ -> null },
    ): JsonSerializer =
        object: JsonSerializer {
            override fun serialize(graph: Any?): ByteArray = serialize.invoke(graph)

            @Suppress("UNCHECKED_CAST")
            override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? =
                deserialize.invoke(bytes, clazz) as T?
        }

    private fun writableTargets(): List<Pair<String, ByteBuffer>> = listOf(
        "heap" to configuredTarget(6),
        "direct" to configuredTarget(6, direct = true),
        "slice" to configuredSliceTarget(6),
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

    private fun sourcePolicyCases(): List<Triple<String, ByteArray, ByteBuffer>> =
        listOf(
            "empty" to ByteArray(0),
            "malformed" to MALFORMED,
        ).flatMap { (kind, bytes) ->
            sourceBuffers(bytes).map { (shape, source) -> Triple("$kind $shape", bytes, source) }
        }

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
        parent.put(byteArrayOf(1, 2, 9, 8, 7))
        parent.put(payload)
        parent.put(byteArrayOf(6, 5))
        parent.flip()
        parent.position(2)
        return parent.slice().apply {
            position(3)
            limit(3 + payload.size)
            order(ByteOrder.LITTLE_ENDIAN)
            mark()
        }
    }

    private fun ByteBuffer.fullBytes(): ByteArray =
        duplicate().clear().let { view -> ByteArray(view.remaining()).also(view::get) }

    private companion object {
        val PAYLOAD = byteArrayOf(11, 12, 13, 14)
        val MALFORMED = byteArrayOf(0x7F, 0x01, 0x02)
        const val FILL: Byte = 0x44
    }
}

private fun verifyJsonBufferConcurrency(
    operation: (worker: Int, repetition: Int) -> Unit,
) {
    val threadSequence = AtomicInteger()
    val executor = Executors.newFixedThreadPool(JSON_WORKERS) { task ->
        Thread(task, "json-buffer-${threadSequence.incrementAndGet()}")
    }
    val startBarrier = CyclicBarrier(JSON_WORKERS + 1)
    val completion = CountDownLatch(JSON_WORKERS)
    val unfinished = ConcurrentHashMap.newKeySet<Int>()
    val failures = ConcurrentLinkedQueue<String>()

    try {
        repeat(JSON_WORKERS) { worker ->
            unfinished += worker
            executor.submit {
                try {
                    startBarrier.await(JSON_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    repeat(JSON_REPETITIONS) { repetition -> operation(worker, repetition) }
                } catch (failure: Throwable) {
                    failures += "worker=$worker ${failure::class.java.simpleName}: ${failure.message}"
                } finally {
                    unfinished -= worker
                    completion.countDown()
                }
            }
        }

        startBarrier.await(JSON_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completion.await(JSON_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            fail<Unit>("JSON buffer workers timed out; unfinished=${unfinished.sorted().take(JSON_MAX_DIAGNOSTICS)}")
        }
        if (failures.isNotEmpty()) {
            fail<Unit>("JSON buffer worker failures: ${failures.take(JSON_MAX_DIAGNOSTICS)}")
        }
    } finally {
        executor.shutdownNow()
        if (!executor.awaitTermination(JSON_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            val threads =
                Thread.getAllStackTraces().keys.asSequence()
                    .filter { it.name.startsWith("json-buffer-") }
                    .map { it.name }
                    .take(JSON_MAX_DIAGNOSTICS)
                    .toList()
            fail<Unit>(
                "JSON buffer executor did not terminate; " +
                    "unfinished=${unfinished.sorted().take(JSON_MAX_DIAGNOSTICS)}, threads=$threads",
            )
        }
    }
}

private const val JSON_WORKERS = 8
private const val JSON_REPETITIONS = 50
private const val JSON_START_TIMEOUT_SECONDS = 2L
private const val JSON_COMPLETION_TIMEOUT_SECONDS = 15L
private const val JSON_SHUTDOWN_TIMEOUT_SECONDS = 5L
private const val JSON_MAX_DIAGNOSTICS = 8
