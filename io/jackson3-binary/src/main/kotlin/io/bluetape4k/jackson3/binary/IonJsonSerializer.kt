package io.bluetape4k.jackson3.binary

import io.bluetape4k.jackson3.JacksonSerializer
import io.bluetape4k.logging.KLogging
import tools.jackson.dataformat.ion.IonObjectMapper

/**
 * 이전 Ion 직렬화기 이름을 유지하는 deprecated 래퍼입니다.
 *
 * ## 동작/계약
 * - 실제 직렬화 경로는 [IonJacksonSerializer]와 동일합니다.
 * - 신규 코드는 [IonJacksonSerializer] 사용을 권장합니다.
 * - [mapper] 기본값은 [JacksonBinary.ION.defaultMapper]입니다.
 *
 * ```kotlin
 * val serializer = IonJsonSerializer()
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 *
 * @param mapper Ion 처리를 위한 Jackson mapper입니다.
 */
@Deprecated("use IonJacksonSerializer", replaceWith = ReplaceWith("IonJacksonSerializer"))
class IonJsonSerializer(
    mapper: IonObjectMapper = JacksonBinary.ION.defaultMapper,
): JacksonSerializer(mapper) {

    companion object: KLogging()
}
