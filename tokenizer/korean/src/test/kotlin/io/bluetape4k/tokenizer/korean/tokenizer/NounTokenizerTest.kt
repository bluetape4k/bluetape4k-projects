package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test

class NounTokenizerTest: TestBase() {

    companion object: KLogging() {
        private val HA = KoreanToken("하", Noun, 0, 0)
        private val HA_UNKNOWN = KoreanToken("하", Noun, 0, 0, unknown = true)
        private val HAHA_UNKNOWN = KoreanToken("하하", Noun, 0, 0, unknown = true)

        private val HUMAN = KoreanToken("사람", Noun, 0, 0)
        private val DOG = KoreanToken("강아지", Noun, 0, 0)

        private val HADA = KoreanToken("하다", Verb, 0, 0)
    }

    private val parsedChunk = ParsedChunk(listOf(HA, HA, HA), 1)
    private val parsedChunkWithTwoTokens = ParsedChunk(listOf(HA, HA), 1)
    private val parsedChunkWithUnknowns = ParsedChunk(listOf(HAHA_UNKNOWN, HA_UNKNOWN, HA), 1)
    private val parsedChunkWithCommonNouns = ParsedChunk(listOf(HUMAN, DOG), 1)
    private val parsedChunkWithVerbs = ParsedChunk(listOf(HUMAN, HADA), 1)
    private val parsedChunkWithExactMatch = ParsedChunk(listOf(DOG), 1)

    @Test
    fun `should count unknowns`() {
        parsedChunkWithUnknowns.countUnknowns shouldBeEqualTo 2
        parsedChunk.countUnknowns shouldBeEqualTo 0
    }

    @Test
    fun `should count tokens`() {
        parsedChunk.countTokens shouldBeEqualTo 3
        parsedChunkWithTwoTokens.countTokens shouldBeEqualTo 2
    }

    @Test
    fun `should get correct frequeency score`() {
        parsedChunkWithTwoTokens.getFreqScore() shouldBeEqualTo 1.0f
        parsedChunkWithCommonNouns.getFreqScore() shouldBeEqualTo 0.4544f
    }

    @Test
    fun `should count POSes`() {
        parsedChunk.countPos(Noun) shouldBeEqualTo 3
        parsedChunkWithVerbs.countPos(Noun) shouldBeEqualTo 1
        parsedChunkWithVerbs.countPos(Verb) shouldBeEqualTo 1
    }

    @Test
    fun `should determine if the chunk is an exact match`() {
        parsedChunk.isExactMatch shouldBeEqualTo 1
        parsedChunkWithExactMatch.isExactMatch shouldBeEqualTo 0
    }

    @Test
    fun `should determine if the chunk is all noun`() {
        parsedChunk.isAllNouns shouldBeEqualTo 0
        parsedChunkWithVerbs.isAllNouns shouldBeEqualTo 1
    }

    @Test
    fun `tokenize should return expected tokens`() = runTest {
        val expected = listOf(
            KoreanToken("개", Noun, 0, 1),
            KoreanToken("루루", Noun, 1, 2),
            KoreanToken("야", Noun, 3, 1, null, true)
        )
        val actual = NounTokenizer.tokenize("개루루야")

        actual shouldContainSame expected

        //    actual = NounTokenizer.tokenize("쵸귀여운")
        //    expected = listOf(KoreanToken("쵸", VerbPrefix, 0, 1),
        //            KoreanToken("귀여운", Adjective, 1, 3, stem = "귀엽다"))

        //    Assertions.assertThat(actual).isEqualTo(expected)

        //    actual = NounTokenizer.tokenize("이사람의")
        //    expected = listOf(KoreanToken("이", Determiner, 0, 1),
        //            KoreanToken("사람", Noun, 1, 2),
        //            KoreanToken("의", Josa, 3, 1))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("엄청작아서귀엽다")
        //    expected = listOf(KoreanToken("엄청", Adverb, 0, 2),
        //            KoreanToken("작아서", Adjective, 2, 3, stem = "작다"),
        //            KoreanToken("귀엽다", Adjective, 5, 3, stem = "귀엽다"))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("안녕하셨어요")
        //    expected = listOf(KoreanToken("안녕하셨어요", Adjective, 0, 6, stem = "안녕하다"))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("쵸귀여운개루루")
        //    expected = listOf(KoreanToken("쵸", VerbPrefix, 0, 1),
        //            KoreanToken("귀여운", Adjective, 1, 3, stem = "귀엽다"),
        //            KoreanToken("개", Noun, 4, 1),
        //            KoreanToken("루루", Noun, 5, 2))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("그리고")
        //    expected = listOf(KoreanToken("그리고", Conjunction, 0, 3))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("안녕ㅋㅋ")
        //    expected = listOf(KoreanToken("안녕", Noun, 0, 2),
        //            KoreanToken("ㅋㅋ", KoreanParticle, 2, 2))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("라고만")
        //    expected = listOf(KoreanToken("라고만", Eomi, 0, 3))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)
        //
        //    actual = NounTokenizer.tokenize("\"라면서 외쳤다")
        //    expected = listOf(KoreanToken("\"", Punctuation, 0, 1),
        //            KoreanToken("라면서", Eomi, 1, 3),
        //            KoreanToken(" ", Space, 4, 1),
        //            KoreanToken("외쳤다", Verb, 5, 3, stem = "외치다"))
        //
        //    Assertions.assertThat(actual).isEqualTo(expected)

        //    actual = NounTokenizer.tokenize("사랑해")
        //    expected = listOf(KoreanToken("사랑", Noun, 0, 2),
        //            KoreanToken("해", Verb, 2, 1, stem = "하다"))

        //    Assertions.assertThat(actual).isEqualTo(expected)

    }

