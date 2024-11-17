package io.bluetape4k.coroutines.flow.extensions.utils

/**
 * 디버거에서 설명이 필요 없는 고유상수를 정의하는 데 사용되는 심볼 클래스입니다.
 *
 * @see [kotlinx.coroutines.internal.Symbol]
 */
data class Symbol(@JvmField val symbol: String) {
    override fun toString(): String = "<$symbol>"

    @Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
    inline fun <T> unbox(value: Any?): T = if (value === this) null as T else value as T
}
