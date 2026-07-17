package io.bluetape4k.avro.impl

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.message.examples.Employee
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.apache.avro.Schema
import org.apache.avro.file.Codec
import org.apache.avro.file.CodecFactory
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.concurrent.atomic.AtomicBoolean

class DefaultAvroSerializerByteBufferTest {

    @Test
    fun `reflect ByteBuffer paths bypass ByteArray siblings and preserve caller state`() {
        val employee = TestMessageProvider.createEmployee()
        val baseline = DefaultAvroReflectSerializer()
        val legacyWire = baseline.serialize(employee)!!
        val serializer = spyk(DefaultAvroReflectSerializer())
        val target = boundedTarget(legacyWire.size, direct = true)

        val written = serializer.serializeTo(employee, target)

        written shouldBeEqualTo legacyWire.size
        target.position() shouldBeEqualTo 2 + written
        target.limit() shouldBeEqualTo 2 + legacyWire.size
        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        verify(exactly = 0) { serializer.serialize(any<Employee>()) }

        sourceVariants(legacyWire).forEach { source ->
            val position = source.position()
            val limit = source.limit()
            val order = source.order()
            source.mark()

            serializer.deserializeFrom(source, Employee::class.java) shouldBeEqualTo employee

            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
        }
        verify(exactly = 0) { serializer.deserialize(any<ByteArray>(), Employee::class.java) }

        baseline.deserialize(target.writtenBytes(2, written), Employee::class.java) shouldBeEqualTo employee
    }

    @Test
    fun `generic ByteBuffer paths bypass ByteArray siblings and retain schema policy`() {
        val employee = TestMessageProvider.createEmployee()
        val schema = Employee.getClassSchema()
        val baseline = DefaultAvroGenericRecordSerializer()
        val legacyWire = baseline.serialize(schema, employee)!!
        val serializer = spyk(DefaultAvroGenericRecordSerializer())
        val target = boundedTarget(legacyWire.size, direct = false)

        val written = serializer.serializeTo(schema, employee, target)

        written shouldBeEqualTo legacyWire.size
        verify(exactly = 0) { serializer.serialize(schema, any<GenericRecord>()) }
        serializer.deserializeFrom(schema, ByteBuffer.wrap(target.writtenBytes(2, written))).toString() shouldBeEqualTo
                employee.toString()
        verify(exactly = 0) { serializer.deserialize(schema, any<ByteArray>()) }

        val mismatched = Schema.create(Schema.Type.STRING)
        val source = ByteBuffer.wrap(legacyWire).order(ByteOrder.LITTLE_ENDIAN)
        source.mark()
        serializer.deserializeFrom(mismatched, source).shouldBeNull()
        source.position() shouldBeEqualTo 0
        source.limit() shouldBeEqualTo legacyWire.size
        source.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        source.reset()
        verify(exactly = 0) { serializer.deserialize(mismatched, any<ByteArray>()) }
    }

    @Test
    fun `specific record and list ByteBuffer paths bypass ByteArray siblings`() {
        val employees = List(3) { TestMessageProvider.createEmployee() }
        val employee = employees.first()
        val baseline = DefaultAvroSpecificRecordSerializer()
        val singleWire = baseline.serialize(employee)!!
        val listWire = baseline.serializeList(employees)!!
        val serializer = spyk(DefaultAvroSpecificRecordSerializer())

        val singleTarget = boundedTarget(singleWire.size, direct = true)
        val singleWritten = serializer.serializeTo(employee, singleTarget)
        serializer.deserializeFrom(
            ByteBuffer.wrap(singleTarget.writtenBytes(2, singleWritten)),
            Employee::class.java,
        ) shouldBeEqualTo employee

        val listTarget = boundedTarget(listWire.size, direct = false)
        val listWritten = serializer.serializeListTo(employees, listTarget)
        serializer.deserializeListFrom(
            ByteBuffer.wrap(listTarget.writtenBytes(2, listWritten)),
            Employee::class.java,
        ) shouldBeEqualTo employees

        verify(exactly = 0) { serializer.serialize(any<Employee>()) }
        verify(exactly = 0) { serializer.deserialize(any<ByteArray>(), Employee::class.java) }
        verify(exactly = 0) { serializer.serializeList(any<List<Employee>>()) }
        verify(exactly = 0) { serializer.deserializeList(any<ByteArray>(), Employee::class.java) }
    }

