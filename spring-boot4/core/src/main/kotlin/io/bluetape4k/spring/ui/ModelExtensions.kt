package io.bluetape4k.spring.ui

import org.springframework.ui.Model

/**
 * 주어진 키-값 쌍을 [Model]에 한 번에 추가합니다.
 *
 * ## 동작/계약
 * - 전달된 [pairs]를 `Map`으로 변환해 `addAllAttributes`로 추가합니다.
 * - 동일 키가 중복되면 마지막 쌍의 값이 사용됩니다.
 *
 * ```kotlin
 * model.addAttributes("name" to "debop", "age" to 42)
 * // model.asMap()["name"] == "debop"
 * ```
 */
fun Model.addAttributes(vararg pairs: Pair<String, Any?>): Model = addAllAttributes(pairs.toMap())

/**
 * 주어진 키-값 쌍을 기존 [Model] 속성과 병합합니다.
 *
 * ## 동작/계약
 * - 전달된 [pairs]를 `Map`으로 변환해 `mergeAttributes`에 전달합니다.
 * - 이미 존재하는 키는 Spring `mergeAttributes` 규칙을 따릅니다.
 *
 * ```kotlin
 * model.mergeAttributes("enabled" to true)
 * // model.asMap()["enabled"] == true
 * ```
 */
fun Model.mergeAttributes(vararg pairs: Pair<String, Any?>): Model = mergeAttributes(pairs.toMap())
