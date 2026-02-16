package io.bluetape4k.crypto

import io.bluetape4k.support.emptyByteArray
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jasypt.salt.ZeroSaltGenerator
import java.security.SecureRandom
import java.security.Security
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
internal val secureRandom: Random = SecureRandom.getInstance(randomNumberGenerationAlgorithm)

/**
 * Random 값을 가지는 [ByteArray]를 빌드합니다.
 *
 * ```
 * val randomBytes = randomBytes(16)
 * ```
 *
 * @param size byte array 크기 (0보다 작으면 empty byte array를 반환)
 * @return random 값을 가지는 byte array
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
