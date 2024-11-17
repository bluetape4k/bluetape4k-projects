package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList

private val logger = KotlinLogging.logger("io.bluetape4k.coroutines.flow.extensions.FlowLogger")

/**
 * [Flow]의 `onStart`, `onEmpty`, `onEach`, `onCompletion` 이벤트를 로깅합니다.
 *
 * ```
 * flowOf(1, 2, 3)
 *    .log("source")
 *    .collect { println(it) }
 * ```
 *
 * @param tag 로깅에 사용할 태그
 */
@Suppress("IMPLICIT_CAST_TO_ANY")
fun <T> Flow<T>.log(tag: Any, log: org.slf4j.Logger = logger): Flow<T> =
    this
        .onStart {
            log.debug { "[$tag] Start " }
        }
        .onEmpty {
            log.debug { "[$tag] Flow is empty" }
        }
        .onEach {
            val item = when (it) {
                is Flow<*> -> it.toList()
                else       -> it
            }
            log.debug { "[$tag] emit $item" }
        }
        .onCompletion {
            if (it == null) {
                log.debug { "[$tag] Completed" }
            } else {
                when (it) {
                    is CancellationException -> log.debug { "[$tag] Canceled" }
                    else                     -> log.debug(it) { "[$tag] Completed by exception" }
                }
            }
        }
