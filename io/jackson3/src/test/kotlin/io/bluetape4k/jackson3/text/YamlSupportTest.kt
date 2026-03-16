package io.bluetape4k.jackson3.text

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * [trimYamlDocMarker] 확장 함수 테스트
 */
class YamlSupportTest {
    @Test
    fun `trimYamlDocMarker - 문서 시작 마커 제거`() {
        val input = "---\nname: debop"
        input.trimYamlDocMarker() shouldBeEqualTo "name: debop"
    }

    @Test
    fun `trimYamlDocMarker - 앞뒤 공백 포함 마커 제거`() {
        val input = "  ---  \nname: debop\n"
        input.trimYamlDocMarker() shouldBeEqualTo "name: debop"
    }

    @Test
    fun `trimYamlDocMarker - 마커 없는 경우 원본 반환`() {
        val input = "name: debop"
        input.trimYamlDocMarker() shouldBeEqualTo "name: debop"
    }

    @Test
    fun `trimYamlDocMarker - 빈 문자열 입력 시 빈 문자열 반환`() {
        "".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `trimYamlDocMarker - 공백만 있는 문자열 입력 시 빈 문자열 반환`() {
        "   ".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `trimYamlDocMarker - 마커만 있는 경우 빈 문자열 반환`() {
        "---".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `trimYamlDocMarker - 마커와 공백만 있는 경우 빈 문자열 반환`() {
        "---   ".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `trimYamlDocMarker - 마커 뒤 콘텐츠 공백 정리`() {
        val input = "---\n  key: value  "
        input.trimYamlDocMarker() shouldBeEqualTo "key: value"
    }
}
