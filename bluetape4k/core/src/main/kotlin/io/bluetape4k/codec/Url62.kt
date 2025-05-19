package io.bluetape4k.codec

import io.bluetape4k.support.toBigInt
import io.bluetape4k.support.toUuid
import java.util.*

/**
 * [Base62] 를 이용하여 [UUID]를 문자열로 encoding/decoding 합니다.
 *
 * ```
 * val uuid = UUID.randomUUID()      // uuid=24738134-9d88-6645-4ec8-d63aa2031015
 * val encoded = Url62.encode(uuid)  // encoded=16mVan3wbAXR6tQwIbfS5d
 * ```
 */
object Url62 {

    /**
     * Base62 알고리즘을 이용하여 encoding 합니다.
     *
     * ```
     * val uuid = UUID.randomUUID()      // uuid=24738134-9d88-6645-4ec8-d63aa2031015
     * val encoded = Url62.encode(uuid)  // 16mVan3wbAXR6tQwIbfS5d
     * ```
     *
     * @param uuid 인코딩한 uuid 값
     * @return  Base62로 인코딩된 문자열
     */
    fun encode(uuid: UUID): String = Base62.encode(uuid.toBigInt())

    /**
     * Base62로 인코딩된 문자열을 디코딩하여 UUID로 변환합니다
     *
     * ```
     * val encoded = "16mVan3wbAXR6tQwIbfS5d"
     * val decoded = Url62.decode(encoded)  // 24738134-9d88-6645-4ec8-d63aa2031015
     * ```
     *
     * @param encoded 디코딩할 Base62 문자열
     * @return Base62 디코딩된 UUID
     */
    fun decode(encoded: String): UUID = Base62.decode(encoded).toUuid()
}

/**
 * [UUID] 를 Base62로 인코딩합니다.
 *
 * ```
 * val uuid = UUID.randomUUID()      // uuid=24738134-9d88-6645-4ec8-d63aa2031015
 * val encoded = uuid.encodeUrl62()  // encoded=16mVan3wbAXR6tQwIbfS5d
 * ```
 */
fun UUID.encodeUrl62(): String = Url62.encode(this)

/**
 * Base62로 인코딩된 문자열을 [UUID]로 디코딩합니다.
 *
 * ```
 * val encoded = "16mVan3wbAXR6tQwIbfS5d"
 * val decoded = encoded.decodeUrl62()  // 24738134-9d88-6645-4ec8-d63aa2031015
 * ```
 */
fun String.decodeUrl62(): UUID = Url62.decode(this)
