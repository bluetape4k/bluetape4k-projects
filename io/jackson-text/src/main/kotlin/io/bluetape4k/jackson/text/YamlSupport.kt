package io.bluetape4k.jackson.text

/**
 * YAML 문서의 시작을 나타내는 마커를 제거합니다.
 */
fun String.trimYamlDocMarker(): String {
    var doc = this.trim()
    if (startsWith("---")) {
        doc = doc.substring(3)
    }
    return doc.trim()
}
