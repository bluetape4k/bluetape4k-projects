package io.bluetape4k.tokenizer.korean

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.korean.KoreanProcessor.addNounsToDictionary
import io.bluetape4k.tokenizer.korean.KoreanProcessor.extractPhrases
import io.bluetape4k.tokenizer.korean.KoreanProcessor.normalize
import io.bluetape4k.tokenizer.korean.KoreanProcessor.splitSentences
import io.bluetape4k.tokenizer.korean.KoreanProcessor.stem
import io.bluetape4k.tokenizer.korean.KoreanProcessor.tokenize
import io.bluetape4k.tokenizer.korean.KoreanProcessor.tokenizeTopN
import io.bluetape4k.tokenizer.korean.KoreanProcessor.tokensToStrings
import io.bluetape4k.tokenizer.korean.block.KoreanBlockwordProcessor
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.Sentence
import io.bluetape4k.tokenizer.korean.tokenizer.TokenizerProfile
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.KoreanParticle
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Space
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.utils.Systemx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis


class KoreanTextProcessorTest: TestBase() {

    companion object: KLogging()

    @Test
    fun `should normailize`() = runSuspendTest {
        normalize("그랰ㅋㅋㅋㅋ 샤릉햌ㅋㅋ") shouldBeEqualTo "그래ㅋㅋㅋ 사랑해ㅋㅋ"
    }

    @Test
    fun `should reflect custom parameters`() = runSuspendTest {
        val tokenizerProfile = TokenizerProfile(
            unknownPosCount = 1.0F,
            allNoun = 10.0F,
            preferredPattern = 4.0F
        )

        val expected = tokenize(
            "스윗박스가 점점 좁아지더니, 의자 두개 붙여놓은 것만큼 좁아졌어요. 맘에드는이성분과 앉으면 가까워질거에요 ㅎㅎ",
            tokenizerProfile
        )
        val actual = tokenize(
            "스윗박스가 점점 좁아지더니, 의자 두개 붙여놓은 것만큼 좁아졌어요. 맘에드는이성분과 앉으면 가까워질거에요 ㅎㅎ"
        )
        actual shouldNotBeEqualTo expected
    }

    @Test
    fun `should tokenize ignoring punctuations`() = runSuspendTest {
        val actual = tokenize("^///^규앙ㅇ").joinToString("/")
        val expected = "^///^(Punctuation: 0, 5)/규앙(Exclamation: 5, 2)/ㅇ(KoreanParticle: 7, 1)"
        actual shouldBeEqualTo expected
    }

    @Test
    fun `should tokenize the example sentence`() = runSuspendTest {
        val actual = tokenize(normalize("한국어를 처리하는 예시입니닼ㅋㅋㅋㅋ"))
        val expected = listOf(
            KoreanToken("한국어", Noun, 0, 3),
            KoreanToken("를", Josa, 3, 1),
            KoreanToken(" ", Space, 4, 1),
            KoreanToken("처리", Noun, 5, 2),
            KoreanToken("하는", Verb, 7, 2, stem = "하다"),
            KoreanToken(" ", Space, 9, 1),
            KoreanToken("예시", Noun, 10, 2),
            KoreanToken("입니다", Adjective, 12, 3, stem = "이다"),
            KoreanToken("ㅋㅋㅋ", KoreanParticle, 15, 3)
        )
        actual shouldBeEqualTo expected
    }

    @Test
    fun `should extract phrase from the example stence`() = runSuspendTest {
        val actual = extractPhrases(tokenize(normalize("한국어를 처리하는 예시입니닼ㅋㅋㅋㅋ"))).joinToString("/")
        val expected = "한국어(Noun: 0, 3)/처리(Noun: 5, 2)/처리하는 예시(Noun: 5, 7)/예시(Noun: 10, 2)"
        actual shouldBeEqualTo expected
    }

