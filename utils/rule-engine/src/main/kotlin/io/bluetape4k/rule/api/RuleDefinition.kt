package io.bluetape4k.rule.api

import io.bluetape4k.rule.DEFAULT_RULE_DESCRIPTION
import io.bluetape4k.rule.DEFAULT_RULE_NAME
import io.bluetape4k.rule.DEFAULT_RULE_PRIORITY
import java.io.Serializable

/**
 * Rule 정의 정보를 담는 DTO입니다. (YAML/JSON/HOCON에서 읽은 Rule 정보)
 *
 * ```kotlin
 * val def = RuleDefinition(
 *     name = "discountRule",
 *     description = "1000원 이상 구매 시 할인",
 *     priority = 1,
 *     condition = "amount > 1000",
 *     actions = listOf("discount = true")
 * )
 * val rule = def.toMvelRule()
 * ```
 *
 * @property name Rule 이름
 * @property description Rule 설명
 * @property priority Rule 우선순위
 * @property condition 조건 스크립트 코드
 * @property actions 액션 스크립트 코드 목록
 */
data class RuleDefinition(
    val name: String = DEFAULT_RULE_NAME,
    val description: String = DEFAULT_RULE_DESCRIPTION,
    val priority: Int = DEFAULT_RULE_PRIORITY,
    val condition: String = "",
    val actions: List<String> = emptyList(),
): Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
