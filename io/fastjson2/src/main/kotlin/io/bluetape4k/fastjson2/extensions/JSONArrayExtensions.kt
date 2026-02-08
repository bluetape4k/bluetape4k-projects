package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.to

inline fun <reified T: Any> JSONArray.readValueOrNull(): T? = to<T>()

inline fun <reified T: Any> JSONArray.readValueOrNull(
    index: Int,
    vararg features: JSONReader.Feature,
): T? = to<T>(index, *features)

inline fun <reified T> JSONArray.readList(vararg features: JSONReader.Feature): List<T> =
    toList(T::class.java, *features)

inline fun <reified T> JSONArray.readArray(vararg features: JSONReader.Feature): Array<T> =
    toArray(T::class.java, *features)
