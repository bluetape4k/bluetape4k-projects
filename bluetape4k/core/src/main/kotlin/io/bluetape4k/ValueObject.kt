package io.bluetape4k

import java.io.Serializable

/**
 * 값 객체(Value Object)의 최상위 인터페이스입니다.
 *
 * DDD(도메인 주도 설계)에서 동일성(identity)이 아닌 값(value)으로 동등성을 판단하는 객체에 사용합니다.
 * `Serializable`을 구현하므로 분산 캐시(Lettuce, Redisson)에 직렬화하여 저장할 수 있습니다.
 *
 * ```kotlin
 * data class Money(val amount: Long, val currency: String) : ValueObject {
 *     companion object {
 *         private const val serialVersionUID = 1L
 *     }
 * }
 *
 * val price = Money(1000L, "KRW")
 * val same  = Money(1000L, "KRW")
 * println(price == same) // true — 값으로 동등성 판단
 * ```
 */
interface ValueObject: Serializable
