package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * 문자열 값을 암호화하여 `VARCHAR` 컬럼에 저장하는 컬럼 타입입니다.
 *
 * @param encryptor 문자열 암/복호화를 수행할 암호화기
 * @param colLength 암호문을 저장할 컬럼 길이 (0보다 커야 함)
 */
class JasyptVarCharColumnType(
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
    colLength: Int,
): ColumnWithTransform<String, String>(
    VarCharColumnType(colLength.requirePositiveNumber("colLength")),
    JasyptStringEncryptionTransformer(encryptor)
)

/**
 * 문자열 값의 DB 저장/조회 시 암호화 및 복호화를 수행합니다.
 */
class JasyptStringEncryptionTransformer(
    private val encryptor: io.bluetape4k.crypto.encrypt.Encryptor,
): ColumnTransformer<String, String> {
    /**
     * Encrypts the given value using the provided [encryptor].
     *
     * @param value The value to encrypt.
     * @return The encrypted value.
     */
    override fun unwrap(value: String) = encryptor.encrypt(value)

    /**
     * Decrypts the given value using the provided [encryptor].
     */
    override fun wrap(value: String) = encryptor.decrypt(value)
}
