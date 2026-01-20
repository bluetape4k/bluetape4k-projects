package io.bluetape4k.jackson3.binary

import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest

@RandomizedTest
abstract class AbstractJacksonBinaryTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker

        private const val REPEAT_SIZE = 5
    }

    protected abstract val binaryJacksonSerializer: JsonSerializer

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize and deserialize simple POJO`(@RandomValue expected: FiveMinuteUser) {
        assertBinarySerialization(expected)
    }


    @RepeatedTest(REPEAT_SIZE)
    fun `serialize nested POJO`(@RandomValue expected: Outer) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize wrapper of data class`(@RandomValue expected: Database) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize with jackson type info`(@RandomValue expected: Address) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Professor - random data`(@RandomValue expected: Professor) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Professor - empty name`() {
        val expected = Professor("", 0, null)
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Student - random data`(@RandomValue expected: Student) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `User - random data`(@RandomValue expected: User) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `User - large fake data`() {
        val expected = createSampleUser(100)
        assertBinarySerialization(expected)
    }

    // NOTE: TypeReference 를 사용하려면 reified 이어야 합니다.
    protected inline fun <reified T: Any> assertBinarySerialization(input: T) {
        val output = binaryJacksonSerializer.serialize(input)
        val actual = binaryJacksonSerializer.deserialize<T>(output).shouldNotBeNull()
        actual shouldBeEqualTo input
    }
}
