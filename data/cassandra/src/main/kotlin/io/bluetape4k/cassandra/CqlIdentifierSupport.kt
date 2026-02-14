@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.cassandra

import com.datastax.oss.driver.api.core.CqlIdentifier
import io.bluetape4k.support.requireNotBlank

/**
 * 문자열을 [CqlIdentifier]로 변환한다.
 *
 * ```
 * val id: CqlIdentifier = "name".toCqlIdentifier()
 * ```
 *
 * @return [CqlIdentifier] 인스턴스
 */
inline fun String.toCqlIdentifier(): CqlIdentifier =
    requireNotBlank("name").let { CqlIdentifier.fromInternal(it) }

/**
 * [CqlIdentifier]를 포맷된 CQL 문자열로 변환한다.
 *
 * ```
 * val id: CqlIdentifier = "name".toCqlIdentifier()
 * println(id.prettyCql()) // "name"
 * ```
 */
fun CqlIdentifier.prettyCql(): String = asCql(true)
