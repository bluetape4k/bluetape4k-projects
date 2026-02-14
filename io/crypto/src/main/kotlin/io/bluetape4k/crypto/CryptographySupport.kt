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
 * 암호화 처리에서 `registBouncCastleProvider` 함수를 제공합니다.
 */
internal fun registBouncCastleProvider() {
    lock.withLock {
        if (Security.getProvider("BC") == null) {
            runCatching { Security.addProvider(BouncyCastleProvider()) }
        }
    }
}

@JvmField
internal val urlBase64Encoder = Base64.getUrlEncoder()

@JvmField
internal val urlBase64Decoder = Base64.getUrlDecoder()
