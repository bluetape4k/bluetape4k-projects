package io.bluetape4k.tokenizer.korean

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.block.KoreanBlockwordProcessor
import io.bluetape4k.tokenizer.korean.normalizer.KoreanNormalizer
import io.bluetape4k.tokenizer.korean.phrase.KoreanPhrase
import io.bluetape4k.tokenizer.korean.phrase.KoreanPhraseExtractor
import io.bluetape4k.tokenizer.korean.phrase.NounPhraseExtractor
import io.bluetape4k.tokenizer.korean.stemmer.KoreanStemmer
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanDetokenizer
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanSentenceSplitter
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.tokenizer.NounTokenizer
import io.bluetape4k.tokenizer.korean.tokenizer.Sentence
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.model.Severity.HIGH
import io.bluetape4k.tokenizer.model.Severity.LOW
import io.bluetape4k.tokenizer.model.Severity.MIDDLE
import io.bluetape4k.tokenizer.utils.CharArraySet

/**
 * 한국어 토크나이저/정규화/구 추출/금칙어 처리를 묶은 퍼사드 API입니다.
 *
 * ## 동작/계약
 * - 각 기능은 `KoreanNormalizer`, `KoreanTokenizer`, `KoreanPhraseExtractor` 등 하위 유틸에 위임한다.
 * - 사전 추가/삭제 API는 `KoreanDictionaryProvider`의 런타임 사전을 직접 갱신한다.
 * - 금칙어 처리는 `KoreanBlockwordProcessor` 정책을 그대로 따른다.
 *
 * ```kotlin
 * val tokens = KoreanProcessor.tokenize("주말특가")
 * // tokens.map { it.text } == ["주말", "특가"]
 * ```
 */
object KoreanProcessor: KLogging() {

    /**
     * 구어체/반복 문자/오타를 정규화합니다.
     *
     * ## 동작/계약
     * - 내부 구현은 `KoreanNormalizer.normalize`를 그대로 위임한다.
     * - 빈 입력/비한글 구간 처리는 정규화기 구현 규칙을 따른다.
     *
     * ```kotlin
     * val normalized = KoreanProcessor.normalize("안됔ㅋㅋㅋㅋ")
     * // normalized == "안돼ㅋㅋㅋ"
     * ```
     */
    fun normalize(text: CharSequence): CharSequence {
        return KoreanNormalizer.normalize(text)
    }

