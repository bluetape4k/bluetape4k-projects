package io.bluetape4k.fastjson2

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.fastjson2.extensions.readBytesOrNull
import io.bluetape4k.fastjson2.extensions.readValueOrNull
import io.bluetape4k.fastjson2.extensions.toJsonBytes
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.emptyByteArray

class FastjsonSerializer: JsonSerializer {

    companion object: KLogging() {
        val Default by lazy { FastjsonSerializer() }
    }

    override fun serialize(graph: Any?): ByteArray {
        return graph?.run { toJsonBytes() } ?: emptyByteArray
    }

    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        return bytes?.run { JSONB.parseObject(this, clazz) }
    }

    override fun serializeAsString(graph: Any?): String {
        return graph?.toJSONString() ?: EMPTY_STRING
    }

    override fun <T: Any> deserializeFromString(jsonText: String?, clazz: Class<T>): T? {
        return jsonText?.readValueOrNull(clazz)
    }
}

inline fun <reified T: Any> FastjsonSerializer.deserialize(bytes: ByteArray?): T? =
    bytes?.readBytesOrNull<T>()

inline fun <reified T: Any> FastjsonSerializer.deserialize(jsonText: String?): T? =
    jsonText.readValueOrNull<T>()
