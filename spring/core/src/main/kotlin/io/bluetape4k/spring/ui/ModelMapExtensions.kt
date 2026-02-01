package io.bluetape4k.spring.ui

import io.bluetape4k.collections.eclipse.toUnifiedMap
import org.springframework.ui.ModelMap

/**
 * [ModelMap]에 속성을 추가합니다.
 *
 * ```
 * model.addAttribute("name" to "value", "name2" to "value2")
 * ```
 *
 * @param pairs 속성 목록
 * @return [ModelMap] 인스턴스
 */
fun ModelMap.addAttributes(vararg pairs: Pair<String, Any?>): ModelMap =
    addAllAttributes(pairs.toUnifiedMap())

/**
 * [ModelMap]에 속성을 합칩니다.
 *
 * ```
 * model.mergeAttributes("name" to "value", "name2" to "value2")
 * ```
 *
 * @param pairs 속성 목록
 * @return [ModelMap] 인스턴스
 */
fun ModelMap.mergeAttributes(vararg pairs: Pair<String, Any?>): ModelMap =
    mergeAttributes(pairs.toUnifiedMap())
