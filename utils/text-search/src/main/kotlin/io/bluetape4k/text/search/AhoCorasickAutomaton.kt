package io.bluetape4k.text.search

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.text.search.internal.InternalTrieConfig
import io.bluetape4k.text.search.internal.OffsetMapping
import io.bluetape4k.text.search.internal.TrieCore
import io.bluetape4k.text.search.internal.applyPipeline
import java.util.Locale

/**
 * 키워드별 값을 보관하는 Aho-Corasick 자동자(Automaton).
 *
 * 빌더([builder])로 키워드와 값을 등록한 뒤 [build][Builder.build]를 호출하여 생성한다.
 * 생성 후에는 불변(immutable)으로 동작하며 thread-safe 하게 검색에 사용할 수 있다.
 *
 * **주의**: `TrieCore` 내부 상태(failure transition 등)에 순환 참조가 존재하므로
 * `AhoCorasickAutomaton<V>`는 `Serializable`을 구현하지 않는다.
 *
 * ```kotlin
 * val automaton = AhoCorasickAutomaton.builder<String>()
 *     .add("apple", "A")
 *     .add("banana", "B")
 *     .options(SearchOptions(ignoreCase = true))
 *     .build()
 *
 * val matches = automaton.parseText("I like APPLE and Banana.")
 * // matches: [(start=7, end=11, keyword="apple", value="A"), ...]
 * ```
 *
 * @param V 키워드와 연관된 값 타입
 */
