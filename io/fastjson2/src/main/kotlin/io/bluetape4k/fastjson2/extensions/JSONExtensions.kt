package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reference
import java.io.InputStream

fun Any?.toJsonString(
    vararg features: JSONWriter.Feature,
): String? =
    JSON.toJSONString(this, *features)

fun String?.readAsJSONObject(
    vararg features: JSONReader.Feature,
): JSONObject = JSON.parseObject(this, *features)

fun <T> String?.readValueOrNull(clazz: Class<T>, vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, clazz, *features)

inline fun <reified T: Any> String?.readValueOrNull(vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, reference(), *features)

inline fun <reified T: Any> String?.readValueAsList(vararg features: JSONReader.Feature): List<T> =
    JSON.parseArray<T>(this, reference<T>().type, *features).orEmpty()

inline fun <reified T: Any> InputStream?.readValueOrNull(vararg features: JSONReader.Feature): T? =
    JSON.parseObject(this, reference<T>().type, *features)

inline fun <reified T: Any> InputStream.readValueAsList(
    vararg features: JSONReader.Feature,
): List<T> =
    JSON.parseArray(this, *features).readList<T>(*features)

inline fun <reified T: Any> InputStream.readValueAsArray(
    vararg features: JSONReader.Feature,
): Array<T> =
    JSON.parseArray(this, *features).readArray<T>(*features)
