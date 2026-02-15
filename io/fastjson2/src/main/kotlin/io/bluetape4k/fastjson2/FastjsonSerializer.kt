package io.bluetape4k.fastjson2

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.toJSONString
import io.bluetape4k.fastjson2.extensions.readBytesOrNull
import io.bluetape4k.fastjson2.extensions.readValueOrNull
import io.bluetape4k.fastjson2.extensions.toJsonBytes
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray

/**
 * Fastjson2 처리에서 사용하는 `FastjsonSerializer` 타입입니다.
 */
class FastjsonSerializer: JsonSerializer {

    companion object: KLogging() {
        val Default by lazy { FastjsonSerializer() }
    }

    /**
     * Fastjson2 처리에서 데이터를 직렬화하는 `serialize` 함수를 제공합니다.
     */
    override fun serialize(graph: Any?): ByteArray {
        return graph?.run { toJsonBytes() } ?: emptyByteArray
    }

    /**
     * Fastjson2 처리에서 데이터를 역직렬화하는 `deserialize` 함수를 제공합니다.
     */
    override fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T? {
        return bytes?.run { JSONB.parseObject(this, clazz) }
    }

    /**
     * Fastjson2 처리에서 데이터를 직렬화하는 `serializeAsString` 함수를 제공합니다.
     */
    override fun serializeAsString(graph: Any?): String {
        return graph?.toJSONString().orEmpty()
    }

    /**
     * Fastjson2 처리에서 데이터를 역직렬화하는 `deserializeFromString` 함수를 제공합니다.
     */
    override fun <T: Any> deserializeFromString(jsonText: String?, clazz: Class<T>): T? {
        return jsonText?.readValueOrNull(clazz)
    }
}

/**
 * Fastjson2 처리에서 데이터를 역직렬화하는 `deserialize` 함수를 제공합니다.
 */
inline fun <reified T: Any> FastjsonSerializer.deserialize(bytes: ByteArray?): T? =
    bytes?.readBytesOrNull<T>()

/**
 * Fastjson2 처리에서 데이터를 역직렬화하는 `deserialize` 함수를 제공합니다.
 */
inline fun <reified T: Any> FastjsonSerializer.deserialize(jsonText: String?): T? =
    jsonText.readValueOrNull<T>()
