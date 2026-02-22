package io.bluetape4k.exposed.core.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.r2dbc.spi.Readable

@PublishedApi
internal fun requiredFastjsonReadableError(column: Any, typeName: String): Nothing =
    error("Column[$column] is null or not convertible to $typeName.")

inline fun <reified T: Any> Readable.getFastjson(
    index: Int,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T = getFastjsonOrNull<T>(index, serializer) ?: requiredFastjsonReadableError(index, T::class.simpleName ?: "T")

inline fun <reified T: Any> Readable.getFastjson(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T = getFastjsonOrNull<T>(name, serializer) ?: requiredFastjsonReadableError(name, T::class.simpleName ?: "T")

inline fun <reified T: Any> Readable.getFastjsonOrNull(
    index: Int,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T? {
    val value = get(index) ?: return null
    return when (value) {
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        is T         -> value
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

inline fun <reified T: Any> Readable.getFastjsonOrNull(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): T? {
    val value = get(name) ?: return null
    return when (value) {
        is String -> serializer.deserializeFromString<T>(value)
        is ByteArray -> serializer.deserialize<T>(value)
        is T         -> value
        else      -> serializer.deserializeFromString<T>(value.toString())
    }
}

fun Readable.getFastjsonObject(index: Int): JSONObject =
    getFastjsonObjectOrNull(index) ?: requiredFastjsonReadableError(index, "JSONObject")

fun Readable.getFastjsonObject(name: String): JSONObject =
    getFastjsonObjectOrNull(name) ?: requiredFastjsonReadableError(name, "JSONObject")

fun Readable.getFastjsonObjectOrNull(index: Int): JSONObject? {
    val value = get(index) ?: return null
    return when (value) {
        is JSONObject -> value
        is String     -> JSON.parseObject(value)
        is ByteArray  -> JSON.parseObject(value)
        else          -> JSON.parseObject(value.toString())
    }
}

fun Readable.getFastjsonObjectOrNull(name: String): JSONObject? {
    val value = get(name) ?: return null
    return when (value) {
        is JSONObject -> value
        is String     -> JSON.parseObject(value)
        is ByteArray  -> JSON.parseObject(value)
        else          -> JSON.parseObject(value.toString())
    }
}

fun Readable.getFastjsonArray(index: Int): JSONArray =
    getFastjsonArrayOrNull(index) ?: requiredFastjsonReadableError(index, "JSONArray")

fun Readable.getFastjsonArray(name: String): JSONArray =
    getFastjsonArrayOrNull(name) ?: requiredFastjsonReadableError(name, "JSONArray")

fun Readable.getFastjsonArrayOrNull(index: Int): JSONArray? {
    val value = get(index) ?: return null
    return when (value) {
        is JSONArray -> value
        is String    -> JSON.parseArray(value)
        is ByteArray -> JSON.parseArray(value)
        else         -> JSON.parseArray(value.toString())
    }
}

fun Readable.getFastjsonArrayOrNull(name: String): JSONArray? {
    val value = get(name) ?: return null
    return when (value) {
        is JSONArray -> value
        is String    -> JSON.parseArray(value)
        is ByteArray -> JSON.parseArray(value)
        else         -> JSON.parseArray(value.toString())
    }
}
