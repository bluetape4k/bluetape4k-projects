package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.geoHashOfString
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test

class StringSupportTest {
    companion object: KLogging()

    @Test
    fun `padLeft 기본 동작`() {
        val result = padLeft("abc", 5, "0")
        result shouldBeEqualTo "00abc"
    }

    @Test
    fun `padLeft 문자열이 이미 충분히 긴 경우`() {
        val result = padLeft("abcdef", 5, "0")
        result shouldBeEqualTo "abcdef"
    }

    @Test
    fun `padLeft 빈 문자열`() {
        val result = padLeft("", 3, "x")
        result shouldBeEqualTo "xxx"
    }

    @Test
    fun `padLeft 다양한 패딩 문자`() {
        padLeft("a", 3, "-") shouldBeEqualTo "--a"
        padLeft("a", 3, " ") shouldBeEqualTo "  a"
        padLeft("a", 3, "#") shouldBeEqualTo "##a"
    }

    @Test
    fun `GeoHash 이진 문자열 패딩`() {
        // GeoHash의 bits를 이진 문자열로 변환할 때 사용
        val hash = geoHashOfString("9q8y")
        val binaryStr = hash.toBinaryString()

        // 결과 검증
        binaryStr.length.shouldBeGreaterOrEqualTo(0)
        binaryStr.length.shouldBeLessOrEqualTo(64)
    }

    @Test
    fun `padLeft 정확한 길이`() {
        val testCases =
            listOf(
                Triple("a", 5, "0") to "0000a",
                Triple("ab", 4, "x") to "xxab",
                Triple("abc", 3, "-") to "abc",
                Triple("", 3, "*") to "***",
            )

        testCases.forEach { (input, expected) ->
            val (str, length, pad) = input
            val result = padLeft(str, length, pad)
            result shouldBeEqualTo expected
            result.length shouldBeEqualTo maxOf(str.length, length)
        }
    }
}
