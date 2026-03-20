package io.bluetape4k.idgenerators.ulid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class UlidGeneratorTest {
    companion object : KLogging() {
        private const val REPEAT_SIZE = 5
        private const val ID_SIZE = 100

        /** Crockford Base32 문자셋 정규식 */
        private val CROCKFORD_REGEX = Regex("^[0-9A-HJKMNP-TV-Z]{26}$")
    }

    private val generator = UlidGenerator()

    @RepeatedTest(REPEAT_SIZE)
    fun `nextId는 26자 Crockford Base32 문자열을 반환한다`() {
        val id = generator.nextId()
        log.debug { "nextId=$id" }

        id.length shouldBeEqualTo 26
        CROCKFORD_REGEX.matches(id).shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `nextIdAsString은 nextId와 동일한 형식을 반환한다`() {
        val id = generator.nextIdAsString()

        id.length shouldBeEqualTo 26
        CROCKFORD_REGEX.matches(id).shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `nextULID는 ULID 값 객체를 반환한다`() {
        val ulid = generator.nextULID()
        log.debug { "nextULID=$ulid, timestamp=${ulid.timestamp}" }

        ulid.timestamp shouldBeGreaterThan 0L
        ulid.toByteArray().size shouldBeEqualTo 16
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `nextULID의 toString은 26자 문자열이다`() {
        val ulid = generator.nextULID()
        val str = ulid.toString()

        str.length shouldBeEqualTo 26
        CROCKFORD_REGEX.matches(str).shouldBeTrue()
    }

    @Test
    fun `nextIds는 요청한 크기만큼 시퀀스를 반환한다`() {
        val ids = generator.nextIds(ID_SIZE).toList()

        ids shouldHaveSize ID_SIZE
        ids.distinct() shouldHaveSize ID_SIZE
    }

    @Test
    fun `nextIdsAsString은 요청한 크기만큼 문자열 시퀀스를 반환한다`() {
        val ids = generator.nextIdsAsString(ID_SIZE).toList()

        ids shouldHaveSize ID_SIZE
        ids.forEach { id ->
            id.length shouldBeEqualTo 26
            CROCKFORD_REGEX.matches(id).shouldBeTrue()
        }
        ids.distinct() shouldHaveSize ID_SIZE
    }

    @Test
    fun `연속 생성된 ULID는 단조 증가한다 (monotonic)`() {
        val ulids = List(ID_SIZE) { generator.nextULID() }

        val sorted = ulids.sorted()
        (ulids == sorted).shouldBeTrue()
    }

    @Test
    fun `연속 생성된 ULID 문자열은 사전순 정렬된다`() {
        val ids = List(ID_SIZE) { generator.nextId() }

        val sorted = ids.sorted()
        (ids == sorted).shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `커스텀 factory로 생성된 UlidGenerator도 정상 동작한다`() {
        val customGenerator = UlidGenerator(ULID.factory())
        val id = customGenerator.nextId()

        id.length shouldBeEqualTo 26
        CROCKFORD_REGEX.matches(id).shouldBeTrue()
    }

    @Test
    fun `nextIds size 1은 단건을 반환한다`() {
        val ids = generator.nextIds(1).toList()
        ids shouldHaveSize 1
        ids.first().shouldNotBeEmpty()
    }
}
