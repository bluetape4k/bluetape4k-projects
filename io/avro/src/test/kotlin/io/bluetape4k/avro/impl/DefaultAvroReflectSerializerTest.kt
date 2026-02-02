package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AbstractAvroTest
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.deserialize
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.avro.file.CodecFactory
import org.junit.jupiter.api.RepeatedTest
import io.bluetape4k.avro.message.examples.v1.VersionedItem as ItemV1
import io.bluetape4k.avro.message.examples.v2.VersionedItem as ItemV2

@RandomizedTest
class DefaultAvroReflectSerializerTest: AbstractAvroTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val serializers = fastListOf(
        DefaultAvroReflectSerializer(),
        DefaultAvroReflectSerializer(CodecFactory.deflateCodec(6)),
        DefaultAvroReflectSerializer(CodecFactory.zstandardCodec(3)),
        DefaultAvroReflectSerializer(CodecFactory.zstandardCodec(3, true)),
        DefaultAvroReflectSerializer(CodecFactory.zstandardCodec(3, true, true)),
        DefaultAvroReflectSerializer(CodecFactory.snappyCodec()),
        DefaultAvroReflectSerializer(CodecFactory.bzip2Codec()),
    )

    private inline fun <reified T: Any> verifySerialization(avroObject: T) {
        serializers.forEach { serializer ->
            val bytes = serializer.serialize(avroObject)!!
            bytes.shouldNotBeEmpty()

            val converted = serializer.deserialize(bytes, T::class.java)
            converted.shouldNotBeNull()
            converted shouldBeEqualTo avroObject
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize single avro object`() {
        val employee = TestMessageProvider.createEmployee()
        verifySerialization(employee)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize nested avro object`() {
        val productRoot = TestMessageProvider.createProductRoot().apply {
            productProperties = fastList(20) { TestMessageProvider.createProductProperty() }
        }
        verifySerialization(productRoot)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize versioned item v1`(@RandomValue item: ItemV1) {
        verifySerialization(item)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize versioned item v2`(@RandomValue item: ItemV2) {
        verifySerialization(item)
    }

    // @Disabled("Reflection에서는 Subclass 로 casting 하지 못한다")
    @RepeatedTest(REPEAT_SIZE)
    fun `serialize v1 and deserialize as v2`(@RandomValue item: ItemV1) {
        serializers.forEach { serializer ->
            val bytes = serializer.serialize(item)!!

            val convertedAsV2 = serializer.deserialize<ItemV2>(bytes)
            convertedAsV2.shouldNotBeNull()
            convertedAsV2.id shouldBeEqualTo item.id
            convertedAsV2.key shouldBeEqualTo item.key
            convertedAsV2.description.shouldBeNull()
            convertedAsV2.action shouldBeEqualTo "action"  // default value
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize V2 and deserialize as V1`(@RandomValue item: ItemV2) {
        serializers.forEach { serializer ->
            val bytes = serializer.serialize(item)

            val convertedAsV1 = serializer.deserialize<ItemV1>(bytes)
            convertedAsV1.shouldNotBeNull()
            convertedAsV1.id shouldBeEqualTo item.id
            convertedAsV1.key shouldBeEqualTo item.key
        }
    }
}
