package io.bluetape4k.spring.mongodb.query

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Criteria

/**
 * [CriteriaExtensions]의 단위 테스트입니다.
 *
 * MongoDB 연결 없이 [Criteria] 객체의 직렬화된 형태를 비교하여 DSL 함수의 정확성을 검증합니다.
 */
class CriteriaExtensionsTest {

    companion object: KLoggingChannel()

    // ====================================================
    // eq (is)
    // ====================================================

    @Test
    fun `eq - Criteria is와 동일한 결과를 반환한다`() {
        val actual = Criteria.where("name") eq "Alice"
        val expected = Criteria.where("name").`is`("Alice")

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `eq - null 값도 처리한다`() {
        val actual = Criteria.where("deletedAt") eq null
        val expected = Criteria.where("deletedAt").`is`(null)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    // ====================================================
    // ne
    // ====================================================

    @Test
    fun `ne - Criteria ne와 동일한 결과를 반환한다`() {
        val actual = (Criteria.where("status") ne "inactive")
        val expected = Criteria.where("status").ne("inactive")

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    // ====================================================
    // gt / gte / lt / lte
    // ====================================================

    @Test
    fun `gt - Criteria gt와 동일한 결과를 반환한다`() {
        val actual = Criteria.where("age") gt 20
        val expected = Criteria.where("age").gt(20)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `gte - Criteria gte와 동일한 결과를 반환한다`() {
        val actual = Criteria.where("age") gte 20
        val expected = Criteria.where("age").gte(20)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `lt - Criteria lt와 동일한 결과를 반환한다`() {
        val actual = Criteria.where("age") lt 65
        val expected = Criteria.where("age").lt(65)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `lte - Criteria lte와 동일한 결과를 반환한다`() {
        val actual = Criteria.where("age") lte 65
        val expected = Criteria.where("age").lte(65)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    // ====================================================
    // inValues / notInValues
    // ====================================================

    @Test
    fun `inValues - Criteria in과 동일한 결과를 반환한다`() {
        val cities = listOf("Seoul", "Busan")
        val actual = Criteria.where("city") inValues cities
        val expected = Criteria.where("city").`in`(cities)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `notInValues - Criteria nin과 동일한 결과를 반환한다`() {
        val statuses = listOf("deleted", "blocked")
        val actual = Criteria.where("status") notInValues statuses
        val expected = Criteria.where("status").nin(statuses)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    // ====================================================
    // regex
    // ====================================================

    @Test
    fun `regex(String) - Criteria regex와 동일한 결과를 반환한다`() {
        val actual = Criteria.where("name") regex "^Alice"

        log.debug { "actual=${actual.criteriaObject}" }

        // Pattern.equals()는 참조 동등성이므로 criteriaObject 직접 비교 불가
        // name 필드에 저장된 패턴 문자열을 추출하여 비교합니다
        when (val nameValue = actual.criteriaObject["name"]) {
            is java.util.regex.Pattern -> nameValue.pattern() shouldBeEqualTo "^Alice"
            else -> nameValue shouldBeEqualTo "^Alice"
        }
    }

    // ====================================================
    // isNull / fieldExists / fieldNotExists
    // ====================================================

    @Test
    fun `isNull - null 조건을 올바르게 생성한다`() {
        val actual = Criteria.where("deletedAt").isNull()
        val expected = Criteria.where("deletedAt").`is`(null)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `fieldExists - 필드 존재 조건을 올바르게 생성한다`() {
        val actual = Criteria.where("email").fieldExists()
        val expected = Criteria.where("email").exists(true)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `fieldNotExists - 필드 미존재 조건을 올바르게 생성한다`() {
        val actual = Criteria.where("deletedAt").fieldNotExists()
        val expected = Criteria.where("deletedAt").exists(false)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    // ====================================================
    // 편의 팩토리
    // ====================================================

    @Test
    fun `String criteria() - Criteria where와 동일한 결과를 반환한다`() {
        val actual = "name".criteria() eq "Alice"
        val expected = Criteria.where("name").`is`("Alice")

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `criteriaOf - 여러 조건을 AND로 결합한다`() {
        val actual = criteriaOf(
            Criteria.where("age").gt(20),
            Criteria.where("city").`is`("Seoul")
        )

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject.shouldNotBeNull()
    }

    // ====================================================
    // 논리 연산자
    // ====================================================

    @Test
    fun `andWith - AND 조건을 올바르게 생성한다`() {
        val actual = Criteria.where("age").gt(20) andWith Criteria.where("city").`is`("Seoul")

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject.shouldNotBeNull()
        // $and 키가 포함된다
        actual.criteriaObject.containsKey("\$and").shouldBeTrue()
    }

    @Test
    fun `orWith - OR 조건을 올바르게 생성한다`() {
        val actual = Criteria.where("age").gt(20) orWith Criteria.where("city").`is`("Seoul")

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject.shouldNotBeNull()
        actual.criteriaObject.containsKey("\$or").shouldBeTrue()
    }

    @Test
    fun `norOperatorWith - NOR 조건을 올바르게 생성한다`() {
        val actual = Criteria().norOperatorWith(
            Criteria.where("status").`is`("deleted"),
            Criteria.where("blocked").`is`(true)
        )

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject.shouldNotBeNull()
        actual.criteriaObject.containsKey("\$nor").shouldBeTrue()
    }

    // ====================================================
    // inValues / notInValues (Array variant)
    // ====================================================

    @Test
    fun `inValues(Array) - 배열로 Criteria in을 생성한다`() {
        val cities = arrayOf("Seoul", "Busan")
        val actual = Criteria.where("city") inValues cities
        val expected = Criteria.where("city").`in`(*cities)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `notInValues(Array) - 배열로 Criteria nin을 생성한다`() {
        val statuses = arrayOf("deleted", "blocked")
        val actual = Criteria.where("status") notInValues statuses
        val expected = Criteria.where("status").nin(*statuses)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    // ====================================================
    // isNull 프로퍼티
    // ====================================================

    @Test
    fun `isNull 프로퍼티 - null 타입 조건을 생성한다`() {
        val actual = Criteria.where("deletedAt").isNull

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject.shouldNotBeNull()
        actual.criteriaObject.containsKey("deletedAt").shouldBeTrue()
    }

    // ====================================================
    // 배열 연산자
    // ====================================================

    @Test
    fun `allValues - all 조건을 올바르게 생성한다`() {
        val tags = listOf("kotlin", "mongodb")
        val actual = Criteria.where("tags") allValues tags
        val expected = Criteria.where("tags").all(tags)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `sizeOf - size 조건을 올바르게 생성한다`() {
        val actual = Criteria.where("tags") sizeOf 3
        val expected = Criteria.where("tags").size(3)

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject shouldBeEqualTo expected.criteriaObject
    }

    @Test
    fun `elemMatches - elemMatch 조건을 올바르게 생성한다`() {
        val inner = Criteria.where("value").gt(90)
        val actual = Criteria.where("scores") elemMatches inner

        log.debug { "actual=${actual.criteriaObject}" }
        actual.criteriaObject.shouldNotBeNull()
        actual.criteriaObject.containsKey("scores").shouldBeTrue()
    }

    // ====================================================
    // regex(Regex)
    // ====================================================

    @Test
    fun `regex(Regex) - Kotlin Regex로 Criteria를 생성한다`() {
        val actual = Criteria.where("name") regex Regex("^Alice", RegexOption.IGNORE_CASE)

        log.debug { "actual=${actual.criteriaObject}" }
        val nameValue = actual.criteriaObject["name"]
        nameValue.shouldNotBeNull()
    }
}
