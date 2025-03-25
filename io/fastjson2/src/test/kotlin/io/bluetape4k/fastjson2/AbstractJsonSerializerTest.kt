package io.bluetape4k.fastjson2

import io.bluetape4k.fastjson2.model.Address
import io.bluetape4k.fastjson2.model.Professor
import io.bluetape4k.fastjson2.model.Student
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

@RandomizedTest
abstract class AbstractJsonSerializerTest: AbstractFastjson2Test() {

    companion object: KLogging()

    protected abstract val serializer: JsonSerializer

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize with json type info`(@RandomValue expected: Address) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Address>(bytes)
        actual.shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize Professor`(@RandomValue expected: Professor) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Professor>(bytes)
        actual.shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `empty name with Professor`() {
        val professor = Professor("", 0, null)
        val bytes = serializer.serialize(professor)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Professor>(bytes)
        actual.shouldNotBeNull()
        actual shouldBeEqualTo professor
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize Student`(@RandomValue expected: Student) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Student>(bytes)
        actual.shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize for User`(@RandomValue expected: User) {

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<User>(bytes)
        actual.shouldNotBeNull()
        actual shouldBeEqualTo expected
    }
}
