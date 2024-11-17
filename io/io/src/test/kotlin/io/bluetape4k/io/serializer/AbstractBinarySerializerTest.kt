package io.bluetape4k.io.serializer

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.*

@RandomizedTest
abstract class AbstractBinarySerializerTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        @JvmStatic
        protected val faker = Fakers.faker
    }

    protected abstract val serializer: BinarySerializer

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

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize primitive types`(@RandomValue expected: Long) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Long>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize primitive array`(@RandomValue(type = Long::class, size = 200) numbers: List<Long>) {
        val expected = numbers.toLongArray()

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<LongArray>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize data class`(@RandomValue expected: SimpleData) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<SimpleData>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize collection of data class`(
        @RandomValue(type = SimpleData::class, size = 200) expected: List<SimpleData>,
    ) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<List<SimpleData>>(bytes)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }


    //
    // Schema Evolution
    //

    open class PersonV1(
        open val name: String,
        open val email: String,
        open val age: Int,
    ): AbstractValueObject() {
        override fun equalProperties(other: Any): Boolean =
            other is PersonV1 && name == other.name && email == other.email && age == other.age
    }

    open class PersonV2(
        override val name: String,
        override val email: String,
        override val age: Int,
        open val ssn: String = "123",
    ): PersonV1(name, email, age) {
        override fun equalProperties(other: Any): Boolean =
            other is PersonV2 && name == other.name && email == other.email && age == other.age
    }

    @Test
    fun `serialize person v1`() {
        val expected = PersonV1(
            faker.name().name(),
            faker.internet().emailAddress(),
            faker.random().nextInt(15, 99),
        )
        val actual = serializer.deserialize<PersonV1>(serializer.serialize(expected))
        actual shouldBeEqualTo expected
    }

    @Test
    fun `serialize person v2`() {
        val expected = PersonV2(
            faker.name().name(),
            faker.internet().emailAddress(),
            faker.random().nextInt(15, 99),
            faker.idNumber().ssnValid()
        )
        val actual = serializer.deserialize<PersonV2>(serializer.serialize(expected))
        actual shouldBeEqualTo expected
    }

    @Test
    fun `serialize person V1 then deserialize as person V2`() {
        val expected = PersonV1(
            faker.name().name(),
            faker.internet().emailAddress(),
            faker.random().nextInt(15, 99),
        )
        val actual = serializer.deserialize<Any>(serializer.serialize(expected))

        actual.shouldNotBeNull()
        log.debug { "actual class = ${actual.javaClass}" }
        actual shouldBeEqualTo expected
    }

    @Test
    fun `serialize person V2 then deserialize as person V1`() {
        val expected = PersonV2(
            faker.name().name(),
            faker.internet().emailAddress(),
            faker.random().nextInt(15, 99),
            faker.idNumber().ssnValid()
        )
        val actual = serializer.deserialize<PersonV1>(serializer.serialize(expected))
        actual shouldBeEqualTo expected
    }
}
