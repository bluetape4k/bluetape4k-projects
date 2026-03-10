package io.bluetape4k.protobuf

import com.google.protobuf.Message
import com.google.protobuf.kotlin.isA
import com.google.protobuf.kotlin.unpack

/**
 * Protobuf [Message]를 `Any`로 감싸 바이트 배열로 패킹합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `ProtoAny.pack(message)` 결과를 직렬화합니다.
 * - 반환값은 새 바이트 배열입니다.
 *
 * ```kotlin
 * val bytes = packMessage(message)
 * // bytes.isNotEmpty() == true
 * ```
 */
fun <T: Message> packMessage(message: T): ByteArray {
    val any = ProtoAny.pack(message)
    return any.toByteArray()
}

/**
 * `Any` 바이트 배열에서 지정 타입 [T] 메시지를 언패킹합니다.
 *
 * ## 동작/계약
 * - 바이트를 `ProtoAny.parseFrom(bytes)`로 파싱합니다.
 * - 실제 타입이 [T]와 다르면 `null`을 반환합니다.
 * - 파싱 실패 예외는 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val msg: MyProto? = unpackMessage(bytes)
 * // msg == null || msg is MyProto
 * ```
 */
inline fun <reified T: Message> unpackMessage(bytes: ByteArray): T? {
    val any = ProtoAny.parseFrom(bytes)
    return if (any.isA<T>()) any.unpack() else null
}
