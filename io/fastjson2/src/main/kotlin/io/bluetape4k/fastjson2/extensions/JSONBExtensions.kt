package io.bluetape4k.fastjson2.extensions

import com.alibaba.fastjson2.JSONB
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import com.alibaba.fastjson2.reference
import java.io.InputStream

/**
 * 객체를 JSONB(바이너리 JSON) 형식의 [ByteArray]로 직렬화합니다.
 *
 * JSONB는 Fastjson2의 바이너리 JSON 형식으로,
 * 텍스트 JSON 대비 직렬화/역직렬화 성능과 압축률이 우수합니다.
 *
 * @param context JSONB 직렬화 컨텍스트. null이면 기본 컨텍스트 사용
 * @return JSONB 직렬화된 바이트 배열. null 객체인 경우 null 반환
 */
fun Any?.toJsonBytes(context: JSONWriter.Context? = null): ByteArray? =
    JSONB.toBytes(this, context)

/**
 * JSONB 바이너리 형식의 [ByteArray]를 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
 *
 * ```kotlin
 * val user = jsonbBytes.readBytesOrNull<User>()
 * ```
 *
 * @param T 역직렬화 대상 타입
 * @param features JSONB 파싱 옵션
 * @return 역직렬화된 객체. null 바이트 배열인 경우 null 반환
 */
inline fun <reified T: Any> ByteArray?.readBytesOrNull(vararg features: JSONReader.Feature): T? =
    JSONB.parseObject(this, reference<T>(), *features)

/**
 * [InputStream]에서 JSONB 바이너리 데이터를 읽어 reified 타입 [T]의 객체로 역직렬화합니다.
 *
 * 내부적으로 스트림의 모든 바이트를 읽은 뒤 [readBytesOrNull]로 위임합니다.
 *
 * @param T 역직렬화 대상 타입
 * @param features JSONB 파싱 옵션
 * @return 역직렬화된 객체. null 스트림인 경우 null 반환
 */
inline fun <reified T: Any> InputStream?.readBytesOrNull(vararg features: JSONReader.Feature): T? =
    this?.readBytes()?.readBytesOrNull(*features)
