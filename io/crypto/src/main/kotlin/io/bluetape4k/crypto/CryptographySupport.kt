package io.bluetape4k.crypto

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.error
import io.bluetape4k.support.emptyByteArray
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jasypt.salt.ZeroSaltGenerator
import java.security.SecureRandom
import java.security.Security
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

/**
 * 난수 발생 기본 알고리즘
 */
internal const val randomNumberGenerationAlgorithm = "SHA1PRNG"

/**
 * [ZeroSaltGenerator] 인스턴스
 */
@JvmField
internal val zeroSaltGenerator = ZeroSaltGenerator()

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

private val lock = ReentrantLock()

/**
 * BouncyCastle 보안 프로바이더를 JVM에 등록합니다.
 *
 * 이미 등록되어 있으면 중복 등록하지 않으며, 스레드 안전하게 동작합니다.
 * Jasypt 및 JCA 기반 암호화 연산 수행 전에 호출해야 합니다.
 */
internal fun registerBouncyCastleProvider() {
    lock.withLock {
        if (Security.getProvider("BC") == null) {
            runCatching { Security.addProvider(BouncyCastleProvider()) }
                .onFailure { e ->
                    log.error(e) { "BouncyCastle 프로바이더 등록에 실패했습니다." }
                }
        }
    }
}

/**
 * [registerBouncyCastleProvider]의 이전 이름입니다.
 * @see registerBouncyCastleProvider
 */
@Deprecated(
    message = "오타 수정. registerBouncyCastleProvider()를 사용하세요.",
    replaceWith = ReplaceWith("registerBouncyCastleProvider()"),
)
internal fun registBouncCastleProvider() {
    registerBouncyCastleProvider()
}

@JvmField
internal val urlBase64Encoder = Base64.getUrlEncoder()

@JvmField
internal val urlBase64Decoder = Base64.getUrlDecoder()
