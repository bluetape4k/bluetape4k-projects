package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.ByteBufferInput
import com.esotericsoftware.kryo.io.ByteBufferOutput
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.util.Pool
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import org.apache.fory.ThreadSafeFory
import org.junit.jupiter.api.Test
import untrusted.payload.UntrustedPayload
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

class JdkBinarySerializerByteBufferTest {

    @Test
    fun `serializeTo preserves JDK wire bytes in the bounded target range`() {
        val serializer = JdkBinarySerializer()
        val expected = "jdk-byte-buffer"
        val wire = serializer.serialize(expected)

        writableTargets(wire.size).forEach { target ->
            val start = target.position()
            val limit = target.limit()
            val capacity = target.capacity()
            val before = target.fullBytes()
            val written = serializer.serializeTo(expected, target)

            written shouldBeEqualTo wire.size
            target.rangeBytes(start, written) shouldBeEqualTo wire
            target.position() shouldBeEqualTo start + written
            target.limit() shouldBeEqualTo limit
            target.capacity() shouldBeEqualTo capacity
            target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
            target.fullBytes().copyOfRange(0, start) shouldBeEqualTo before.copyOfRange(0, start)
            target.fullBytes().copyOfRange(limit, capacity) shouldBeEqualTo before.copyOfRange(limit, capacity)
            target.reset()
            target.position() shouldBeEqualTo start
        }
    }

    @Test
    fun `deserializeFrom reads every JDK source shape without changing caller state`() {
        val serializer = JdkBinarySerializer()
        val expected = "jdk-source-shapes"
        val wire = serializer.serialize(expected)

        sourceBuffers(wire).forEach { source ->
            val start = source.position()
            val limit = source.limit()
            val order = source.order()

            serializer.deserializeFrom<String>(source) shouldBeEqualTo expected

            source.position() shouldBeEqualTo start
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
            source.position() shouldBeEqualTo start
        }
    }

    @Test
    fun `fixed JDK output overflows before a large custom write completes and remains reusable`() {
        val serializer = JdkBinarySerializer()
        val payload = JdkOverflowProbe()
        val target = configuredTarget(32)
        val start = target.position()

        JdkOverflowProbe.writes.set(0)
        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo(payload, target)
        }

        JdkOverflowProbe.writes.get() shouldBeLessThan JdkOverflowProbe.WRITE_COUNT
        target.position() shouldBeEqualTo start

        val required = serializer.serialize(payload).size
        JdkOverflowProbe.writes.set(0)
        serializer.serializeTo(payload, configuredTarget(required)) shouldBeEqualTo required
        JdkOverflowProbe.writes.get() shouldBeEqualTo JdkOverflowProbe.WRITE_COUNT
    }

    @Test
    fun `deserializeFrom applies the configured JDK object input filter`() {
        val unfiltered = JdkBinarySerializer(objectInputFilter = null)
        val filtered = JdkBinarySerializer()
        val wire = unfiltered.serialize(UntrustedPayload("blocked"))
        val source = configuredSource(wire, direct = true)
        val start = source.position()

        val failure = assertFailsWith<BinarySerializationException> {
            filtered.deserializeFrom<UntrustedPayload>(source)
        }

        (failure.cause is java.io.InvalidClassException).shouldBeTrue()
        source.position() shouldBeEqualTo start
        source.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
    }

    @Test
    fun `serialization failure does not render the caller graph in diagnostics`() {
        val failure = assertFailsWith<BinarySerializationException> {
            JdkBinarySerializer().serializeTo(ThrowingToStringPayload(), configuredTarget(4 * 1024))
        }

        (failure.cause is IllegalStateException).shouldBeTrue()
    }
}

class KryoBinarySerializerByteBufferTest {

    @Test
    fun `serializeTo preserves Kryo wire bytes in every writable target shape`() {
        val serializer = KryoBinarySerializer()
        val expected = "kryo-byte-buffer"
        val wire = serializer.serialize(expected)

        writableTargets(wire.size).forEach { target ->
            val start = target.position()
            val limit = target.limit()
            val capacity = target.capacity()
            val before = target.fullBytes()
            val written = serializer.serializeTo(expected, target)

            target.rangeBytes(start, written) shouldBeEqualTo wire
            target.position() shouldBeEqualTo start + written
            target.limit() shouldBeEqualTo limit
            target.capacity() shouldBeEqualTo capacity
            target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
            target.fullBytes().copyOfRange(0, start) shouldBeEqualTo before.copyOfRange(0, start)
            target.fullBytes().copyOfRange(limit, capacity) shouldBeEqualTo before.copyOfRange(limit, capacity)
            target.reset()
            target.position() shouldBeEqualTo start
        }
    }