    @Test
    fun `should handle unknown nouns`() {
        //    var actual = NounTokenizer.tokenize("개컁컁아")
        //    var expected = listOf(KoreanToken("개컁컁", Noun, 0, 3, unknown = true),
        //            KoreanToken("아", Josa, 3, 1))
        //    Assertions.assertThat(actual).isEqualTo(expected)

        //    var actual = NounTokenizer.tokenize("안녕하세요쿛툐캬님")
        //    var expected = listOf(KoreanToken("안녕하세요", Adjective, 0, 5, stem = "안녕하다"),
        //            KoreanToken("쿛툐캬", Noun, 5, 3, unknown = true),
        //            KoreanToken("님", Suffix, 8, 1))
        //    Assertions.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should be able to tokenize long non-space-correctable ones`() = runTest {
        val actual = NounTokenizer.tokenize("훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌쩍훌")
        val expected =
            (0 until 24).map { KoreanToken("훌쩍", Noun, it * 2, 2) } + KoreanToken("훌", Noun, 48, 1, unknown = true)
        actual shouldContainSame expected
    }

    @Test
    fun `should tokenize edge cases`() = runTest {
        val actual = NounTokenizer.tokenize("해쵸쵸쵸쵸쵸쵸쵸쵸춏")
        val expected = listOf(
            KoreanToken("해", Noun, 0, 1),
            KoreanToken("쵸쵸쵸쵸쵸쵸쵸쵸", Noun, 1, 8, unknown = true),
            KoreanToken("춏", Noun, 9, 1, unknown = true)
        )
        actual shouldContainSame expected
    }

    //  @Test
    //  fun `should add user-added nouns to dictionary`() {
    //    Assertions.assertThat(KoreanDictionaryProvider.koreanDictionary[Noun]!!.contains("뇬뇨")).isFalse()
    //    Assertions.assertThat(KoreanDictionaryProvider.koreanDictionary[Noun]!!.contains("츄쵸")).isFalse()
    //
    //    var actual = NounTokenizer.tokenize("뇬뇨뇬뇨뇬뇨뇬뇨츄쵸")
    //    var expected = listOf(KoreanToken("뇬뇨뇬뇨뇬뇨뇬뇨", Noun, 0, 8, unknown = true),
    //            KoreanToken("츄쵸", Noun, 8, 2, unknown = true))
    //    Assertions.assertThat(actual).isEqualTo(expected)
    //
    //    KoreanDictionaryProvider.addWordsToDictionary(Noun, listOf("뇬뇨", "츄쵸"))
    //
    //    Assertions.assertThat(KoreanDictionaryProvider.koreanDictionary[Noun]!!.contains("뇬뇨")).isTrue()
    //    Assertions.assertThat(KoreanDictionaryProvider.koreanDictionary[Noun]!!.contains("츄쵸")).isTrue()
    //
    //    actual = NounTokenizer.tokenize("뇬뇨뇬뇨뇬뇨뇬뇨츄쵸")
    //    expected = listOf(KoreanToken("뇬뇨", Noun, 0, 2),
    //            KoreanToken("뇬뇨", Noun, 2, 2),
    //            KoreanToken("뇬뇨", Noun, 4, 2),
    //            KoreanToken("뇬뇨", Noun, 6, 2),
    //            KoreanToken("츄쵸", Noun, 8, 2))
    //    Assertions.assertThat(actual).isEqualTo(expected)
    //  }

}
