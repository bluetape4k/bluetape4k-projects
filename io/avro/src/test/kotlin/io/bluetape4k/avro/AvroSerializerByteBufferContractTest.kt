package io.bluetape4k.avro

import io.bluetape4k.avro.message.examples.Employee
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
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

class AvroSerializerByteBufferContractTest {

    @Test
    fun `all Avro output defaults write within caller bounds and preserve metadata`() {
        outputOperations().forEach { (name, operation) ->
            writableTargets().forEach { (shape, target) ->
                val before = target.fullBytes()
                val start = target.position()
                val limit = target.limit()
                val capacity = target.capacity()
                val order = target.order()

                assertEquals(PAYLOAD.size, operation(target), "$name $shape")

                assertEquals(start + PAYLOAD.size, target.position(), "$name $shape")
                assertEquals(limit, target.limit(), "$name $shape")
                assertEquals(capacity, target.capacity(), "$name $shape")
                assertEquals(order, target.order(), "$name $shape")
                assertArrayEquals(PAYLOAD, target.fullBytes().copyOfRange(start, start + PAYLOAD.size), "$name $shape")
                assertArrayEquals(before.copyOfRange(0, start), target.fullBytes().copyOfRange(0, start), "$name $shape")
                assertArrayEquals(
                    before.copyOfRange(limit, capacity),
                    target.fullBytes().copyOfRange(limit, capacity),
                    "$name $shape",
                )
                target.reset()
                assertEquals(start, target.position(), "$name $shape mark")
            }
        }
    }

    @Test
    fun `all Avro null and empty output policies return zero`() {
        val reflect = ReflectFake(serializeResult = null)
        val generic = GenericFake(serializeResult = null)
        val specific = SpecificFake(serializeResult = null, listResult = null)
        val operations = listOf<Pair<String, (ByteBuffer) -> Int>>(
            "reflect null" to { target -> reflect.serializeTo<String>(null, target) },
            "generic null" to { target -> generic.serializeTo(SCHEMA, null, target) },
            "specific null" to { target -> specific.serializeTo<Employee>(null, target) },
            "specific list null" to { target -> specific.serializeListTo<Employee>(null, target) },
            "specific list empty" to { target -> specific.serializeListTo(emptyList<Employee>(), target) },
        )

        operations.forEach { (name, operation) ->
            val target = configuredTarget(0)
            val before = target.fullBytes()
            assertEquals(0, operation(target), name)
            assertEquals(3, target.position(), name)
            assertArrayEquals(before, target.fullBytes(), name)
        }
    }

    @Test
    fun `read-only preflight rejects every Avro output before invoking ByteArray siblings`() {
        val reflect = ReflectFake(serializeResult = null)
        val generic = GenericFake(serializeResult = null)
        val specific = SpecificFake(serializeResult = null, listResult = null)
        val operations = listOf<Pair<String, (ByteBuffer) -> Int>>(
            "reflect" to { target -> reflect.serializeTo<String>(null, target) },
            "generic" to { target -> generic.serializeTo(SCHEMA, null, target) },
            "specific" to { target -> specific.serializeTo<Employee>(null, target) },
            "specific list" to { target -> specific.serializeListTo<Employee>(emptyList(), target) },
        )

        operations.forEach { (name, operation) ->
            assertThrows(ReadOnlyBufferException::class.java, { operation(ByteBuffer.allocate(8).asReadOnlyBuffer()) }, name)
        }
        assertEquals(0, reflect.serializeInvocations)
        assertEquals(0, generic.serializeInvocations)
        assertEquals(0, specific.serializeInvocations)
        assertEquals(0, specific.listInvocations)
    }

    @Test
    fun `all Avro output defaults restore position on overflow`() {
        outputOperations().forEach { (name, operation) ->
            val target = configuredTarget(PAYLOAD.size - 1)

            assertThrows(BufferOverflowException::class.java, { operation(target) }, name)

            assertEquals(3, target.position(), name)
        }

        outputOperations().forEach { (name, operation) ->
            val retry = configuredTarget(PAYLOAD.size)
            assertEquals(PAYLOAD.size, operation(retry), "$name retry")
        }
    }

