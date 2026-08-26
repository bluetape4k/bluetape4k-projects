package io.bluetape4k.json

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.expectThat
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
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

            expectThat(PAYLOAD.size, name) { serializer.serializeTo("value", target) }

            expectThat(start + PAYLOAD.size, name) { target.position() }
            expectThat(limit, name) { target.limit() }
            expectThat(capacity, name) { target.capacity() }
            expectThat(order, name) { target.order() }
            expectThat(PAYLOAD.toList(), name) {
                target.fullBytes().copyOfRange(start, start + PAYLOAD.size).toList()
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
    fun `read-only validation precedes null serialization`() {
        val invocations = AtomicInteger()
        val serializer = jsonSerializer(serialize = {
            invocations.incrementAndGet()
            ByteArray(0)
        })
        val target = ByteBuffer.allocate(8).asReadOnlyBuffer()

        assertFailsWith<ReadOnlyBufferException> {
            serializer.serializeTo(null, target)
        }

        invocations.get() shouldBeEqualTo 0
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `empty result returns zero without changing content or position`() {
        val serializer = jsonSerializer(serialize = { ByteArray(0) })
        val target = configuredTarget(0)
        val before = target.fullBytes()

        serializer.serializeTo(null, target) shouldBeEqualTo 0

        target.position() shouldBeEqualTo 3
        target.fullBytes() shouldContentEqual before
    }

    @Test
    fun `overflow and backend failures restore position while Error identity is retained`() {
        val overflowTarget = configuredTarget(PAYLOAD.size - 1)
        val serializer = jsonSerializer(serialize = { PAYLOAD })
        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo("value", overflowTarget)
        }
        overflowTarget.position() shouldBeEqualTo 3

        val fatal = AssertionError("fatal")
        val fatalTarget = configuredTarget(PAYLOAD.size)
        val fatalSerializer = jsonSerializer(serialize = { throw fatal })
        val actual = assertFailsWith<AssertionError> {
            fatalSerializer.serializeTo("value", fatalTarget)
        }
        actual shouldBeSameInstanceAs fatal
        fatalTarget.position() shouldBeEqualTo 3

        val retry = configuredTarget(PAYLOAD.size)
        serializer.serializeTo("value", retry) shouldBeEqualTo PAYLOAD.size
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

            expectThat("decoded", name) { serializer.deserializeFrom(source, String::class.java) }
            expectThat("decoded", "$name reified") { serializer.deserialize<String>(source) }
            expectThat(PAYLOAD.toList(), name) { received?.toList() }
            expectThat(start, name) { source.position() }
            expectThat(limit, name) { source.limit() }
            expectThat(order, name) { source.order() }
            source.reset()
            expectThat(start, "$name mark") { source.position() }
        }
    }

    @Test
    fun `deserialize failure preserves source and fatal identity`() {
        val fatal = AssertionError("fatal")
        sourceBuffers(PAYLOAD).forEach { (name, source) ->
            val start = source.position()
            val limit = source.limit()
            val serializer = jsonSerializer(deserialize = { _, _ -> throw fatal })

            val actual = assertFailsWith<AssertionError> {
                serializer.deserializeFrom(source, String::class.java)
            }

            actual shouldBeSameInstanceAs fatal
            expectThat(start, name) { source.position() }
            expectThat(limit, name) { source.limit() }
            source.reset()
            expectThat(start, "$name mark") { source.position() }
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

            val failure = assertFailsWith<JsonSerializationException>(name) {
                serializer.deserializeFrom(source, String::class.java)
            }

            failure.message shouldBeEqualTo "${if (expectedBytes.isEmpty()) "empty" else "malformed"} JSON source"
            received shouldContentEqual expectedBytes
            source.position() shouldBeEqualTo start
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
            source.position() shouldBeEqualTo start
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
                serializer.serializeTo(value, target) shouldBeEqualTo expected.size
                target.flip()
                serializer.deserializeFrom(target.asReadOnlyBuffer(), String::class.java) shouldBeEqualTo value
            } else {
                val target = ByteBuffer.allocate(value.encodeToByteArray().size - 1)
                assertFailsWith<BufferOverflowException> { serializer.serializeTo(value, target) }
                target.position() shouldBeEqualTo 0

                val source = ByteBuffer.wrap(MALFORMED.copyOf()).asReadOnlyBuffer()
                assertFailsWith<JsonSerializationException> {
                    serializer.deserializeFrom(source, String::class.java)
                }
                source.position() shouldBeEqualTo 0
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
            fail("JSON buffer workers timed out; unfinished=${unfinished.sorted().take(JSON_MAX_DIAGNOSTICS)}")
        }
        if (failures.isNotEmpty()) {
            fail("JSON buffer worker failures: ${failures.take(JSON_MAX_DIAGNOSTICS)}")
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
            fail(
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
