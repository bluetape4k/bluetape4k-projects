package io.bluetape4k.avro

import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.expectThat
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContentEqual
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
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

                val context = "$name $shape"
                expectThat(PAYLOAD.size, context) { operation(target) }

                expectThat(start + PAYLOAD.size, context) { target.position() }
                expectThat(limit, context) { target.limit() }
                expectThat(capacity, context) { target.capacity() }
                expectThat(order, context) { target.order() }
                expectThat(PAYLOAD.toList(), context) {
                    target.fullBytes().copyOfRange(start, start + PAYLOAD.size).toList()
                }
                expectThat(before.copyOfRange(0, start).toList(), context) {
                    target.fullBytes().copyOfRange(0, start).toList()
                }
                expectThat(before.copyOfRange(limit, capacity).toList(), context) {
                    target.fullBytes().copyOfRange(limit, capacity).toList()
                }
                target.reset()
                expectThat(start, "$context mark") { target.position() }
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
            expectThat(0, name) { operation(target) }
            expectThat(3, name) { target.position() }
            expectThat(before.toList(), name) { target.fullBytes().toList() }
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
            assertFailsWith<ReadOnlyBufferException>(name) { operation(ByteBuffer.allocate(8).asReadOnlyBuffer()) }
        }
        reflect.serializeInvocations shouldBeEqualTo 0
        generic.serializeInvocations shouldBeEqualTo 0
        specific.serializeInvocations shouldBeEqualTo 0
        specific.listInvocations shouldBeEqualTo 0
    }

    @Test
    fun `all Avro output defaults restore position on overflow`() {
        outputOperations().forEach { (name, operation) ->
            val target = configuredTarget(PAYLOAD.size - 1)

            assertFailsWith<BufferOverflowException>(name) { operation(target) }

            target.position() shouldBeEqualTo 3
        }

        outputOperations().forEach { (name, operation) ->
            val retry = configuredTarget(PAYLOAD.size)
            expectThat(PAYLOAD.size, "$name retry") { operation(retry) }
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
            val actual = assertFailsWith<AssertionError>(name) { operation(target) }
            actual shouldBeSameInstanceAs fatal
            target.position() shouldBeEqualTo 3
        }
    }

    @Test
    fun `all Avro input defaults and Kotlin facades preserve source state`() {
        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val reflect = ReflectFake(deserializeResult = "decoded")
            assertSourcePreserved("reflect $shape", source) {
                reflect.deserializeFrom(it, String::class.java) shouldBeEqualTo "decoded"
                reflect.deserialize<String>(it) shouldBeEqualTo "decoded"
            }
            reflect.received shouldContentEqual PAYLOAD
        }

        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val generic = GenericFake(deserializeResult = GENERIC_RECORD)
            assertSourcePreserved("generic $shape", source) {
                generic.deserializeFrom(SCHEMA, it) shouldBeSameInstanceAs GENERIC_RECORD
                generic.deserialize(SCHEMA, it) shouldBeSameInstanceAs GENERIC_RECORD
            }
            generic.received shouldContentEqual PAYLOAD
        }

        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val specific = SpecificFake(deserializeResult = EMPLOYEE, listDeserializeResult = listOf(EMPLOYEE))
            assertSourcePreserved("specific $shape", source) {
                specific.deserializeFrom(it, Employee::class.java) shouldBeSameInstanceAs EMPLOYEE
                specific.deserialize<Employee>(it) shouldBeSameInstanceAs EMPLOYEE
            }
            specific.received shouldContentEqual PAYLOAD
        }

        sourceBuffers(PAYLOAD).forEach { (shape, source) ->
            val specific = SpecificFake(deserializeResult = EMPLOYEE, listDeserializeResult = listOf(EMPLOYEE))
            assertSourcePreserved("specific list $shape", source) {
                specific.deserializeListFrom(it, Employee::class.java) shouldBeEqualTo listOf(EMPLOYEE)
                specific.deserializeList<Employee>(it) shouldBeEqualTo listOf(EMPLOYEE)
            }
            specific.listReceived shouldContentEqual PAYLOAD
        }
    }

    @Test
    fun `all Avro input defaults preserve source when backend fails`() {
        val fatal = AssertionError("fatal")
        val operations = listOf<Pair<String, (ByteBuffer) -> Any?>>(
            "reflect" to { source -> ReflectFake(inputFailure = fatal).deserializeFrom(source, String::class.java) },
            "generic" to { source -> GenericFake(inputFailure = fatal).deserializeFrom(SCHEMA, source) },
            "specific" to { source ->
                SpecificFake(inputFailure = fatal).deserializeFrom(source, Employee::class.java)
            },
            "specific list" to { source ->
                SpecificFake(inputFailure = fatal).deserializeListFrom(source, Employee::class.java)
            },
        )

        operations.forEach { (name, operation) ->
            val source = configuredSource(PAYLOAD, direct = false)
            val start = source.position()
            val limit = source.limit()

            val actual = assertFailsWith<AssertionError>(name) { operation(source) }

            actual shouldBeSameInstanceAs fatal
            source.position() shouldBeEqualTo start
            source.limit() shouldBeEqualTo limit
            source.reset()
            source.position() shouldBeEqualTo start
        }
    }

    @Test
    fun `empty and malformed sources preserve state and retain every Avro family policy`() {
        sourcePolicyCases().forEach { (name, expectedBytes, source) ->
            val reflect = ReflectFake()
            assertSourcePreserved("reflect $name", source) {
                reflect.deserializeFrom(it, String::class.java) shouldBeEqualTo null
            }
            reflect.received shouldContentEqual expectedBytes

            val generic = GenericFake()
            assertSourcePreserved("generic $name", source) {
                generic.deserializeFrom(SCHEMA, it) shouldBeEqualTo null
            }
            generic.received shouldContentEqual expectedBytes

            val specific = SpecificFake()
            assertSourcePreserved("specific $name", source) {
                specific.deserializeFrom(it, Employee::class.java) shouldBeEqualTo null
            }
            specific.received shouldContentEqual expectedBytes

            assertSourcePreserved("specific list $name", source) {
                specific.deserializeListFrom(it, Employee::class.java) shouldBeEqualTo emptyList<Employee>()
            }
            specific.listReceived shouldContentEqual expectedBytes
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
                    read = { source -> reflect.deserializeFrom(source, String::class.java) shouldBeEqualTo "decoded" },
                )
                assertAvroWriteAndRead(
                    write = { target -> generic.serializeTo(SCHEMA, GENERIC_RECORD, target) },
                    read = { source ->
                        generic.deserializeFrom(SCHEMA, source) shouldBeSameInstanceAs GENERIC_RECORD
                    },
                )
                assertAvroWriteAndRead(
                    write = { target -> specific.serializeTo(EMPLOYEE, target) },
                    read = { source ->
                        specific.deserializeFrom(source, Employee::class.java) shouldBeSameInstanceAs EMPLOYEE
                    },
                )
                assertAvroWriteAndRead(
                    write = { target -> specific.serializeListTo(listOf(EMPLOYEE), target) },
                    read = { source ->
                        specific.deserializeListFrom(source, Employee::class.java) shouldBeEqualTo listOf(EMPLOYEE)
                    },
                )
            } else {
                assertAvroOverflow { target -> reflect.serializeTo("value", target) }
                assertAvroOverflow { target -> generic.serializeTo(SCHEMA, GENERIC_RECORD, target) }
                assertAvroOverflow { target -> specific.serializeTo(EMPLOYEE, target) }
                assertAvroOverflow { target -> specific.serializeListTo(listOf(EMPLOYEE), target) }

                assertAvroMalformed { source ->
                    reflect.deserializeFrom(source, String::class.java) shouldBeEqualTo null
                }
                assertAvroMalformed { source ->
                    generic.deserializeFrom(SCHEMA, source) shouldBeEqualTo null
                }
                assertAvroMalformed { source ->
                    specific.deserializeFrom(source, Employee::class.java) shouldBeEqualTo null
                }
                assertAvroMalformed { source ->
                    specific.deserializeListFrom(source, Employee::class.java) shouldBeEqualTo emptyList<Employee>()
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
        write(target) shouldBeEqualTo PAYLOAD.size
        target.flip()
        read(target.asReadOnlyBuffer())
    }

    private fun assertAvroOverflow(write: (ByteBuffer) -> Int) {
        val target = ByteBuffer.allocate(PAYLOAD.size - 1)
        assertFailsWith<BufferOverflowException> { write(target) }
        target.position() shouldBeEqualTo 0
    }

    private fun assertAvroMalformed(read: (ByteBuffer) -> Unit) {
        val source = ByteBuffer.wrap(MALFORMED.copyOf()).asReadOnlyBuffer()
        read(source)
        source.position() shouldBeEqualTo 0
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

        expectThat(start, name) { source.position() }
        source.limit() shouldBeEqualTo limit
        source.order() shouldBeEqualTo order
        source.reset()
        source.position() shouldBeEqualTo start
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
            fail("Avro buffer workers timed out; unfinished=${unfinished.sorted().take(AVRO_MAX_DIAGNOSTICS)}")
        }
        if (failures.isNotEmpty()) {
            fail("Avro buffer worker failures: ${failures.take(AVRO_MAX_DIAGNOSTICS)}")
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
            fail(
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
