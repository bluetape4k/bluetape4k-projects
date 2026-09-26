package io.bluetape4k.concurrent

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


fun <E: Any> LinkedBlockingQueue<E>.offer(item: E, timeout: kotlin.time.Duration): Boolean =
    offer(item, timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)

fun <E: Any> LinkedBlockingQueue<E>.poll(timeout: kotlin.time.Duration): E? =
    poll(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
