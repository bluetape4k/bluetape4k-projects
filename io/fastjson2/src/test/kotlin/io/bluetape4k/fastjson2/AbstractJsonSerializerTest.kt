package io.bluetape4k.fastjson2

import io.bluetape4k.fastjson2.model.Address
import io.bluetape4k.fastjson2.model.OptionalCollection
import io.bluetape4k.fastjson2.model.OptionalData
import io.bluetape4k.fastjson2.model.Professor
import io.bluetape4k.fastjson2.model.Student
import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.*

@RandomizedTest
abstract class AbstractJsonSerializerTest: AbstractFastjson2Test() {

    companion object: KLogging()

    protected abstract val serializer: FastjsonSerializer

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

    @Test
    fun `json serialize user list`() {
        val expected = listOf(newUser(), newUser(), newUser())
        val bytes = serializer.serialize(expected)
        bytes.shouldNotBeEmpty()

        val actual = serializer.deserialize<List<User>>(bytes)
        actual.shouldNotBeNull()
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

        val actual = serializer.deserialize<OptionalData>(bytes)
        actual.shouldNotBeNull()
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

        val actual = serializer.deserialize<OptionalCollection>(bytes)
        actual.shouldNotBeNull()
        actual shouldBeEqualTo expected
    }

    @Test
    fun `json serialize for User in multi threadings`() {
        MultithreadingTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16)
            .add {
                val user = User(
                    id = faker.random().nextInt(),
                    name = faker.credentials().username()
                )
                val bytes = serializer.serialize(user)
                bytes.shouldNotBeEmpty()

                val actual = serializer.deserialize<User>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo user
            }
            .add {
                val expected = Student(
                    name = faker.name().fullName(),
                    age = faker.random().nextInt(10, 80),
                    degree = faker.university().degree()
                )
                val bytes = serializer.serialize(expected)
                val actual = serializer.deserialize<Student>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo expected
            }
            .add {
                val expected = Professor(
                    name = faker.name().fullName(),
                    age = faker.random().nextInt(10, 80),
                    spec = faker.university().name()
                )
                val bytes = serializer.serialize(expected)
                val actual = serializer.deserialize<Professor>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo expected
            }
            .run()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `json serialize for User in virtual threads`() {
        StructuredTaskScopeTester()
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val user = User(
                    id = faker.random().nextInt(),
                    name = faker.credentials().username()
                )
                val bytes = serializer.serialize(user)
                bytes.shouldNotBeEmpty()

                val actual = serializer.deserialize<User>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo user
            }
            .add {
                val expected = Student(
                    name = faker.name().fullName(),
                    age = faker.random().nextInt(10, 80),
                    degree = faker.university().degree()
                )
                val bytes = serializer.serialize(expected)
                val actual = serializer.deserialize<Student>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo expected
            }
            .add {
                val expected = Professor(
                    name = faker.name().fullName(),
                    age = faker.random().nextInt(10, 80),
                    spec = faker.university().name()
                )
                val bytes = serializer.serialize(expected)
                val actual = serializer.deserialize<Professor>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo expected
            }
            .run()
    }

    @Test
    fun `json serialize for User in suspended jobs`() = runTest {
        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(16 * 2 * Runtimex.availableProcessors)
            .add {
                val user = User(
                    id = faker.random().nextInt(),
                    name = faker.credentials().username()
                )
                val bytes = serializer.serialize(user)
                bytes.shouldNotBeEmpty()

                val actual = serializer.deserialize<User>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo user
            }
            .add {
                val expected = Student(
                    name = faker.name().fullName(),
                    age = faker.random().nextInt(10, 80),
                    degree = faker.university().degree()
                )
                val bytes = serializer.serialize(expected)
                val actual = serializer.deserialize<Student>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo expected
            }
            .add {
                val expected = Professor(
                    name = faker.name().fullName(),
                    age = faker.random().nextInt(10, 80),
                    spec = faker.university().name()
                )
                val bytes = serializer.serialize(expected)
                val actual = serializer.deserialize<Professor>(bytes)
                actual.shouldNotBeNull() shouldBeEqualTo expected
            }
            .run()
    }
}
