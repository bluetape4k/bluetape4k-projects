package io.bluetape4k.okio

import java.util.concurrent.TimeUnit

/**
 * 시간 제한이 없는 [okio.Timeout]을 생성합니다.
 *
 * ```kotlin
 * val timeout = infiniteTimeout()
 * val hasDeadline = timeout.hasDeadline()
 * // hasDeadline == false
 * ```
 */
fun infiniteTimeout(): okio.Timeout =
    okio.Timeout().timeout(0, TimeUnit.NANOSECONDS)

/**
 * [java.time.Duration]을 타임아웃으로 사용하는 [okio.Timeout]으로 변환합니다.
 *
 * ```kotlin
 * val timeout = java.time.Duration.ofSeconds(5).toTimeout()
 * val nanos = timeout.timeoutNanos()
 * // nanos == 5_000_000_000L
 * ```
 */
fun java.time.Duration.toTimeout(): okio.Timeout =
    okio.Timeout().timeout(this.toNanos(), TimeUnit.NANOSECONDS)

/**
 * [java.time.Duration]을 데드라인으로 사용하는 [okio.Timeout]으로 변환합니다.
 *
 * ```kotlin
 * val timeout = java.time.Duration.ofSeconds(10).toDeadline()
 * val hasDeadline = timeout.hasDeadline()
 * // hasDeadline == true
 * ```
 */
fun java.time.Duration.toDeadline(): okio.Timeout =
    okio.Timeout().deadline(this.toNanos(), TimeUnit.NANOSECONDS)
