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
 * 한글 형태소 분석기
 */
object KoreanProcessor: KLogging() {

    /**
     * 한글 구어체를 맞춤법에 맞게 정규화합니다.
     *
     * ```
     * val expected = "안돼ㅋㅋㅋ내 심장을 가격했어ㅋㅋㅋ"
     * val actual = normalize("안됔ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ내 심장을 가격했엌ㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋㅋ")
     * actual shouldBeEqualTo expected
     *
     * normalize("무의식중에 손들어버려섴ㅋㅋㅋㅋ") shouldBeEqualTo "무의식중에 손들어버려서ㅋㅋㅋ"
     * normalize("기억도 나지아낳ㅎㅎㅎ") shouldBeEqualTo "기억도 나지아나ㅎㅎㅎ"
     * normalize("근데비싸서못머구뮤ㅠㅠ") shouldBeEqualTo "근데비싸서못먹음ㅠㅠ"
     *
     * normalize("미친 존잘니뮤ㅠㅠㅠㅠ") shouldBeEqualTo "미친 존잘님ㅠㅠㅠ"
     * normalize("만나무ㅜㅜㅠ") shouldBeEqualTo "만남ㅜㅜㅠ"
     * normalize("가루ㅜㅜㅜㅜ") shouldBeEqualTo "가루ㅜㅜㅜ"
     *
     * normalize("유성우ㅠㅠㅠ") shouldBeEqualTo "유성우ㅠㅠㅠ"
     *
     * normalize("예뿌ㅠㅠ") shouldBeEqualTo "예뻐ㅠㅠ"
     * normalize("고수야고수ㅠㅠㅠ") shouldBeEqualTo "고수야고수ㅠㅠㅠ"
     *
     * normalize("안돼ㅋㅋㅋㅋㅋ") shouldBeEqualTo "안돼ㅋㅋㅋ"
     * ```
     *
     * ```
     * normalize("사브작사브작사브작사브작사브작사브작사브작사브작") shouldBeEqualTo "사브작사브작"
     * normalize("ㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎㅋㅋㅎ") shouldBeEqualTo "ㅋㅋㅎㅋㅋㅎ"
     * ```
     *
     * ```
     * normalize("가쟝 용기있는 사람이 머굼 되는거즤") shouldBeEqualTo "가장 용기있는 사람이 먹음 되는거지"
     * ```
     *
     * ```
     * normalizeCodaN("버슨가") shouldBeEqualTo "버스인가"
     * normalizeCodaN("보슨지") shouldBeEqualTo "보스인지"
     * normalizeCodaN("쵸킨데") shouldBeEqualTo "쵸킨데"
     * ```
     *
     * @param input 입력 문자열
     * @return 정규화된 문자열
     */
    fun normalize(text: CharSequence): CharSequence {
        return KoreanNormalizer.normalize(text)
    }

    /**
     * 한글 문장을 형태소 분석하여 [KoreanToken]의 리스트로 반환합니다.
     *
     * ```
     * tokenize("엄청작아서귀엽다") shouldBeEqualTo listOf(
     *     KoreanToken("엄청", Adverb, 0, 2),
     *     KoreanToken("작아서", Adjective, 2, 3, stem = "작다"),
     *     KoreanToken("귀엽다", Adjective, 5, 3, stem = "귀엽다")
     * )
     * ```
     *
     * ```
     * tokenize("야이건뭐") shouldBeEqualTo listOf(
     *     KoreanToken("야", Exclamation, 0, 1),
     *     KoreanToken("이건", Noun, 1, 2),
     *     KoreanToken("뭐", Noun, 3, 1)
     * )
     * ```
     *
     * ```
     * tokenize("주말특가") shouldBeEqualTo listOf(
     *     KoreanToken("주말", Noun, 0, 2),
     *     KoreanToken("특가", Noun, 2, 2)
     * )
     * ```
     *
     * ```
     * val text = """우리 동네사람들"""
     * val tokens = tokenize(text)
     * tokens shouldBeEqualTo listOf(
     *     KoreanToken("우리", Noun, 0, 2),
     *     KoreanToken(" ", Space, 2, 1),
     *     KoreanToken("동네", Noun, 3, 2),
     *     KoreanToken("사람", Noun, 5, 2),
     *     KoreanToken("들", Suffix, 7, 1)
     * )
     * ```
     *
     * @param text 한글 문장
     * @return 형태소 분석된 [KoreanToken] 리스트
     */
    fun tokenize(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        return KoreanTokenizer.tokenize(text, profile)
    }

    /**
     * 명사 위주의 형태소 분석을 수행합니다.
     */
    fun tokenizeForNoun(
        text: CharSequence,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<KoreanToken> {
        return NounTokenizer.tokenize(text, profile)
    }

    /**
     * 한글 문장을 [n] 개수만큼 형태소 분석하여 [KoreanToken]의 리스트로 반환합니다.
     *
     * @param text 한글 문장
     * @param topN 최대 분석 개수 (default: 1)
     * @return 형태소 분석된 [KoreanToken] 리스트
     */
    fun tokenizeTopN(
        text: CharSequence,
        n: Int = 1,
        profile: TokenizerProfile = TokenizerProfile.DefaultProfile,
    ): List<List<List<KoreanToken>>> = KoreanTokenizer.tokenizeTopN(text, n, profile)

    /**
     * 명사들[words]를 명사 사전에 추가합니다. 띄어쓰기가 포함된 단어는 추가할 수 없습니다.
     *
     * @param words 추가할 명사 단어들
     */
    fun addNounsToDictionary(words: List<String>) {
        KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, words)
    }

