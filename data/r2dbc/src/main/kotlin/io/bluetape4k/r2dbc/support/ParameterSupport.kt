package io.bluetape4k.r2dbc.support

import io.r2dbc.spi.Parameter
import io.r2dbc.spi.Parameters

/**
 * 수신 객체를 R2DBC [Parameter]로 변환합니다.
 *
 * 이미 [Parameter] 타입이면 그대로 반환하고, 그 외 타입은 [Parameters.in] 으로 래핑합니다.
 */
@PublishedApi
internal fun Any.toParameter(): Parameter =
    when (this) {
        is Parameter -> this
        else -> Parameters.`in`(this)
    }

/**
 * nullable 값을 R2DBC [Parameter]로 변환합니다.
 *
 * - null 이면 타입 정보를 보존하는 null [Parameter]를 반환합니다.
 * - 이미 [Parameter] 타입이면 그대로 반환합니다.
 * - 그 외 타입은 [Parameters.in] 으로 래핑합니다.
 *
 * @param type 파라미터 값의 Java 클래스 타입
 */
@PublishedApi
internal fun <V : Any> Any?.toParameter(type: Class<V>): Parameter =
    when (this) {
        null -> Parameters.`in`(type)
        is Parameter -> this
        else -> Parameters.`in`(this)
    }

/**
 * [Class]를 null 값을 담는 R2DBC [Parameter]로 변환합니다.
 */
@PublishedApi
internal fun Class<*>.toParameter(): Parameter = Parameters.`in`(this)
