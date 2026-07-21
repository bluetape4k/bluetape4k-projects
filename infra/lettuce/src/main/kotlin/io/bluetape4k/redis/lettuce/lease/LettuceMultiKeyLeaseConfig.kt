package io.bluetape4k.redis.lettuce.lease

import java.io.Serializable

/**
 * Configures bounded multi-key lease operations. The limit bounds one Lua script's O(n) Redis work and should be
 * reviewed together with the deployment's latency characterization when changed.
 *
 * @property maxKeys maximum number of same-slot keys accepted by one operation
 */
data class LettuceMultiKeyLeaseConfig(
    val maxKeys: Int = 32,
): Serializable {
    init {
        require(maxKeys > 0) { "maxKeys must be greater than zero." }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
