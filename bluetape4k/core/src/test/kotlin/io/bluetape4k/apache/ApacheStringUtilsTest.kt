package io.bluetape4k.apache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldEndWith
import io.bluetape4k.assertions.shouldStartWith
import org.junit.jupiter.api.Test

class ApacheStringUtilsTest {

    @Test
    fun `abbr와 abbrMiddle는 길이를 줄여준다`() {
        "abcdefghijklmno".abbr(maxWidth = 10).length shouldBeEqualTo 10
        "abcdef".abbrMiddle(length = 4, middle = ".") shouldBeEqualTo "ab.f"
    }

    @Test
    fun `appendIfMissing는 접미사를 추가한다`() {
        "abc".appendIfMissing("xyz") shouldBeEqualTo "abcxyz"
        "abc".appendIfMissingIgnoreCase("XYZ", "mno") shouldEndWith "XYZ"
    }

    @Test
    fun `center는 패딩을 적용한다`() {
        "ab".center(4) shouldBeEqualTo " ab "
        "a".center(4, "yz") shouldStartWith "y"
    }
}
