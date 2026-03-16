package io.bluetape4k.jackson.text

import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

/**
 * [trimYamlDocMarker] 확장 함수 단위 테스트.
 */
class YamlSupportTest {
    @Test
    fun `빈 문자열은 빈 문자열을 반환한다`() {
        "".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `공백만 있는 문자열은 빈 문자열을 반환한다`() {
        "   ".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `YAML 문서 시작 마커를 제거한다`() {
        val input = "---\nname: debop"
        input.trimYamlDocMarker() shouldBeEqualTo "name: debop"
    }

    @Test
    fun `마커 앞뒤 공백도 제거한다`() {
        val input = "  ---  \nname: debop"
        input.trimYamlDocMarker() shouldBeEqualTo "name: debop"
    }

    @Test
    fun `마커 없는 문자열은 그대로 반환한다`() {
        val input = "name: debop\nage: 54"
        input.trimYamlDocMarker() shouldBeEqualTo input
    }

    @Test
    fun `마커만 있는 경우 빈 문자열을 반환한다`() {
        "---".trimYamlDocMarker().shouldBeEmpty()
    }

    @Test
    fun `마커 뒤 내용이 여러 줄인 경우 전체 내용을 보존한다`() {
        val input = "---\nfoo: bar\nbaz: 1"
        input.trimYamlDocMarker() shouldBeEqualTo "foo: bar\nbaz: 1"
    }
}