    @Test
    fun `should tokenize a long chunk within reasonable time`() = runSuspendTest(Dispatchers.Default) {
        val timeout = 15_000L

        measureTimeMillis {
            tokenize("아그리고선생님".repeat(10))
        } shouldBeLessThan timeout

        measureTimeMillis {
            tokenize("아그리고선생님이사람의정말귀여운헐쵸귀여운개루루엄청작아서귀엽다안녕ㅋㅋ".repeat(10))
        } shouldBeLessThan timeout

        measureTimeMillis {
            tokenize("강원랜드잭팟이용하세요".repeat(10))
        } shouldBeLessThan timeout

        measureTimeMillis {
            tokenize("강원랜드잭팟강원랜드잭팟강원랜드잭팟강원랜드잭팟".repeat(10))
        } shouldBeLessThan timeout


        measureTimeMillis {
            tokenize("감동적인강남카지노브라보카지노라오스카지노강원랜드잭팟강원랜드잭팟강원랜드잭팟강원랜드잭팟강원랜드잭팟".repeat(10))
        } shouldBeLessThan timeout

        measureTimeMillis {
            tokenize("마키코레썸크리스마스블랙프라이데이".repeat(10))
        } shouldBeLessThan timeout
    }

    @Test
    fun `should tokenize company names correctly`() = runTest {
        var actual = tokenize("삼성전자서비스")
        var expected = listOf(
            KoreanToken("삼성", Noun, 0, 2),
            KoreanToken("전자", Noun, 2, 2),
            KoreanToken("서비스", Noun, 4, 3)
        )
        actual shouldBeEqualTo expected

        actual = tokenize("삼성정밀화학")
        expected = listOf(
            KoreanToken("삼성", Noun, 0, 2),
            KoreanToken("정밀", Noun, 2, 2),
            KoreanToken("화학", Noun, 4, 2)
        )
        actual shouldBeEqualTo expected

        actual = tokenize("삼성그룹 현대중공업 한화케미칼 삼성전자스토어")
        expected = listOf(
            KoreanToken("삼성", Noun, 0, 2),
            KoreanToken("그룹", Noun, 2, 2),
            KoreanToken(" ", Space, 4, 1),
            KoreanToken("현대", Noun, 5, 2),
            KoreanToken("중공업", Noun, 7, 3),
            KoreanToken(" ", Space, 10, 1),
            KoreanToken("한화", Noun, 11, 2),
            KoreanToken("케미칼", Noun, 13, 3),
            KoreanToken(" ", Space, 16, 1),
            KoreanToken("삼성", Noun, 17, 2),
            KoreanToken("전자", Noun, 19, 2),
            KoreanToken("스토어", Noun, 21, 3)
        )
        actual shouldBeEqualTo expected
    }

    /**
     * TODO: 몇가지 경우 틀린 경우가 있다
     */
    @Test
    fun `should correctly tokenize the example set with normalization`() = runSuspendTest {
        assertExamples("current_parsing.txt", log) { input ->
            tokenize(normalize(input)).joinToString("/")
        }
    }

    @Test
    fun `should split sentences`() = runSuspendTest {
        val actual = splitSentences("가을이다! 남자는 가을을 탄다...... 그렇지? 루루야! 버버리코트 사러 가자!!!!").toList()
        val expected = listOf(
            Sentence("가을이다!", 0, 5),
            Sentence("남자는 가을을 탄다......", 6, 22),
            Sentence("그렇지?", 23, 27),
            Sentence("루루야!", 28, 32),
            Sentence("버버리코트 사러 가자!!!!", 33, 48)
        )
        actual shouldBeEqualTo expected
    }

    @Test
    fun `add nouns to the dictionary`() = runSuspendTest {
        val dictionary = KoreanDictionaryProvider.koreanDictionary[Noun]!!

        dictionary.contains("후랴오교").shouldBeFalse()

        addNounsToDictionary("후랴오교")
        dictionary.contains("후랴오교").shouldBeTrue()
    }