    @Test
    fun `all Avro output defaults restore position and preserve fatal identity`() {
        val fatal = AssertionError("fatal")
        val reflect = ReflectFake(outputFailure = fatal)
        val generic = GenericFake(outputFailure = fatal)
        val specific = SpecificFake(outputFailure = fatal)
        val operations = listOf<Pair<String, (ByteBuffer) -> Int>>(
            "reflect" to { target -> reflect.serializeTo("value", target) },
            "generic" to { target -> generic.serializeTo(SCHEMA, GENERIC_RECORD, target) },
            "specific" to { target -> specific.serializeTo(EMPLOYEE, target) },
            "specific list" to { target -> specific.serializeListTo(listOf(EMPLOYEE), target) },
        )

        operations.forEach { (name, operation) ->
            val target = configuredTarget(PAYLOAD.size)
            val actual = assertThrows(AssertionError::class.java, { operation(target) }, name)
            assertSame(fatal, actual, name)
            assertEquals(3, target.position(), name)
        }
    }

    @Test
    fun `all Avro input defaults and Kotlin facades preserve source state`() {
        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val reflect = ReflectFake(deserializeResult = "decoded")
            assertSourcePreserved("reflect $shape", source) {
                assertEquals("decoded", reflect.deserializeFrom(it, String::class.java))
                assertEquals("decoded", reflect.deserialize<String>(it))
            }
            assertArrayEquals(PAYLOAD, reflect.received)
        }

        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val generic = GenericFake(deserializeResult = GENERIC_RECORD)
            assertSourcePreserved("generic $shape", source) {
                assertSame(GENERIC_RECORD, generic.deserializeFrom(SCHEMA, it))
                assertSame(GENERIC_RECORD, generic.deserialize(SCHEMA, it))
            }
            assertArrayEquals(PAYLOAD, generic.received)
        }

        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val specific = SpecificFake(deserializeResult = EMPLOYEE, listDeserializeResult = listOf(EMPLOYEE))
            assertSourcePreserved("specific $shape", source) {
                assertSame(EMPLOYEE, specific.deserializeFrom(it, Employee::class.java))
                assertSame(EMPLOYEE, specific.deserialize<Employee>(it))
            }
            assertArrayEquals(PAYLOAD, specific.received)
        }

        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val specific = SpecificFake(deserializeResult = EMPLOYEE, listDeserializeResult = listOf(EMPLOYEE))
            assertSourcePreserved("specific list $shape", source) {
                assertEquals(listOf(EMPLOYEE), specific.deserializeListFrom(it, Employee::class.java))
                assertEquals(listOf(EMPLOYEE), specific.deserializeList<Employee>(it))
            }
            assertArrayEquals(PAYLOAD, specific.listReceived)
        }
    }

    @Test
    fun `all Avro input defaults preserve source when backend fails`() {
        val fatal = AssertionError("fatal")
        val operations = listOf<Pair<String, (ByteBuffer) -> Any?>>(
            "reflect" to { source -> ReflectFake(inputFailure = fatal).deserializeFrom(source, String::class.java) },
            "generic" to { source -> GenericFake(inputFailure = fatal).deserializeFrom(SCHEMA, source) },
            "specific" to { source -> SpecificFake(inputFailure = fatal).deserializeFrom(source, Employee::class.java) },
            "specific list" to { source -> SpecificFake(inputFailure = fatal).deserializeListFrom(source, Employee::class.java) },
        )

        operations.forEach { (name, operation) ->
            val source = configuredSource(PAYLOAD, direct = false)
            val start = source.position()
            val limit = source.limit()

            val actual = assertThrows(AssertionError::class.java, { operation(source) }, name)

            assertSame(fatal, actual, name)
            assertEquals(start, source.position(), name)
            assertEquals(limit, source.limit(), name)
            source.reset()
            assertEquals(start, source.position(), "$name mark")
        }
    }

    @Test
    fun `empty and malformed sources preserve state and retain every Avro family policy`() {
        sourcePolicyCases().forEach { (name, expectedBytes, source) ->
            val reflect = ReflectFake()
            assertSourcePreserved("reflect $name", source) {
                assertEquals(null, reflect.deserializeFrom(it, String::class.java))
            }
            assertArrayEquals(expectedBytes, reflect.received, "reflect $name")

            val generic = GenericFake()
            assertSourcePreserved("generic $name", source) {
                assertEquals(null, generic.deserializeFrom(SCHEMA, it))
            }
            assertArrayEquals(expectedBytes, generic.received, "generic $name")

            val specific = SpecificFake()
            assertSourcePreserved("specific $name", source) {
                assertEquals(null, specific.deserializeFrom(it, Employee::class.java))
            }
            assertArrayEquals(expectedBytes, specific.received, "specific $name")

            assertSourcePreserved("specific list $name", source) {
                assertEquals(emptyList<Employee>(), specific.deserializeListFrom(it, Employee::class.java))
            }
            assertArrayEquals(expectedBytes, specific.listReceived, "specific list $name")
        }
    }

    @Test
    fun `reusable Avro serializers support bounded concurrent valid and invalid calls`() {
        val reflect = ReflectFake(deserializeResult = "decoded", trackState = false)
        val generic = GenericFake(deserializeResult = GENERIC_RECORD, trackState = false)
        val specific =
            SpecificFake(
                deserializeResult = EMPLOYEE,
                listDeserializeResult = listOf(EMPLOYEE),
                trackState = false,
            )

        verifyAvroBufferConcurrency { _, repetition ->
            if (repetition % 2 == 0) {
                assertAvroWriteAndRead(
                    write = { target -> reflect.serializeTo("value", target) },
                    read = { source -> assertEquals("decoded", reflect.deserializeFrom(source, String::class.java)) },
                )
                assertAvroWriteAndRead(
                    write = { target -> generic.serializeTo(SCHEMA, GENERIC_RECORD, target) },
                    read = { source -> assertSame(GENERIC_RECORD, generic.deserializeFrom(SCHEMA, source)) },
                )
                assertAvroWriteAndRead(
                    write = { target -> specific.serializeTo(EMPLOYEE, target) },
                    read = { source -> assertSame(EMPLOYEE, specific.deserializeFrom(source, Employee::class.java)) },
                )
                assertAvroWriteAndRead(
                    write = { target -> specific.serializeListTo(listOf(EMPLOYEE), target) },
                    read = { source ->
                        assertEquals(listOf(EMPLOYEE), specific.deserializeListFrom(source, Employee::class.java))
                    },
                )
            } else {
                assertAvroOverflow { target -> reflect.serializeTo("value", target) }
                assertAvroOverflow { target -> generic.serializeTo(SCHEMA, GENERIC_RECORD, target) }
                assertAvroOverflow { target -> specific.serializeTo(EMPLOYEE, target) }
                assertAvroOverflow { target -> specific.serializeListTo(listOf(EMPLOYEE), target) }

                assertAvroMalformed { source -> assertEquals(null, reflect.deserializeFrom(source, String::class.java)) }
                assertAvroMalformed { source -> assertEquals(null, generic.deserializeFrom(SCHEMA, source)) }
                assertAvroMalformed { source -> assertEquals(null, specific.deserializeFrom(source, Employee::class.java)) }
                assertAvroMalformed { source ->
                    assertEquals(emptyList<Employee>(), specific.deserializeListFrom(source, Employee::class.java))
                }
            }
        }
    }

    private fun outputOperations(): List<Pair<String, (ByteBuffer) -> Int>> {
        val reflect = ReflectFake()
        val generic = GenericFake()
        val specific = SpecificFake()
        return listOf(
            "reflect" to { target -> reflect.serializeTo("value", target) },
            "generic" to { target -> generic.serializeTo(SCHEMA, GENERIC_RECORD, target) },
            "specific" to { target -> specific.serializeTo(EMPLOYEE, target) },
            "specific list" to { target -> specific.serializeListTo(listOf(EMPLOYEE), target) },
        )
    }

    private fun assertAvroWriteAndRead(
        write: (ByteBuffer) -> Int,
        read: (ByteBuffer) -> Unit,
    ) {
        val target = ByteBuffer.allocate(PAYLOAD.size)
        assertEquals(PAYLOAD.size, write(target))
        target.flip()
        read(target.asReadOnlyBuffer())
    }

    private fun assertAvroOverflow(write: (ByteBuffer) -> Int) {
        val target = ByteBuffer.allocate(PAYLOAD.size - 1)
        assertThrows(BufferOverflowException::class.java) { write(target) }
        assertEquals(0, target.position())
    }

    private fun assertAvroMalformed(read: (ByteBuffer) -> Unit) {
        val source = ByteBuffer.wrap(MALFORMED.copyOf()).asReadOnlyBuffer()
        read(source)
        assertEquals(0, source.position())
    }

    private fun assertSourcePreserved(
        name: String,
        source: ByteBuffer,
        operation: (ByteBuffer) -> Unit,
    ) {
        val start = source.position()
        val limit = source.limit()
        val order = source.order()

        operation(source)

        assertEquals(start, source.position(), name)
        assertEquals(limit, source.limit(), name)
        assertEquals(order, source.order(), name)
        source.reset()
        assertEquals(start, source.position(), "$name mark")
    }

    private class ReflectFake(
        private val serializeResult: ByteArray? = PAYLOAD,
        private val deserializeResult: Any? = null,
        private val outputFailure: Throwable? = null,
        private val inputFailure: Throwable? = null,
        private val trackState: Boolean = true,
    ): AvroReflectSerializer {
        var serializeInvocations: Int = 0
        var received: ByteArray? = null

        override fun <T> serialize(graph: T?): ByteArray? {
            if (trackState) serializeInvocations++
            outputFailure?.let { throw it }
            return serializeResult
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? {
            if (trackState) received = avroBytes
            inputFailure?.let { throw it }
            return if (avroBytes.contentEquals(PAYLOAD)) deserializeResult as T? else null
        }
    }

    private class GenericFake(
        private val serializeResult: ByteArray? = PAYLOAD,
        private val deserializeResult: GenericData.Record? = null,
        private val outputFailure: Throwable? = null,
        private val inputFailure: Throwable? = null,
        private val trackState: Boolean = true,
    ): AvroGenericRecordSerializer {
        var serializeInvocations: Int = 0
        var received: ByteArray? = null

        override fun serialize(schema: Schema, graph: GenericRecord?): ByteArray? {
            if (trackState) serializeInvocations++
            outputFailure?.let { throw it }
            return serializeResult
        }

        override fun deserialize(schema: Schema, avroBytes: ByteArray?): GenericData.Record? {
            if (trackState) received = avroBytes
            inputFailure?.let { throw it }
            return if (avroBytes.contentEquals(PAYLOAD)) deserializeResult else null
        }
    }

    private class SpecificFake(
        private val serializeResult: ByteArray? = PAYLOAD,
        private val deserializeResult: SpecificRecord? = null,
        private val listResult: ByteArray? = PAYLOAD,
        private val listDeserializeResult: List<SpecificRecord> = emptyList(),
        private val outputFailure: Throwable? = null,
        private val inputFailure: Throwable? = null,
        private val trackState: Boolean = true,
    ): AvroSpecificRecordSerializer {
        var serializeInvocations: Int = 0
        var listInvocations: Int = 0
        var received: ByteArray? = null
        var listReceived: ByteArray? = null

        override fun <T: SpecificRecord> serialize(graph: T?): ByteArray? {
            if (trackState) serializeInvocations++
            outputFailure?.let { throw it }
            return serializeResult
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T: SpecificRecord> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T? {
            if (trackState) received = avroBytes
            inputFailure?.let { throw it }
            return if (avroBytes.contentEquals(PAYLOAD)) deserializeResult as T? else null
        }

        override fun <T: SpecificRecord> serializeList(collection: List<T>?): ByteArray? {
            if (trackState) listInvocations++
            outputFailure?.let { throw it }
            return if (collection.isNullOrEmpty()) null else listResult
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T: SpecificRecord> deserializeList(avroBytes: ByteArray?, clazz: Class<T>): List<T> {
            if (trackState) listReceived = avroBytes
            inputFailure?.let { throw it }
            return if (avroBytes.contentEquals(PAYLOAD)) listDeserializeResult as List<T> else emptyList()
        }
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
        val PAYLOAD = byteArrayOf(21, 22, 23, 24)
        val MALFORMED = byteArrayOf(0x7F, 0x01, 0x02)
        val SCHEMA: Schema = Employee.getClassSchema()
        val EMPLOYEE: Employee = TestMessageProvider.createEmployee()
        val GENERIC_RECORD: GenericData.Record = GenericData.Record(SCHEMA)
        const val FILL: Byte = 0x33
    }
}

private fun verifyAvroBufferConcurrency(
    operation: (worker: Int, repetition: Int) -> Unit,
) {
    val threadSequence = AtomicInteger()
    val executor = Executors.newFixedThreadPool(AVRO_WORKERS) { task ->
        Thread(task, "avro-buffer-${threadSequence.incrementAndGet()}")
    }
    val startBarrier = CyclicBarrier(AVRO_WORKERS + 1)
    val completion = CountDownLatch(AVRO_WORKERS)
    val unfinished = ConcurrentHashMap.newKeySet<Int>()
    val failures = ConcurrentLinkedQueue<String>()

    try {
        repeat(AVRO_WORKERS) { worker ->
            unfinished += worker
            executor.submit {
                try {
                    startBarrier.await(AVRO_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    repeat(AVRO_REPETITIONS) { repetition -> operation(worker, repetition) }
                } catch (failure: Throwable) {
                    failures += "worker=$worker ${failure::class.java.simpleName}: ${failure.message}"
                } finally {
                    unfinished -= worker
                    completion.countDown()
                }
            }
        }

        startBarrier.await(AVRO_START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!completion.await(AVRO_COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            fail<Unit>("Avro buffer workers timed out; unfinished=${unfinished.sorted().take(AVRO_MAX_DIAGNOSTICS)}")
        }
        if (failures.isNotEmpty()) {
            fail<Unit>("Avro buffer worker failures: ${failures.take(AVRO_MAX_DIAGNOSTICS)}")
        }
    } finally {
        executor.shutdownNow()
        if (!executor.awaitTermination(AVRO_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            val threads =
                Thread.getAllStackTraces().keys.asSequence()
                    .filter { it.name.startsWith("avro-buffer-") }
                    .map { it.name }
                    .take(AVRO_MAX_DIAGNOSTICS)
                    .toList()
            fail<Unit>(
                "Avro buffer executor did not terminate; " +
                    "unfinished=${unfinished.sorted().take(AVRO_MAX_DIAGNOSTICS)}, threads=$threads",
            )
        }
    }
}

private const val AVRO_WORKERS = 8
private const val AVRO_REPETITIONS = 50
private const val AVRO_START_TIMEOUT_SECONDS = 2L
private const val AVRO_COMPLETION_TIMEOUT_SECONDS = 15L
private const val AVRO_SHUTDOWN_TIMEOUT_SECONDS = 5L
private const val AVRO_MAX_DIAGNOSTICS = 8
