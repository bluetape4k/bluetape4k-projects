package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.type.codec.TypeCodec
import io.bluetape4k.collections.eclipse.toUnifiedMap
import io.bluetape4k.support.EMPTY_STRING

/**
 * [Row]에서 인덱스[i]에 해당하는 컬럼의 값을 문자열 수형으로 가져옵니다. 값이 없다면 빈 문자열을 반환합니다.
 */
fun Row.getStringOrEmpty(i: Int): String = getString(i) ?: EMPTY_STRING

/**
 * [Row]에서 [name]에 해당하는 컬럼의 값을 문자열 수형으로 가져옵니다. 값이 없다면 빈 문자열을 반환합니다.
 */
fun Row.getStringOrEmpty(name: String): String = getString(name) ?: EMPTY_STRING

/**
 * [Row]에서 [id]에 해당하는 컬럼의 값을 문자열 수형으로 가져옵니다. 값이 없다면 빈 문자열을 반환합니다.
 */
fun Row.getStringOrEmpty(id: CqlIdentifier): String = getString(id) ?: EMPTY_STRING

/**
 * [Row] 정보를 [Map] 으로 변환합니다.
 */
fun Row.toMap(): Map<Int, Any?> =
    columnDefinitions
        .mapIndexed { i, definition ->
            val codec = codecRegistry().codecFor<Any?>(definition.type)
            val value =
                if (isNull(definition.name)) null
                else codec.decode(getBytesUnsafe(i), protocolVersion())

            i to value
        }
        .toUnifiedMap()

/**
 * [Row] 정보를 `Named Map` 으로 변환합니다.
 */
fun Row.toNamedMap(): Map<String, Any?> =
    columnDefinitions
        .mapIndexed { i, definition ->
            val name = definition.name.asCql(true)
            val codec = codecRegistry().codecFor<Any?>(definition.type)
            val value =
                if (isNull(definition.name)) null
                else codec.decode(getBytesUnsafe(i), protocolVersion())

            name to value
        }
        .toUnifiedMap()

/**
 * [Row] 정보를 [transform]을 통해 [Map] 으로 반환합니다.
 */
inline fun <T> Row.map(transform: (Any?) -> T): Map<Int, T> =
    columnDefinitions
        .mapIndexed { i, definition ->
            val codec = codecRegistry().codecFor<Any?>(definition.type)
            val value =
                if (isNull(definition.name)) null
                else codec.decode(getBytesUnsafe(i), protocolVersion())

            i to transform(value)
        }
        .toUnifiedMap()

/**
 * [Row] 정보를 [transform]을 통해 `Named Map` 으로 변환합니다.
 */
inline fun <T> Row.mapWithName(transform: (Any?) -> T): Map<String, T> =
    columnDefinitions
        .mapIndexed { i, definition ->
            val name = definition.name.asCql(true)
            val codec = codecRegistry().codecFor<Any?>(definition.type)
            val value =
                if (isNull(definition.name)) null
                else codec.decode(getBytesUnsafe(i), protocolVersion())

            name to transform(value)
        }
        .toUnifiedMap()

/**
 * [Row] 정보를 `CqlIdentifier 기준의 Map` 으로 변환합니다.
 */
fun Row.toCqlIdentifierMap(): Map<CqlIdentifier, Any?> =
    columnDefinitions
        .mapIndexed { i, definition ->
            val name = definition.name
            val codec = codecRegistry().codecFor<Any?>(definition.type)
            val value =
                if (isNull(name)) null
                else codec.decode(getBytesUnsafe(i), protocolVersion())

            name to value
        }
        .toUnifiedMap()

/**
 * [Row] 정보에서 컬럼 정의와 [TypeCodec]의 [Map] 으로 반환합니다.
 */
fun Row.columnCodecs(): Map<CqlIdentifier, TypeCodec<Any?>> {
    val codecRegistry = codecRegistry()
    return columnDefinitions
        .associate { columnDef ->
            val identifier = columnDef.name
            val codec = codecRegistry.codecFor<Any?>(columnDef.type)
            identifier to codec
        }
}
