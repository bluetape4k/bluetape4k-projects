package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.LibraryName
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.toList

@PublishedApi
internal val logger by lazy { KotlinLogging.logger(LibraryName) }

/**
 * Flow 생명주기(start/each/empty/completion)를 디버그 로그로 기록합니다.
 *
 * ## 동작/계약
 * - `onStart`, `onEach`, `onEmpty`, `onCompletion` 연산자를 연결한 새 Flow를 반환합니다.
 * - 요소가 `Flow<*>` 타입이면 `toList()`로 풀어서 로그 문자열을 구성하므로 추가 수집/할당이 발생할 수 있습니다.
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
@Suppress("IMPLICIT_CAST_TO_ANY")
fun <T> Flow<T>.log(tag: Any, log: org.slf4j.Logger = logger) =
    this
        .onStart {
            log.debug { "[$tag] \uD83D\uDE80" }
        }
        .onEmpty {
            log.debug { "[$tag] \uD83D\uDEAB" }
        }
        .onEach {
            val item = when (it) {
                is Flow<*> -> it.toList()
                else -> it
            }
            log.debug { "[$tag] ➡️emit $item" }
        }
        .onCompletion {
            if (it == null) {
                log.debug { "[$tag] ✅" }
            } else {
                when (it) {
                    is CancellationException -> log.debug { "[$tag] \uD83D\uDEAB" }
                    else -> log.debug(it) { "[$tag] 🔥" }
                }
            }
        }