    @Test
    fun `fixed output overflow restores position and remains reusable`() {
        val employee = TestMessageProvider.createEmployee()
        val baseline = DefaultAvroSpecificRecordSerializer()
        val legacyWire = baseline.serialize(employee)!!
        val serializer = spyk(DefaultAvroSpecificRecordSerializer())
        val target = ByteBuffer.allocate(legacyWire.size + 2)
        target.position(2)
        target.limit(target.capacity() - 1)

        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo(employee, target)
        }
        target.position() shouldBeEqualTo 2

        target.limit(target.capacity())
        serializer.serializeTo(employee, target) shouldBeEqualTo legacyWire.size
        verify(exactly = 0) { serializer.serialize(any<Employee>()) }
    }

    @Test
    fun `backend buffer overflow keeps the established handled failure policy`() {
        val record = spyk(TestMessageProvider.createEmployee())
        every { record.get(any<Int>()) } throws BufferOverflowException()
        val serializer = DefaultAvroGenericRecordSerializer()
        val target = ByteBuffer.allocate(4096).apply { position(7) }

        serializer.serializeTo(Employee.getClassSchema(), record, target) shouldBeEqualTo 0

        target.position() shouldBeEqualTo 7
    }

    @Test
    fun `specific list null empty and malformed policies remain unchanged without ByteArray fallback`() {
        val serializer = spyk(DefaultAvroSpecificRecordSerializer())
        val target = ByteBuffer.allocate(8)
        target.position(3)

        serializer.serializeListTo<Employee>(null, target) shouldBeEqualTo 0
        serializer.serializeListTo(emptyList<Employee>(), target) shouldBeEqualTo 0
        target.position() shouldBeEqualTo 3
        serializer.deserializeListFrom(ByteBuffer.allocate(0), Employee::class.java).shouldBeEmpty()
        serializer.deserializeListFrom(
            ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02)),
            Employee::class.java,
        ).shouldBeEmpty()

        verify(exactly = 0) { serializer.serializeList(any<List<Employee>>()) }
        verify(exactly = 0) { serializer.deserializeList(any<ByteArray>(), Employee::class.java) }
    }

    @Test
    fun `ByteBuffer paths preserve configured Avro codecs`() {
        val employee = TestMessageProvider.createEmployee()
        codecFactories().forEach { codecFactory ->
            val baseline = DefaultAvroSpecificRecordSerializer(codecFactory)
            val legacyWire = baseline.serialize(employee)!!
            val serializer = spyk(DefaultAvroSpecificRecordSerializer(codecFactory))
            val target = boundedTarget(legacyWire.size, direct = true)

            val written = serializer.serializeTo(employee, target)
            val optimizedWire = target.writtenBytes(2, written)

            baseline.deserialize(optimizedWire, Employee::class.java) shouldBeEqualTo employee
            serializer.deserializeFrom(ByteBuffer.wrap(legacyWire), Employee::class.java) shouldBeEqualTo employee
            verify(exactly = 0) { serializer.serialize(any<Employee>()) }
            verify(exactly = 0) { serializer.deserialize(any<ByteArray>(), Employee::class.java) }
        }
    }

    @Test
    fun `read only targets fail before null and empty policies run`() {
        val target = ByteBuffer.allocate(16).asReadOnlyBuffer()

        assertFailsWith<ReadOnlyBufferException> {
            DefaultAvroReflectSerializer().serializeTo<Employee>(null, target)
        }
        assertFailsWith<ReadOnlyBufferException> {
            DefaultAvroGenericRecordSerializer().serializeTo(Employee.getClassSchema(), null, target)
        }
        assertFailsWith<ReadOnlyBufferException> {
            DefaultAvroSpecificRecordSerializer().serializeTo<Employee>(null, target)
        }
        assertFailsWith<ReadOnlyBufferException> {
            DefaultAvroSpecificRecordSerializer().serializeListTo<Employee>(emptyList(), target)
        }
    }

    @Test
    fun `flush failure rolls back position and does not poison the next call`() {
        val failed = AtomicBoolean(false)
        val codec = codecFactory {
            passThroughCodec { data ->
                if (failed.compareAndSet(false, true)) throw IOException("flush failed")
                data
            }
        }
        val employee = TestMessageProvider.createEmployee()
        val serializer = DefaultAvroSpecificRecordSerializer(codec)
        val target = ByteBuffer.allocate(4096).apply { position(3) }

        serializer.serializeTo(employee, target) shouldBeEqualTo 0
        target.position() shouldBeEqualTo 3

        val written = serializer.serializeTo(employee, target)
        (written > 0).shouldBeTrue()
        DefaultAvroSpecificRecordSerializer().deserialize(
            target.writtenBytes(3, written),
            Employee::class.java,
        ) shouldBeEqualTo employee
    }

    @Test
    fun `fatal codec failure preserves Error identity and rolls back position`() {
        val fatal = OutOfMemoryError("fatal codec failure")
        val codec = codecFactory { passThroughCodec { throw fatal } }
        val target = ByteBuffer.allocate(4096).apply { position(5) }

        val thrown = assertFailsWith<OutOfMemoryError> {
            DefaultAvroSpecificRecordSerializer(codec).serializeTo(TestMessageProvider.createEmployee(), target)
        }

        (thrown === fatal).shouldBeTrue()
        target.position() shouldBeEqualTo 5
    }

    private fun boundedTarget(size: Int, direct: Boolean): ByteBuffer {
        val target = if (direct) ByteBuffer.allocateDirect(size + 4) else ByteBuffer.allocate(size + 4)
        return target.order(ByteOrder.LITTLE_ENDIAN).apply {
            position(2)
            limit(2 + size)
        }
    }

    private fun sourceVariants(bytes: ByteArray): List<ByteBuffer> {
        val heap = ByteBuffer.wrap(byteArrayOf(9, 8) + bytes + byteArrayOf(7)).apply {
            position(2)
            limit(2 + bytes.size)
        }
        val direct = ByteBuffer.allocateDirect(bytes.size + 3).apply {
            put(9)
            put(8)
            put(bytes)
            put(7)
            position(2)
            limit(2 + bytes.size)
        }
        val sliced = ByteBuffer.wrap(byteArrayOf(9, 8) + bytes + byteArrayOf(7)).apply {
            position(2)
            limit(2 + bytes.size)
        }.slice()
        val readOnly = heap.asReadOnlyBuffer()
        return listOf(heap, direct, sliced, readOnly)
    }

    private fun ByteBuffer.writtenBytes(start: Int, count: Int): ByteArray =
        duplicate().apply {
            position(start)
            limit(start + count)
        }.let { view -> ByteArray(view.remaining()).also(view::get) }

    private fun codecFactories(): List<CodecFactory> =
        listOf(
            CodecFactory.nullCodec(),
            CodecFactory.deflateCodec(6),
            CodecFactory.snappyCodec(),
            CodecFactory.zstandardCodec(3),
        )

    private fun codecFactory(create: () -> Codec): CodecFactory =
        object: CodecFactory() {
            override fun createInstance(): Codec = create()
        }

    private fun passThroughCodec(compress: (ByteBuffer) -> ByteBuffer): Codec =
        object: Codec() {
            override fun getName(): String = "null"

            override fun compress(uncompressedData: ByteBuffer): ByteBuffer = compress(uncompressedData)

            override fun decompress(compressedData: ByteBuffer): ByteBuffer = compressedData

            override fun equals(other: Any?): Boolean = this === other

            override fun hashCode(): Int = System.identityHashCode(this)
        }
}
