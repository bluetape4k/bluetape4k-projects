package io.bluetape4k.okio

import okio.AsyncTimeout
import okio.ForwardingTimeout
import okio.Timeout

interface TimeoutFactory {

    fun newTimeout(): Timeout

    companion object {
        val BASE: TimeoutFactory = object: TimeoutFactory {
            override fun newTimeout() = Timeout()
        }

        val FORWARDING: TimeoutFactory = object: TimeoutFactory {
            override fun newTimeout() = ForwardingTimeout(BASE.newTimeout())
        }

        val ASYNC: TimeoutFactory = object: TimeoutFactory {
            override fun newTimeout() = AsyncTimeout()
        }

        val factories: List<TimeoutFactory> = listOf(BASE, FORWARDING, ASYNC)
    }
}
