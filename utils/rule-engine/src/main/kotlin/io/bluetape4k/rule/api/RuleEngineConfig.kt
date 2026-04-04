package io.bluetape4k.rule.api

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * Rule Engine 실행을 위한 설정 정보
 *
 * ```kotlin
 * val config = RuleEngineConfig(
 *     skipOnFirstAppliedRule = true,
 *     priorityThreshold = 100
 * )
 * ```
 *
 * @property skipOnFirstAppliedRule Rule이 처음으로 성공적으로 적용된 경우, 그 이후의 Rule은 적용하지 않습니다.
 * @property skipOnFirstFailedRule Rule이 실패한 경우, 그 이후의 Rule은 적용하지 않습니다.
 * @property skipOnFirstNonTriggeredRule Rule이 evaluation에서 false가 되면, 그 이후의 Rule은 적용하지 않습니다.
 * @property priorityThreshold Rule의 priority 값이 이 값보다 크면 적용하지 않습니다.
 */
data class RuleEngineConfig(
    val skipOnFirstAppliedRule: Boolean = false,
    val skipOnFirstFailedRule: Boolean = false,
    val skipOnFirstNonTriggeredRule: Boolean = false,
    val priorityThreshold: Int = DEFAULT_PRIORITY_THRESHOLD,
): Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L

        /**
         * 기본 Priority threshold (Int.MAX_VALUE)
         */
        const val DEFAULT_PRIORITY_THRESHOLD = Int.MAX_VALUE

        /**
         * 기본 설정 인스턴스
         */
        @JvmField
        val DEFAULT = RuleEngineConfig()
    }

    init {
        require(priorityThreshold >= 0) { "priorityThreshold는 0 이상이어야 합니다." }
    }
}
