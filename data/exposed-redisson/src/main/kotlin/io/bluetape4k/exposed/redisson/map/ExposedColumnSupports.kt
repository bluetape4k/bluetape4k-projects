package io.bluetape4k.exposed.redisson.map

import io.bluetape4k.exposed.sql.getLanguageType
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.asByteArrayOrNull
import io.bluetape4k.support.asIntOrNull
import io.bluetape4k.support.asLongOrNull
import io.bluetape4k.support.asShortOrNull
import io.bluetape4k.support.asStringOrNull
import io.bluetape4k.support.asUUIDOrNull
import org.jetbrains.exposed.v1.core.Column
import java.util.*
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

/**
 * Column의 언어 타입을 찾습니다.
 * @param column Column
 * @return Column의 언어 타입
 */
internal fun <K: Any> Iterable<K>.mapToLanguageType(column: Column<*>): List<Any> {
    val langType = column.getLanguageType() ?: error("Column의 언어 타입을 찾을 수 없습니다. column=$column")
    return mapNotNull { id -> convertToLanguageType(id, langType) }
}

/**
 * id를 언어 타입으로 변환합니다.
 * @param id 변환할 ID
 * @param langType 변환할 언어 타입
 * @return 변환된 언어 타입
 */
internal fun <K: Any> convertToLanguageType(id: K, langType: KClass<*>): Any? {
    return when (langType) {
        Short::class -> id.asShortOrNull()
        Int::class -> id.asIntOrNull()
        Long::class -> id.asLongOrNull()
        String::class -> id.asStringOrNull()
        UUID::class -> id.asUUIDOrNull()
        ByteArray::class -> id.asByteArrayOrNull()
        else -> {
            log.warn { "지원하지 않는 ID 타입입니다. id=$id, langType=$langType" }
            null
        }
    }
}
