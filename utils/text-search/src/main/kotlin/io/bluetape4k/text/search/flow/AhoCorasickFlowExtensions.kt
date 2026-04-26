package io.bluetape4k.text.search.flow

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

/**
 * 텍스트에서 Aho-Corasick 매치를 [Flow]로 반환한다.
 *
 * [AhoCorasickAutomaton.parseText]를 기반으로 하며, 협력 취소(cooperative cancellation)를 지원한다.
 * `take(N)` 으로 조기 종료 시 producer가 즉시 멈춘다.
 *
 * **메모리 절감**: `channelFlow` + `flowOn(Dispatchers.Default)` 패턴으로
 * collector 측 backpressure에 따라 매치를 emit한다.
 *
 * **주의**: [AhoCorasickAutomaton.options]의 [io.bluetape4k.text.search.SearchOptions.stopOnFirstMatch]는
 * 이 Flow에서 무시된다. 첫 매치만 원하면 `take(1)`을 사용하라.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf("he", "she", "his", "hers")
 * val matches: List<AhoCorasickMatch<String>> = automaton.matchesAsFlow("ushers")
 *     .take(5)
 *     .toList()
 * ```
 *
 * @param V 키워드와 연관된 값 타입
 * @param text 검색 대상 텍스트
 * @return [AhoCorasickMatch] 스트림 ([Flow])
 */
fun <V> AhoCorasickAutomaton<V>.matchesAsFlow(text: CharSequence): Flow<AhoCorasickMatch<V>> =
    channelFlow {
        val matches = parseText(text)
        for (match in matches) {
            currentCoroutineContext().ensureActive()
            send(match)
        }
    }.flowOn(Dispatchers.Default)
