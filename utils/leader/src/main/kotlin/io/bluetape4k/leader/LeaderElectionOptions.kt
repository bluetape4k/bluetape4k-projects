package io.bluetape4k.leader

import java.io.Serializable
import java.time.Duration

data class LeaderElectionOptions(
    val waitTime: Duration = Duration.ofSeconds(5),
    val leaseTime: Duration = Duration.ofSeconds(60),
): Serializable {
    companion object {
        @JvmField
        val Default = LeaderElectionOptions()
    }
}
