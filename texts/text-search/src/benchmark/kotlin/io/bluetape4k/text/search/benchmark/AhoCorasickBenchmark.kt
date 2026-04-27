package io.bluetape4k.text.search.benchmark

import io.bluetape4k.text.search.AhoCorasickAutomaton
import io.bluetape4k.text.search.AhoCorasickMatch
import io.bluetape4k.text.search.ahoCorasickOf
import io.bluetape4k.text.search.flow.matchesAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * Aho-Corasick 자동자의 세 가지 검색 경로에 대한 JMH 처리량 벤치마크.
 *
 * - [parseText]: 내부 TrieCore 기반 배치 검색 — 기준선(baseline)
 * - [matchesAsFlowCollect]: `channelFlow` + `Dispatchers.Default` 기반 스트리밍 검색
 * - [naiveContains]: `String.contains` 순차 비교 — 비교군(naive baseline)
 *
 * **벤치마크 설계**:
 * - 키워드 1,000개 + 텍스트 내 키워드 매치 약 1,000회로 구성
 * - `keyword${it % 1_000}` 패턴으로 모든 토큰이 등록 키워드와 정확히 대응
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
open class AhoCorasickBenchmark {

    lateinit var matcher: AhoCorasickAutomaton<String>
    lateinit var keywords: List<String>
    lateinit var largeText: String

    @Setup(Level.Trial)
    fun setup() {
        keywords = (1..1_000).map { "keyword$it" }
        matcher = ahoCorasickOf(keywords)
        largeText = buildString {
            repeat(1_000) { append("some keyword${it % 1_000} text ") }
        }
    }

    /**
     * Aho-Corasick [AhoCorasickAutomaton.parseText]로 배치 검색 — 내부 TrieCore 직접 호출.
     *
     * @return 매치 결과 리스트 (검색 경로 유지용 반환값)
     */
    @Benchmark
    fun parseText(): List<AhoCorasickMatch<String>> = matcher.parseText(largeText)

    /**
     * [matchesAsFlow]로 Flow를 생성하고 전체 매치를 수집한다.
     *
     * `channelFlow` + `Dispatchers.Default` 오버헤드를 [parseText]와 비교한다.
     *
     * @return 수집된 매치 수 (JMH dead-code 제거 방지용 반환값)
     */
    @Benchmark
    fun matchesAsFlowCollect(): Int = runBlocking {
        matcher.matchesAsFlow(largeText).toList().size
    }

    /**
     * 순진한(naive) `String.contains` 순차 비교 — Aho-Corasick 대비 성능 열위를 확인한다.
     *
     * O(k × n) 복잡도로 키워드 수(k)와 텍스트 길이(n)에 비례한다.
     *
     * @return 매치 횟수 (JMH dead-code 제거 방지용 반환값)
     */
    @Benchmark
    fun naiveContains(): Int = keywords.count { largeText.contains(it) }
}
