package io.bluetape4k.protobuf

import com.google.protobuf.Message
import com.google.protobuf.kotlin.isA
import com.google.protobuf.kotlin.unpack

fun <T: Message> packMessage(message: T): ByteArray {
    val any = ProtoAny.pack(message)
    return any.toByteArray()
}

inline fun <reified T: Message> unpackMessage(bytes: ByteArray): T? {
    // https://developers.google.com/protocol-buffers/docs/proto3#any
    val any = ProtoAny.parseFrom(bytes)
    return if (any.isA<T>()) any.unpack() else null
}
