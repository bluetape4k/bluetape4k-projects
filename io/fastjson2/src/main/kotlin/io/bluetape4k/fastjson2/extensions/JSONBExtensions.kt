package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reference
import java.io.InputStream

/**
 * 객체를 JSONB 바이트 배열로 직렬화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `JSONB.toBytes(this, context)`를 호출합니다.
 * - 수신 객체가 null이면 null 바이트 배열을 반환합니다.
 * - 직렬화 옵션은 [context] 설정을 따릅니다.
 *
 * ```kotlin
 * val bytes = mapOf("id" to 1).toJsonBytes()
 * // bytes?.isNotEmpty() == true
 * ```
 */
fun Any?.toJsonBytes(context: JSONWriter.Context? = null): ByteArray? =
    JSONB.toBytes(this, context)

/**
 * JSONB 바이트 배열을 reified 타입 객체로 역직렬화합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `JSONB.parseObject(this, reference<T>(), *features)`를 호출합니다.
 * - 수신 바이트 배열이 null이면 null을 반환합니다.
 * - 타입 불일치/파싱 실패는 fastjson2 예외로 전파될 수 있습니다.
 *
 * ```kotlin
 * val bytes = listOf(1, 2, 3).toJsonBytes()
 * val value = bytes.readBytesOrNull<List<Int>>()
 * // value == listOf(1, 2, 3)
 * ```
 */
inline fun <reified T: Any> ByteArray?.readBytesOrNull(vararg features: JSONReader.Feature): T? =
    JSONB.parseObject(this, reference<T>(), *features)

/**
 * 입력 스트림의 JSONB 데이터를 reified 타입 객체로 역직렬화합니다.
 *
 * ## 동작/계약
 * - 스트림 전체를 `readBytes()`로 읽은 뒤 [readBytesOrNull]에 위임합니다.
 * - 수신 스트림이 null이면 null을 반환합니다.
 * - 대용량 스트림은 전체 바이트를 메모리에 적재합니다.
 *
 * ```kotlin
 * val bytes = listOf("a", "b").toJsonBytes()!!
 * val value = bytes.inputStream().readBytesOrNull<List<String>>()
 * // value == listOf("a", "b")
 * ```
 */
inline fun <reified T: Any> InputStream?.readBytesOrNull(vararg features: JSONReader.Feature): T? =
    this?.readBytes()?.readBytesOrNull(*features)
