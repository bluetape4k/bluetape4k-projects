package io.bluetape4k.ktor.observability

import io.opentelemetry.sdk.common.CompletableResultCode
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun CompletableResultCode.join(timeout: Duration): CompletableResultCode =
    join(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
