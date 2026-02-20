package io.bluetape4k

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
class ToStringBuilder private constructor(private val className: String): Serializable {
    companion object {
        @JvmStatic
        operator fun invoke(className: String): ToStringBuilder {
            require(className.isNotBlank()) { "className[$className] must not be blank" }
            return ToStringBuilder(className)
        }

        @JvmStatic
        operator fun invoke(obj: Any): ToStringBuilder {
            return ToStringBuilder(obj.javaClass.simpleName)
        }
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

    fun add(name: String, value: Any?) = apply {
        props[name] = value.asString()
        cachedToString = null
    }

    fun toString(limit: Int): String = toStringValue(limit)

    override fun toString(): String = toStringValue(-1)
}
