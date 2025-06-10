package io.bluetape4k.io.okio

import java.util.concurrent.TimeUnit

fun infiniteTimeout(): okio.Timeout =
    okio.Timeout().timeout(0, TimeUnit.NANOSECONDS)

fun java.time.Duration.toTimeout(): okio.Timeout =
    okio.Timeout().timeout(this.toNanos(), TimeUnit.NANOSECONDS)

fun java.time.Duration.toDeadline(): okio.Timeout =
    okio.Timeout().deadline(this.toNanos(), TimeUnit.NANOSECONDS)
