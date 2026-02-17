package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import io.bluetape4k.logging.KLogging

/**
 * Kuromoji IPAdic 기반 일본어 형태소 분석기
 *
 * 일본어 텍스트를 형태소 단위로 분리하고, 품사별 필터링 기능을 제공합니다.
 */
object JapaneseTokenizer: KLogging() {

    internal val tokenizer: Tokenizer by lazy { Tokenizer.Builder().build() }

    /**
     * 일본어 형태소 분석을 수행합니다.
     *
     * ```
     * val tokens: List<Token> = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     *
     * // 결과
     * token=お: 接頭詞,名詞接続,*,*,*,*,お,オ,オ, 0
     * token=寿司: 名詞,一般,*,*,*,*,寿司,スシ,スシ, 1
     * token=が: 助詞,格助詞,一般,*,*,*,が,ガ,ガ, 3
     * token=食べ: 動詞,自立,*,*,一段,連用形,食べる,タベ,タベ, 4
     * token=たい: 助動詞,*,*,*,特殊・タイ,基本形,たい,タイ,タイ, 6
     * token=。: 記号,句点,*,*,*,*,。,。,。, 8
     * ```
     */
    fun tokenize(text: String): List<Token> {
        return tokenizer.tokenize(text)
    }

    /**
     * 형태소 분석 결과에서 특정 조건에 맞는 토큰만 필터링합니다.
     *
     * ```
     * val tokens: List<Token> = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val filtered = filter(tokens) { it.partOfSpeechLevel1 == "名詞" }
     * ```
     */
    fun filter(tokens: List<Token>, predicate: (Token) -> Boolean): List<Token> {
        return tokens.filter(predicate)
    }

    /**
     * 형태소 분석 결과에서 명사만 필터링합니다.
     *
     * ```
     * val tokens: List<Token> = JapaneseTokenizer.tokenize("お寿司が食べたい。")
     * val nouns = filterNoun(tokens)  // [寿司]
     * ```
     */
    fun filterNoun(tokens: List<Token>): List<Token> {
        return filter(tokens) { it.isNoun() }
    }
}
