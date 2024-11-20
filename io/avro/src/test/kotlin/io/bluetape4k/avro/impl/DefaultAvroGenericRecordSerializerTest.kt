package io.bluetape4k.avro.impl

import io.bluetape4k.avro.AbstractAvroTest
import io.bluetape4k.avro.TestMessageProvider
import io.bluetape4k.avro.message.examples.Employee
import io.bluetape4k.avro.message.examples.EmployeeList
import io.bluetape4k.avro.message.examples.ProductRoot
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.avro.file.CodecFactory
import org.apache.avro.generic.GenericData
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest

class DefaultAvroGenericRecordSerializerTest: AbstractAvroTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val serializers = listOf(
        DefaultAvroGenericRecordSerializer(),
        DefaultAvroGenericRecordSerializer(CodecFactory.deflateCodec(6)),
        DefaultAvroGenericRecordSerializer(CodecFactory.zstandardCodec(3)),
    )


    @RepeatedTest(REPEAT_SIZE)
    fun `serialize employee`() {
        serializers.forEach { serializer ->
            val emp = TestMessageProvider.createEmployee()
            val schema = Employee.getClassSchema()

            val bytes = serializer.serialize(schema, emp)!!
            bytes.shouldNotBeEmpty()

            val record: GenericData.Record = serializer.deserialize(schema, bytes)!!
            log.trace { "record=$record" }
            record.toString() shouldBeEqualTo emp.toString()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize collections`() {
        serializers.forEach { serializer ->
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
    }

    @Disabled("map<string> 에 대해 key를 long type으로 해석합니다. SpecificRecord를 사용하세요")
    @RepeatedTest(REPEAT_SIZE)
    fun `serialize nested entity`() {
        serializers.forEach { serializer ->
            val producct = TestMessageProvider.createProductProperty()
            val schema = ProductRoot.getClassSchema()

            val bytes = serializer.serialize(schema, producct)!!
            bytes.shouldNotBeEmpty()

            val record: GenericData.Record = serializer.deserialize(schema, bytes)!!
            record.shouldNotBeNull()
            log.trace { "record=$record" }
        }
    }
}
