package io.bluetape4k.idgenerators.hashids

import io.bluetape4k.support.assertNotBlank
import io.bluetape4k.support.toLongArray
import io.bluetape4k.support.toUUID
import java.util.*

/**
 * [UUID]를 HashIds 로 인코딩합니다.
 *
 * @param uuid 인코딩할 UUID
 * @return HashIds 로 인코딩된 문자열
 */
fun Hashids.encodeUUID(uuid: UUID): String = encode(*uuid.toLongArray())

/**
 * HashIds 로 인코딩된 문자열을 [UUID]로 디코딩합니다.
 *
 * @param hash 디코딩할 HashIds 문자열
 * @return 디코딩된 UUID
 */
fun Hashids.decodeUUID(hash: String): UUID {
    hash.assertNotBlank("hash")
    return decode(hash).toUUID()
}