    /**
     * 문장을 1-best 형태소 토큰 리스트로 분석합니다.
     *
     * ## 동작/계약
     * - `KoreanTokenizer.tokenize(text, profile)`를 그대로 위임한다.
     * - 결과 토큰에는 필요 시 용언 `stem` 정보가 포함된다.
     *
     * ```kotlin
     * val tokens = KoreanProcessor.tokenize("주말특가")
     * // tokens.map { it.text } == ["주말", "특가"]
     * ```
     */
    fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        return KoreanTokenizer.tokenize(text, profile)
    }

    /**
     * 명사 중심 규칙으로 문장을 분석합니다.
     *
     * ## 동작/계약
     * - `NounTokenizer.tokenize(text, profile)`를 위임 호출한다.
     * - phrase 추출 전처리용 명사 토큰화 경로로 사용된다.
     *
     * ```kotlin
     * val tokens = KoreanProcessor.tokenizeForNoun("떡 만두국")
     * // tokens.isNotEmpty() == true
     * ```
     */
    fun tokenizeForNoun(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        return NounTokenizer.tokenize(text, profile)
    }

    /**
     * 문장을 청크별 상위 `n` 후보로 분석합니다.
     *
     * ## 동작/계약
     * - `KoreanTokenizer.tokenizeTopN(text, n, profile)`를 그대로 위임한다.
     *
     * ```kotlin
     * val top = KoreanProcessor.tokenizeTopN("가느다란", n = 1)
     * // top.isNotEmpty() == true
     * ```
     */
    fun tokenizeTopN(
        text: CharSequence,
        n: Int = 1,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<List<List<KoreanToken>>> = KoreanTokenizer.tokenizeTopN(text, n, profile)

    /**
     * 명사 사전에 단어 목록을 추가합니다.
     *
     * ## 동작/계약
     * - `KoreanDictionaryProvider.addWordsToDictionary(Noun, words)`를 호출한다.
     *
     * ```kotlin
     * KoreanProcessor.addNounsToDictionary(listOf("후랴오교"))
     * // KoreanDictionaryProvider.koreanDictionary[KoreanPos.Noun]!!.contains("후랴오교") == true
     * ```
     */
    fun addNounsToDictionary(words: List<String>) {
        KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, words)
    }

    /**
     * 명사 사전에 가변 인자 단어를 추가합니다.
     *
     * ## 동작/계약
     * - `addWordsToDictionary(Noun, *words)`를 호출한다.
     *
     * ```kotlin
     * KoreanProcessor.addNounsToDictionary("주말특가")
     * // KoreanDictionaryProvider.koreanDictionary[KoreanPos.Noun]!!.contains("주말특가") == true
     * ```
     */
    fun addNounsToDictionary(vararg words: String) {
        KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, *words)
    }


    /**
     * 심각도별 금칙어 사전에 단어를 추가합니다.
     *
     * ## 동작/계약
     * - `severity`에 맞는 blockWords 사전에 단어를 추가한다.
     * - 복합 명사 탐지를 위해 동일 단어를 명사 사전과 `properNouns`에도 추가한다.
     *
     * ```kotlin
     * KoreanProcessor.addBlockwords(listOf("분수쑈"), Severity.HIGH)
     * // KoreanDictionaryProvider.blockWords[Severity.HIGH]!!.contains("분수쑈") == true
     * ```
     */
    fun addBlockwords(
        words: List<String>,
        severity: Severity = Severity.DEFAULT,
    ) {
        withBlockwordDictionary(severity) {
            addAll(words)
        }
        // 복합명사의 경우 등록되지 않으면 형태소 분석을 못한다 (예: 분수쑈 -> `분수 + 쑈` 로 분석하면 `분수쑈` 라는 금칙어를 처리할 수 없다)
        addNounsToDictionary(words)
        KoreanDictionaryProvider.properNouns.addAll(words)
    }

    /**
     * 금칙어 사전에서 단어를 제거합니다. (Deprecated)
     *
     * ## 동작/계약
     * - `removeBlockwords(words, severity)`와 동일하게 severity 범위 사전에서 제거한다.
     *
     * ```kotlin
     * KoreanProcessor.removeBlockword(listOf("금칙어"), Severity.HIGH)
     * // deprecated
     * ```
     */
    @Deprecated("Use removeBlockwords instead", replaceWith = ReplaceWith("removeBlockwords(words, severity)"))
    fun removeBlockword(
        words: List<String>,
        severity: Severity = Severity.DEFAULT,
    ) {
        withBlockwordDictionary(severity) {
            removeAll(words)
        }
    }

    /**
     * 금칙어 사전에서 단어를 제거합니다.
     *
     * ## 동작/계약
     * - `severity`에 해당하는 blockWords 사전(또는 포함 범위)에서 `removeAll(words)`를 수행한다.
     *
     * ```kotlin
     * KoreanProcessor.removeBlockwords(listOf("금칙어"), Severity.HIGH)
     * // removed from high dictionary
     * ```
     */
    fun removeBlockwords(
        words: List<String>,
        severity: Severity = Severity.DEFAULT,
    ) {
        withBlockwordDictionary(severity) {
            removeAll(words)
        }
    }

    /**
     * 심각도 범위에 해당하는 금칙어 사전을 비웁니다.
     *
     * ## 동작/계약
     * - `Severity.LOW`면 low/middle/high 모두, `MIDDLE`이면 middle/high, 그 외는 high만 clear한다.
     *
     * ```kotlin
     * KoreanProcessor.clearBlockwords(Severity.HIGH)
     * // high dictionary cleared
     * ```
     */
    fun clearBlockwords(severity: Severity = Severity.DEFAULT) {
        withBlockwordDictionary(severity) {
            clear()
        }
    }

    private inline fun withBlockwordDictionary(
        severity: Severity,
        action: CharArraySet.() -> Unit,
    ) {
        when (severity) {
            LOW    -> {
                KoreanDictionaryProvider.blockWords[LOW]?.action()
                KoreanDictionaryProvider.blockWords[MIDDLE]?.action()
                KoreanDictionaryProvider.blockWords[HIGH]?.action()
            }

            MIDDLE -> {
                KoreanDictionaryProvider.blockWords[MIDDLE]?.action()
                KoreanDictionaryProvider.blockWords[HIGH]?.action()
            }

            else   -> KoreanDictionaryProvider.blockWords[HIGH]?.action()
        }
    }

    /**
     * 토큰 리스트에서 공백 토큰을 제외한 텍스트 목록을 반환합니다.
     *
     * ## 동작/계약
     * - `KoreanPos.Space` 토큰은 제거하고 나머지 `text`만 추출한다.
     *
     * ```kotlin
     * val words = KoreanProcessor.tokensToStrings(listOf(KoreanToken("안녕", KoreanPos.Noun, 0, 2)))
     * // words == ["안녕"]
     * ```
     */
    fun tokensToStrings(tokens: List<KoreanToken>): List<String> =
        tokens.filterNot { it.pos == KoreanPos.Space }.map { it.text }

    /**
     * 문장을 `Sentence` 시퀀스로 분리합니다.
     *
     * ## 동작/계약
     * - `KoreanSentenceSplitter.split(text)`를 그대로 위임한다.
     *
     * ```kotlin
     * val sentences = KoreanProcessor.splitSentences("안녕? 세상아?").toList()
     * // sentences.size == 2
     * ```
     */
    fun splitSentences(text: CharSequence): Sequence<Sentence> =
        KoreanSentenceSplitter.split(text)

    /**
     * 토큰 목록에서 phrase를 추출합니다.
     *
     * ## 동작/계약
     * - `KoreanPhraseExtractor.extractPhrases(tokens, filterSpam, enableHashtags)`를 위임 호출한다.
     *
     * ```kotlin
     * val phrases = KoreanProcessor.extractPhrases(KoreanProcessor.tokenize("성탄절 쇼핑"), filterSpam = false)
     * // phrases.isNotEmpty() == true
     * ```
     */
    fun extractPhrases(
        tokens: List<KoreanToken>,
        filterSpam: Boolean = false,
        enableHashtags: Boolean = true,
    ): List<KoreanPhrase> {
        return KoreanPhraseExtractor.extractPhrases(tokens, filterSpam, enableHashtags)
    }


    /**
     * 명사 중심 토큰에서 phrase를 추출합니다.
     *
     * ## 동작/계약
     * - `NounPhraseExtractor.extractPhrases(tokens)`를 그대로 위임한다.
     *
     * ```kotlin
     * val phrases = KoreanProcessor.extractPhrasesForNoun(KoreanProcessor.tokenizeForNoun("떡 만두국"))
     * // phrases.map { it.text }.contains("만두국") == true
     * ```
     */
    fun extractPhrasesForNoun(tokens: List<KoreanToken>): List<KoreanPhrase> {
        return NounPhraseExtractor.extractPhrases(tokens)
    }

    /**
     * 토큰 리스트의 어미 병합 및 용언 원형 복원을 수행합니다.
     *
     * ## 동작/계약
     * - `KoreanStemmer.stem(tokens)`를 그대로 위임한다.
     *
     * ```kotlin
     * val stemmed = KoreanProcessor.stem(KoreanProcessor.tokenize("가느다란"))
     * // stemmed.first().stem == "갈다"
     * ```
     */
    fun stem(tokens: List<KoreanToken>): List<KoreanToken> {
        return KoreanStemmer.stem(tokens)
    }


    /**
     * 토큰 문자열 목록을 문장 문자열로 복원합니다.
     *
     * ## 동작/계약
     * - `KoreanDetokenizer.detokenize(tokens)`를 그대로 위임한다.
     *
     * ```kotlin
     * val text = KoreanProcessor.detokenize(listOf("뭐", "완벽", "하진", "않", "지만"))
     * // text == "뭐 완벽하진 않지만"
     * ```
     */
    fun detokenize(tokens: Collection<String>): String {
        return KoreanDetokenizer.detokenize(tokens)
    }

    /**
     * 요청 옵션에 따라 금칙어를 마스킹합니다.
     *
     * ## 동작/계약
     * - `KoreanBlockwordProcessor.maskBlockwords(request)`를 그대로 위임한다.
     *
     * ```kotlin
     * val response = KoreanProcessor.maskBlockwords(BlockwordRequest("미니미와 니미"))
     * // response.text.contains("**")
     * ```
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        return KoreanBlockwordProcessor.maskBlockwords(request)
    }
}
