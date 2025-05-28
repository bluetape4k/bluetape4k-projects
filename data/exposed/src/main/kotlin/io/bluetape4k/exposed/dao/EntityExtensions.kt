package io.bluetape4k.exposed.dao

import io.bluetape4k.ToStringBuilder
import org.jetbrains.exposed.v1.dao.Entity

/**
 * Exposed Entity의 ID의 `_value` 값을 반환합니다.
 */
inline val <ID: Any> Entity<ID>.idValue: Any? get() = id._value

/**
 * Exposed Entity 들을 Identity 값으로 비교합니다.
 */
fun Entity<*>.idEquals(other: Any?): Boolean = when {
    other == null -> false
    this === other -> true
    // NOTE: one-to-one 관계의 id.table 값은 다를 수 있습니다. (backReferencedOn 인 경우 - BlogSchema의 Post.detail 와 PostDetail)
    other is Entity<*> -> this.javaClass.isAssignableFrom(other.javaClass) && idValue == other.idValue
    else -> false
}

/**
 * Exposed Entity 의 Id 값의 HashCode 를 반환합니다.
 */
fun <ID: Any> Entity<ID>.idHashCode(): Int = idValue.hashCode()

/**
 * Exposed Entity 를 문자열로 표현하기 위해 [ToStringBuilder] 를 생성합니다.
 */
fun <ID: Any> Entity<ID>.toStringBuilder(): ToStringBuilder =
    ToStringBuilder(this).add("id", idValue)
