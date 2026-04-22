package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.RedisCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

class LettuceJsonCodecTest: AbstractLettuceTest() {

    companion object: KLogging()

    data class CustomData(
        val id: Int,
        val name: String,
    ): java.io.Serializable

    private fun getJsonCodecs(): List<LettuceJsonCodec<CustomData>> = listOf(
        LettuceJsonCodecs.jackson3(),
        LettuceJsonCodecs.fastjson2(),
    )

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getJsonCodecs")
    fun `JSON codec for kotlin data class`(codec: RedisCodec<String, CustomData>) {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin = CustomData(Random.nextInt(), Fakers.randomString(1024, 4096))

            commands.set(key, origin)
            commands.get(key) shouldBeEqualTo origin

            commands.del(key)
        }
    }

    @Test
    fun `Jackson3 codec encode_decode roundtrip without Redis`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        val original = CustomData(42, "test-data")

        val encoded = codec.encodeValue(original)
        val decoded = codec.decodeValue(encoded)

        decoded shouldBeEqualTo original
    }

    @Test
    fun `Fastjson2 codec encode_decode roundtrip without Redis`() {
        val codec = LettuceJsonCodecs.fastjson2<CustomData>()
        val original = CustomData(99, "fastjson-test")

        val encoded = codec.encodeValue(original)
        val decoded = codec.decodeValue(encoded)

        decoded shouldBeEqualTo original
    }

    @Test
    fun `encodeKey decodeKey roundtrip`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        val key = "my-redis-key"

        val encoded = codec.encodeKey(key)
        val decoded = codec.decodeKey(encoded)

        decoded shouldBeEqualTo key
    }

    @Test
    fun `encodeKey with null returns empty buffer`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        val encoded = codec.encodeKey(null)
        encoded.remaining() shouldBeEqualTo 0
    }

    @Test
    fun `decodeValue with null returns null`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        codec.decodeValue(null).shouldBeNull()
    }

    @Test
    fun `estimateSize returns -1 for value type`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        codec.estimateSize(CustomData(1, "test")) shouldBeEqualTo -1
    }

    @Test
    fun `estimateSize returns byte length for String key`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        val key = "hello"
        codec.estimateSize(key) shouldBeEqualTo key.toByteArray(Charsets.UTF_8).size
    }

    @Test
    fun `estimateSize returns 0 for null`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        codec.estimateSize(null) shouldBeEqualTo 0
    }

    @Test
    fun `toString contains serializer and valueType names`() {
        val codec = LettuceJsonCodecs.jackson3<CustomData>()
        val str = codec.toString()
        str.contains("JacksonSerializer") shouldBeEqualTo true
        str.contains("CustomData") shouldBeEqualTo true
    }

    @Test
    fun `LettuceJsonCodecs factory creates non-null codecs`() {
        LettuceJsonCodecs.jackson3<CustomData>().shouldNotBeNull()
        LettuceJsonCodecs.fastjson2<CustomData>().shouldNotBeNull()
        LettuceJsonCodecs.jackson3(CustomData::class.java).shouldNotBeNull()
        LettuceJsonCodecs.fastjson2(CustomData::class.java).shouldNotBeNull()
    }
}
