package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart

private val logger = KotlinLogging.logger { }

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
            log.debug { "[$tag] \uD83D\uDE80" }  // Flow 시작
        }
        .onEmpty {
            log.debug { "[$tag] \uD83D\uDEAB" }  // Empty flow
        }
        .onEach {
            val item = when (it) {
                is Flow<*> -> it.toFastList()
                else       -> it
            }
            log.debug { "[$tag] ➡️emit $item" } // Flow의 각 아이템을 emit할 때 로깅
        }
        .onCompletion {
            if (it == null) {
                log.debug { "[$tag] ✅" }
            } else {
                when (it) {
                    is CancellationException -> log.debug { "[$tag] \uD83D\uDEAB" }   // Flow가 취소되었을 때 로깅
                    else -> log.debug(it) { "[$tag] 🔥" } // Flow가 예외로 종료되었을 때 로깅
                }
            }
        }
