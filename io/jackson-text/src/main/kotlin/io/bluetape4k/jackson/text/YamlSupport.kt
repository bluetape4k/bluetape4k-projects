package io.bluetape4k.jackson.text

/**
 * YAML 문서의 시작을 나타내는 `---` 마커를 제거하고, 앞뒤 공백을 정리합니다.
 *
 * @return 마커가 제거된 YAML 문서 문자열
 */
fun String.trimYamlDocMarker(): String {
    var doc = this.trim()
    if (startsWith("---")) {
        doc = doc.substring(3)
    }
    return doc.trim()
}
