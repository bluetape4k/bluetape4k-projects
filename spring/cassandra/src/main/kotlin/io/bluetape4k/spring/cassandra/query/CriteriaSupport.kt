package io.bluetape4k.spring.cassandra.query

import org.springframework.data.cassandra.core.query.Criteria
import org.springframework.data.cassandra.core.query.CriteriaDefinition

/**
 * Criteria 중 `is` 연산자와 같은 기능을 제공하는 `eq` infix 함수입니다.
 *
 * ```
 * val criteria = Criteria.where("column") eq "value"
 * ```
 */
infix fun Criteria.eq(value: Any?): CriteriaDefinition = `is`(value)
