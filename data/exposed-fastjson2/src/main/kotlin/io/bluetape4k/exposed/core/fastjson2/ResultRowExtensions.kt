package io.bluetape4k.exposed.core.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.bluetape4k.fastjson2.FastjsonSerializer
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow

@PublishedApi
internal fun ResultRow.anyValueOrNull(expression: Expression<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    return getOrNull(expression as Expression<Any?>)
}

@PublishedApi
internal fun requiredFastjsonError(expression: Expression<*>, typeName: String): Nothing =
    error("Expression[$expression] is null or not convertible to $typeName.")

inline fun <reified T: Any> ResultRow.getFastjson(
    expression: Expression<*>,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T = getFastjsonOrNull<T>(expression, serializer) ?: requiredFastjsonError(expression, T::class.simpleName ?: "T")

inline fun <reified T: Any> ResultRow.getFastjsonOrNull(
    expression: Expression<*>,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        is T         -> value
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

fun ResultRow.getFastjsonObject(expression: Expression<*>): JSONObject =
    getFastjsonObjectOrNull(expression) ?: requiredFastjsonError(expression, "JSONObject")

fun ResultRow.getFastjsonObjectOrNull(expression: Expression<*>): JSONObject? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is JSONObject -> value
        is String     -> JSON.parseObject(value)
        is ByteArray  -> JSON.parseObject(value)
        else          -> JSON.parseObject(value.toString())
    }
}

fun ResultRow.getFastjsonArray(expression: Expression<*>): JSONArray =
    getFastjsonArrayOrNull(expression) ?: requiredFastjsonError(expression, "JSONArray")

fun ResultRow.getFastjsonArrayOrNull(expression: Expression<*>): JSONArray? {
    val value = anyValueOrNull(expression) ?: return null
    return when (value) {
        is JSONArray -> value
        is String    -> JSON.parseArray(value)
        is ByteArray -> JSON.parseArray(value)
        else         -> JSON.parseArray(value.toString())
    }
}
