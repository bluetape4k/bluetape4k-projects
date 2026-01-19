package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reference
import java.io.InputStream

fun Any?.toJsonBytes(context: JSONWriter.Context? = null): ByteArray? =
    JSONB.toBytes(this, context)

inline fun <reified T: Any> ByteArray?.readBytesOrNull(vararg features: JSONReader.Feature): T? =
    JSONB.parseObject(this, reference<T>(), *features)

inline fun <reified T: Any> InputStream?.readBytesOrNull(
    length: Int = this@readBytesOrNull?.available() ?: 0,
    vararg features: JSONReader.Feature,
): T? = JSONB.parseObject(this, length, reference<T>().type, *features)
