package io.bluetape4k.hibernate.querydsl.jpa

import com.querydsl.core.types.Expression
import com.querydsl.jpa.Conversions

/**
 * [Expression]을 JPA에서 사용할 수 있도록 변환합니다.
 */
fun <T> Expression<T>.convert(): Expression<T> =
    Conversions.convert(this)

/**
 * [Expression]을 Native Query에서 사용할 수 있도록 변환합니다.
 */
fun <T> Expression<T>.convertForNativeQuery(): Expression<T> =
    Conversions.convertForNativeQuery(this)
