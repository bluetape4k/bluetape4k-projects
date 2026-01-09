package io.bluetape4k.io.serializer

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
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

    @ParameterizedTest(name = "serialize and compress object - {0}")
    @MethodSource("getSerializers")
    fun `serialize and compress object`(serializer: BinarySerializer, @RandomValue origin: SimpleData) {
        val compressed = serializer.serialize(origin)
        val actual = serializer.deserialize<SimpleData>(compressed)

        log.debug { "compressed size=${compressed.size}" }
        actual.shouldNotBeNull() shouldBeEqualTo origin
    }

    @ParameterizedTest(name = "serialize and compress collection - {0}")
    @MethodSource("getSerializers")
    fun `serialize and compress collection`(
        serializer: BinarySerializer,
        @RandomValue(type = SimpleData::class, size = 500) origins: List<SimpleData>,
    ) {
        val compressed = serializer.serialize(origins)
        val actual = serializer.deserialize<List<SimpleData>>(compressed)

        log.debug { "Compressed size=${compressed.size}" }
        actual.shouldNotBeNull() shouldBeEqualTo origins
    }

    data class SimpleData(
        val id: Long,
        val name: String,
        val age: Int,
        val birth: Date,
        val biography: String,
        val zip: String,
        val address: String,
        val amount: Double? = null,  // NOTE: Fury 가 BigDecimal, BigInteger를 지원하지 않음
    ): Serializable
}
