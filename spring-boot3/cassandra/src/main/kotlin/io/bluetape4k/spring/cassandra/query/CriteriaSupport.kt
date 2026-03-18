package io.bluetape4k.spring.cassandra.query

import org.springframework.data.cassandra.core.query.Criteria
import org.springframework.data.cassandra.core.query.CriteriaDefinition

/**
 * [Criteria.`is`]를 infix 형태로 호출할 수 있게 해 주는 별칭 함수입니다.
 *
 * ## 동작/계약
 * - 구현은 `is(value)`를 그대로 위임 호출하므로 생성되는 조건식은 동일합니다.
 * - 반환 타입은 [CriteriaDefinition]이며 기존 Criteria 체이닝에 그대로 연결할 수 있습니다.
 *
 * ```kotlin
 * val criteria = Criteria.where("name") eq "alice"
 * val expected = Criteria.where("name").`is`("alice")
 * // result == (criteria.toString() == expected.toString())
 * ```
 */
infix fun Criteria.eq(value: Any?): CriteriaDefinition = `is`(value)
