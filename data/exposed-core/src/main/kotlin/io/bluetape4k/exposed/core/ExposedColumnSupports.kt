package io.bluetape4k.exposed.core

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.asByteArrayOrNull
import io.bluetape4k.support.asIntOrNull
import io.bluetape4k.support.asKotlinUuidOrNull
import io.bluetape4k.support.asLongOrNull
import io.bluetape4k.support.asShortOrNull
import io.bluetape4k.support.asStringOrNull
import io.bluetape4k.support.asUUIDOrNull
import org.jetbrains.exposed.v1.core.Column
import org.slf4j.Logger
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi

private val log: Logger by lazy { KotlinLogging.logger {} }

/**
 * 식별자 컬렉션을 지정한 컬럼의 Kotlin 타입으로 변환합니다.
 *
 * ## 동작/계약
 * - `column.getLanguageType()`를 찾지 못하면 즉시 예외를 던집니다.
 * - 각 원소는 [convertToLanguageType]로 변환하며 실패한 값은 결과에서 제외됩니다.
 * - 입력 `Iterable`은 변경하지 않고 새 리스트를 반환합니다.
 *
 * ```kotlin
 * val mapped = listOf("1", "2").mapToLanguageType(table.id)
 * // mapped.isNotEmpty() == true
 * ```
 */
fun <K: Any> Iterable<K>.mapToLanguageType(column: Column<*>): List<Any> {
    val langType = column.getLanguageType() ?: error("Column의 언어 타입을 찾을 수 없습니다. column=$column")
    return mapNotNull { id -> convertToLanguageType(id, langType) }
}

/**
 * 단일 식별자 값을 지정한 Kotlin 타입으로 변환합니다.
 *
 * ## 동작/계약
 * - 지원 타입은 `Short/Int/Long/String/UUID/ByteArray`입니다.
 * - 변환할 수 없는 값/타입이면 경고 로그를 남기고 `null`을 반환합니다.
 * - 입력 객체 자체를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val value = convertToLanguageType("42", Int::class)
 * // value == 42
 * ```
 */
@OptIn(ExperimentalUuidApi::class)
fun <K: Any> convertToLanguageType(id: K, langType: KClass<*>): Any? {
    return when (langType) {
        Short::class            -> id.asShortOrNull()
        Int::class              -> id.asIntOrNull()
        Long::class             -> id.asLongOrNull()
        String::class           -> id.asStringOrNull()
        java.util.UUID::class   -> id.asUUIDOrNull()
        kotlin.uuid.Uuid::class -> id.asKotlinUuidOrNull()
        ByteArray::class        -> id.asByteArrayOrNull()
        else                    -> {
            log.warn { "지원하지 않는 ID 타입입니다. id=$id, langType=$langType" }
            null
        }
    }
}
