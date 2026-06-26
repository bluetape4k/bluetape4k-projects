package io.bluetape4k.hibernate.converters

import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.daead.TinkDeterministicAead
import io.bluetape4k.tink.encrypt.TinkAeadEncryptor
import io.bluetape4k.tink.encrypt.TinkDaeadEncryptor
import io.bluetape4k.tink.keyset.keysetHandleOf
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.concurrent.atomic.AtomicReference

/**
 * Registry for persistent key material used by Hibernate encrypted string converters.
 *
 * Hibernate creates [AttributeConverter] instances with no dependency injection by default, so the built-in
 * converters resolve their encryptors from this registry at conversion time. Applications must load key material
 * from an external protected store, such as a secret manager, KMS envelope-encrypted file, or HSM-backed source,
 * before encrypted entity fields are read or written.
 *
 * The JSON keyset helpers expect cleartext Tink keyset JSON. Protect that JSON as secret key material and avoid
 * embedding it in source code, logs, or plain configuration files.
 */
object EncryptedStringConverterKeysets {

    private val aesEncryptor = AtomicReference<TinkEncryptor?>()
    private val deterministicEncryptor = AtomicReference<TinkEncryptor?>()

    internal fun configureAes(encryptor: TinkEncryptor) {
        aesEncryptor.set(encryptor)
    }

    /**
     * Configures [AESStringConverter] from externally persisted Tink keyset JSON.
     */
    @JvmStatic
    fun configureAesKeyset(jsonKeyset: String) {
        configureAes(TinkAeadEncryptor(TinkAead(keysetHandleOf(jsonKeyset))))
    }

    internal fun configureDeterministic(encryptor: TinkEncryptor) {
        deterministicEncryptor.set(encryptor)
    }

    /**
     * Configures [DeterministicAESStringConverter] from externally persisted Tink keyset JSON.
     */
    @JvmStatic
    fun configureDeterministicKeyset(jsonKeyset: String) {
        configureDeterministic(TinkDaeadEncryptor(TinkDeterministicAead(keysetHandleOf(jsonKeyset))))
    }

    internal fun requireAesEncryptor(): TinkEncryptor =
        aesEncryptor.get() ?: error(
            "AESStringConverter requires externally persisted key material. " +
                "Call EncryptedStringConverterKeysets.configureAesKeyset(...) during application bootstrap."
        )

    internal fun requireDeterministicEncryptor(): TinkEncryptor =
        deterministicEncryptor.get() ?: error(
            "DeterministicAESStringConverter requires externally persisted key material. " +
                "Call EncryptedStringConverterKeysets.configureDeterministicKeyset(...) during application bootstrap."
        )

    internal fun resetForTesting() {
        aesEncryptor.set(null)
        deterministicEncryptor.set(null)
    }
}

/**
 * Base JPA converter that stores encrypted strings in a single database column.
 *
 * Built-in subclasses fail fast unless [EncryptedStringConverterKeysets] has been configured with externally
 * persisted key material. This avoids writing ciphertext with process-local generated keys that cannot be decrypted
 * after restart or by another application instance.
 *
 * ```kotlin
 * @Entity
 * class User {
 *      @Id
 *      @GeneratedValue
 *      var id:Long? = null
 *
 *      @Convert(converter=AESStringConverter::class)
 *      var password: String? = null
 * }
 * ```
 */
@Converter
abstract class EncryptedStringConverter protected constructor(
    private val encryptorProvider: () -> TinkEncryptor,
): AttributeConverter<String?, String?> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.run { encryptorProvider().encrypt(this) }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.run { encryptorProvider().decrypt(this) }
    }
}

/**
 * Encrypts a string column with AES-256-GCM.
 *
 * This converter is non-deterministic: the same plaintext produces different ciphertext values. Configure
 * [EncryptedStringConverterKeysets] with persisted AES key material before converting non-null values.
 */
@Converter
class AESStringConverter private constructor(
    encryptorProvider: () -> TinkEncryptor,
): EncryptedStringConverter(encryptorProvider) {

    constructor(): this(EncryptedStringConverterKeysets::requireAesEncryptor)
}

/**
 * Encrypts a string column with deterministic AES-256-SIV.
 *
 * This converter is deterministic: the same plaintext produces the same ciphertext for lookup support. Configure
 * [EncryptedStringConverterKeysets] with persisted deterministic key material before converting non-null values.
 */
@Converter
class DeterministicAESStringConverter private constructor(
    encryptorProvider: () -> TinkEncryptor,
): EncryptedStringConverter(encryptorProvider) {

    constructor(): this(EncryptedStringConverterKeysets::requireDeterministicEncryptor)
}
