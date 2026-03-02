package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.ion.IonObjectMapper

/**
 * Amazon Ion 포맷을 사용하는 Jackson 3 기반 바이너리 직렬화기입니다.
 *
 * ## 동작/계약
 * - [mapper] 기본값은 [JacksonBinary.ION.defaultMapper]입니다.
 * - 직렬화/역직렬화 동작은 상위 [JacksonSerializer] 구현을 따릅니다.
 * - 입력 객체를 mutate 하지 않습니다.
 *
 * ```kotlin
 * val serializer = IonJacksonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper Ion 처리를 위한 Jackson mapper입니다.
 */
class IonJacksonSerializer(
    mapper: IonObjectMapper = JacksonBinary.ION.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
