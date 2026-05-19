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
 * - 역직렬화 시 `typeUrl`의 클래스명을 [allowedClassPrefixes] 허용 목록으로 검증한 후 로딩합니다.
 * - 허용 목록에 없는 클래스명은 [SecurityException]을 발생시킵니다.
 * - Protobuf 경로 실패 시 [fallback]으로 자동 위임합니다.
 *
 * ## 보안 주의
 * - `typeUrl`에서 추출한 클래스명은 [allowedClassPrefixes]에 해당하는 것만 로딩합니다.
 * - `Class.forName` 호출 시 static initializer를 실행하지 않아 gadget chain 실행을 방지합니다.
 * - 사용자 정의 Protobuf 클래스가 허용 목록 외 패키지에 있으면 [allowedClassPrefixes]에 추가하세요.
 *
 * ```kotlin
 * // 기본 사용 — io.bluetape4k.** 과 com.google.protobuf.** 클래스만 허용
 * val serializer = ProtobufSerializer()
 * val bytes = serializer.serialize(message)
 *
 * // 사용자 패키지 추가
 * val serializer = ProtobufSerializer(
 *     allowedClassPrefixes = setOf("io.bluetape4k.", "com.google.protobuf.", "com.example.proto.")
 * )
 * ```
 */
class ProtobufSerializer(
    private val fallback: BinarySerializer = BinarySerializers.Kryo,
    private val allowedClassPrefixes: Set<String> = DEFAULT_ALLOWED_PREFIXES,
): AbstractBinarySerializer() {
    init {
        require(allowedClassPrefixes.all { it.isNotBlank() }) {
            "allowedClassPrefixes must not contain blank entries."
        }
    }

    companion object {
        private val messageTypes = ConcurrentHashMap<String, Class<out ProtoMessage>>()

        /**
         * 기본 허용 클래스 패키지 접두사 목록.
         * Protobuf 역직렬화 시 `typeUrl`에서 추출된 클래스명은 이 목록 중 하나로 시작해야 합니다.
         */
        val DEFAULT_ALLOWED_PREFIXES: Set<String> = setOf(
            "io.bluetape4k.",
            "com.google.protobuf."
        )
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

            // 보안: 허용 목록에 없는 클래스는 로딩 거부
            require(allowedClassPrefixes.any { className.matchesAllowedPrefix(it) }) {
                "신뢰할 수 없는 Protobuf 클래스: $className. allowedClassPrefixes에 추가하세요."
            }

            val clazz =
                messageTypes.getOrPut(className) {
                    // initialize=false: static initializer 실행 방지 (gadget chain RCE 예방)
                    @Suppress("UNCHECKED_CAST")
                    Class.forName(className, false, Thread.currentThread().contextClassLoader) as Class<ProtoMessage>
                }
            protoAny.unpack(clazz) as? T
        } catch (e: IllegalArgumentException) {
            throw SecurityException("Protobuf 역직렬화 차단: ${e.message}", e)
        } catch (e: Throwable) {
            log.debug(e) { "Protobuf 역직렬화 실패, fallback serializer로 대체합니다." }
            fallback.deserialize(bytes)
        }
    }

    private fun String.matchesAllowedPrefix(prefix: String): Boolean =
        this == prefix || startsWith(prefix.ensurePackagePrefix())

    private fun String.ensurePackagePrefix(): String =
        if (endsWith(".")) this else "$this."
}