    @Test
    fun `deserializeFrom reads every Kryo source shape without changing caller state`() {
        val serializer = KryoBinarySerializer()
        val expected = "kryo-source-shapes"
        val wire = serializer.serialize(expected)

        sourceBuffers(wire).forEach { source ->
            val start = source.position()
            val limit = source.limit()
            val order = source.order()

            serializer.deserializeFrom<String>(source) shouldBeEqualTo expected

            source.position() shouldBeEqualTo start
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
            source.position() shouldBeEqualTo start
        }
    }

    @Test
    fun `Kryo overflow is public and a failed call does not poison the next call`() {
        val serializer = KryoBinarySerializer()
        val expected = "kryo-overflow-retry"
        val wire = serializer.serialize(expected)
        val tooSmall = configuredTarget(wire.size - 1)
        val start = tooSmall.position()

        assertFailsWith<BufferOverflowException> {
            serializer.serializeTo(expected, tooSmall)
        }

        tooSmall.position() shouldBeEqualTo start
        val retry = configuredTarget(wire.size)
        serializer.serializeTo(expected, retry) shouldBeEqualTo wire.size
    }

    @Test
    fun `external Kryo pools keep array backed compatibility adapters`() {
        val created = AtomicInteger()
        val pool = object: Pool<Kryo>(true, false, 1) {
            override fun create(): Kryo = KryoProvider.createKryo().apply {
                created.incrementAndGet()
                register(KryoArrayBackedPayload::class.java, KryoArrayBackedPayloadSerializer())
            }
        }
        val serializer = KryoBinarySerializer(kryoPool = pool)
        val expected = KryoArrayBackedPayload("array-backed")
        val wire = serializer.serialize(expected)
        val target = configuredTarget(wire.size)
        val start = target.position()

        val written = serializer.serializeTo(expected, target)
        val restored = serializer.deserializeFrom<KryoArrayBackedPayload>(
            configuredSource(target.rangeBytes(start, written), direct = true)
        )

        target.rangeBytes(start, written) shouldBeEqualTo wire
        restored shouldBeEqualTo expected
        created.get() shouldBeEqualTo 1
    }

    @Test
    fun `scoped Kryo ByteBuffer adapters detach caller buffers on return`() {
        val inputBuffer = configuredSource(byteArrayOf(1, 2, 3), direct = true)
        lateinit var input: ByteBufferInput
        KryoProvider.useByteBufferInput(inputBuffer) { adapter ->
            input = adapter
            (adapter.byteBuffer === inputBuffer).shouldBeTrue()
        }
        (input.byteBuffer !== inputBuffer).shouldBeTrue()
        input.byteBuffer.capacity() shouldBeEqualTo 0

        val outputBuffer = ByteBuffer.allocateDirect(16)
        lateinit var output: ByteBufferOutput
        KryoProvider.useByteBufferOutput(outputBuffer) { adapter ->
            output = adapter
            (adapter.byteBuffer === outputBuffer).shouldBeTrue()
        }
        (output.byteBuffer !== outputBuffer).shouldBeTrue()
        output.byteBuffer.capacity() shouldBeEqualTo 0
    }

    @Test
    fun `secure and fast Kryo configurations remain active on ByteBuffer paths`() {
        val secure = KryoBinarySerializer.secure(KryoRegisteredPayload::class.java)
        val expected = KryoRegisteredPayload(7, "registered")
        val wire = secure.serialize(expected)
        val target = configuredTarget(wire.size)
        val start = target.position()

        val written = secure.serializeTo(expected, target)
        val source = configuredSource(target.rangeBytes(start, written), direct = true)

        secure.deserializeFrom<KryoRegisteredPayload>(source) shouldBeEqualTo expected
        assertFailsWith<BinarySerializationException> {
            secure.serializeTo(KryoUnregisteredPayload("blocked"), configuredTarget(256))
        }
        val unregisteredWire = KryoBinarySerializer().serialize(KryoUnregisteredPayload("blocked"))
        assertFailsWith<BinarySerializationException> {
            secure.deserializeFrom<KryoUnregisteredPayload>(configuredSource(unregisteredWire, direct = true))
        }

        val fast = KryoBinarySerializer.fast()
        val fastWire = fast.serialize("fast-kryo")
        val fastTarget = configuredTarget(fastWire.size)
        val fastStart = fastTarget.position()
        val fastWritten = fast.serializeTo("fast-kryo", fastTarget)
        fast.deserializeFrom<String>(
            configuredSource(fastTarget.rangeBytes(fastStart, fastWritten), direct = true)
        ) shouldBeEqualTo "fast-kryo"
    }

