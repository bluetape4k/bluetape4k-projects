package io.bluetape4k.exposed.dao

import io.bluetape4k.ToStringBuilder
import org.jetbrains.exposed.v1.dao.Entity

/**
 * Exposed `EntityID` 래퍼를 벗긴 원시 식별자 값을 반환합니다.
 *
 * ## 동작/계약
 * - `id._value`를 그대로 노출하며 추가 변환을 수행하지 않습니다.
 * - 엔티티 상태를 변경하지 않는 읽기 전용 확장 프로퍼티입니다.
 *
 * ```kotlin
 * val rawId = entity.idValue
 * // rawId != null
 * ```
 */
inline val <ID: Any> Entity<ID>.idValue: Any? get() = id._value

/**
 * 두 Exposed 엔티티를 클래스 호환성과 식별자 값으로 비교합니다.
 *
 * ## 동작/계약
 * - `other == null`이면 `false`, 동일 참조(`===`)면 `true`를 반환합니다.
 * - `other`가 `Entity`일 때 `javaClass.isAssignableFrom`과 `idValue` 동등성을 함께 확인합니다.
 * - 상태를 변경하지 않으며 비교 결과만 반환합니다.
 *
 * ```kotlin
 * val same = entity.idEquals(other)
 * // same == (entity.idValue == otherEntity.idValue)
 * ```
 */
fun Entity<*>.idEquals(other: Any?): Boolean = when {
    other == null      -> false
    this === other     -> true
    // NOTE: one-to-one 관계의 id.table 값은 다를 수 있습니다. (backReferencedOn 인 경우 - BlogSchema의 Post.detail 와 PostDetail)
    other is Entity<*> -> this.javaClass.isAssignableFrom(other.javaClass) && idValue == other.idValue
    else               -> false
}

/**
 * 식별자 값 기반 hash code를 반환합니다.
 */
fun <ID: Any> Entity<ID>.idHashCode(): Int = idValue.hashCode()

/**
 * `entityToStringBuilder()`의 이전 이름입니다.
 */
@Deprecated("use entityToStringBuilder()", replaceWith = ReplaceWith("entityToStringBuilder()"))
fun <ID: Any> Entity<ID>.toStringBuilder(): ToStringBuilder =
    ToStringBuilder(this).add("id", idValue)

/**
 * 엔티티 `id`를 포함한 [ToStringBuilder]를 생성합니다.
 *
 * ## 동작/계약
 * - `ToStringBuilder(this).add("id", idValue)` 형태의 새 빌더를 반환합니다.
 * - 엔티티 자체를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val builder = entity.entityToStringBuilder()
 * // builder.toString().contains("id")
 * ```
 */
fun <ID: Any> Entity<ID>.entityToStringBuilder(): ToStringBuilder =
    ToStringBuilder(this).add("id", idValue)
