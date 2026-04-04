package io.bluetape4k.io.serializer

/**
 * Binary 직렬화/역직렬화 실패를 나타내는 예외입니다.
 *
 * [BinarySerializer] 구현체에서 직렬화 또는 역직렬화 중 오류가 발생하면 이 예외를 던집니다.
 *
 * ```kotlin
 * val serializer = BinarySerializers.Kryo
 *
 * try {
 *     val data = serializer.serialize(myObject)
 *     val restored = serializer.deserialize<MyClass>(data)
 * } catch (e: BinarySerializationException) {
 *     println("직렬화 실패: ${e.message}")
 *     e.cause?.let { println("원인: $it") }
 * }
 * ```
 *
 * @param message 오류 메시지
 * @param cause   원인 예외 (선택)
 */
class BinarySerializationException(
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)
