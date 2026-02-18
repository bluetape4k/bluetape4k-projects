package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AbstractAvroTest
import io.bluetape4k.avro.AvroGenericRecordSerializer
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.avro.message.examples.EmployeeList
import io.bluetape4k.avro.message.examples.ProductRoot
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.XZCodec.DEFAULT_COMPRESSION
import org.apache.avro.generic.GenericData
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * [DefaultAvroGenericRecordSerializer]의 직렬화/역직렬화 기능을 검증하는 테스트입니다.
 *
 * 다양한 [CodecFactory]를 사용한 파라미터화 테스트를 통해
 * 모든 코덱에서 정상 동작하는지 확인합니다.
 */
class DefaultAvroGenericRecordSerializerTest: AbstractAvroTest() {

    companion object: KLogging()

    private fun serializers(): List<Arguments> = listOf(
        "default" to DefaultAvroGenericRecordSerializer(),
        "deflate" to DefaultAvroGenericRecordSerializer(CodecFactory.deflateCodec(6)),
        "zstd-3" to DefaultAvroGenericRecordSerializer(CodecFactory.zstandardCodec(3)),
        "zstd-3-true" to DefaultAvroGenericRecordSerializer(CodecFactory.zstandardCodec(3, true)),
        "zstd-3-true-true" to DefaultAvroGenericRecordSerializer(CodecFactory.zstandardCodec(3, true, true)),
        "snappy" to DefaultAvroGenericRecordSerializer(CodecFactory.snappyCodec()),
        "xz" to DefaultAvroGenericRecordSerializer(CodecFactory.xzCodec(DEFAULT_COMPRESSION)),
        "bzip" to DefaultAvroGenericRecordSerializer(CodecFactory.bzip2Codec()),
    ).map {
        Arguments.of(it.first, it.second)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize employee`(name: String, serializer: AvroGenericRecordSerializer) {
        val emp = TestMessageProvider.createEmployee()
        val schema = Employee.getClassSchema()

        val bytes = serializer.serialize(schema, emp)!!
        bytes.shouldNotBeEmpty()

        val record: GenericData.Record = serializer.deserialize(schema, bytes)!!
        log.trace { "record=$record" }
        record.toString() shouldBeEqualTo emp.toString()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize collections`(name: String, serializer: AvroGenericRecordSerializer) {

        val emps = List(20) { TestMessageProvider.createEmployee() }
        val empList = EmployeeList.newBuilder().setEmps(emps).build()
        val schema = EmployeeList.getClassSchema()

        val bytes = serializer.serialize(schema, empList)!!
        bytes.shouldNotBeEmpty()

        val record: GenericData.Record = serializer.deserialize(schema, bytes)!!
        log.trace { "record=$record" }

        // generic record 는 이렇게 비교할 수 밖에 없다 (수형이 없고, map 형식이므로)
        record.toString() shouldBeEqualTo empList.toString()
    }

    @Disabled("map<string> 에 대해 key를 long type으로 해석합니다. SpecificRecord를 사용하세요")
    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `serialize nested entity`(name: String, serializer: AvroGenericRecordSerializer) {

        val producct = TestMessageProvider.createProductProperty()
        val schema = ProductRoot.getClassSchema()

        val bytes = serializer.serialize(schema, producct)!!
        bytes.shouldNotBeEmpty()

        val record: GenericData.Record = serializer.deserialize(schema, bytes)!!
        record.shouldNotBeNull()
        log.trace { "record=$record" }
    }

    @Test
    fun `null 입력에 대해 null을 반환한다`() {
        val serializer = DefaultAvroGenericRecordSerializer()
        val schema = Employee.getClassSchema()

        serializer.serialize(schema, null).shouldBeNull()
        serializer.deserialize(schema, null).shouldBeNull()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializers")
    fun `Base64 문자열로 직렬화 및 역직렬화`(name: String, serializer: AvroGenericRecordSerializer) {
        val emp = TestMessageProvider.createEmployee()
        val schema = Employee.getClassSchema()

        val text = serializer.serializeAsString(schema, emp)
        text.shouldNotBeNull()
        text.shouldNotBeEmpty()
        log.trace { "Base64 text length=${text.length}" }

        val record = serializer.deserializeFromString(schema, text)
        record.shouldNotBeNull()
        record.toString() shouldBeEqualTo emp.toString()
    }

    @Test
    fun `Base64 문자열 직렬화에서 null 입력 시 null 반환`() {
        val serializer = DefaultAvroGenericRecordSerializer()
        val schema = Employee.getClassSchema()

        serializer.serializeAsString(schema, null).shouldBeNull()
        serializer.deserializeFromString(schema, null).shouldBeNull()
    }
}
