package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.dataformat.ion.IonObjectMapper
import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Amazon Ion 포맷을 사용하는 Jackson 기반 바이너리 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonBinary.ION.defaultMapper]입니다.
 * - 직렬화/역직렬화는 [JacksonSerializer] 구현 경로를 따릅니다.
 * - 객체 변환 결과는 새 인스턴스로 생성됩니다.
 *
 * ```kotlin
 * val serializer = IonJacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper Ion 처리에 사용할 Jackson mapper입니다.
 */
class IonJacksonSerializer(
    mapper: IonObjectMapper = JacksonBinary.ION.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
