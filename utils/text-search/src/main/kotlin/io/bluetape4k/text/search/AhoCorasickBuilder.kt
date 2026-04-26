package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank

/**
 * [AhoCorasickBuilder] DSL 빌더에 사용할 마커 어노테이션.
 *
 * `@DslMarker`를 통해 중첩 DSL 스코프에서 외부 빌더를 암묵적으로 참조하는 것을 방지한다.
 */
@DslMarker
annotation class AhoCorasickDsl

/**
 * [AhoCorasickAutomaton]을 DSL 방식으로 구성하는 빌더.
 *
 * `ahoCorasick { }` 최상위 함수를 통해 사용한다.
 *
 * ```kotlin
 * val automaton = ahoCorasick<String> {
 *     ignoreCase = true
 *     allowOverlaps = false
 *     keyword("apple", "APPLE")
 *     keyword("banana", "BANANA")
 * }
 * ```
 *
 * @param V 키워드와 연관된 값 타입
 */
@AhoCorasickDsl
class AhoCorasickBuilder<V> internal constructor() {

    companion object : KLogging()

    /** 대소문자를 무시할지 여부 (기본값: `false`) */
    var ignoreCase: Boolean = false

    /** 겹치는 매치를 허용할지 여부 (기본값: `true`) */
    var allowOverlaps: Boolean = true

    /** 단어 경계 탐지 방식 (기본값: [WordBoundary.NONE]) */
    var wordBoundary: WordBoundary = WordBoundary.NONE

    /** 유니코드 정규화 형식 (기본값: [NormalizationForm.NONE]) */
    var normalization: NormalizationForm = NormalizationForm.NONE

    /** 첫 번째 매치 발견 시 즉시 중단할지 여부 (기본값: `false`) */
    var stopOnFirstMatch: Boolean = false

    private val entries = mutableMapOf<String, V>()

    /**
     * 키워드와 그에 대응하는 값을 등록한다.
     *
     * @param keyword 등록할 키워드 (blank 불가)
     * @param value   키워드에 연관할 값
     * @throws IllegalArgumentException [keyword]가 blank이면
     */
    fun keyword(keyword: String, value: V) {
        keyword.requireNotBlank("keyword")
        entries[keyword] = value
    }

    /**
     * 여러 키워드/값 쌍을 한 번에 등록한다.
     *
     * @param pairs 키워드→값 쌍 목록 (각 키는 blank 불가)
     */
    fun keywords(vararg pairs: Pair<String, V>) {
        pairs.forEach { (k, v) -> keyword(k, v) }
    }

    /**
     * 맵으로 여러 키워드/값 쌍을 한 번에 등록한다.
     *
     * @param map 키워드→값 맵 (각 키는 blank 불가)
     */
    fun keywords(map: Map<String, V>) {
        map.forEach { (k, v) -> keyword(k, v) }
    }

    /**
     * 현재까지 등록된 설정으로 [AhoCorasickAutomaton]을 생성한다.
     *
     * @return 불변 상태의 [AhoCorasickAutomaton]
     */
    internal fun build(): AhoCorasickAutomaton<V> {
        val opts = SearchOptions(
            ignoreCase = ignoreCase,
            allowOverlaps = allowOverlaps,
            wordBoundary = wordBoundary,
            normalization = normalization,
            stopOnFirstMatch = stopOnFirstMatch,
        )
        val builder = AhoCorasickAutomaton.builder<V>()
        entries.forEach { (k, v) -> builder.add(k, v) }
        return builder.options(opts).build()
    }
}

/**
 * DSL 블록으로 [AhoCorasickAutomaton]을 생성한다.
 *
 * ```kotlin
 * val automaton = ahoCorasick<Int> {
 *     ignoreCase = true
 *     keyword("foo", 1)
 *     keyword("bar", 2)
 * }
 * ```
 *
 * @param V 키워드와 연관된 값 타입
 * @param block [AhoCorasickBuilder] DSL 블록
 * @return 생성된 [AhoCorasickAutomaton]
 */
fun <V> ahoCorasick(block: AhoCorasickBuilder<V>.() -> Unit): AhoCorasickAutomaton<V> =
    AhoCorasickBuilder<V>().apply(block).build()

/**
 * 키워드 문자열 배열로부터 `keyword == value` 매핑의 [AhoCorasickAutomaton]을 생성한다.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf("apple", "banana", "cherry")
 * ```
 *
 * @param keywords 등록할 키워드 목록 (각 키워드는 blank 불가)
 * @param options  검색 옵션 (기본값: [SearchOptions])
 * @return 생성된 [AhoCorasickAutomaton]
 */
fun ahoCorasickOf(
    vararg keywords: String,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String> =
    ahoCorasickOf(keywords.toList(), options)

/**
 * 키워드 컬렉션으로부터 `keyword == value` 매핑의 [AhoCorasickAutomaton]을 생성한다.
 *
 * ```kotlin
 * val automaton = ahoCorasickOf(listOf("apple", "banana"), SearchOptions(ignoreCase = true))
 * ```
 *
 * @param keywords 등록할 키워드 컬렉션 (각 키워드는 blank 불가)
 * @param options  검색 옵션 (기본값: [SearchOptions])
 * @return 생성된 [AhoCorasickAutomaton]
 */
fun ahoCorasickOf(
    keywords: Collection<String>,
    options: SearchOptions = SearchOptions(),
): AhoCorasickAutomaton<String> {
    val builder = AhoCorasickAutomaton.builder<String>()
    keywords.forEach { builder.add(it, it) }
    return builder.options(options).build()
}
