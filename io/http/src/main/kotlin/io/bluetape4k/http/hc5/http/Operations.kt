package io.bluetape4k.http.hc5.http

import org.apache.hc.client5.http.impl.Operations
import org.apache.hc.core5.concurrent.Cancellable
import java.util.concurrent.Future

/**
 * [Future] 를 [Cancellable] 로 변환합니다.
 *
 * ```
 * val cancellable = future.toCancellable()
 * ```
 *
 * @receiver [Future]
 * @return [Cancellable]
 */
fun Future<*>.toCancellable(): Cancellable = Operations.cancellable(this)
