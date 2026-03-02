@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlIdentifier
import io.bluetape4k.support.requireNotBlank

/**
 * 문자열을 내부 식별자 규칙의 [CqlIdentifier]로 변환합니다.
 *
 * ## 동작/계약
 * - `requireNotBlank("name")` 검증 후 `CqlIdentifier.fromInternal`을 호출합니다.
 * - blank 입력이면 `IllegalArgumentException`이 발생합니다.
 * - 수신 문자열을 변경하지 않고 새 식별자 객체를 반환합니다.
 *
 * ```kotlin
 * val id = "name".toCqlIdentifier()
 * // id.asInternal() == "name"
 * ```
 */
inline fun String.toCqlIdentifier(): CqlIdentifier =
    requireNotBlank("name").let { CqlIdentifier.fromInternal(it) }

/**
 * 식별자를 CQL 출력 형식 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - `asCql(true)`를 호출해 필요 시 따옴표가 포함된 CQL 표현을 반환합니다.
 * - 입력 식별자를 변경하지 않습니다.
 * - 포맷 문자열을 새로 생성해 반환합니다.
 *
 * ```kotlin
 * val cql = "name".toCqlIdentifier().prettyCql()
 * // cql == "name"
 * ```
 */
fun CqlIdentifier.prettyCql(): String = asCql(true)
