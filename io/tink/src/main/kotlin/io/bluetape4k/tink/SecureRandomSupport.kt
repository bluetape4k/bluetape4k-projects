package io.bluetape4k.tink

import io.bluetape4k.support.emptyByteArray
import java.security.SecureRandom

/**
 * 난수 발생 기본 알고리즘
 */
internal const val randomNumberGenerationAlgorithm = "SHA1PRNG"

/**
 * 기본 [SecureRandom] 인스턴스
 */
@JvmField
internal val secureRandom: SecureRandom = SecureRandom.getInstance(randomNumberGenerationAlgorithm)

/**
 * 지정 길이의 난수 바이트 배열을 생성합니다.
 *
 * ## 동작/계약
 * - [size]가 0 이하이면 빈 배열을 반환합니다.
 * - [size]가 양수이면 [secureRandom]으로 채운 새 [ByteArray]를 반환합니다.
 * - 호출마다 새 배열을 할당하며 입력 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val bytes = randomBytes(4)
 * // bytes.size == 4
 * ```
 *
 * @param size 생성할 바이트 배열 길이. 0 이하이면 빈 배열을 반환합니다.
 */
fun randomBytes(size: Int): ByteArray {
    if (size <= 0) {
        return emptyByteArray
    }

    return ByteArray(size).apply { secureRandom.nextBytes(this) }
}
