package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.ahocorasick.interval.IntervalTree
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.util.*

/**
 * Trie
 *
 * Based on the Aho-Corasick white paper, [Bell technologies](http://cr.yp.to/bib/1975/aho.pdf)
 *
 * ```
 * val trie = Trie.builder()
 *     .addKeywords("NYC")
 *     .addKeywords("APPL")
 *     .addKeywords("java_2e", "java programming")
 *     .addKeywords("PM", "product manager")
 *     .build()
 *
 * val text = "I am a PM for a java_2e platform working from APPL, NYC"
 * val emits = trie.parseText(text)
 *
 * emits shouldBeEqualTo listOf(
 *     Emit(7, 8, "PM"),
 *     Emit(16, 22, "java_2e"),
 *     Emit(46, 49, "APPL"),
 *     Emit(52, 54, "NYC")
 * )
 * ```
 *
 * ## 동작/계약
 * - [build] 시점에 failure transition을 구성한 뒤 검색을 수행합니다.
 * - 옵션([TrieConfig])에 따라 대소문자 무시/겹침 제거/단어 경계 필터를 적용합니다.
 * - 검색 결과는 [Emit]의 시작 위치 기준으로 처리됩니다.
 *
 * @property config Trie 환경설정 (기본값: [TrieConfig.DEFAULT])
 */
class Trie(private val config: TrieConfig = TrieConfig.DEFAULT) {

    companion object: KLogging() {
        @JvmStatic
        /** [TrieBuilder]를 생성합니다. */
        fun builder(): TrieBuilder = TrieBuilder()
    }

    private val rootState = State()

    private val ignoreCase: Boolean get() = config.ignoreCase

    /**
     * 문장에서 키워드를 치환합니다.
     *
     * ```
     * val map = mapOf(
     *    "APPL" to "Apple",
     *    "NYC" to "New york",
     *    "java_2e" to "java programming",
     *    "PM" to "product manager"
     * )
     *
     * val replaced = trie.replace("I am a PM for a java_2e platform working from APPL, NYC", map)
     * replaced shouldBeEqualTo "I am a product manager for a java programming platform working from Apple, New york"
     * ```
     *
     * @param text 치환할 문자열
     * @param map 치환할 키워드 맵
     * @return 치환된 문자열
     *
     * ## 동작/계약
     * - [tokenize] 결과를 순회하며 매칭 토큰만 [map] 값으로 치환합니다.
     * - 키가 없는 매칭은 원문 fragment를 유지합니다.
     */
    fun replace(text: String, map: Map<String, String>): String {
        val tokens = tokenize(text)

        return buildString {
            tokens.forEach { token ->
                val keyword = token.emit?.keyword
                if (keyword != null && map.containsKey(keyword)) {
                    append(map[keyword])
                } else {
                    append(token.fragment)
                }
            }
        }
    }

    /**
     * 문장을 형태소 분석합니다.
     *
     * ```
     * val GREEK_LETTERS = listOf("Alpha", "Beta", "Gamma")
     * val trie = Trie.builder()
     *     .addKeywords(GREEK_LETTERS)
     *     .build()
     *
     * val tokens = trie.tokenize("Alpha Beta Gamma").toList()
     * log.debug { "tokens=$tokens" }
     * tokens.size shouldBeEqualTo 5   // 2 space
     * ```
     *
     * @param text 형태소 분석할 문자열
     * @return 형태소 리스트
     *
     * ## 동작/계약
     * - 매칭 구간은 [MatchToken], 비매칭 구간은 [FragmentToken]으로 반환합니다.
     * - 빈 입력은 빈 리스트를 반환합니다.
     */
    fun tokenize(
        text: String,
        destination: MutableList<Token> = mutableListOf(),
    ): List<Token> {
        if (text.isEmpty()) {
            return emptyList()
        }
        var lastCollectionIndex = -1
        val collectedEmits = parseText(text)

        collectedEmits
            .forEach { emit ->
                if (emit.start - lastCollectionIndex > 1) {
                    destination.add(createFragment(emit, text, lastCollectionIndex))
                }
                destination.add(createMatch(emit, text))
                lastCollectionIndex = emit.end
            }

        if (text.length - lastCollectionIndex > 1) {
            destination.add(createFragment(null, text, lastCollectionIndex))
        }
        return destination
    }

