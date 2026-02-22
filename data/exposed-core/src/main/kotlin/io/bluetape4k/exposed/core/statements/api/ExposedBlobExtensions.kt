package io.bluetape4k.exposed.core.statements.api

import io.bluetape4k.support.toUtf8String
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.io.InputStream

/**
 * 문자열을 UTF-8 바이트 배열로 변환하여 [ExposedBlob]으로 변환합니다.
 *
 * @receiver 변환할 문자열
 * @return 변환된 [ExposedBlob] 객체
 */
fun String.toExposedBlob(): ExposedBlob = ExposedBlob(toByteArray())

/**
 * 바이트 배열을 [ExposedBlob]으로 변환합니다.
 *
 * @receiver 변환할 바이트 배열
 * @return 변환된 [ExposedBlob] 객체
 */
fun ByteArray.toExposedBlob(): ExposedBlob = ExposedBlob(this)

/**
 * [InputStream]을 [ExposedBlob]으로 변환합니다.
 *
 * @receiver 변환할 입력 스트림
 * @return 변환된 [ExposedBlob] 객체
 */
fun InputStream.toExposedBlob(): ExposedBlob = ExposedBlob(this)

/**
 * [ExposedBlob]을 UTF-8 문자열로 변환합니다.
 *
 * @receiver 변환할 [ExposedBlob]
 * @return 변환된 문자열
 */
fun ExposedBlob.toUtf8String(): String = bytes.toUtf8String()

/**
 * [ExposedBlob]을 [InputStream]으로 변환합니다.
 *
 * @receiver 변환할 [ExposedBlob]
 * @return 변환된 [InputStream]
 */
fun ExposedBlob.toInputStream(): InputStream = bytes.inputStream()

/**
 * [ExposedBlob]의 바이트 배열을 반환합니다.
 *
 * @receiver 변환할 [ExposedBlob]
 * @return 바이트 배열
 */
fun ExposedBlob.toByteArray(): ByteArray = bytes
