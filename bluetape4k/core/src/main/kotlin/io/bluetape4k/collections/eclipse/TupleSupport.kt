package io.bluetape4k.collections.eclipse

import org.eclipse.collections.impl.tuple.Tuples

/**
 * Kotlin [Pair]와 Eclipse Collections `Pair`의 타입 별칭입니다.
 *
 * ## 동작/계약
 * - 런타임에 새 타입을 만들지 않고 기존 타입의 별칭만 제공합니다.
 * - null 허용 여부는 원래 제네릭 타입의 nullability를 따릅니다.
 *
 * ```kotlin
 * val pair: EcPair<String, Int> = ("a" to 1).toTuplePair()
 * check(pair.one == "a")
 * check(pair.two == 1)
 * ```
 */
typealias EcPair<K, V> = org.eclipse.collections.api.tuple.Pair<K, V>

/**
 * Kotlin [Pair]를 Eclipse Collections [EcPair]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 [Pair]는 mutate 하지 않습니다.
 * - 항상 새 [EcPair] 인스턴스를 allocate 합니다.
 * - `first/second` 값은 그대로 복사됩니다.
 *
 * ```kotlin
 * val src = "k" to 10
 * val dst = src.toTuplePair()
 * check(dst.one == "k" && dst.two == 10)
 * ```
 */
fun <K, V> Pair<K, V>.toTuplePair(): EcPair<K, V> =
    Tuples.pair(first, second)

/**
 * Eclipse Collections [EcPair]를 Kotlin [Pair]로 변환합니다.
 *
 * ## 동작/계약
 * - 수신 [EcPair]는 mutate 하지 않습니다.
 * - 항상 새 Kotlin [Pair]를 allocate 합니다.
 * - `one/two` 값을 각각 `first/second`로 매핑합니다.
 *
 * ```kotlin
 * val src = Tuples.pair("k", 10)
 * val dst = src.toPair()
 * check(dst == ("k" to 10))
 * ```
 */
fun <K, V> EcPair<K, V>.toPair(): Pair<K, V> = one to two
