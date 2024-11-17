package io.bluetape4k.io.okio

import okio.AsyncTimeout
import okio.ForwardingTimeout
import okio.Timeout

enum class TimeoutFactory {

    BASE {
        override fun newTimeout() = Timeout()
    },
    FORWARDING {
        override fun newTimeout() = ForwardingTimeout(BASE.newTimeout())
    },
    ASYNC {
        override fun newTimeout() = AsyncTimeout()
    };

    abstract fun newTimeout(): Timeout
}
