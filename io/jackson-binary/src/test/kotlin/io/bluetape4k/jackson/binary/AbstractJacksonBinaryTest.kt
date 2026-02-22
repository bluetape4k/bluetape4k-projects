package io.bluetape4k.jackson.binary

import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Jackson 바이너리 포맷(CBOR, ION, Smile) Serializer의 공통 직렬화/역직렬화 테스트를 정의하는 추상 클래스입니다.
 *
 * 각 바이너리 포맷별 구현체는 이 클래스를 상속받아 [binaryJacksonSerializer]를 구현하면 됩니다.
 */
@RandomizedTest
abstract class AbstractJacksonBinaryTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker

        private const val REPEAT_SIZE = 5
    }

    protected abstract val binaryJacksonSerializer: JacksonSerializer

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

    @Test
    fun `Collection list 역직렬화`() {
        val expected = listOf(
            CollectionItem(1, faker.name().firstName()),
            CollectionItem(2, faker.name().firstName())
        )
        assertBinarySerialization(expected)
    }

    @Test
    fun `OptionalData 역직렬화`() {
        val expected = OptionalData(
            name = faker.name().fullName(),
            age = faker.random().nextInt(10, 80),
            spec = Optional.of(faker.university().name())
        )
        assertBinarySerialization(expected)
    }

    @Test
    fun `OptionalCollection 역직렬화`() {
        val expected = OptionalCollection(
            name = faker.name().fullName(),
            age = faker.random().nextInt(10, 80),
            spec = Optional.of(faker.book().title()),
            options = listOf(Optional.of("A"), Optional.of("B"), Optional.of("C"))
        )
        assertBinarySerialization(expected)
    }

    // NOTE: TypeReference 를 사용하려면 reified 이어야 합니다.
    protected inline fun <reified T: Any> assertBinarySerialization(input: T) {
        val output = binaryJacksonSerializer.serialize(input)
        val actual = binaryJacksonSerializer.deserialize<T>(output).shouldNotBeNull()
        actual shouldBeEqualTo input
    }
}
