package io.bluetape4k.io.serializer

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.math.BigDecimal
import java.util.*
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

    @ParameterizedTest
    @MethodSource("getSerializers")
    fun `serialize and compress string`(serializer: BinarySerializer) {
        val origin = Fakers.faker.lorem().paragraph(100).repeat(4)
        val compressed = serializer.serialize(origin)
        log.debug { "origin=${BinarySerializers.Jdk.serialize(origin).size}, compressed=${compressed.size}" }

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
        BinarySerializers.Jdk.serialize(this).size

    private fun List<SimpleData>.memorySize(): Int =
        BinarySerializers.Jdk.serialize(this).size


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
