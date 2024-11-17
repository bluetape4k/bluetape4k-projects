package io.bluetape4k

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class ValueObjectTest {

    companion object: KLogging()

    class Person(val name: String, val age: Int, val address: String? = null): AbstractValueObject() {
        override fun equalProperties(other: Any): Boolean =
            other is Person && name == other.name && age == other.age

        override fun buildStringHelper(): ToStringBuilder {
            return super.buildStringHelper()
                .add("name", name)
                .add("age", age)
                .add("address", address)
        }
    }

    private val people = listOf(
        Person("debop", 50, "hanam"),
        Person("debop", 44, "hanam"),
        Person("sunghyouk", 50),
        Person("debop", 50)
    )

    @Test
    fun `check equals value object`() {
        people[0] shouldNotBeEqualTo people[1]
        people[0] shouldNotBeEqualTo people[2]
        people[1] shouldNotBeEqualTo people[2]
        people[1] shouldNotBeEqualTo people[3]

        people[0] shouldBeEqualTo people[3]  // equal value
        people[0] shouldNotBe people[3]  // not equal reference address
    }

    @Test
    fun `value object toString`() {
        people[0].toString() shouldBeEqualTo "Person(name=debop,age=50,address=hanam)"
        people[1].toString() shouldBeEqualTo "Person(name=debop,age=44,address=hanam)"
        people[2].toString() shouldBeEqualTo "Person(name=sunghyouk,age=50,address=<null>)"
        people[3].toString() shouldBeEqualTo "Person(name=debop,age=50,address=<null>)"
    }
}