    @Test
    fun `custom Kryo reference configuration remains active on compatibility buffer paths`() {
        val pool = object: Pool<Kryo>(true, false, 4) {
            override fun create(): Kryo = KryoProvider.createKryo().apply {
                references = true
            }
        }
        val serializer = KryoBinarySerializer(kryoPool = pool)
        val shared = KryoReferencePayload(11)
        val expected = listOf(shared, shared)
        val wire = serializer.serialize(expected)
        val target = configuredTarget(wire.size)
        val start = target.position()

        val written = serializer.serializeTo(expected, target)
        val actual = serializer.deserializeFrom<List<KryoReferencePayload>>(
            configuredSource(target.rangeBytes(start, written), direct = true)
        )

        actual.shouldNotBeNull()
        actual shouldBeEqualTo expected
        (actual[0] === actual[1]).shouldBeTrue()
    }

    @Test
    fun `shared Kryo serializer isolates concurrent success overflow and malformed input calls`() {
        val serializer = KryoBinarySerializer()

        verifySerializerBufferConcurrency { worker, repetition ->
            val value = "$worker:$repetition"
            val wire = serializer.serialize(value)
            when (repetition % 3) {
                0 -> {
                    val target = configuredTarget(wire.size, direct = repetition % 2 == 0)
                    val start = target.position()
                    val written = serializer.serializeTo(value, target)
                    serializer.deserializeFrom<String>(
                        configuredSource(target.rangeBytes(start, written), direct = true)
                    ) shouldBeEqualTo value
                }
                1 -> assertFailsWith<BufferOverflowException> {
                    serializer.serializeTo(value, configuredTarget(wire.size - 1))
                }
                else -> assertFailsWith<BinarySerializationException> {
                    serializer.deserializeFrom<String>(configuredSource(byteArrayOf(1, 2, 3), direct = true))
                }
            }
        }
    }

    @Test
    fun `optimized serializers reject read-only targets before null handling`() {
        listOf<BinarySerializer>(JdkBinarySerializer(), KryoBinarySerializer()).forEach { serializer ->
            val target = ByteBuffer.allocate(16).asReadOnlyBuffer()

            assertFailsWith<ReadOnlyBufferException> {
                serializer.serializeTo(null, target)
            }
        }
    }
}

class CoreBinarySerializerByteBufferTest {

