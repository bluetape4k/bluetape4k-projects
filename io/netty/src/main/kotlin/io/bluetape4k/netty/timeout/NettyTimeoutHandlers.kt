package io.bluetape4k.netty.timeout

import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import reactor.netty.Connection
import java.util.concurrent.TimeUnit

/** Reactor Netty adapter. loop/pool/SO_LINGER 같은 모듈별 튜닝은 소비자가 소유합니다. */
object NettyTimeoutHandlers {

    /** connection에 양방향 timeout handler를 설치합니다. */
    fun install(connection: Connection, policy: NettyTimeoutPolicy) {
        connection.addHandlerLast(
            ReadTimeoutHandler(policy.readTimeout.toMillis(), TimeUnit.MILLISECONDS),
        )
        connection.addHandlerLast(
            WriteTimeoutHandler(policy.writeTimeout.toMillis(), TimeUnit.MILLISECONDS),
        )
    }
}
