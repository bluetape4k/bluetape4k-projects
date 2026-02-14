package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.to

/**
 * Fastjson2 처리에서 데이터를 읽어오는 `readValueOrNull` 함수를 제공합니다.
 */
inline fun <reified T: Any> JSONArray.readValueOrNull(): T? = to<T>()

/**
 * Fastjson2 처리에서 데이터를 읽어오는 `readValueOrNull` 함수를 제공합니다.
 */
inline fun <reified T: Any> JSONArray.readValueOrNull(
    index: Int,
    vararg features: JSONReader.Feature,
): T? = to<T>(index, *features)

/**
 * Fastjson2 처리에서 데이터를 읽어오는 `readList` 함수를 제공합니다.
 */
inline fun <reified T> JSONArray.readList(vararg features: JSONReader.Feature): List<T> =
    toList(T::class.java, *features)

/**
 * Fastjson2 처리에서 데이터를 읽어오는 `readArray` 함수를 제공합니다.
 */
inline fun <reified T> JSONArray.readArray(vararg features: JSONReader.Feature): Array<T> =
    toArray(T::class.java, *features)
