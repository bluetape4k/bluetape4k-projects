package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AbstractAvroTest
import io.bluetape4k.avro.AvroReflectSerializer
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.deserialize
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.XZCodec.DEFAULT_COMPRESSION
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.bluetape4k.avro.message.examples.v1.VersionedItem as ItemV1
import io.bluetape4k.avro.message.examples.v2.VersionedItem as ItemV2

@RandomizedTest
class DefaultAvroReflectSerializerTest: AbstractAvroTest() {

    companion object: KLogging()

    private fun serializers(): List<Arguments> = listOf(
        "default" to DefaultAvroReflectSerializer(),
        "deflate" to DefaultAvroReflectSerializer(CodecFactory.deflateCodec(6)),
        "zstd-3" to DefaultAvroReflectSerializer(CodecFactory.zstandardCodec(3)),
        "zstd-3-true" to DefaultAvroReflectSerializer(CodecFactory.zstandardCodec(3, true)),
        "zstd-3-true-true" to DefaultAvroReflectSerializer(CodecFactory.zstandardCodec(3, true, true)),
        "snappy" to DefaultAvroReflectSerializer(CodecFactory.snappyCodec()),
        "xz" to DefaultAvroReflectSerializer(CodecFactory.xzCodec(DEFAULT_COMPRESSION)),
        "bzip" to DefaultAvroReflectSerializer(CodecFactory.bzip2Codec()),
    ).map {
        Arguments.of(it.first, it.second)
    }

    private inline fun <reified T: Any> AvroReflectSerializer.verifySerialization(avroObject: T) {
        val bytes = serialize(avroObject)!!
        bytes.shouldNotBeEmpty()

        val converted = deserialize(bytes, T::class.java)
        converted.shouldNotBeNull()
        converted shouldBeEqualTo avroObject
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize single avro object`(name: String, serializer: AvroReflectSerializer) {
        val employee = TestMessageProvider.createEmployee()
        serializer.verifySerialization(employee)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize nested avro object`(name: String, serializer: AvroReflectSerializer) {
        val productRoot = TestMessageProvider.createProductRoot().apply {
            productProperties = fastList(20) { TestMessageProvider.createProductProperty() }
        }
        serializer.verifySerialization(productRoot)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize versioned item v1`(name: String, serializer: AvroReflectSerializer, @RandomValue item: ItemV1) {
        serializer.verifySerialization(item)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize versioned item v2`(name: String, serializer: AvroReflectSerializer, @RandomValue item: ItemV2) {
        serializer.verifySerialization(item)
    }

    // @Disabled("Reflection에서는 Subclass 로 casting 하지 못한다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize v1 and deserialize as v2`(
        name: String,
        serializer: AvroReflectSerializer,
        @RandomValue item: ItemV1,
    ) {
        val bytes = serializer.serialize(item)!!

        val convertedAsV2 = serializer.deserialize<ItemV2>(bytes)
        convertedAsV2.shouldNotBeNull()
        convertedAsV2.id shouldBeEqualTo item.id
        convertedAsV2.key shouldBeEqualTo item.key
        convertedAsV2.description.shouldBeNull()
        convertedAsV2.action shouldBeEqualTo "action"  // default value
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize V2 and deserialize as V1`(
        name: String,
        serializer: AvroReflectSerializer,
        @RandomValue item: ItemV2,
    ) {
        val bytes = serializer.serialize(item)

        val convertedAsV1 = serializer.deserialize<ItemV1>(bytes)
        convertedAsV1.shouldNotBeNull()
        convertedAsV1.id shouldBeEqualTo item.id
        convertedAsV1.key shouldBeEqualTo item.key
    }
}
