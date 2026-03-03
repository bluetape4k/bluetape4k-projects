package io.bluetape4k.exposed.core.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * 문자열 값을 암호화해 `VARCHAR`에 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 [Encryptor.encrypt], 조회 시 [Encryptor.decrypt]를 사용합니다.
 * - 길이는 평문이 아니라 암호문 길이 기준으로 충분히 크게 설정해야 합니다.
 * - [encryptor]는 반드시 명시적으로 전달해야 합니다. 기본 암호화 키에 의존하지 마세요.
 *
 * ```kotlin
 * val secret = table.encryptedVarChar("secret", colLength = 512, encryptor = Encryptors.AES)
 * // secret.columnType.sqlType().contains("VARCHAR")
 * ```
 */
fun Table.encryptedVarChar(
    name: String,
    colLength: Int = 255,
    encryptor: Encryptor,
): Column<String> =
    registerColumn(name, EncryptedVarCharColumnType(encryptor, colLength))

/** `VARCHAR` + 문자열 암복호화 변환기를 결합한 컬럼 타입입니다. */
class EncryptedVarCharColumnType(
    encryptor: Encryptor,
    colLength: Int,
): ColumnWithTransform<String, String>(VarCharColumnType(colLength), StringEncryptionTransformer(encryptor))

/** 문자열 암복호화 변환기입니다. */
class StringEncryptionTransformer(private val encryptor: Encryptor): ColumnTransformer<String, String> {
    /** 엔티티 문자열을 암호화해 DB 저장 문자열로 변환합니다. */
    override fun unwrap(value: String): String = encryptor.encrypt(value)

    /** DB 문자열을 복호화해 엔티티 문자열로 변환합니다. */
    override fun wrap(value: String): String = encryptor.decrypt(value)
}
