package io.bluetape4k.tokenizer.japanese

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.japanese.block.JapaneseBlockwordProcessor
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse

/**
 * 일본어 토큰화와 금칙어 검출/마스킹 기능을 한 곳에서 제공하는 파사드입니다.
 *
 * ## 동작/계약
 * - 형태소 분석 계열 API는 `JapaneseTokenizer`로 위임합니다.
 * - 금칙어 계열 API는 `JapaneseBlockwordProcessor`와 `JapaneseDictionaryProvider`로 위임합니다.
 * - 내부적으로 상태를 저장하지 않고 하위 컴포넌트의 동작을 그대로 노출합니다.
 *
 * ```kotlin
 * val nouns = JapaneseProcessor
 *     .filterNoun(JapaneseProcessor.tokenize("私は、日本語の勉強をしています。"))
 *     .map { it.surface }
 *
 * // result == ["私", "日本語", "勉強"]
 * ```
 */
object JapaneseProcessor: KLogging() {

    /**
     * 입력 문장을 형태소 토큰 목록으로 분석해 반환합니다.
     *
     * ## 동작/계약
     * - `JapaneseTokenizer.tokenize`를 그대로 위임 호출합니다.
     * - 빈 문자열 입력 시 빈 리스트를 반환합니다.
     * - 반환 순서는 원문 토큰 순서를 유지합니다.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
     * val surfaces = tokens.map { it.surface }
     *
     * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
     * ```
     */
    fun tokenize(text: String): List<Token> {
        return JapaneseTokenizer.tokenize(text)
    }

    /**
     * 토큰 목록에서 조건식을 만족하는 항목만 필터링해 반환합니다.
     *
     * ## 동작/계약
     * - `JapaneseTokenizer.filter`를 그대로 위임 호출합니다.
     * - 입력 리스트는 변경하지 않고 새 리스트를 반환합니다.
     * - `predicate`의 평가 순서는 입력 순서를 따릅니다.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("お寿司が食べたい。")
     * val nouns = JapaneseProcessor.filter(tokens) { it.isNoun() }.map { it.surface }
     *
     * // result == ["寿司"]
     * ```
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return JapaneseTokenizer.filter(tokens, predicate)
    }

    /**
     * 토큰 목록에서 명사(`名詞`)만 필터링해 반환합니다.
     *
     * ## 동작/계약
     * - `JapaneseTokenizer.filterNoun`를 그대로 위임 호출합니다.
     * - 명사 판정은 `TokenBaseSupport.isNoun` 기준을 따릅니다.
     * - 입력 순서를 유지한 새 리스트를 반환합니다.
     *
     * ```kotlin
     * val tokens = JapaneseProcessor.tokenize("私は、日本語の勉強をしています。")
     * val nouns = JapaneseProcessor.filterNoun(tokens).map { it.surface }
     *
     * // result == ["私", "日本語", "勉強"]
     * ```
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return JapaneseTokenizer.filterNoun(tokens)
    }

    /**
     * 문장에서 금칙어 사전에 등록된 토큰을 찾아 반환합니다.
     *
     * ## 동작/계약
     * - `JapaneseBlockwordProcessor.findBlockwords`를 그대로 위임 호출합니다.
     * - 입력이 공백 문자열이면 빈 리스트를 반환합니다.
     * - 기본 토큰 매칭 실패 시 복합어(명사+명사/동사) 조합 검사 결과를 포함할 수 있습니다.
     *
     * ```kotlin
     * val blockwords = JapaneseProcessor.findBlockwords("ホモの男性を理解できない").map { it.surface }
     *
     * // result == ["ホモ"]
     * ```
     */
    fun findBlockwords(text: String): List<Token> {
        return JapaneseBlockwordProcessor.findBlockwords(text)
    }

    /**
     * 금칙어를 마스크 문자열로 치환한 결과를 반환합니다.
     *
     * ## 동작/계약
     * - `JapaneseBlockwordProcessor.maskBlockwords`를 그대로 위임 호출합니다.
     * - 금칙어가 없으면 `maskedText`는 원문과 동일하고 `blockwordExists`는 `false`입니다.
     * - 금칙어가 있으면 토큰 길이만큼 마스크 문자를 반복해 해당 위치를 치환합니다.
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない")
     * val response = JapaneseProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        return JapaneseBlockwordProcessor.maskBlockwords(request)
    }

    /**
     * 금칙어 사전에 단어를 추가합니다.
     *
     * ## 동작/계약
     * - `JapaneseDictionaryProvider.addBlockwords`를 그대로 위임 호출합니다.
     * - 이미 존재하는 단어는 사전 집합 특성상 중복 저장되지 않습니다.
     * - 이후 `findBlockwords`/`maskBlockwords` 호출부터 즉시 반영됩니다.
     *
     * ```kotlin
     * JapaneseProcessor.addBlockwords(listOf("東京"))
     * val found = JapaneseProcessor.findBlockwords("これは東京です").map { it.surface }
     *
     * // result == ["東京"]
     * ```
     */
    fun addBlockwords(words: List<String>) {
        JapaneseDictionaryProvider.addBlockwords(words)
    }

    /**
     * 금칙어 사전에서 단어를 제거합니다.
     *
     * ## 동작/계약
     * - `JapaneseDictionaryProvider.removeBlockwords`를 그대로 위임 호출합니다.
     * - 존재하지 않는 단어를 전달해도 예외 없이 무시됩니다.
     * - 제거 후 동일 단어는 금칙어 탐지 대상에서 제외됩니다.
     *
     * ```kotlin
     * JapaneseProcessor.addBlockwords(listOf("東京"))
     * JapaneseProcessor.removeBlockwords(listOf("東京"))
     * val found = JapaneseProcessor.findBlockwords("これは東京です")
     *
     * // result == []
     * ```
     */
    fun removeBlockwords(words: List<String>) {
        JapaneseDictionaryProvider.removeBlockwords(words)
    }

    /**
     * 메모리에 로드된 금칙어 사전을 비웁니다.
     *
     * ## 동작/계약
     * - `JapaneseDictionaryProvider.clearBlockwords`를 그대로 위임 호출합니다.
     * - 호출 이후 현재 프로세스 내 사전은 빈 상태가 됩니다.
     * - 원본 리소스 파일을 삭제하지 않으며 재초기화 시 다시 로드됩니다.
     *
     * ```kotlin
     * JapaneseProcessor.clearBlockwords()
     * val found = JapaneseProcessor.findBlockwords("ホモの男性を理解できない")
     *
     * // result == []
     * ```
     */
    fun clearBlockwords() {
        JapaneseDictionaryProvider.clearBlockwords()
    }
}
