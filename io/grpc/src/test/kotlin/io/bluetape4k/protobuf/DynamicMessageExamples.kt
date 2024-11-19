package io.bluetape4k.protobuf

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.nestedMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest

class DynamicMessageExamples {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse TestMessage as DynamicMessage`() {
        val origin = testMessage {
            id = Fakers.random.nextLong()
            name = Fakers.randomString(1024, 2048, true)
        }
        val bytes = origin.toByteArray()

        val descriptor: Descriptors.Descriptor = TestMessage.getDescriptor()
        val dynamicMessage: DynamicMessage = DynamicMessage.parseFrom(descriptor, bytes)

        dynamicMessage.getField(descriptor.findFieldByName("id")) shouldBeEqualTo origin.id
        dynamicMessage.getField(descriptor.findFieldByName("name")) shouldBeEqualTo origin.name
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse NestedMessage as DynamicMessage`() {
        val testMessage = testMessage {
            id = Fakers.random.nextLong()
            name = Fakers.randomString(1024, 2048, true)
        }
        val nested = nestedMessage {
            id = Fakers.random.nextLong()
            name = Fakers.randomString(1024, 2048, true)
            this.nested = testMessage
        }
        val bytes = nested.toByteArray()
        // val bytes = AnyMessage.pack(nested).toByteArray()

        val descriptor = NestedMessage.getDescriptor()
        val dynamicMessage = DynamicMessage.parseFrom(descriptor, bytes)

        dynamicMessage.shouldNotBeNull()
        dynamicMessage.getField(descriptor.findFieldByNumber(NestedMessage.ID_FIELD_NUMBER)) shouldBeEqualTo nested.id
        dynamicMessage.getField(descriptor.findFieldByNumber(NestedMessage.NAME_FIELD_NUMBER)) shouldBeEqualTo nested.name
        dynamicMessage.getField(descriptor.findFieldByNumber(NestedMessage.NESTED_FIELD_NUMBER)) shouldBeEqualTo nested.nested
    }
}
