package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AbstractAvroTest
import io.bluetape4k.avro.AvroGenericRecordSerializer
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.avro.message.examples.EmployeeList
import io.bluetape4k.avro.message.examples.ProductRoot
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.XZCodec.DEFAULT_COMPRESSION
import org.apache.avro.generic.GenericData
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

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

        val emps = fastList(20) { TestMessageProvider.createEmployee() }
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
}
