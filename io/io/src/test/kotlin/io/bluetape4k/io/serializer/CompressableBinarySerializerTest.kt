package io.bluetape4k.io.serializer

import com.esotericsoftware.kryo.io.KryoBufferOverflowException
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.math.BigDecimal
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.stream.Stream

@RandomizedTest
class CompressableBinarySerializerTest {

    companion object: KLogging() {

        private val compressableSerializers = listOf(
            BinarySerializers.BZip2Jdk,
            BinarySerializers.DeflateJdk,
            BinarySerializers.GZipJdk,
            BinarySerializers.LZ4Jdk,
            BinarySerializers.SnappyJdk,
            BinarySerializers.ZstdJdk,

            BinarySerializers.BZip2Kryo,
            BinarySerializers.DeflateKryo,
            BinarySerializers.GZipKryo,
            BinarySerializers.LZ4Kryo,
            BinarySerializers.SnappyKryo,
            BinarySerializers.ZstdKryo,

            BinarySerializers.BZip2Fory,
            BinarySerializers.DeflateFory,
            BinarySerializers.GZipFory,
            BinarySerializers.LZ4Fory,
            BinarySerializers.SnappyFory,
            BinarySerializers.ZstdFory,
        )
    }

    private fun getSerializers(): Stream<out BinarySerializer> = compressableSerializers.stream()

    private fun nestedControlFailures(): Stream<Arguments> = Stream.of(
        Arguments.of("cancellation", CancellationException("cancelled")),
        Arguments.of("error", AssertionError("fatal")),
    )

    private fun nestedOverflowFailures(): Stream<Arguments> = Stream.of(
        Arguments.of("JDK overflow", BufferOverflowException()),
        Arguments.of("Kryo overflow", KryoBufferOverflowException("native overflow")),
    )

