package io.bluetape4k.jackson3.crypto

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.jackson3.crypto.JsonEncryptors.getEncryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.newInstanceOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt]에서 사용하는 [Encryptor] 인스턴스를 타입별로 캐시하는 레지스트리입니다.
 *
 * ## 동작/계약
 * - [getEncryptor]는 타입별 단일 인스턴스를 캐시에 저장합니다.
 * - 기본 생성자 생성에 실패하면 예외가 전파됩니다.
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
     * - 캐시에 없으면 새 인스턴스를 생성해 저장합니다.
     * - 동시 접근은 ConcurrentHashMap 경로로 처리됩니다.
     */
    fun getEncryptor(encryptorType: KClass<out Encryptor>): Encryptor {
        return encryptors.getOrPut(encryptorType) {
            requireNotNull(encryptorType.newInstanceOrNull()) {
                "Encryptor 인스턴스 생성에 실패했습니다. 기본 생성자가 있는지 확인하세요. type=${encryptorType.qualifiedName}"
            }
        }
    }
}
