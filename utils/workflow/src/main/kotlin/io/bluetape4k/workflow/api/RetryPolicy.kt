package io.bluetape4k.workflow.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * 재시도 정책입니다.
 *
 * [Duration] 기반의 타입 안전한 지연 시간을 사용합니다.
 *
 * ```kotlin
 * val policy = RetryPolicy(
 *     maxAttempts = 3,
 *     delay = 100.milliseconds,
 *     backoffMultiplier = 2.0,
 *     maxDelay = 1.minutes,
 * )
 * // 최초 실행 1회 + 재시도 2회 = 총 3회 시도
 * ```
 *
 * @property maxAttempts 최대 총 시도 횟수. 최초 실행 1회 + 재시도 횟수.
 *   예: maxAttempts = 3 이면 최초 실행 1회 + 재시도 2회 = 총 3회 시도.
 *   (1 = 재시도 없음, 최초 실행만)
 * @property delay 재시도 간 대기 시간
 * @property backoffMultiplier 지수 백오프 배율 (1.0 = 고정 지연)
 * @property maxDelay 백오프 적용 시 최대 지연 시간 상한
 */
data class RetryPolicy(
    val maxAttempts: Int = 1,
    val delay: Duration = Duration.ZERO,
    val backoffMultiplier: Double = 1.0,
    val maxDelay: Duration = 1.minutes,
) {
    /** 편의 프로퍼티: 재시도 횟수 (= maxAttempts - 1) */
    val maxRetries: Int get() = maxAttempts - 1

    init {
        require(maxAttempts >= 1) { "maxAttempts는 1 이상이어야 합니다. maxAttempts=$maxAttempts" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier는 1.0 이상이어야 합니다. backoffMultiplier=$backoffMultiplier" }
    }

    companion object {
        /** 재시도 없음 (최초 실행 1회만) */
        val NONE = RetryPolicy()

        /** 기본값: 총 3회 시도 (최초 1회 + 재시도 2회), 100ms 간격, 지수 백오프 x2, 최대 1분 */
        val DEFAULT = RetryPolicy(
            maxAttempts = 3,
            delay = 100.milliseconds,
            backoffMultiplier = 2.0,
            maxDelay = 1.minutes,
        )
    }
}