    /**
     * 문장을 파싱하여 키워드를 추출합니다.
     *
     * ```
     * val trie = Trie.builder()
     *     .addKeywords("NYC")
     *     .addKeywords("APPL")
     *     .addKeywords("java_2e", "java programming")
     *     .addKeywords("PM", "product manager")
     *     .build()
     *
     * val text = "I am a PM for a java_2e platform working from APPL, NYC"
     * val emits = trie.parseText(text)
     *
     * emits shouldBeEqualTo listOf(
     *     Emit(7, 8, "PM"),
     *     Emit(16, 22, "java_2e"),
     *     Emit(46, 49, "APPL"),
     *     Emit(52, 54, "NYC")
     * )
     * ```
     *
     * @param text 키워드를 찾을 문자열
     * @param emitHandler Emit 처리기 (기본값: [DefaultEmitHandler])
     * @return Emit 리스트
     *
     * ## 동작/계약
     * - 내부 파싱 후 config 옵션에 따라 partial/overlap 필터를 적용합니다.
     * - [allowOverlaps]가 `false`면 [IntervalTree]로 겹침을 제거합니다.
     */
    fun parseText(text: CharSequence, emitHandler: StatefulEmitHandler = DefaultEmitHandler()): List<Emit> {
        runParseText(text, emitHandler)
        var collectedEmits = emitHandler.emits

        if (config.onlyWholeWords) {
            removePartialMatches(text, collectedEmits)
            log.trace { "onlyWholeWords : collectedEmits=$collectedEmits" }
        }
        if (config.onlyWholeWordsWhiteSpaceSeparated) {
            removePartialMatchesWhiteSpaceSeparated(text, collectedEmits)
            log.trace { "onlyWholeWordsWhiteSpaceSeparated : collectedEmits=$collectedEmits" }
        }
        if (!config.allowOverlaps) {
            val intervalTree = IntervalTree(collectedEmits)
            collectedEmits = intervalTree.removeOverlaps(collectedEmits)
            log.trace { "!allowOverlaps : collectedEmits=$collectedEmits" }
        }

        return collectedEmits
    }

    /** 텍스트에 매칭 키워드가 하나라도 있으면 `true`를 반환합니다. */
    fun containsMatch(text: CharSequence): Boolean = firstMatch(text) != null

    /**
     * [text]를 파싱하고, [emitHandler]를 통해 Emit을 처리합니다.
     *
     * ```
     *  val PRONOUNS = listOf("hers", "his", "she", "he")
     *  val trie = Trie.builder()
     *      .addKeywords(PRONOUNS)
     *      .build()
     *
     *  val emits = mutableListOf<Emit>()
     *  val emitHandler = EmitHandler { emit -> emits }
     *
     *  trie.runParseText("ushers", emitHandler)
     *
     *  emits.size shouldBeEqualTo 3
     *  checkEmit(emits[0], 2, 3, "he")
     *  checkEmit(emits[1], 1, 3, "she")
     *  checkEmit(emits[2], 2, 5, "hers")
     *  ```
     *
     *  @param text 키워드를 찾을 문자열
     *  @param emitHandler Emit 처리기
     *
     *  ## 동작/계약
     *  - 문자 단위로 상태 전이를 진행하고 각 상태 emit을 [emitHandler]에 전달합니다.
     *  - [config.stopOnHit]가 `true`이고 handler가 `true`를 반환하면 즉시 중단합니다.
     */
    fun runParseText(text: CharSequence, emitHandler: EmitHandler) {
        var currentState = rootState

        text.forEachIndexed { pos, ch ->
            currentState = when {
                config.ignoreCase -> getState(currentState, ch.lowercaseChar())
                else -> getState(currentState, ch)
            }
            val stored = storeEmits(pos, currentState, emitHandler)
            if (stored && config.stopOnHit) {
                return
            }
        }
    }

    /**
     * 문장에서 첫 번째 매치를 찾습니다.
     *
     * ```
     * val UNICODE = listOf("turning", "once", "again", "börkü")
     *
     * val trie = Trie.builder()
     *     .ignoreCase()
     *     .onlyWholeWords()
     *     .addKeywords(UNICODE)
     *     .build()
     *
     * val firstMatch = trie.firstMatch("TurninG OnCe AgAiN BÖRKÜ")
     * checkEmit(firstMatch, 0, 6, "turning")
     * ```
     *
     * @param text 키워드를 찾을 문자열
     * @return Emit 첫 번째 매치되는 키워드를 담은 [Emit] 객체, 없으면 null
     *
     * ## 동작/계약
     * - [allowOverlaps]가 `false`면 [parseText]의 첫 결과를 반환합니다.
     * - [onlyWholeWords] 옵션이 켜져 있으면 부분 단어 매칭을 제외합니다.
     */
    fun firstMatch(text: CharSequence): Emit? {
        if (!config.allowOverlaps) {
            return parseText(text).firstOrNull()
        }

        var currentState = rootState

        text.forEachIndexed { pos, ch ->
            currentState = when {
                config.ignoreCase -> getState(currentState, ch.lowercaseChar())
                else -> getState(currentState, ch)
            }

            currentState.emit().forEach { emitStr ->
                val emit = Emit(pos - emitStr.length + 1, pos, emitStr)
                if (config.onlyWholeWords) {
                    if (!isPartialMatch(text, emit)) {
                        return emit
                    }
                } else {
                    return emit
                }
            }
        }
        log.trace { "Not found matches. text=$text" }
        return null
    }

    private fun addKeyword(keyword: String) {
        if (keyword.isNotEmpty()) {
            val adder = if (ignoreCase) keyword.lowercase() else keyword
            addState(adder).addEmit(adder)
        }
    }

    private fun addKeywords(vararg keywords: String) {
        keywords.forEach { addKeyword(it) }
    }

    private fun addKeywords(keywords: Collection<String>) {
        keywords.forEach { addKeyword(it) }
    }

