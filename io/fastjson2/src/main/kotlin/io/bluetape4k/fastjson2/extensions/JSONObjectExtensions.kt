package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.reference

/**
 * Fastjson2 처리에서 데이터를 읽어오는 `readValueOrNull` 함수를 제공합니다.
 */
inline fun <reified T: Any> JSONObject.readValueOrNull(vararg features: JSONReader.Feature): T? =
    to(reference<T>(), *features)

/**
 * Fastjson2 처리에서 데이터를 읽어오는 `readValueOrNull` 함수를 제공합니다.
 */
inline fun <reified T: Any> JSONObject.readValueOrNull(
    key: String,
    vararg features: JSONReader.Feature,
): T? =
    getObject(key, reference<T>(), *features)