    @Test
    fun `serializeBinaryToStream delegates to the Fory OutputStream overload`() {
        val wire = byteArrayOf(4, 3, 2, 1)
        val handler = RecordingForyHandler(
            encoded = wire,
            rejectByteArraySerialize = true,
            streamWrite = { output ->
                output.write(wire)
                output.flush()
                output.close()
            },
        )
        val serializer = ForyBinarySerializer(handler.proxy())
        val target = RecordingForyOutputStream()

        val written = serializer.serializeBinaryToStream("value", target)

        written shouldBeEqualTo wire.size
        target.toByteArray() shouldBeEqualTo wire
        handler.streamSerializeCalls shouldBeEqualTo 1
        handler.byteArraySerializeCalls shouldBeEqualTo 0
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `Fory stream normalizes primary serialization failure while preserving borrowed target failures`() {
        val primaryFailure = IllegalArgumentException("primary failure")
        val primaryHandler = RecordingForyHandler(
            rejectByteArraySerialize = true,
            streamSerializeFailure = primaryFailure,
        )
        val primaryTarget = RecordingForyOutputStream()

        val primaryActual = assertFailsWith<BinarySerializationException> {
            ForyBinarySerializer(primaryHandler.proxy()).serializeBinaryToStream("value", primaryTarget)
        }

        primaryActual.cause shouldBeSameInstanceAs primaryFailure
        primaryTarget.toByteArray() shouldBeEqualTo byteArrayOf()
        primaryTarget.flushCount shouldBeEqualTo 0
        primaryTarget.closeCount shouldBeEqualTo 0

        val primaryCancellation = CancellationException("primary cancellation")
        val cancellationTarget = RecordingForyOutputStream()

        val cancellationActual = assertFailsWith<BinarySerializationException> {
            ForyBinarySerializer(
                RecordingForyHandler(
                    rejectByteArraySerialize = true,
                    streamSerializeFailure = primaryCancellation,
                ).proxy()
            ).serializeBinaryToStream("value", cancellationTarget)
        }

        cancellationActual.cause shouldBeSameInstanceAs primaryCancellation
        cancellationTarget.toByteArray() shouldBeEqualTo byteArrayOf()
        cancellationTarget.flushCount shouldBeEqualTo 0
        cancellationTarget.closeCount shouldBeEqualTo 0

        listOf<Throwable>(
            IOException("target I/O failure"),
            IllegalStateException("target runtime failure"),
            AssertionError("target error failure"),
            CancellationException("target cancellation failure"),
        ).forEach { targetFailure ->
            val handler = RecordingForyHandler(encoded = byteArrayOf(1, 2, 3), rejectByteArraySerialize = true)
            val target = RecordingForyOutputStream(writeFailure = targetFailure)

            val actual = assertFailsWith<Throwable> {
                ForyBinarySerializer(handler.proxy()).serializeBinaryToStream("value", target)
            }

            actual shouldBeSameInstanceAs targetFailure
            target.flushCount shouldBeEqualTo 0
            target.closeCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `Fory stream counts only complete writes when a target leaves partial output then fails`() {
        val writeFailure = IOException("partial target failure")
        val handler = RecordingForyHandler(
            encoded = byteArrayOf(1, 2, 3, 4),
            rejectByteArraySerialize = true,
            streamWrite = { output ->
                output.write(byteArrayOf(1, 2))
                output.write(byteArrayOf(3, 4))
            },
        )
        val target = RecordingForyOutputStream(writeFailure = writeFailure, bytesBeforeFailure = 1, failOnWrite = 2)

        val actual = assertFailsWith<IOException> {
            ForyBinarySerializer(handler.proxy()).serializeBinaryToStream("value", target)
        }

        actual shouldBeSameInstanceAs writeFailure
        target.toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3)
        val countedOutput = handler.receivedStream.shouldNotBeNull()
        countedOutput.writtenCount() shouldBeEqualTo 2
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `Fory stream rejects output counts above Int MAX before touching the target`() {
        val handler = RecordingForyHandler(
            encoded = byteArrayOf(1),
            rejectByteArraySerialize = true,
            beforeStreamWrite = { output -> output.setWrittenCount(Int.MAX_VALUE) },
        )
        val target = RecordingForyOutputStream()

        val actual = assertFailsWith<IllegalStateException> {
            ForyBinarySerializer(handler.proxy()).serializeBinaryToStream("value", target)
        }

        actual.message shouldBeEqualTo "Serialized output exceeds Int.MAX_VALUE bytes."
        (actual.cause is ArithmeticException).shouldBeTrue()
        target.toByteArray() shouldBeEqualTo byteArrayOf()
        target.flushCount shouldBeEqualTo 0
        target.closeCount shouldBeEqualTo 0
    }

    @Test
    fun `deserializeFrom delegates the caller range to the Fory ByteBuffer overload`() {
        val handler = RecordingForyHandler(decoded = "decoded")
        val serializer = ForyBinarySerializer(handler.proxy())
        val source = configuredSource(byteArrayOf(1, 2, 3, 4), direct = true)
        val start = source.position()

        serializer.deserializeFrom<String>(source) shouldBeEqualTo "decoded"

        handler.byteBufferDeserializeCalls shouldBeEqualTo 1
        handler.byteArrayDeserializeCalls shouldBeEqualTo 0
        handler.receivedBuffer?.position() shouldBeEqualTo start
        handler.receivedBuffer?.limit() shouldBeEqualTo source.limit()
        (handler.receivedBuffer !== source).shouldBeTrue()
        source.position() shouldBeEqualTo start
        source.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
    }

    @Test
    fun `serializeTo keeps the Fory ByteArray compatibility fallback`() {
        val wire = byteArrayOf(4, 3, 2, 1)
        val handler = RecordingForyHandler(encoded = wire)
        val serializer = ForyBinarySerializer(handler.proxy())
        val target = configuredTarget(wire.size)
        val start = target.position()

        serializer.serializeTo("value", target) shouldBeEqualTo wire.size

        handler.byteArraySerializeCalls shouldBeEqualTo 1
        target.rangeBytes(start, wire.size) shouldBeEqualTo wire
    }

    @Test
    fun `fast Fory configuration remains active on the ByteBuffer input path`() {
        val serializer = ForyBinarySerializer.fast()
        val expected = "fast-fory"
        val wire = serializer.serialize(expected)

        serializer.deserializeFrom<String>(configuredSource(wire, direct = true)) shouldBeEqualTo expected
    }

    @Test
    fun `deserializeFrom reads every Fory source shape without changing caller state`() {
        val serializer = ForyBinarySerializer()
        val expected = "fory-source-shapes"
        val wire = serializer.serialize(expected)

        sourceBuffers(wire).forEach { source ->
            val start = source.position()
            val limit = source.limit()
            val order = source.order()

            serializer.deserializeFrom<String>(source) shouldBeEqualTo expected

            source.position() shouldBeEqualTo start
            source.limit() shouldBeEqualTo limit
            source.order() shouldBeEqualTo order
            source.reset()
            source.position() shouldBeEqualTo start
        }
    }
}

private class JdkOverflowProbe: Serializable {

    private fun writeObject(output: ObjectOutputStream) {
        repeat(WRITE_COUNT) { index ->
            writes.incrementAndGet()
            output.writeInt(index)
        }
    }

    @Suppress("unused")
    private fun readObject(input: ObjectInputStream) {
        repeat(WRITE_COUNT) { input.readInt() }
    }

    companion object {
        const val WRITE_COUNT = 10_000
        val writes = AtomicInteger()
    }
}

private class ThrowingToStringPayload: Serializable {

    @Suppress("unused")
    private fun writeObject(output: ObjectOutputStream) {
        error("expected serialization failure")
    }

    override fun toString(): String = error("caller graph must not be rendered")
}

data class KryoRegisteredPayload(
    val id: Int = 0,
    val text: String = "",
)

data class KryoUnregisteredPayload(
    val text: String = "",
)

data class KryoReferencePayload(
    val id: Int = 0,
)

data class KryoArrayBackedPayload(
    val text: String = "",
)

private class KryoArrayBackedPayloadSerializer: Serializer<KryoArrayBackedPayload>() {

    override fun write(kryo: Kryo, output: Output, value: KryoArrayBackedPayload) {
        check(output.buffer.isNotEmpty())
        output.writeString(value.text)
    }

    override fun read(
        kryo: Kryo,
        input: Input,
        type: Class<out KryoArrayBackedPayload>,
    ): KryoArrayBackedPayload {
        check(input.buffer.isNotEmpty())
        return KryoArrayBackedPayload(input.readString())
    }
}

private class RecordingForyHandler(
    private val decoded: Any? = null,
    private val encoded: ByteArray = byteArrayOf(),
    private val rejectByteArraySerialize: Boolean = false,
    private val streamSerializeFailure: Throwable? = null,
    private val beforeStreamWrite: (OutputStream) -> Unit = {},
    private val streamWrite: (OutputStream) -> Unit = { output -> output.write(encoded) },
): InvocationHandler {

    var byteBufferDeserializeCalls: Int = 0
    var byteArrayDeserializeCalls: Int = 0
    var byteArraySerializeCalls: Int = 0
    var streamSerializeCalls: Int = 0
    var receivedBuffer: ByteBuffer? = null
    var receivedStream: OutputStream? = null

    fun proxy(): ThreadSafeFory = Proxy.newProxyInstance(
        ThreadSafeFory::class.java.classLoader,
        arrayOf(ThreadSafeFory::class.java),
        this,
    ) as ThreadSafeFory

    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        if (method.declaringClass == Any::class.java) {
            return when (method.name) {
                "toString" -> "RecordingThreadSafeFory"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }

        val argument = args?.singleOrNull()
        return when {
            method.name == "deserialize" && argument is ByteBuffer -> {
                byteBufferDeserializeCalls++
                receivedBuffer = argument
                decoded
            }
            method.name == "deserialize" && argument is ByteArray -> {
                byteArrayDeserializeCalls++
                decoded
            }
            method.name == "serialize" && args?.size == 1 -> {
                if (rejectByteArraySerialize) {
                    error("Unexpected ThreadSafeFory call: serialize(Object)")
                }
                byteArraySerializeCalls++
                encoded
            }
            method.name == "serialize" && args?.size == 2 && args[0] is OutputStream -> {
                streamSerializeCalls++
                val output = args[0] as OutputStream
                receivedStream = output
                beforeStreamWrite(output)
                streamSerializeFailure?.let { throw it }
                streamWrite(output)
                null
            }
            else -> error("Unexpected ThreadSafeFory call: ${method.name}")
        }
    }
}

internal class RecordingForyOutputStream(
    private val writeFailure: Throwable? = null,
    private val bytesBeforeFailure: Int = 0,
    private val failOnWrite: Int = 1,
): OutputStream() {
    private val output = ByteArrayOutputStream()

    var flushCount: Int = 0
        private set

    var closeCount: Int = 0
        private set

    private var writeCount: Int = 0

    override fun write(value: Int) {
        writeCount++
        writeFailure?.takeIf { writeCount == failOnWrite }?.let { throw it }
        output.write(value)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        writeCount++
        writeFailure?.takeIf { writeCount == failOnWrite }?.let { failure ->
            output.write(bytes, offset, minOf(bytesBeforeFailure, length))
            throw failure
        }
        output.write(bytes, offset, length)
    }

    override fun flush() {
        flushCount++
    }

    override fun close() {
        closeCount++
    }

    fun toByteArray(): ByteArray = output.toByteArray()
}

private const val FORY_CALLER_OWNED_COUNTING_OUTPUT_STREAM_CLASS =
    "io.bluetape4k.io.serializer.ForyCallerOwnedCountingOutputStream"

private fun OutputStream.writtenCount(): Int = withForyWrittenField { it.getInt(this) }

private fun OutputStream.setWrittenCount(value: Int): Unit = withForyWrittenField { it.setInt(this, value) }

private inline fun <T> OutputStream.withForyWrittenField(access: (java.lang.reflect.Field) -> T): T {
    check(javaClass.name == FORY_CALLER_OWNED_COUNTING_OUTPUT_STREAM_CLASS) {
        "Count-overflow test requires runtime class $FORY_CALLER_OWNED_COUNTING_OUTPUT_STREAM_CLASS, " +
            "but received ${javaClass.name}."
    }
    val field = try {
        javaClass.getDeclaredField("written").apply { isAccessible = true }
    } catch (failure: ReflectiveOperationException) {
        throw AssertionError(
            "Count-overflow test requires $FORY_CALLER_OWNED_COUNTING_OUTPUT_STREAM_CLASS to expose field 'written'.",
            failure,
        )
    }
    return access(field)
}

private fun configuredTarget(
    remaining: Int,
    direct: Boolean = false,
): ByteBuffer {
    val target = if (direct) ByteBuffer.allocateDirect(remaining + 8) else ByteBuffer.allocate(remaining + 8)
    repeat(target.capacity()) { target.put(0x55) }
    target.clear()
    target.order(ByteOrder.LITTLE_ENDIAN)
    target.position(3)
    target.mark()
    target.limit(3 + remaining)
    return target
}

private fun configuredSource(
    payload: ByteArray,
    direct: Boolean,
): ByteBuffer {
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

private fun writableTargets(remaining: Int): List<ByteBuffer> = listOf(
    configuredTarget(remaining),
    configuredTarget(remaining, direct = true),
    configuredSliceTarget(remaining),
)

private fun configuredSliceTarget(remaining: Int): ByteBuffer {
    val parent = ByteBuffer.allocate(remaining + 12)
    repeat(parent.capacity()) { parent.put(0x55) }
    parent.position(2)
    parent.limit(parent.capacity() - 2)
    return parent.slice().apply {
        order(ByteOrder.LITTLE_ENDIAN)
        position(3)
        mark()
        limit(3 + remaining)
    }
}

private fun sourceBuffers(payload: ByteArray): List<ByteBuffer> = listOf(
    configuredSource(payload, direct = false),
    configuredSource(payload, direct = true),
    configuredSliceSource(payload),
    configuredSource(payload, direct = false).asReadOnlyBuffer().apply {
        order(ByteOrder.LITTLE_ENDIAN)
        mark()
    },
)

private fun configuredSliceSource(payload: ByteArray): ByteBuffer {
    val parent = ByteBuffer.allocate(payload.size + 7)
    parent.put(byteArrayOf(1, 2))
    parent.put(byteArrayOf(9, 8, 7))
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

private fun ByteBuffer.rangeBytes(start: Int, count: Int): ByteArray =
    duplicate().apply {
        position(start)
        limit(start + count)
    }.let { view -> ByteArray(count).also(view::get) }

private fun ByteBuffer.fullBytes(): ByteArray =
    duplicate().clear().let { view -> ByteArray(view.remaining()).also(view::get) }
