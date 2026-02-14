package io.bluetape4k.io.okio

import java.util.concurrent.TimeUnit

/**
 * Okio I/O에서 `infiniteTimeout` 함수를 제공합니다.
 */
fun infiniteTimeout(): okio.Timeout =
    okio.Timeout().timeout(0, TimeUnit.NANOSECONDS)

/**
 * Okio I/O 타입 변환을 위한 `toTimeout` 함수를 제공합니다.
 */
fun java.time.Duration.toTimeout(): okio.Timeout =
    okio.Timeout().timeout(this.toNanos(), TimeUnit.NANOSECONDS)

/**
 * Okio I/O 타입 변환을 위한 `toDeadline` 함수를 제공합니다.
 */
fun java.time.Duration.toDeadline(): okio.Timeout =
    okio.Timeout().deadline(this.toNanos(), TimeUnit.NANOSECONDS)
