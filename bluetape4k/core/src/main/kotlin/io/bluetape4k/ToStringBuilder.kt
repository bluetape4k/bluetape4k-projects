package io.bluetape4k

import io.bluetape4k.support.requireNotBlank
import java.io.Serializable

/**
 * Business Entity의 toString()을 손쉽게 설정할 수 있게 해주는 Builder 입니다.
 *
 * ```
 * val builder = ToStringBuilder("object").apply {
 *     add("a", 1)
 *     add("b", "two")
 * }
 * builder.toString() shouldBeEqualTo "object(a=1,b=two)"
 * ```
 *
 * @see AbstractValueObject
 */
class ToStringBuilder private constructor(
    private val className: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        operator fun invoke(className: String): ToStringBuilder {
            className.requireNotBlank("className")
            return ToStringBuilder(className)
        }

        @JvmStatic
        operator fun invoke(obj: Any): ToStringBuilder = ToStringBuilder(obj.javaClass.simpleName)
    }

    private val props = LinkedHashMap<String, String>()
    private var cachedToString: String? = null

    private fun toStringValue(limit: Int): String {
        if (limit < 0) {
            return cachedToString ?: run {
                val text = props.entries.joinToString(",") { "${it.key}=${it.value}" }
                "$className($text)".also { cachedToString = it }
            }
        }
        val text = props.entries.joinToString(",", limit = limit) { "${it.key}=${it.value}" }
        return "$className($text)"
    }

    private fun Any?.asString(): String = this?.toString() ?: "<null>"

    /**
     * 프로퍼티를 추가합니다.
     *
     * ```kotlin
     * val builder = ToStringBuilder("Person")
     *     .add("name", "Alice")
     *     .add("age", 30)
     * builder.toString()  // "Person(name=Alice,age=30)"
     * ```
     *
     * @param name  프로퍼티 이름
     * @param value 프로퍼티 값 (null 이면 `<null>` 으로 표시)
     * @return 현재 [ToStringBuilder] 인스턴스 (체이닝용)
     */
    fun add(
        name: String,
        value: Any?,
    ) = apply {
        props[name] = value.asString()
        cachedToString = null
    }

    /**
     * 최대 [limit]개의 프로퍼티만 포함하는 문자열을 반환합니다.
     *
     * ```kotlin
     * val builder = ToStringBuilder("Item")
     *     .add("a", 1).add("b", 2).add("c", 3)
     * builder.toString(2)  // "Item(a=1,b=2, ...and 1 more)"
     * ```
     *
     * @param limit 포함할 최대 프로퍼티 수
     * @return 제한된 문자열 표현
     */
    fun toString(limit: Int): String = toStringValue(limit)

    override fun toString(): String = toStringValue(-1)
}
