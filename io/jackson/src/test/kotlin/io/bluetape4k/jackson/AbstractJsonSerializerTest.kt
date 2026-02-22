package io.bluetape4k.jackson

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*

@RandomizedTest
abstract class AbstractJsonSerializerTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5

        @JvmStatic
        protected val faker = Fakers.faker
    }

    protected abstract val serializer: JacksonSerializer

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize with json type info`(@RandomValue expected: Address) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Address>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize Professor`(@RandomValue expected: Professor) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Professor>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `empty name with Professor`() {
        val professor = Professor("", 0, null)
        val bytes = serializer.serialize(professor)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Professor>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo professor
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize Student`(@RandomValue expected: Student) {
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<Student>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `json serialize for User`(@RandomValue expected: User) {

        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<User>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `json serialize collection list`() {
        val expected = listOf(
            CollectionItem(1, faker.name().firstName()),
            CollectionItem(2, faker.name().firstName())
        )
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<List<CollectionItem>>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `json serialize OptionalData`() {
        val expected = OptionalData(
            name = faker.name().fullName(),
            age = faker.random().nextInt(10, 80),
            spec = Optional.of(faker.university().name())
        )
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<OptionalData>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `json serialize OptionalCollection`() {
        val expected = OptionalCollection(
            name = faker.name().fullName(),
            age = faker.random().nextInt(10, 80),
            spec = Optional.of(faker.book().title()),
            options = listOf(Optional.of("A"), Optional.of("B"), Optional.of("C"))
        )
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<OptionalCollection>(bytes).shouldNotBeNull()
        actual shouldBeEqualTo expected
    }
}
