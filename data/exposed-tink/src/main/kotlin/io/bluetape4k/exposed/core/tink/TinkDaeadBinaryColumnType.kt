package io.bluetape4k.exposed.core.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.daead.TinkDeterministicAead
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform

/**
 * 바이트 배열 값을 Google Tink Deterministic AEAD로 암호화해서 `VARBINARY` 컬럼에 저장하는 변환 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [ByteArrayTinkDaeadEncryptionTransformer.unwrap]로 결정적 암호화하고, 조회 시 `wrap`으로 복호화합니다.
 * - Deterministic AEAD(AES256-SIV)는 동일한 평문에 대해 항상 동일한 암호문을 생성하므로
 *   인덱스/조건 검색에 사용할 수 있습니다.
 * - 인덱스 검색이 불필요하고 보안이 더 중요한 경우에는 [TinkAeadBinaryColumnType]을 사용하세요.
 * - 컬럼 길이 검증은 생성 전에 호출되는 `tinkDaeadBinary(...).requirePositiveNumber(...)` 경로에서 수행됩니다.
 *
 * ```kotlin
 * object T1: IntIdTable("searchable_binary_table") {
 *     val fingerprint = tinkDaeadBinary("fingerprint", 128)
 * }
 * val data = "fingerprint-data".toByteArray()
 * val id = T1.insertAndGetId { it[fingerprint] = data }
 * val count = T1.selectAll().where { T1.fingerprint eq data }.count()
 * // count == 1L
 * ```
 *
 * @param encryptor Tink Deterministic AEAD 암/복호화를 수행할 인스턴스입니다.
 * @param length 암호문 바이트를 저장할 컬럼 길이입니다.
 */
class TinkDaeadBinaryColumnType(
    length: Int,
    private val encryptor: TinkDeterministicAead,
): ColumnWithTransform<ByteArray, ByteArray>(
    BinaryColumnType(length),
    ByteArrayTinkDaeadEncryptionTransformer(encryptor)
)

/**
 * 바이너리 컬럼의 저장/조회 경계에서 Tink Deterministic AEAD 암복호화를 수행하는 transformer입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 바이트 배열을 결정적으로 암호화해 DB 저장 값으로 사용합니다.
 * - [wrap]은 DB에서 읽은 암호문 바이트 배열을 복호화해 원본 바이트 배열로 변환합니다.
 * - 결정적(deterministic) 암호화이므로 동일 평문에 대해 항상 동일한 암호문이 생성됩니다.
 * - round-trip(`wrap(unwrap(x))`)은 원본 바이트 배열과 동일합니다.
 *
 * ```kotlin
 * val transformer = ByteArrayTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
 * val source = "tink-daead-binary-source".toByteArray()
 * val e1 = transformer.unwrap(source)
 * val e2 = transformer.unwrap(source)
 * // e1.contentEquals(e2) == true (결정적 암호화)
 * val restored = transformer.wrap(e1)
 * // restored.contentEquals(source) == true
 * ```
 *
 * @param encryptor Tink Deterministic AEAD 암/복호화 인스턴스입니다.
 */
class ByteArrayTinkDaeadEncryptionTransformer(
    private val encryptor: TinkDeterministicAead,
): ColumnTransformer<ByteArray, ByteArray> {

    companion object: KLogging()

    /**
     * 평문 바이트 배열을 Tink Deterministic AEAD로 결정적으로 암호화합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 [encryptor]로 결정적 암호화합니다.
     * - 동일 입력에 대해 항상 동일한 암호문이 반환됩니다 (인덱스 검색 가능).
     *
     * @param value 암호화할 평문 바이트 배열입니다.
     */
    override fun unwrap(value: ByteArray): ByteArray {
        return encryptor.encryptDeterministically(value)
    }

    /**
     * 암호문 바이트 배열을 Tink Deterministic AEAD로 복호화합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 [encryptor]로 복호화합니다.
     * - 암호문 형식이 아니거나 키가 맞지 않으면 Tink 예외가 전파됩니다.
     *
     * @param value 복호화할 암호문 바이트 배열입니다.
     */
    override fun wrap(value: ByteArray): ByteArray {
        return encryptor.decryptDeterministically(value)
    }
}
