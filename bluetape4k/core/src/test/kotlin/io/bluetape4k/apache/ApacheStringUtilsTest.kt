package io.bluetape4k.apache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldEndWith
import org.amshove.kluent.shouldStartWith
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
