package io.bluetape4k.leader

import java.io.Serializable
import java.time.Duration

/**
 * 복수 리더 그룹 선출에 사용하는 옵션 데이터 클래스입니다.
 *
 * ```kotlin
 * val options = LeaderGroupElectionOptions(
 *     maxLeaders = 3,
 *     waitTime = Duration.ofSeconds(3),
 *     leaseTime = Duration.ofSeconds(30),
 * )
 * val election = LocalLeaderGroupElection(options)
 * val result = election.runIfLeader("batch-job") { "done" }
 * // result == "done"
 * ```
 *
 * @property maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 * @property waitTime 리더 획득 대기 최대 시간. 기본값 5초
 * @property leaseTime 리더 보유(임대) 최대 시간. 기본값 60초
 */
data class LeaderGroupElectionOptions(
    val maxLeaders: Int = 2,
    val waitTime: Duration = Duration.ofSeconds(5),
    val leaseTime: Duration = Duration.ofSeconds(60),
): Serializable {
    companion object {
        /**
         * 기본 옵션 인스턴스 (`maxLeaders=2`, `waitTime=5s`, `leaseTime=60s`).
         *
         * ```kotlin
         * val election = LocalLeaderGroupElection(LeaderGroupElectionOptions.Default)
         * ```
         */
        @JvmField
        val Default = LeaderGroupElectionOptions()
    }
}
