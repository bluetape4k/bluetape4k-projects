package io.bluetape4k.aws.core

import software.amazon.awssdk.core.SdkBytes
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

fun ByteArray.toSdkBytes(): SdkBytes = SdkBytes.fromByteArray(this)
fun ByteArray.toSdkBytesUnsafe(): SdkBytes = SdkBytes.fromByteArrayUnsafe(this)

fun String.toSdkBytes(cs: Charset = Charsets.UTF_8): SdkBytes = SdkBytes.fromString(this, cs)
fun String.toUtf8SdkBytes(): SdkBytes = SdkBytes.fromUtf8String(this)

fun InputStream.toSdkBytes(): SdkBytes = SdkBytes.fromInputStream(this)
fun ByteBuffer.toSdkBytes(): SdkBytes = SdkBytes.fromByteBuffer(this)
