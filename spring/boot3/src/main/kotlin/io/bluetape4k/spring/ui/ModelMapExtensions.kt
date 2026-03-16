package io.bluetape4k.spring.ui

import org.springframework.ui.ModelMap

/**
 * 주어진 키-값 쌍을 [ModelMap]에 한 번에 추가합니다.
 *
 * ## 동작/계약
 * - 전달된 [pairs]를 `Map`으로 변환해 `addAllAttributes`로 추가합니다.
 * - 동일 키가 중복되면 마지막 쌍의 값이 사용됩니다.
 *
 * ```kotlin
 * modelMap.addAttributes("name" to "debop", "age" to 42)
 * // modelMap["age"] == 42
 * ```
 */
fun ModelMap.addAttributes(vararg pairs: Pair<String, Any?>): ModelMap =
    addAllAttributes(pairs.toMap())

/**
 * 주어진 키-값 쌍을 기존 [ModelMap] 속성과 병합합니다.
 *
 * ## 동작/계약
 * - 전달된 [pairs]를 `Map`으로 변환해 `mergeAttributes`에 전달합니다.
 * - 이미 존재하는 키는 Spring `mergeAttributes` 규칙을 따릅니다.
 *
 * ```kotlin
 * modelMap.mergeAttributes("enabled" to true)
 * // modelMap["enabled"] == true
 * ```
 */
fun ModelMap.mergeAttributes(vararg pairs: Pair<String, Any?>): ModelMap =
    mergeAttributes(pairs.toMap())
