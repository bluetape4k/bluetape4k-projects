package io.bluetape4k.r2dbc.convert

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter

/**
 * [Row]를 reified 타입으로 역직렬화합니다.
 *
 * ## 동작/계약
 * - `read(T::class.java, row, metadata)`를 그대로 호출합니다.
 * - [metadata]가 null이면 converter 기본 컬럼 해석 로직을 사용합니다.
 * - 매핑 불일치/필수 컬럼 누락은 converter 예외로 전파됩니다.
 *
 * ```kotlin
 * val user: User = converter.read(row, metadata)
 * // user.id > 0
 * ```
 */
inline fun <reified T> MappingR2dbcConverter.read(row: Row, metadata: RowMetadata? = null): T =
    read(T::class.java, row, metadata)

/**
 * source 타입 [T]에 대한 target 매핑 타입을 조회합니다.
 *
 * ## 동작/계약
 * - `getTargetType(T::class.java)`를 호출해 converter 내부 매핑 타입을 반환합니다.
 * - 등록된 커스텀 컨버터가 있으면 그 target 타입이 반환될 수 있습니다.
 * - 조회만 수행하며 converter 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val target = converter.getTargetType<User>()
 * // target != null
 * ```
 */
inline fun <reified T> MappingR2dbcConverter.getTargetType(): Class<*> = getTargetType(T::class.java)
