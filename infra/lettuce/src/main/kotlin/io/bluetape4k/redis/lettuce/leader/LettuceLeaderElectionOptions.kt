package io.bluetape4k.redis.lettuce.leader

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Lettuce 기반 리더 선출 옵션을 정의하는 데이터 클래스입니다.
 *
 * ```kotlin
 * val options = LettuceLeaderElectionOptions(
 *     waitTime = 10.seconds,
 *     leaseTime = 30.seconds,
 * )
 * ```
 *
 * @property waitTime  리더 획득 대기 시간 (기본값: 5초)
 * @property leaseTime 리더 유지 시간 (기본값: 60초)
 */
data class LettuceLeaderElectionOptions(
    val waitTime: Duration = 5.seconds,
    val leaseTime: Duration = 60.seconds,
)
