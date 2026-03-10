package io.bluetape4k.redis.redisson.leader

import java.io.Serializable
import java.time.Duration

/**
 * Leader 선출 옵션을 나타냅니다.
 *
 * @property waitTime 리더 선출을 위한 대기 시간
 * @property leaseTime 리더가 작업하기 위한 임대 시간
 */
data class RedissonLeaderElectionOptions(
    val waitTime: Duration = Duration.ofSeconds(5),
    val leaseTime: Duration = Duration.ofSeconds(60),
): Serializable {

    companion object {
        /**
         * 기본 Leader 선출 옵션입니다.
         */
        @JvmField
        val Default = RedissonLeaderElectionOptions()
    }
}