    private val memorySizeSerializer = JdkBinarySerializer()

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedControlFailures")
    fun `compressed buffer output restores nested control failure identity`(
        @Suppress("UNUSED_PARAMETER") name: String,
        failure: Throwable,
    ) {
        val serializer = CompressableBinarySerializer(WrappingArraySerializer(failure), Compressors.LZ4)

        val actual = assertFailsWith<Throwable> {
            serializer.serializeTo("payload", ByteBuffer.allocate(1024))
        }

        actual shouldBeSameInstanceAs failure
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedControlFailures")
    fun `compressed buffer input restores nested control failure identity`(
        @Suppress("UNUSED_PARAMETER") name: String,
        failure: Throwable,
    ) {
        val serializer = CompressableBinarySerializer(WrappingArraySerializer(failure), Compressors.LZ4)
        val wire = Compressors.LZ4.compress(byteArrayOf(1))

        val actual = assertFailsWith<Throwable> {
            serializer.deserializeFrom<Any>(ByteBuffer.wrap(wire).asReadOnlyBuffer())
        }

        actual shouldBeSameInstanceAs failure
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedOverflowFailures")
    fun `compressed buffer output keeps ordinary wrapper around nested overflow`(
        @Suppress("UNUSED_PARAMETER") name: String,
        overflow: Throwable,
    ) {
        val outer = BinarySerializationException("ordinary wrapper", overflow)
        val serializer = CompressableBinarySerializer(ThrowingBinarySerializer(outer), Compressors.LZ4)

        val actual = assertFailsWith<BinarySerializationException> {
            serializer.serializeTo("payload", ByteBuffer.allocate(1024))
        }

        actual shouldBeSameInstanceAs outer
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedOverflowFailures")
    fun `compressed buffer input keeps ordinary wrapper around nested overflow`(
        @Suppress("UNUSED_PARAMETER") name: String,
        overflow: Throwable,
    ) {
        val outer = BinarySerializationException("ordinary wrapper", overflow)
        val serializer = CompressableBinarySerializer(ThrowingBinarySerializer(outer), Compressors.LZ4)
        val wire = Compressors.LZ4.compress(byteArrayOf(1))

        val actual = assertFailsWith<BinarySerializationException> {
            serializer.deserializeFrom<Any>(ByteBuffer.wrap(wire).asReadOnlyBuffer())
        }

        actual shouldBeSameInstanceAs outer
    }

    @Test
    fun `compressed buffer fallback preserves ordinary wrappers and raw target failures`() {
        val ordinary = BinarySerializationException("wrapped", IllegalStateException("ordinary"))
        val serializer = CompressableBinarySerializer(ThrowingBinarySerializer(ordinary), Compressors.LZ4)
        val compressedWire = Compressors.LZ4.compress(byteArrayOf(1))
        val readOnly = ByteBuffer.allocate(32).asReadOnlyBuffer().apply { position(3) }
        val tooSmall = ByteBuffer.allocate(0)

        assertFailsWith<BinarySerializationException> {
            serializer.serializeTo("payload", ByteBuffer.allocate(1024))
        } shouldBeSameInstanceAs ordinary
        assertFailsWith<BinarySerializationException> {
            serializer.deserializeFrom<Any>(ByteBuffer.wrap(compressedWire))
        } shouldBeSameInstanceAs ordinary
        assertFailsWith<ReadOnlyBufferException> {
            serializer.serializeTo("payload", readOnly)
        }
        assertFailsWith<BufferOverflowException> {
            CompressableBinarySerializer(BinarySerializers.Jdk, Compressors.LZ4)
                .serializeTo("payload", tooSmall)
        }

        readOnly.position() shouldBeEqualTo 3
        tooSmall.position() shouldBeEqualTo 0
    }

    @ParameterizedTest
    @MethodSource("getSerializers")
    fun `buffer output remains compatible with standard compressed wire`(serializer: BinarySerializer) {
        val origin = "compressible-wire-payload-".repeat(256)
        val target = ByteBuffer.allocateDirect(64 * 1024).apply {
            position(7)
            limit(capacity() - 11)
        }
        val start = target.position()
        val limit = target.limit()

        val written = serializer.serializeTo(origin, target)
        val wire = target.duplicate().apply {
            position(start)
            limit(start + written)
        }.let { view -> ByteArray(view.remaining()).also(view::get) }

        written shouldBeGreaterThan 0
        target.position() shouldBeEqualTo start + written
        target.limit() shouldBeEqualTo limit
        serializer.deserialize<String>(wire) shouldBeEqualTo origin
    }

    @ParameterizedTest
    @MethodSource("getSerializers")
    fun `standard compressed wire remains compatible with buffer input`(serializer: BinarySerializer) {
        val origin = "compressible-wire-payload-".repeat(256)
        val wire = serializer.serialize(origin)
        val source = ByteBuffer.allocateDirect(wire.size + 8).apply {
            position(3)
            put(wire)
            flip()
            position(3)
            limit(3 + wire.size)
        }.slice().asReadOnlyBuffer().apply { mark() }
        val position = source.position()
        val limit = source.limit()

        serializer.deserializeFrom<String>(source) shouldBeEqualTo origin

        source.position() shouldBeEqualTo position
        source.limit() shouldBeEqualTo limit
        source.reset().position() shouldBeEqualTo position
    }

    @ParameterizedTest
    @MethodSource("getSerializers")
    fun `serialize and compress string`(serializer: BinarySerializer) {
        val origin = Fakers.faker.lorem().paragraph(100).repeat(4)
        val compressed = serializer.serialize(origin)
        log.debug { "origin=${origin.length}, compressed=${compressed.size}" }

        val actual = serializer.deserialize<String>(compressed)
        actual shouldBeEqualTo origin
    }

    @ParameterizedTest
    @MethodSource("getSerializers")
    fun `serialize and compress object`(serializer: BinarySerializer, @RandomValue origin: SimpleData) {
        val compressed = serializer.serialize(origin)
        log.debug { "origin=${origin.memorySize()}, compressed=${compressed.size}" }

        val actual = serializer.deserialize<SimpleData>(compressed)
        actual.shouldNotBeNull() shouldBeEqualTo origin
    }

    @ParameterizedTest
    @MethodSource("getSerializers")
    fun `serialize and compress collection`(
        serializer: BinarySerializer,
        @RandomValue(type = SimpleData::class, size = 500) origins: List<SimpleData>,
    ) {
        val compressed = serializer.serialize(origins)
        log.debug { "origins=${origins.memorySize()}, compressed=${compressed.size}" }

        val actual = serializer.deserialize<List<SimpleData>>(compressed)
        actual.shouldNotBeNull() shouldBeEqualTo origins
    }

    private fun SimpleData.memorySize(): Int =
        memorySizeSerializer.serialize(this).size

    private fun List<SimpleData>.memorySize(): Int =
        memorySizeSerializer.serialize(this).size


    data class SimpleData(
        val id: Long,
        val name: String,
        val age: Int,
        val birth: Date,
        val biography: String,
        val zip: String,
        val address: String,
        val price: BigDecimal,
        val amount: Double,
    ): Serializable
}

private class WrappingArraySerializer(
    private val failure: Throwable,
): AbstractBinarySerializer() {
    override fun doSerialize(graph: Any): ByteArray = throw failure
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? = throw failure
}

private class ThrowingBinarySerializer(
    private val failure: Throwable,
): BinarySerializer {
    override fun serialize(graph: Any?): ByteArray = throw failure
    override fun <T: Any> deserialize(bytes: ByteArray?): T? = throw failure
}
