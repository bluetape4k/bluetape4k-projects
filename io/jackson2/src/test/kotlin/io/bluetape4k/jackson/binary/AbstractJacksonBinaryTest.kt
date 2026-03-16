package io.bluetape4k.jackson.binary

import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
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
    companion object : KLogging() {
        @JvmStatic
        val faker = Fakers.faker

        private const val REPEAT_SIZE = 5
    }

    protected abstract val binaryJacksonSerializer: JacksonSerializer

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize and deserialize simple POJO`(
        @RandomValue expected: FiveMinuteUser,
    ) {
        assertBinarySerialization(expected)
    }

    @Test
    fun `FiveMinuteUser - empty byteArray 경계값`() {
        val expected =
            FiveMinuteUser(
                firstName = faker.name().firstName(),
                lastName = faker.name().lastName(),
                verified = false,
                gender = Gender.FEMALE,
                userImage = ByteArray(0)
            )
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize nested POJO`(
        @RandomValue expected: Outer,
    ) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize wrapper of data class`(
        @RandomValue expected: Database,
    ) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize with jackson type info`(
        @RandomValue expected: Address,
    ) {
        assertBinarySerialization(expected)
    }

    @Test
    fun `Address - null 필드 경계값`() {
        val expected = Address(street = null, phone = null, props = emptyList())
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Professor - random data`(
        @RandomValue expected: Professor,
    ) {
        assertBinarySerialization(expected)
    }

    @Test
    fun `Professor - empty name 경계값`() {
        val expected = Professor("", 0, null)
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Student - random data`(
        @RandomValue expected: Student,
    ) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `User - random data`(
        @RandomValue expected: User,
    ) {
        assertBinarySerialization(expected)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `User - large fake data`() {
        val expected = createSampleUser(100)
        assertBinarySerialization(expected)
    }

    @Test
    fun `Box 직렬화`() {
        val expected = Box(x = 10, y = 20)
        assertBinarySerialization(expected)
    }

    @Test
    fun `Box 경계값 - 최소 정수`() {
        val expected = Box(x = Int.MIN_VALUE, y = Int.MAX_VALUE)
        assertBinarySerialization(expected)
    }

    @Test
    fun `Container 직렬화`() {
        val expected = Container(boxes = listOf(Box(1, 2), Box(3, 4)))
        assertBinarySerialization(expected)
    }

    @Test
    fun `Container - 빈 리스트 경계값`() {
        val expected = Container(boxes = emptyList())
        assertBinarySerialization(expected)
    }

    @Test
    fun `Point 직렬화`() {
        val expected = Point(x = 5, y = 7)
        assertBinarySerialization(expected)
    }

    @Test
    fun `Points 직렬화`() {
        val expected = Points(Point(0, 0), Point(1, 1), Point(2, 2))
        assertBinarySerialization(expected)
    }

    @Test
    fun `Points - 빈 목록 경계값`() {
        val expected = Points(p = emptyList())
        assertBinarySerialization(expected)
    }

    @Test
    fun `Rectangle 직렬화`() {
        val expected = Rectangle(topLeft = Point(0, 10), bottomRight = Point(10, 0))
        assertBinarySerialization(expected)
    }

    @Test
    fun `IdDesc 직렬화`() {
        val expected = IdDesc(id = faker.internet().uuid(), desc = faker.lorem().sentence())
        assertBinarySerialization(expected)
    }

    @Test
    fun `IdDesc - 빈 문자열 경계값`() {
        val expected = IdDesc(id = "", desc = "")
        assertBinarySerialization(expected)
    }

    @Test
    fun `Collection list 역직렬화`() {
        val expected =
            listOf(
                CollectionItem(1, faker.name().firstName()),
                CollectionItem(2, faker.name().firstName())
            )
        assertBinarySerialization(expected)
    }

    @Test
    fun `Collection - 빈 리스트 경계값`() {
        val expected = emptyList<CollectionItem>()
        assertBinarySerialization(expected)
    }

    @Test
    fun `OptionalData 역직렬화`() {
        val expected =
            OptionalData(
                name = faker.name().fullName(),
                age = faker.random().nextInt(10, 80),
                spec = Optional.of(faker.university().name())
            )
        assertBinarySerialization(expected)
    }

    @Test
    fun `OptionalData - Optional empty 경계값`() {
        val expected =
            OptionalData(
                name = faker.name().fullName(),
                age = faker.random().nextInt(10, 80),
                spec = Optional.empty()
            )
        assertBinarySerialization(expected)
    }

    @Test
    fun `OptionalCollection 역직렬화`() {
        val expected =
            OptionalCollection(
                name = faker.name().fullName(),
                age = faker.random().nextInt(10, 80),
                spec = Optional.of(faker.book().title()),
                options = listOf(Optional.of("A"), Optional.of("B"), Optional.of("C"))
            )
        assertBinarySerialization(expected)
    }

    @Test
    fun `OptionalCollection - 빈 options 경계값`() {
        val expected =
            OptionalCollection(
                name = faker.name().fullName(),
                age = faker.random().nextInt(10, 80),
                spec = Optional.empty(),
                options = emptyList()
            )
        assertBinarySerialization(expected)
    }

    @Test
    fun `직렬화 바이트 결과는 비어있지 않아야 한다`() {
        val input = Box(x = 1, y = 2)
        val bytes = binaryJacksonSerializer.serialize(input)
        bytes.shouldNotBeNull()
        bytes.size shouldBeGreaterThan 0
    }

    // NOTE: TypeReference 를 사용하려면 reified 이어야 합니다.
    protected inline fun <reified T : Any> assertBinarySerialization(input: T) {
        val output = binaryJacksonSerializer.serialize(input)
        output.size shouldBeGreaterThan 0
        val actual = binaryJacksonSerializer.deserialize<T>(output).shouldNotBeNull()
        actual shouldBeEqualTo input
    }
}
