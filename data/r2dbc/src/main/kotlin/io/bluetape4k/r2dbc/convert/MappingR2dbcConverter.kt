package io.bluetape4k.r2dbc.convert

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter

/**
 * [MappingR2dbcConverter]의 확장 함수로서 [row]를 읽어 [T] 타입으로 변환한다.
 */
inline fun <reified T> MappingR2dbcConverter.read(row: Row, metadata: RowMetadata? = null): T =
    read(T::class.java, row, metadata)

/**
 * [MappingR2dbcConverter]의 `getTargetType` 함수
 */
inline fun <reified T> MappingR2dbcConverter.getTargetType(): Class<*> = getTargetType(T::class.java)