    /**
     * 명사들[words]를 명사 사전에 추가합니다. 띄어쓰기가 포함된 단어는 추가할 수 없습니다.
     *
     * @param words 추가할 명사 단어들
     */
    fun addNounsToDictionary(vararg words: String) {
        KoreanDictionaryProvider.addWordsToDictionary(KoreanPos.Noun, *words)
    }


    /**
     * 금칙어를 금칙어 Dictionary에 추가합니다.
     *
     * @param words 금칙어에 등록할 단어들
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
     * 등록된 금칙어를 제외시킵니다
     *
     * @param words
     * @param severity
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
     * 등록된 금칙어를 제외시킵니다
     *
     * @param words
     * @param severity
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
     * 등록된 금칙어를 모두 삭제합니다.
     *
     * @param severity
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
     * Tokenize text into a sequence of token strings. This excludes spaces.
     *
     * @param tokens Korean tokens
     * @return A sequence of token strings.
     */
    fun tokensToStrings(tokens: List<KoreanToken>): List<String> =
        tokens.filterNot { it.pos == KoreanPos.Space }.map { it.text }

    /**
     * 한글 문장을 문장(Sentence) 단위로 분리합니다.
     *
     * ```
     * var actual = split("안녕? iphone6안녕? 세상아?").toList()
     * actual shouldContainSame listOf(
     *     Sentence("안녕?", 0, 3),
     *     Sentence("iphone6안녕?", 4, 14),
     *     Sentence("세상아?", 15, 19)
     * )
     *```
     */
    fun splitSentences(text: CharSequence): List<Sentence> =
        KoreanSentenceSplitter.split(text)

    /**
     * 형태소 분석한 결과에서 [filterSpam], [addHashtags]를 적용한 구문만을 추출합니다.
     *
     * ```
     * val phrases = extractPhrases(tokenize("성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑 성탄절 쇼핑"), filterSpam = false)
     *
     * val expected = phrases.map { it.text }.distinct()
     * val actual = phrases.map { it.text }
     * actual shouldBeEqualTo expected
     * ```
     *
     * @param tokens A sequence of tokens
     * @param filterSpam true if spam words and slangs to be filtered out
     * @param enableHashtags true if #hashtags to be included
     * @return A list of KoreanPhrase
     */
    fun extractPhrases(
        tokens: List<KoreanToken>,
        filterSpam: Boolean = false,
        enableHashtags: Boolean = true,
    ): List<KoreanPhrase> {
        return KoreanPhraseExtractor.extractPhrases(tokens, filterSpam, enableHashtags)
    }


    /**
     * 명사 위주의 형태소 분석 후 추출된 구문을 반환합니다.
     *
     * @param tokens         Korean tokens
     * @return A sequence of extracted phrases
     */
    fun extractPhrasesForNoun(tokens: List<KoreanToken>): List<KoreanPhrase> {
        return NounPhraseExtractor.extractPhrases(tokens)
    }

    /**
     * 마지막 어미를 제거하여 동사의 원형을 복원합니다.
     *
     * ```
     * 새로운 스테밍을 추가했었다. -> 새롭다 + 스테밍 + 을 + 추가 + 하다
     * ```
     *
     * ```
     * val tokens = KoreanTokenizer.tokenizeTopN("가느다란").flatMap { it.first() }  // KoreanToken("가느다란", Noun, 0, 4)
     * val actual = KoreanStemmer.stem(tokens)  // KoreanToken("가느다란", Verb, 0, 4, stem = "갈다")
     * ```
     *
     * @param tokens 형태소 분석 토큰 컬렉션
     * @return 원형을 복원한 토큰 컬렉션
     */
    fun stem(tokens: List<KoreanToken>): List<KoreanToken> {
        return KoreanStemmer.stem(tokens)
    }


    /**
     * 한글 형태소 분석기로 분석된 단어들을 하나의 문장으로 만듭니다.
     *
     * ```
     * var actual = detokenize(listOf("연세", "대학교", "보건", "대학원", "에", "오신", "것", "을", "환영", "합니다", "!"))
     * actual shouldBeEqualTo "연세대학교 보건 대학원에 오신것을 환영합니다!"
     *
     * actual = detokenize(listOf("뭐", "완벽", "하진", "않", "지만", "그럭저럭", "쓸", "만", "하군", "..."))
     * actual shouldBeEqualTo "뭐 완벽하진 않지만 그럭저럭 쓸 만하군..."
     * ```
     *
     * @param input 분석된 단어들
     * @return 복원된 문장
     */
    fun detokenize(tokens: Collection<String>): String {
        return KoreanDetokenizer.detokenize(tokens)
    }

    /**
     * 금칙어 (Block words) 를 masking 합니다.
     *
     * 예:
     * - 미니미와 니미 -> 미니미와 **     // `니미` 는 속어
     * - ㅆ.ㅂ 웃기네 -> *** 웃기네      // ㅆㅂ, ㅆ.ㅂ, ㅆ~ㅂ 등을 처리
     *
     *
     * ```
     * val request = BlockwordRequest("미니미와 니미", BlockwordOptions())
     * val response = maskBlockwords(request)
     * println(response.text)  // 미니미와 **
     * ```
     *
     * @param request 금칙어 처리 요청 정보 [BlockwordRequest]
     * @return 금칙어를 처리한 결과 [BlockwordResponse]
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        return KoreanBlockwordProcessor.maskBlockwords(request)
    }
}
