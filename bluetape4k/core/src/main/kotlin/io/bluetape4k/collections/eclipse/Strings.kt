package io.bluetape4k.collections.eclipse

import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

/**
 * 문자열의 각 문자를 [FastList] 기반 리스트로 변환합니다.
 *
 * ## 동작/계약
 * - null 수신은 허용하지 않습니다.
 * - [destination]을 mutate 하며 결과로 같은 컬렉션을 반환합니다.
 * - 문자 순서는 원본 문자열 순서를 유지합니다.
 *
 * ```kotlin
 * val list = "abc".toFastList()
 * check(list == listOf('a', 'b', 'c'))
 * check(list.size == 3)
 * ```
 *
 * @param destination 결과를 누적할 대상 리스트
 */
fun CharSequence.toFastList(destination: MutableList<Char> = FastList.newList()): List<Char> =
    toCollection(destination)


/**
 * 문자열의 각 문자를 [UnifiedSet] 기반 집합으로 변환합니다.
 *
 * ## 동작/계약
 * - null 수신은 허용하지 않습니다.
 * - [destination]을 mutate 하며 결과로 같은 컬렉션을 반환합니다.
 * - set 특성상 중복 문자는 제거됩니다.
 *
 * ```kotlin
 * val set = "abca".toUnifiedSet()
 * check(set.containsAll(listOf('a', 'b', 'c')))
 * check(set.size == 3)
 * ```
 *
 * @param destination 결과를 누적할 대상 집합
 */
fun CharSequence.toUnifiedSet(destination: MutableSet<Char> = UnifiedSet.newSet()): Set<Char> =
    toCollection(destination)
