package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.LibraryName
import io.bluetape4k.coroutines.flow.AsyncFlow
import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart

internal val logger by lazy { KotlinLogging.logger(LibraryName) }

/**
 * Flow 생명주기(start/each/empty/completion)를 디버그 로그로 기록합니다.
 *
 * ## 동작/계약
 * - `onStart`, `onEach`, `onEmpty`, `onCompletion` 연산자를 연결한 새 Flow를 반환합니다.
 * - 요소가 `Flow<*>` 타입이면 `"<Flow>"`로 표시합니다 (재수집을 피함).
 * - 예외 종료는 `🔥`, 취소는 `🚫`, 정상 완료는 `✅`로 구분해 기록합니다.
 *
 * ```kotlin
 * val traced = flowOf(1, 2).log("sample")
 * traced.collect()
 * // 로그: [sample] 🚀, [sample] ➡️emit 1, [sample] ➡️emit 2, [sample] ✅
 * ```
 *
 * @param tag 로그 메시지에 포함할 태그입니다.
 * @param log 출력에 사용할 SLF4J Logger입니다.
 */
@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
fun <T> Flow<T>.log(tag: Any, log: org.slf4j.Logger = logger): Flow<T> {
    return this
        .onStart {
            log.debug { "[$tag] \uD83D\uDE80" }
        }
        .onEmpty {
            log.debug { "[$tag] \uD83D\uDEAB" }
        }
        .onEach {
            val item = when (it) {
                is Flow<*> -> "<Flow>"
                else       -> it
            }
            log.debug { "[$tag] ➡️emit $item" }
        }
        .onCompletion {
            if (it == null) {
                log.debug { "[$tag] ✅" }
            } else {
                when (it) {
                    is CancellationException -> log.debug { "[$tag] \uD83D\uDEAB" }
                    else                     -> log.debug(it) { "[$tag] 🔥" }
                }
            }
        }
}

/**
 * AsyncFlow 생명주기(start/each/empty/completion)를 디버그 로그로 기록합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Flow<T>.log()`에 위임하고, 결과를 다시 `AsyncFlow`로 감싸 반환합니다.
 * - 로그 형식과 이모지 규칙은 `Flow<T>.log()`와 동일합니다.
 *
 * ```kotlin
 * val traced = flowOf(1, 2).async { it }.log("sample")
 * traced.collect()
 * // 로그: [sample] 🚀, [sample] ➡️emit 1, [sample] ➡️emit 2, [sample] ✅
 * ```
 *
 * @param tag 로그 메시지에 포함할 태그입니다.
 * @param log 출력에 사용할 SLF4J Logger입니다.
 */
fun <T> AsyncFlow<T>.log(tag: Any, log: org.slf4j.Logger = logger): AsyncFlow<T> =
    (this as Flow<T>).log(tag, log).async { it }
