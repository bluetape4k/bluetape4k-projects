package io.bluetape4k.jackson.text

/**
 * YAML 문서 시작 마커(`---`)를 제거하고 앞뒤 공백을 정리합니다.
 *
 * ## 동작/계약
 * - 문자열 앞뒤 공백을 먼저 제거한 뒤, 시작이 `---`이면 해당 마커를 제거합니다.
 * - 원본 문자열은 변경하지 않고 새 문자열을 반환합니다.
 * - 입력이 빈 문자열이면 빈 문자열을 반환합니다.
 *
 * ```kotlin
 * val text = "---\nname: debop".trimYamlDocMarker()
 * // text == "name: debop"
 * ```
 */
fun String.trimYamlDocMarker(): String {
    var doc = this.trim()
    if (doc.startsWith("---")) {
        doc = doc.substring(3)
    }
    return doc.trim()
}
