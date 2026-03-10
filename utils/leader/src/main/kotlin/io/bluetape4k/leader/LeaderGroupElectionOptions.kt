package io.bluetape4k.leader

import java.io.Serializable
import java.time.Duration

data class LeaderGroupElectionOptions(
    val maxLeaders: Int = 2,
    val waitTime: Duration = Duration.ofSeconds(5),
    val leaseTime: Duration = Duration.ofSeconds(60),
): Serializable {
    companion object {
        @JvmField
        val Default = LeaderGroupElectionOptions()
    }
}
