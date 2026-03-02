package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import io.bluetape4k.logging.KLogging

/**
 * Kuromoji IPAdic 분석기를 사용해 일본어 문장을 형태소 토큰 목록으로 변환하는 진입점입니다.
 *
 * ## 동작/계약
 * - 내부 `Tokenizer` 인스턴스는 지연 초기화되며 객체 수명 동안 재사용됩니다.
 * - `tokenize` 결과는 Kuromoji 분석 순서(원문 등장 순서)를 유지합니다.
 * - 품사 필터링은 `filter`, `filterNoun`으로 후처리합니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val surfaces = tokens.map { it.surface }
 *
 * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
 * ```
 */
object JapaneseTokenizer: KLogging() {

    internal val tokenizer: Tokenizer by lazy { Tokenizer.Builder().build() }

    /**
     * 입력 문장을 Kuromoji IPAdic 규칙으로 형태소 분석해 토큰 목록으로 반환합니다.
     *
     * ## 동작/계약
     * - 빈 문자열 입력 시 빈 리스트를 반환합니다.
     * - 반환 리스트의 각 원소는 표면형(`surface`), 위치(`position`), 품사 정보를 포함합니다.
     * - 동일 입력에 대해 사전 상태가 같다면 동일한 분해 결과를 반환합니다.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val surfaces = tokens.map { it.surface }
     *
     * // result == ["お", "寿司", "が", "食べ", "たい", "。"]
     * ```
     */
    fun tokenize(text: String): List<Token> {
        return tokenizer.tokenize(text)
    }

    /**
     * 토큰 목록에서 전달한 조건식을 만족하는 항목만 새 리스트로 반환합니다.
     *
     * ## 동작/계약
     * - 입력 리스트는 변경하지 않고 `List.filter` 결과를 새로 생성합니다.
     * - `predicate`는 토큰 순서대로 호출되며 `true`인 항목만 유지됩니다.
     * - 조건식은 호출 측에서 품사/표면형/위치 기준으로 자유롭게 정의할 수 있습니다.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val punctuations = JapaneseTokenizer.filter(tokens) { it.isPunctuation() }
     *
     * // result == ["。"]
     * ```
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return tokens.filter(predicate)
    }

    /**
     * 토큰 목록에서 명사 품사(`名詞`)인 항목만 추려 반환합니다.
     *
     * ## 동작/계약
     * - 내부적으로 `filter(tokens) { it.isNoun() }`를 호출합니다.
     * - 입력 순서를 유지한 새 리스트를 반환하며 원본 리스트는 변경하지 않습니다.
     * - 품사 판정은 `TokenBaseSupport.isNoun`의 기준(`allFeaturesArray[0] == "名詞"`)을 따릅니다.
     *
     * ```kotlin
     * val tokens = JapaneseTokenizer.tokenize("私は、日本語の勉強をしています。")
     * val nouns = JapaneseTokenizer.filterNoun(tokens).map { it.surface }
     *
     * // result == ["私", "日本語", "勉強"]
     * ```
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return filter(tokens) { it.isNoun() }
    }
}
