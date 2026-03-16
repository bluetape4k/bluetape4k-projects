package io.bluetape4k.jackson.crypto

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.jackson.crypto.JsonEncryptors.getEncryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.newInstanceOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt]에서 사용하는 [Encryptor] 인스턴스를 타입별로 캐시하는 레지스트리입니다.
 *
 * ## 동작/계약
 * - [getEncryptor]는 [encryptorType]별 단일 인스턴스를 캐시에 보관합니다.
 * - 인스턴스는 기본 생성자로 생성되며 실패 시 예외가 발생할 수 있습니다.
 * - 동시 접근은 [ConcurrentHashMap] 기반으로 처리됩니다.
 *
 * ```kotlin
 * val encryptor = JsonEncryptors.getEncryptor(io.bluetape4k.crypto.encrypt.AES::class)
 * // encryptor.algorithm.isNotBlank() == true
 * ```
 *
 * @see [JsonEncrypt]
 */
object JsonEncryptors: KLogging() {

    private val encryptors = ConcurrentHashMap<KClass<*>, Encryptor>()

    /**
     * 지정 타입의 [Encryptor]를 반환합니다.
     *
     * ## 동작/계약
     * - 캐시에 없으면 기본 생성자로 생성 후 저장합니다.
     * - 생성 실패 시 예외가 전파됩니다.
     *
     * ```kotlin
     * val encryptor = JsonEncryptors.getEncryptor(io.bluetape4k.crypto.encrypt.AES::class)
     * // encryptor::class == io.bluetape4k.crypto.encrypt.AES::class
     * ```
     * @param encryptorType Encryptor 구현 타입
     */
    fun getEncryptor(encryptorType: KClass<out Encryptor>): Encryptor {
        return encryptors.getOrPut(encryptorType) {
            requireNotNull(encryptorType.newInstanceOrNull()) {
                "Encryptor 인스턴스 생성에 실패했습니다. 기본 생성자가 있는지 확인하세요. type=${encryptorType.qualifiedName}"
            }
        }
    }
}
