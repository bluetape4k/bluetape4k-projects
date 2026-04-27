package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.TestBase
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class TwitterCompatPatternsTest : TestBase() {

    companion object : KLogging()

    // --- VALID_URL ---

    @Test
    fun `VALID_URL matches scheme URL`() {
        val m = TwitterCompatPatterns.VALID_URL.matcher("https://openkoreantext.org")
        m.find().shouldBeTrue()
        m.group() shouldBeEqualTo "https://openkoreantext.org"
    }

    @Test
    fun `VALID_URL matches bare domain`() {
        val m = TwitterCompatPatterns.VALID_URL.matcher("openkoreantext.org에서 API 테스트")
        m.find().shouldBeTrue()
        m.group() shouldBeEqualTo "openkoreantext.org"
    }

    @Test
    fun `VALID_URL matches bare domain after space`() {
        val m = TwitterCompatPatterns.VALID_URL.matcher("한국어와 English와 1234와 pic.twitter.com")
        m.find().shouldBeTrue()
        m.group() shouldBeEqualTo "pic.twitter.com"
    }

    @Test
    fun `VALID_URL includes leading paren but excludes trailing paren`() {
        val m = TwitterCompatPatterns.VALID_URL.matcher("스팀(https://store.steampowered.com)에서")
        m.find().shouldBeTrue()
        m.group() shouldBeEqualTo "(https://store.steampowered.com"
        m.start() shouldBeEqualTo 2
        m.end() shouldBeEqualTo 33
    }

    @Test
    fun `VALID_URL matches long URL with query params`() {
        val url = "http://news.kukinews.com/article/view.asp?page=1&gCode=soc&arcid=0008599913&code=41121111"
        val m = TwitterCompatPatterns.VALID_URL.matcher(url)
        m.find().shouldBeTrue()
        m.group() shouldBeEqualTo url
    }

    @Test
    fun `VALID_URL does not match plain word`() {
        val m = TwitterCompatPatterns.VALID_URL.matcher("hello world")
        m.find() shouldBeEqualTo false
    }

    // --- VALID_HASHTAG ---

    @Test
    fun `VALID_HASHTAG matches hashtag after space`() {
        val m = TwitterCompatPatterns.VALID_HASHTAG.matcher("구글에는 #Google")
        m.find().shouldBeTrue()
        m.group(1) shouldBeEqualTo " "
        m.group(2) shouldBeEqualTo "#Google"
        m.group() shouldBeEqualTo " #Google"
    }

    @Test
    fun `VALID_HASHTAG matches hashtag at start of string`() {
        val m = TwitterCompatPatterns.VALID_HASHTAG.matcher("#해쉬태그 이라는 것")
        m.find().shouldBeTrue()
        m.group(1) shouldBeEqualTo ""
        m.group(2) shouldBeEqualTo "#해쉬태그"
    }

    @Test
    fun `VALID_HASHTAG matches Korean hashtag`() {
        val m = TwitterCompatPatterns.VALID_HASHTAG.matcher("구글에는 정말로 이쁜 자전거가 있다. #Google #이쁜자전거 #갖고싶다")
        val results = mutableListOf<String>()
        while (m.find()) results.add(m.group())
        results shouldBeEqualTo listOf(" #Google", " #이쁜자전거", " #갖고싶다")
    }

    @Test
    fun `VALID_HASHTAG matches hashtag with underscore`() {
        val m = TwitterCompatPatterns.VALID_HASHTAG.matcher("#korean_tokenizer_rocks")
        m.find().shouldBeTrue()
        m.group(2) shouldBeEqualTo "#korean_tokenizer_rocks"
    }

    // --- VALID_MENTION_OR_LIST ---

    @Test
    fun `VALID_MENTION_OR_LIST matches mention after space`() {
        val input = "트위터 아이디는 언제든지 변경이 가능합니다. @ironman을 @drstrange로 바꿀 수 있습니다."
        val m = TwitterCompatPatterns.VALID_MENTION_OR_LIST.matcher(input)
        val results = mutableListOf<String>()
        while (m.find()) results.add(m.group())
        results shouldBeEqualTo listOf(" @ironman", " @drstrange")
    }

    @Test
    fun `VALID_MENTION_OR_LIST matches mention at start of string`() {
        val m = TwitterCompatPatterns.VALID_MENTION_OR_LIST.matcher("@nlpenguin @edeng")
        m.find().shouldBeTrue()
        m.group(1) shouldBeEqualTo ""   // ^ anchor: zero-width group
        m.group(2) shouldBeEqualTo "@nlpenguin"
    }

    @Test
    fun `VALID_MENTION_OR_LIST full match includes leading space`() {
        val m = TwitterCompatPatterns.VALID_MENTION_OR_LIST.matcher("hello @world")
        m.find().shouldBeTrue()
        m.group() shouldBeEqualTo " @world"
        m.start() shouldBeEqualTo 5
        m.end() shouldBeEqualTo 12
    }

    @Test
    fun `VALID_MENTION_OR_LIST matches mention after non-word char`() {
        val m = TwitterCompatPatterns.VALID_MENTION_OR_LIST.matcher(""""@user: hello""")
        m.find().shouldBeTrue()
        m.group(1) shouldBeEqualTo "\""
        m.group(2) shouldBeEqualTo "@user"
    }

    // --- VALID_CASHTAG ---

    @Test
    fun `VALID_CASHTAG matches lowercase cashtag`() {
        val input = $$"주식정보 트윗 안내 : Twitter의 주식은 $twtr, Apple의 주식은 $appl 입니다."
        val m = TwitterCompatPatterns.VALID_CASHTAG.matcher(input)
        val results = mutableListOf<String>()
        while (m.find()) results.add(m.group())
        results shouldBeEqualTo listOf($$" $twtr", $$" $appl")
    }

    @Test
    fun `VALID_CASHTAG matches uppercase cashtag`() {
        val m = TwitterCompatPatterns.VALID_CASHTAG.matcher($$"주식 $AAPL 매수")
        m.find().shouldBeTrue()
        m.group(2) shouldBeEqualTo $$"$AAPL"
    }

    @Test
    fun `VALID_CASHTAG matches cashtag with dot suffix`() {
        val m = TwitterCompatPatterns.VALID_CASHTAG.matcher($$"버크셔 $BRK.A 어때")
        m.find().shouldBeTrue()
        m.group(2) shouldBeEqualTo $$"$BRK.A"
    }

    @Test
    fun `VALID_CASHTAG matches cashtag at start of string`() {
        val m = TwitterCompatPatterns.VALID_CASHTAG.matcher($$"$twtr 매수")
        m.find().shouldBeTrue()
        m.group(1) shouldBeEqualTo ""
        m.group(2) shouldBeEqualTo $$"$twtr"
    }
}
