package io.bluetape4k.leader

import java.io.Serializable
import java.time.Duration

/**
 * 리더 선출에 사용하는 옵션 데이터 클래스입니다.
 *
 * ```kotlin
 * val options = LeaderElectionOptions(
 *     waitTime = Duration.ofSeconds(3),
 *     leaseTime = Duration.ofSeconds(30),
 * )
 * val election = LocalLeaderElection(options)
 * val result = election.runIfLeader("job-lock") { "done" }
 * // result == "done"
 * ```
 *
 * @property waitTime 리더 획득 대기 최대 시간. 기본값 5초
 * @property leaseTime 리더 보유(임대) 최대 시간. 기본값 60초
 */
data class LeaderElectionOptions(
    val waitTime: Duration = Duration.ofSeconds(5),
    val leaseTime: Duration = Duration.ofSeconds(60),
): Serializable {
    companion object {
        /**
         * 기본 옵션 인스턴스 (`waitTime=5s`, `leaseTime=60s`).
         *
         * ```kotlin
         * val election = LocalLeaderElection(LeaderElectionOptions.Default)
         * ```
         */
        @JvmField
        val Default = LeaderElectionOptions()
    }
}