    private fun addState(keyword: String): State = rootState.addState(keyword)

    private fun createFragment(emit: Emit?, text: String, lastcollectedPosition: Int): Token {
        return FragmentToken(text.substring(lastcollectedPosition + 1, emit?.start ?: text.length))
    }

    private fun createMatch(emit: Emit, text: String): Token {
        return MatchToken(text.substring(emit.start, emit.end + 1), emit)
    }

    private fun isPartialMatch(searchText: CharSequence, emit: Emit): Boolean {
        val isAlphabeticStart =
            emit.start != 0 && Character.isAlphabetic(searchText[emit.start - 1].code)

        if (isAlphabeticStart) {
            return true
        }

        val isAlphabeticEnd: Boolean =
            emit.end + 1 != searchText.length && Character.isAlphabetic(searchText[emit.end + 1].code)

        return isAlphabeticEnd
    }

    private fun removePartialMatches(searchText: CharSequence, collectedEmits: MutableList<Emit>) {
        collectedEmits.removeIf { isPartialMatch(searchText, it) }
    }

    private fun removePartialMatchesWhiteSpaceSeparated(searchText: CharSequence, collectedEmits: MutableList<Emit>) {
        val size = searchText.length

        collectedEmits.removeIf { emit ->
            val isEmptyStart = emit.start == 0 || Character.isWhitespace(searchText[emit.start - 1])
            if (!isEmptyStart) {
                true
            } else {
                val isEmptyEnd = emit.end + 1 == size || Character.isWhitespace(searchText[emit.end + 1])
                !isEmptyEnd
            }
        }
    }

    private fun getState(currentState: State, ch: Char): State {
        var thisState = currentState
        var nextState = thisState.nextState(ch)
        while (nextState == null) {
            thisState = thisState.failure!!
            nextState = thisState.nextState(ch)
        }
        return nextState
    }

    private fun constructFailureStates() {
        val queue = ArrayDeque<State>()
        val startState = rootState

        // First, set the fail state of all depth 1 states to the root state
        startState.getStates().forEach { depthOneState ->
            depthOneState.failure = startState
            queue.add(depthOneState)
        }

        // Second, determine the fail state for all depth > 1 state
        while (queue.isNotEmpty()) {
            val currentState = queue.remove()
            log.trace { "currentState=$currentState" }

            currentState.getTransitions().forEach { transition ->
                val targetState = currentState.nextState(transition)
                check(targetState != null) {
                    "targetState must not be null. transition=$transition, currentState=$currentState"
                }
                queue.add(targetState)

                var traceFailureState = currentState.failure!!
                while (traceFailureState.nextState(transition) == null) {
                    traceFailureState = traceFailureState.failure!!
                }

                val newFailureState = traceFailureState.nextState(transition)!!
                targetState.failure = newFailureState
                targetState.addEmits(newFailureState.emit())
            }
        }
    }

    private fun storeEmits(position: Int, currentState: State, emitHandler: EmitHandler): Boolean {
        var emitted = false
        val emits = currentState.emit()

        emits.forEach { emit ->
            emitted = emitHandler.emit(Emit(position - emit.length + 1, position, emit))
            if (emitted && config.stopOnHit) {
                return true
            }
        }
        return emitted
    }


    class TrieBuilder {
        private val configBuilder = TrieConfig.builder()
        private val keywords: MutableList<String> = mutableListOf()

        /** 단일 키워드를 추가합니다. */
        fun addKeyword(keyword: String) = apply {
            this.keywords.add(keyword)
        }

        /** 가변 인자 키워드를 추가합니다. */
        fun addKeywords(vararg keywords: String) = apply {
            this.keywords.addAll(keywords)
        }

        /** 컬렉션 키워드를 추가합니다. */
        fun addKeywords(keywords: Collection<String>) = apply {
            this.keywords.addAll(keywords)
        }

        /** 겹침 결과를 허용하지 않도록 설정합니다. */
        fun ignoreOverlaps() = apply {
            configBuilder.allowOverlaps(false)
        }

        /** 완전 단어 매칭만 허용하도록 설정합니다. */
        fun onlyWholeWords() = apply {
            configBuilder.onlyWholeWords(true)
        }

        /** 공백 경계 기준 완전 단어 매칭만 허용하도록 설정합니다. */
        fun onlyWholeWordsWhiteSpaceSeparated() = apply {
            configBuilder.onlyWholeWordsWhiteSpaceSeparated(true)
        }

        /** 대소문자 무시 모드를 활성화합니다. */
        fun ignoreCase() = apply {
            configBuilder.ignoreCase(true)
        }

        /** 첫 매칭 발견 시 검색 중단 모드를 활성화합니다. */
        fun stopOnHit() = apply {
            configBuilder.stopOnHit(true)
        }


        /**
         * 설정과 키워드로 [Trie]를 생성합니다.
         *
         * ## 동작/계약
         * - 키워드 추가 후 failure state를 구성한 완성 Trie를 반환합니다.
         */
        fun build(): Trie {
            return Trie(configBuilder.build()).apply {
                addKeywords(keywords)
                constructFailureStates()
            }
        }
    }
}
