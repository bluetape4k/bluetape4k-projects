package io.bluetape4k.exposed.core.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table

/**
 * `ByteArray`를 암호화해 `VARBINARY`에 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 [Encryptor.encrypt], 조회 시 [Encryptor.decrypt]를 적용합니다.
 * - 입력/출력 버퍼는 매 호출마다 새로 생성될 수 있습니다.
 * - [encryptor]는 반드시 명시적으로 전달해야 합니다. 기본 암호화 키에 의존하지 마세요.
 *
 * ```kotlin
 * val secret = table.encryptedBinary("secret", 512, Encryptors.AES)
 * // secret.columnType.sqlType().contains("VARBINARY")
 * ```
 */
@Deprecated("use io.bluetape4k.exposed.core.tink.tinkDaeadBinary in bluetape4k-exposed-tink")
fun Table.encryptedBinary(
    name: String,
    length: Int = 255,
    encryptor: Encryptor,
): Column<ByteArray> =
    registerColumn(name, EncryptedBinaryColumnType(encryptor, length))

/** `VARBINARY` + 바이트 암복호화 변환기를 결합한 컬럼 타입입니다. */
class EncryptedBinaryColumnType(
    encryptor: Encryptor,
    length: Int,
): ColumnWithTransform<ByteArray, ByteArray>(BinaryColumnType(length), ByteArrayEncryptionTransformer(encryptor))

/** 바이트 배열 암복호화 변환기입니다. */
class ByteArrayEncryptionTransformer(
    private val encryptor: Encryptor,
): ColumnTransformer<ByteArray, ByteArray> {

    /** 엔티티 바이트를 암호화해 DB 저장 값으로 변환합니다. */
    override fun unwrap(value: ByteArray): ByteArray = encryptor.encrypt(value)

    /** DB 바이트를 복호화해 엔티티 값으로 변환합니다. */
    override fun wrap(value: ByteArray): ByteArray = encryptor.decrypt(value)
}
