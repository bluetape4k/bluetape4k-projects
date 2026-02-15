package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AbstractAvroTest
import io.bluetape4k.avro.AvroSpecificRecordSerializer
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.deserialize
import io.bluetape4k.avro.deserializeFromString
import io.bluetape4k.avro.deserializeList
import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.XZCodec.DEFAULT_COMPRESSION
import org.apache.avro.specific.SpecificRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.bluetape4k.avro.message.examples.v1.VersionedItem as ItemV1
import io.bluetape4k.avro.message.examples.v2.VersionedItem as ItemV2

/**
 * [DefaultAvroSpecificRecordSerializer]의 직렬화/역직렬화 기능을 검증하는 테스트입니다.
 *
 * 단일 객체, 중첩 객체, 리스트, 스키마 진화(v1 <-> v2) 등
 * 다양한 시나리오를 다양한 [CodecFactory]로 테스트합니다.
 */
@RandomizedTest
class DefaultAvroSpecificRecordSerializerTest: AbstractAvroTest() {

    companion object: KLogging()

    private fun serializers(): List<Arguments> = listOf(
        "default" to DefaultAvroSpecificRecordSerializer(),
        "deflate" to DefaultAvroSpecificRecordSerializer(CodecFactory.deflateCodec(6)),
        "zstd-3" to DefaultAvroSpecificRecordSerializer(CodecFactory.zstandardCodec(3)),
        "zstd-3-true" to DefaultAvroSpecificRecordSerializer(CodecFactory.zstandardCodec(3, true)),
        "zstd-3-true-true" to DefaultAvroSpecificRecordSerializer(CodecFactory.zstandardCodec(3, true, true)),
        "snappy" to DefaultAvroSpecificRecordSerializer(CodecFactory.snappyCodec()),
        "xz" to DefaultAvroSpecificRecordSerializer(CodecFactory.xzCodec(DEFAULT_COMPRESSION)),
        "bzip" to DefaultAvroSpecificRecordSerializer(CodecFactory.bzip2Codec()),
    ).map {
        Arguments.of(it.first, it.second)
    }

    private inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.verifySerialization(
        avroObject: T,
    ) {
        val bytes = serialize(avroObject)!!
        bytes.shouldNotBeEmpty()

        val converted = deserialize<T>(bytes)
        converted.shouldNotBeNull()
        converted shouldBeEqualTo avroObject
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize single avro object`(name: String, serializer: AvroSpecificRecordSerializer) {
        val employee = TestMessageProvider.createEmployee()
        serializer.verifySerialization(employee)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize nested avro object`(name: String, serializer: AvroSpecificRecordSerializer) {
        val productRoot = TestMessageProvider.createProductRoot().apply {
            productProperties = List(20) { TestMessageProvider.createProductProperty() }
        }
        serializer.verifySerialization(productRoot)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize versioned item v1`(
        name: String, serializer: AvroSpecificRecordSerializer,
        @RandomValue item: ItemV1,
    ) {
        serializer.verifySerialization(item)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize versioned item v2`(
        name: String, serializer: AvroSpecificRecordSerializer,
        @RandomValue item: ItemV2,
    ) {
        serializer.verifySerialization(item)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize v1 and deserialize as v2`(
        name: String, serializer: AvroSpecificRecordSerializer,
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
        name: String, serializer: AvroSpecificRecordSerializer,
        @RandomValue item: ItemV2,
    ) {
        val bytes = serializer.serialize(item)

        val convertedAsV1 = serializer.deserialize<ItemV1>(bytes)
        convertedAsV1.shouldNotBeNull()
        convertedAsV1.id shouldBeEqualTo item.id
        convertedAsV1.key shouldBeEqualTo item.key
    }

    @Test
    fun `null 입력에 대해 null을 반환한다`() {
        val serializer = DefaultAvroSpecificRecordSerializer()

        serializer.serialize(null as Employee?).shouldBeNull()
        serializer.deserialize(null, Employee::class.java).shouldBeNull()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `Base64 문자열로 직렬화 및 역직렬화`(name: String, serializer: AvroSpecificRecordSerializer) {
        val employee = TestMessageProvider.createEmployee()

        val text = serializer.serializeAsString(employee)
        text.shouldNotBeNull()
        text.shouldNotBeEmpty()
        log.trace { "Base64 text length=${text.length}" }

        val deserialized = serializer.deserializeFromString<Employee>(text)
        deserialized.shouldNotBeNull()
        deserialized shouldBeEqualTo employee
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `리스트 직렬화 및 역직렬화`(name: String, serializer: AvroSpecificRecordSerializer) {
        val employees = List(50) { TestMessageProvider.createEmployee() }

        val bytes = serializer.serializeList(employees)
        bytes.shouldNotBeNull()
        bytes.shouldNotBeEmpty()
        log.trace { "serialized list size=${bytes.size} bytes" }

        val deserialized = serializer.deserializeList<Employee>(bytes)
        deserialized.shouldNotBeEmpty()
        deserialized.size shouldBeEqualTo employees.size
        deserialized shouldBeEqualTo employees
    }

    @Test
    fun `빈 리스트 직렬화 시 null을 반환한다`() {
        val serializer = DefaultAvroSpecificRecordSerializer()

        serializer.serializeList(emptyList<Employee>()).shouldBeNull()
        serializer.serializeList<Employee>(null).shouldBeNull()
    }

    @Test
    fun `null 바이트 배열 역직렬화 시 빈 리스트를 반환한다`() {
        val serializer = DefaultAvroSpecificRecordSerializer()

        serializer.deserializeList(null, Employee::class.java).shouldBeEmpty()
        serializer.deserializeList(byteArrayOf(), Employee::class.java).shouldBeEmpty()
    }
}
