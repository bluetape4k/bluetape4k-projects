package io.bluetape4k.netty.timeout

import java.time.Duration

/** Netty adapter가 사용할 양방향 timeout 값입니다. */
data class NettyTimeoutPolicy(
    val readTimeout: Duration,
    val writeTimeout: Duration,
) {
    init {
        require(!readTimeout.isZero && !readTimeout.isNegative) { "readTimeout must be positive" }
        require(!writeTimeout.isZero && !writeTimeout.isNegative) { "writeTimeout must be positive" }
    }

    companion object {
        val DEFAULT: NettyTimeoutPolicy = NettyTimeoutPolicy(
            readTimeout = Duration.ofSeconds(10),
            writeTimeout = Duration.ofSeconds(10),
        )
    }
}
