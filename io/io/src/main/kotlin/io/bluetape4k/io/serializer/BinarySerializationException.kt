package io.bluetape4k.io.serializer

/**
 * Binary 직렬화/역직렬화 실패를 나타내는 예외입니다.
 */
class BinarySerializationException(
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)
