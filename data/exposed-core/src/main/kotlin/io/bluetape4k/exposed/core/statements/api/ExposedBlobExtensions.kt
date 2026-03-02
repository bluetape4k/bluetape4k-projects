package io.bluetape4k.exposed.core.statements.api

import io.bluetape4k.support.toUtf8String
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.io.InputStream

/**
 * 문자열을 바이트 배열로 인코딩해 [ExposedBlob]으로 변환합니다.
 *
 * ## 동작/계약
 * - `toByteArray()` 기본 charset(UTF-8)에 따라 인코딩합니다.
 * - 원본 문자열은 변경하지 않고 새 [ExposedBlob]을 반환합니다.
 *
 * ```kotlin
 * val blob = "hello".toExposedBlob()
 * // blob.bytes.size == 5
 * ```
 */
fun String.toExposedBlob(): ExposedBlob = ExposedBlob(toByteArray())

/** 바이트 배열을 [ExposedBlob]으로 감쌉니다. */
fun ByteArray.toExposedBlob(): ExposedBlob = ExposedBlob(this)

/** 입력 스트림 전체를 읽어 [ExposedBlob]으로 변환합니다. */
fun InputStream.toExposedBlob(): ExposedBlob = ExposedBlob(this)

/** blob 바이트를 UTF-8 문자열로 디코딩합니다. */
fun ExposedBlob.toUtf8String(): String = bytes.toUtf8String()

/** blob 바이트를 읽는 [InputStream]을 반환합니다. */
fun ExposedBlob.toInputStream(): InputStream = bytes.inputStream()

/** blob 바이트 배열을 반환합니다. */
fun ExposedBlob.toByteArray(): ByteArray = bytes
