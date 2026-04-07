package io.bluetape4k.protobuf.serializers

import io.bluetape4k.io.serializer.AbstractBinarySerializer
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.ProtoAny
import io.bluetape4k.protobuf.ProtoMessage
import io.bluetape4k.support.isNullOrEmpty
import java.util.concurrent.ConcurrentHashMap

/**
 * Protobuf 메시지는 `Any`로 직렬화하고, 그 외 타입은 fallback serializer로 처리하는 바이너리 직렬화기입니다.
 *
 * ## 동작/계약
 * - [ProtoMessage] 입력은 `ProtoAny.pack(message).toByteArray()`로 직렬화합니다.
 * - 역직렬화 시 `typeUrl`의 클래스명을 기준으로 message 타입을 캐시해 언패킹합니다.
 * - Protobuf 경로 실패 시 [fallback]으로 자동 위임합니다.
 *
 * ```kotlin
 * val serializer = ProtobufSerializer()
 * val bytes = serializer.serialize(message)
 * // bytes.isNotEmpty() == true
 * ```
 */
class ProtobufSerializer(
    private val fallback: BinarySerializer = BinarySerializers.Jdk,
): AbstractBinarySerializer() {
    companion object {
        private val messageTypes = ConcurrentHashMap<String, Class<out ProtoMessage>>()
    }

    override fun doSerialize(graph: Any): ByteArray =
        if (graph is ProtoMessage) {
            ProtoAny.pack(graph).toByteArray()
        } else {
            fallback.serialize(graph)
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> doDeserialize(bytes: ByteArray): T? {
        if (bytes.isNullOrEmpty()) {
            return null
        }

        return try {
            val protoAny = ProtoAny.parseFrom(bytes)
            val className = protoAny.typeUrl.substringAfterLast("/")
            val clazz =
                messageTypes.getOrPut(className) {
                    Class.forName(className) as Class<ProtoMessage>
                }
            protoAny.unpack(clazz) as? T
        } catch (e: Throwable) {
            log.debug(e) { "Protobuf 역직렬화 실패, fallback serializer로 대체합니다." }
            fallback.deserialize(bytes)
        }
    }
}