class AhoCorasickAutomaton<V> internal constructor(
    private val core: TrieCore,
    private val values: Map<String, V>,
    internal val options: SearchOptions,
) {

    companion object: KLogging() {
        /**
         * 새로운 [Builder] 인스턴스를 생성한다.
         *
         * @param V 키워드와 연관된 값 타입
         * @return 키워드/값/옵션을 등록할 수 있는 [Builder]
         */
        @JvmStatic
        fun <V> builder(): Builder<V> = Builder()
    }

    /**
     * 입력 텍스트에서 등록된 모든 키워드의 매치 결과를 반환한다.
     *
     * - [SearchOptions.ignoreCase]가 `true`이면 검색 전 텍스트를 소문자로 변환한다.
     * - [SearchOptions.stopOnFirstMatch]가 `true`이면 첫 매치만 반환한다.
     * - 결과의 키워드는 정규화 후의 형태(필요시 소문자) 이며, [values] 맵에서 값을 조회한다.
     *
     * @param text 검색할 입력 텍스트
     * @return 키워드 매치 결과 리스트 (시작 위치 기준 오름차순)
     */
    fun parseText(text: CharSequence): List<AhoCorasickMatch<V>> {
        if (text.isEmpty() || values.isEmpty()) {
            return emptyList()
        }

        // 1. 유니코드 정규화 + offset mapping 구축 (NONE이면 mapping은 null)
        val (normalizedText, mapping) = OffsetMapping.build(text, options.normalization)

        // 2. ignoreCase 적용 — Locale.ROOT 기준 소문자 변환
        val processedText: CharSequence = if (options.ignoreCase) {
            normalizedText.lowercase(Locale.ROOT)
        } else {
            normalizedText
        }

        val emits = core.parseText(processedText)
        if (emits.isEmpty()) {
            return emptyList()
        }

        val matches = ArrayList<AhoCorasickMatch<V>>(emits.size)
        for (emit in emits) {
            val keyword = emit.keyword ?: continue
            val value = values[keyword] ?: continue
            // 정규화된 offset → 원본 offset 복원
            val origStart = mapping?.toOriginal(emit.start) ?: emit.start
            val origEnd = mapping?.toOriginalEndInclusive(emit.end) ?: emit.end
            matches.add(
                AhoCorasickMatch(
                    start = origStart,
                    end = origEnd,
                    keyword = keyword,
                    value = value,
                )
            )
            if (options.stopOnFirstMatch) {
                break
            }
        }
        return matches
    }

    /**
     * 입력 텍스트의 leftmost-longest 매치(R5 규칙) 한 건을 반환한다.
     *
     * - 시작 위치가 가장 빠른 매치를 우선 선택한다.
     * - 시작 위치가 같으면 길이가 더 긴 매치를 선택한다.
     *
     * @param text 검색할 입력 텍스트
     * @return 첫 번째 매치 또는 매치가 없으면 `null`
     */
    fun firstMatch(text: CharSequence): AhoCorasickMatch<V>? {
        val matches = parseText(text)
        if (matches.isEmpty()) {
            return null
        }
        // leftmost (start ASC) → longer wins (length DESC)
        return matches.minWithOrNull(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )
    }

    /**
     * 입력 텍스트에 등록된 키워드 중 하나라도 매치되면 `true`를 반환한다.
     *
     * `parseText()`와 달리 전체 매치 목록을 생성하지 않고 첫 번째 매치 발견 시 즉시 반환하므로
     * 존재 여부만 확인할 때 더 효율적이다.
     *
     * @param text 검색할 입력 텍스트
     * @return 매치 존재 여부
     */
    fun containsMatch(text: CharSequence): Boolean {
        if (text.isEmpty() || values.isEmpty()) return false
        val (normalizedText, _) = OffsetMapping.build(text, options.normalization)
        val processedText: CharSequence = if (options.ignoreCase) {
            normalizedText.lowercase(Locale.ROOT)
        } else {
            normalizedText
        }
        return core.containsMatch(processedText)
    }

    /**
     * 입력 텍스트를 매치([SearchToken.Match])와 비매치([SearchToken.Fragment]) 토큰으로 분해한다.
     *
     * - 빈 입력은 빈 리스트를 반환한다.
     * - 매치 사이의 비매치 구간도 [SearchToken.Fragment]로 emit 된다.
     * - 마지막 매치 뒤의 꼬리 텍스트도 [SearchToken.Fragment]로 emit 된다.
     *
     * @param text 분해할 입력 텍스트
     * @return 매치/비매치 토큰 리스트 (텍스트 순서)
     */
    fun tokenize(text: CharSequence): List<SearchToken<V>> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val matches = parseText(text)
        if (matches.isEmpty()) {
            return listOf(SearchToken.Fragment(text.toString()))
        }

        val original = text.toString()
        val tokens = ArrayList<SearchToken<V>>(matches.size * 2 + 1)
        var lastEnd = 0  // exclusive cursor in `original`

        // allowOverlaps=true일 때도 tokenize는 비겹침 시퀀스를 생성해야 하므로
        // replaceAll과 동일하게 start ASC, length DESC 정렬 후 겹침 skip
        val sorted = matches.sortedWith(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )

        for (match in sorted) {
            if (match.start < lastEnd) continue  // 앞 매치와 겹치는 경우 skip
            // 비매치 구간 (lastEnd ~ match.start)
            if (match.start > lastEnd) {
                tokens.add(SearchToken.Fragment(original.substring(lastEnd, match.start)))
            }
            // 매치 구간 (match.start ~ match.end inclusive)
            val matchText = original.substring(match.start, match.end + 1)
            tokens.add(SearchToken.Match(matchText, match))
            lastEnd = match.end + 1
        }

        // 꼬리 처리
        if (lastEnd < original.length) {
            tokens.add(SearchToken.Fragment(original.substring(lastEnd)))
        }
        return tokens
    }

    /**
     * 매치된 키워드를 [transform] 결과로 모두 치환한 새 문자열을 반환한다.
     *
     * - 매치가 없으면 입력 텍스트의 [toString] 결과를 그대로 반환한다.
     * - `start ASC, length DESC` 순으로 정렬 후 처리한다.
     * - [SearchOptions.allowOverlaps]가 `true`인 경우, 이전 매치와 겹치는 매치는 skip 한다.
     *
     * @param text 치환할 입력 텍스트
     * @param transform 매치를 치환할 문자열로 변환하는 함수
     * @return 치환된 문자열
     */
    fun replaceAll(
        text: CharSequence,
        transform: (AhoCorasickMatch<V>) -> CharSequence,
    ): String {
        val matches = parseText(text)
        if (matches.isEmpty()) {
            return text.toString()
        }

        val original = text.toString()
        val sorted = matches.sortedWith(
            compareBy<AhoCorasickMatch<V>> { it.start }.thenByDescending { it.length }
        )

        val sb = StringBuilder(original.length)
        var cursor = 0  // exclusive cursor

        for (match in sorted) {
            // allowOverlaps=true 인 경우에도 치환은 비겹침이어야 함 — 이전 매치와 겹치면 skip
            if (match.start < cursor) {
                continue
            }
            // 비매치 prefix
            if (match.start > cursor) {
                sb.append(original, cursor, match.start)
            }
            sb.append(transform(match))
            cursor = match.end + 1
        }

        // 꼬리 처리
        if (cursor < original.length) {
            sb.append(original, cursor, original.length)
        }
        return sb.toString()
    }

    /**
     * [AhoCorasickAutomaton]을 단계별로 구성하는 빌더.
     *
     * 사용 예:
     * ```kotlin
     * val automaton = AhoCorasickAutomaton.builder<Int>()
     *     .add("foo", 1)
     *     .add("bar", 2)
     *     .options(SearchOptions(ignoreCase = true, allowOverlaps = false))
     *     .build()
     * ```
     *
     * @param V 키워드와 연관된 값 타입
     */
    class Builder<V> {
        companion object: KLogging()

        private val entries: MutableMap<String, V> = mutableMapOf()
        private var opts: SearchOptions = SearchOptions()

        /**
         * 키워드와 그에 대응하는 값을 등록한다.
         *
         * @param keyword 등록할 키워드 (blank 불가)
         * @param value   키워드에 연관할 값
         * @return 자기 자신 (체이닝)
         */
        fun add(keyword: String, value: V): Builder<V> = apply {
            keyword.requireNotBlank("keyword")
            entries[keyword] = value
        }

        /**
         * 여러 키워드/값 쌍을 한 번에 등록한다.
         *
         * @param map 키워드→값 맵 (각 키는 blank 불가)
         * @return 자기 자신 (체이닝)
         */
        fun addAll(map: Map<String, V>): Builder<V> = apply {
            map.forEach { (keyword, value) -> add(keyword, value) }
        }

        /**
         * 검색 옵션을 설정한다.
         *
         * @param options [SearchOptions]
         * @return 자기 자신 (체이닝)
         */
        fun options(options: SearchOptions): Builder<V> = apply {
            this.opts = options
        }

        /**
         * 등록된 키워드/값과 옵션으로 [AhoCorasickAutomaton]을 생성한다.
         *
         * - [SearchOptions.ignoreCase]가 `true`면 모든 키워드를 소문자로 정규화한 후 [TrieCore]에 등록한다.
         * - [SearchOptions]를 [InternalTrieConfig]로 매핑하여 TrieCore의 동작을 결정한다.
         *
         * @return 불변 상태의 [AhoCorasickAutomaton]
         */
        fun build(): AhoCorasickAutomaton<V> {
            // 검색 시점과 동일한 파이프라인(NFC/NFKC 정규화 + ignoreCase)을 키워드에도 적용해야
            // 매치 일관성이 보장된다.
            val normalizedValues = HashMap<String, V>(entries.size)
            entries.forEach { (keyword, value) ->
                val normalized = applyPipeline(keyword, opts)
                normalizedValues[normalized] = value
            }

            val coreBuilder = TrieCore.builder()
            normalizedValues.keys.forEach { coreBuilder.addKeyword(it) }

            // SearchOptions → TrieCore Builder 옵션 매핑
            if (opts.ignoreCase) {
                coreBuilder.ignoreCase()
            }
            if (!opts.allowOverlaps) {
                coreBuilder.ignoreOverlaps()
            }
            when (opts.wordBoundary) {
                WordBoundary.NONE -> { /* default */ }
                WordBoundary.LATIN_ALPHA -> coreBuilder.onlyWholeWords()
                WordBoundary.WHITESPACE_SEPARATED -> coreBuilder.onlyWholeWordsWhiteSpaceSeparated()
            }
            if (opts.stopOnFirstMatch) {
                coreBuilder.stopOnHit()
            }

            val core = coreBuilder.build()
            return AhoCorasickAutomaton(core, normalizedValues.toMap(), opts)
        }
    }
}
