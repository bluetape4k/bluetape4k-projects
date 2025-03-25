package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.reference

inline fun <reified T: Any> JSONObject.readValueOrNull(
    vararg features: JSONReader.Feature,
): T? = to(reference(), *features)


inline fun <reified T: Any> JSONObject.readValueOrNull(
    key: String,
    vararg features: JSONReader.Feature,
): T? = getObject(key, reference(), *features)
