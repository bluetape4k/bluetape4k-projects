package io.bluetape4k.apache

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

/**
 * Reflection을 사용하여 객체의 속성 정보를 문자열로 변환합니다.
 *
 * ```kotlin
 * data class Person(val name: String, val age: Int)
 * val person = Person("Alice", 30)
 * person.reflectionToString()
 * // "Person@1a2b3c[name=Alice,age=30]"
 *
 * person.reflectionToString(ToStringStyle.JSON_STYLE)
 * // "{\"name\":\"Alice\",\"age\":30}"
 * ```
 *
 * @receiver 변환할 객체, null 이면 빈 문자열 반환
 * @param style 출력 형식 (기본: [ToStringStyle.DEFAULT_STYLE])
 * @return Reflection으로 생성된 문자열 표현
 * @see org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString
 */
fun Any?.reflectionToString(style: ToStringStyle = ToStringStyle.DEFAULT_STYLE): String {
    return if (this != null) ToStringBuilder.reflectionToString(this, style) else ""
}
