package io.bluetape4k.aws.core

import software.amazon.awssdk.core.SdkBytes
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * [ByteArray]를 복사 기반 [SdkBytes]로 변환합니다.
 *
 * ## 동작/계약
 * - [SdkBytes.fromByteArray]를 호출해 입력 배열을 복사한다.
 * - 원본 배열을 이후 변경해도 생성된 [SdkBytes] 값은 유지된다.
 *
 * ```kotlin
 * val source = byteArrayOf(1, 2, 3)
 * val sdkBytes = source.toSdkBytes()
 * source[0] = 9
 * // sdkBytes.asByteArray()[0] == 1
 * ```
 */
fun ByteArray.toSdkBytes(): SdkBytes = SdkBytes.fromByteArray(this)

/**
 * [ByteArray]를 무복사 방식 [SdkBytes]로 변환합니다.
 *
 * ## 동작/계약
 * - [SdkBytes.fromByteArrayUnsafe]를 호출해 배열 참조를 그대로 사용한다.
 * - 원본 배열을 변경하면 [SdkBytes]가 노출하는 바이트도 함께 바뀔 수 있다.
 *
 * ```kotlin
 * val source = byteArrayOf(1, 2, 3)
 * val sdkBytes = source.toSdkBytesUnsafe()
 * source[0] = 9
 * // sdkBytes.asByteArrayUnsafe()[0] == 9
 * ```
 */
fun ByteArray.toSdkBytesUnsafe(): SdkBytes = SdkBytes.fromByteArrayUnsafe(this)

/**
 * 문자열을 지정한 문자셋으로 인코딩한 [SdkBytes]로 변환합니다.
 *
 * ## 동작/계약
 * - 기본 문자셋은 [Charsets.UTF_8]이다.
 * - [SdkBytes.fromString] 호출 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val sdkBytes = "가나다".toSdkBytes()
 * // sdkBytes.asUtf8String() == "가나다"
 * ```
 */
fun String.toSdkBytes(cs: Charset = Charsets.UTF_8): SdkBytes = SdkBytes.fromString(this, cs)

/**
 * 문자열을 UTF-8 기반 [SdkBytes]로 변환합니다.
 *
 * ## 동작/계약
 * - [SdkBytes.fromUtf8String]을 사용해 UTF-8 인코딩을 고정한다.
 * - UTF-8 문자열 역직렬화 시 원문과 동일한 값을 얻는다.
 *
 * ```kotlin
 * val sdkBytes = "hello".toUtf8SdkBytes()
 * // sdkBytes.asUtf8String() == "hello"
 * ```
 */
fun String.toUtf8SdkBytes(): SdkBytes = SdkBytes.fromUtf8String(this)

/**
 * [InputStream] 전체 내용을 읽어 [SdkBytes]로 변환합니다.
 *
 * ## 동작/계약
 * - [SdkBytes.fromInputStream]를 호출해 스트림 내용을 끝까지 읽는다.
 * - 생성된 [SdkBytes]는 읽은 시점의 바이트 스냅샷을 가진다.
 *
 * ```kotlin
 * val input = "abc".byteInputStream()
 * val sdkBytes = input.toSdkBytes()
 * // sdkBytes.asUtf8String() == "abc"
 * ```
 */
fun InputStream.toSdkBytes(): SdkBytes = SdkBytes.fromInputStream(this)

/**
 * [ByteBuffer]의 현재 상태를 기반으로 [SdkBytes]를 생성합니다.
 *
 * ## 동작/계약
 * - [SdkBytes.fromByteBuffer]를 호출해 버퍼의 `position..limit` 구간을 읽는다.
 * - 입력 버퍼를 직접 전달하므로 호출 전 `position/limit` 설정이 결과를 결정한다.
 *
 * ```kotlin
 * val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4)).apply { position(1) }
 * val sdkBytes = buffer.toSdkBytes()
 * // sdkBytes.asByteArray().contentEquals(byteArrayOf(2, 3, 4)) == true
 * ```
 */
fun ByteBuffer.toSdkBytes(): SdkBytes = SdkBytes.fromByteBuffer(this)