    @Test
    fun `tokenizeTopN return top cadidates`() = runSuspendTest {
        val actual = tokenizeTopN("대선 후보", 3).toString()
        val expected =
            """
            |[
            |[[대선(Noun: 0, 2)], [대(Modifier: 0, 1), 선(Noun: 1, 1)], [대(Verb: 0, 1), 선(Noun: 1, 1)]], 
            |[[ (Space: 2, 1)]], 
            |[[후보(Noun: 3, 2)], [후보*(Noun: 3, 2)], [후(Noun: 3, 1), 보(Verb: 4, 1)]]
            |]""".trimMargin().replace(Systemx.LineSeparator, "")

        actual shouldBeEqualTo expected
    }

    @Test
    fun `tokenizeTopN with a given profile return different top cadidates from the defualt tokenizeTopN`() =
        runSuspendTest {
            val topN1 = tokenizeTopN("대선 후보", 3)
            val topN2 = tokenizeTopN(
                "대선 후보", 3, TokenizerProfile(
                    unknownPosCount = 1.0f,
                    allNoun = 10f,
                    preferredPattern = 4f
                )
            )
            topN1 shouldNotBeEqualTo topN2
        }

    @Test
    fun `tokensToStrings return correct strings`() = runSuspendTest {
        val actual = tokensToStrings(tokenize("사랑해"))
        val expected = listOf("사랑", "해")
        actual shouldBeEqualTo expected
    }

    @Test
    fun `tokenize product name`() = runSuspendTest {
        val productName = "[주간특가] 홈쇼핑 1위 착즙 주스! \"산지애\" 원액 배그대로 주스30팩+사과주스30팩"
        val actual = tokensToStrings(tokenize(productName))
        val expected = listOf(
            "[", "주간", "특가", "]", "홈쇼핑", "1", "위", "착즙", "주스", "!",
            "\"", "산", "지애", "\"", "원", "액", "배", "그대로", "주스", "30", "팩", "+", "사과주스", "30", "팩"
        )
        log.debug { "actual=$actual" }
        actual shouldBeEqualTo expected
    }

    @Test
    fun `tokenize and stem product name`() = runSuspendTest {
        val productName = "[주간특가] 홈쇼핑 1위 착즙 주스! \"산지애\" 원액 배그대로 주스30팩+사과주스30팩"
        val actual = stem(tokenize(productName))
        val expected = listOf(
            "[", "주간", "특가", "]", "홈쇼핑", "1", "위", "착즙", "주스", "!",
            "\"", "산", "지애", "\"", "원", "액", "배", "그대로", "주스", "30", "팩", "+", "사과주스", "30", "팩"
        )
        log.debug { "actual=$actual" }
        tokensToStrings(actual) shouldBeEqualTo expected
    }


    @Test
    fun `mask block words`() = runSuspendTest {
        // `걸.레` 는 resources/koreantext/block/block_low.txt 에 이미 등록되어 있다
        // KoreanTextProcessor.addBlockwordToDictionary(listOf("걸.레"), Severity.LOW)

        val original = "홈쇼핑 미니미는 무슨 걸.레 어쩌라구?"
        val expected = "홈쇼핑 미니미는 무슨 ** 어쩌라구?"
        val request = BlockwordRequest(original)
        val response = KoreanBlockwordProcessor.maskBlockwords(request)

        log.debug { "maskedText=$response" }
        response.maskedText shouldBeEqualTo expected
        response.blockWords shouldContainSame setOf("걸레")
    }

    @Test
    fun `add block word dynamically`() = runSuspendTest {
        io.bluetape4k.tokenizer.korean.KoreanProcessor.addBlockwords(
            listOf("은꼴사", "물쑈", "혼숙"),
            severity = Severity.LOW
        )

        val original = "홈쇼핑 미니미는 무슨 은꼴사야 어쩌라구? 물쑈야? 혼숙이야?"
        val expected = "홈쇼핑 미니미는 무슨 ***야 어쩌라구? **야? **이야?"
        val request = BlockwordRequest(original)
        val response = KoreanBlockwordProcessor.maskBlockwords(request)

        log.debug { "response=$response" }
        response.maskedText shouldBeEqualTo expected
        response.blockWords shouldContainAll arrayOf("은꼴사", "물쑈", "혼숙")
    }
}
