package io.bluetape4k.jackson3.crypto

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.newInstanceOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt] annotation이 적용된 필드의 암호화된 값을 JSON 역직렬화 시에 복호화를 수행하는 [Encryptor]를 관리합니다.
 *
 * @see [JsonEncrypt]
 */
object JsonEncryptors: KLogging() {

    private val encryptors = ConcurrentHashMap<KClass<*>, Encryptor>()

    /**
     * [encryptorType]에 해당하는 [Encryptor]를 반환합니다.
     */
    fun getEncryptor(encryptorType: KClass<out Encryptor>): Encryptor {
        return encryptors.getOrPut(encryptorType) {
            encryptorType.newInstanceOrNull()!!
        }
    }
}
