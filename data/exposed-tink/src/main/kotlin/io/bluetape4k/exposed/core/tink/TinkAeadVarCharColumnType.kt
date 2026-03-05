package io.bluetape4k.exposed.core.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.aead.TinkAeads
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * 문자열 값을 Google Tink AEAD로 암호화해서 `VARCHAR` 컬럼에 저장하는 변환 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [StringTinkAeadEncryptionTransformer.unwrap]로 암호화(Base64 인코딩)하고, 조회 시 `wrap`으로 복호화합니다.
 * - AEAD(Authenticated Encryption with Associated Data)는 매번 다른 nonce를 사용하므로
 *   같은 평문이라도 암호화 결과가 달라집니다. 따라서 인덱스/조건 검색에는 사용할 수 없습니다.
 * - 인덱스/조건 검색이 필요한 경우에는 [TinkDaeadVarCharColumnType]을 사용하세요.
 * - 컬럼 길이 검증은 생성 전에 호출되는 `tinkAeadVarChar(...).requirePositiveNumber(...)` 경로에서 수행됩니다.
 *
 * ```kotlin
 * object T1: IntIdTable("secret_table") {
 *     val secret = tinkAeadVarChar("secret", 512)
 * }
 * val id = T1.insertAndGetId { it[secret] = "민감한 데이터" }
 * val row = T1.selectAll().where { T1.id eq id }.single()
 * // row[T1.secret] == "민감한 데이터"
 * ```
 *
 * @param encryptor Tink AEAD 암/복호화를 수행할 인스턴스입니다.
 * @param colLength 암호문(Base64 인코딩)을 저장할 컬럼 길이입니다.
 */
class TinkAeadVarCharColumnType(
    private val encryptor: TinkAead,
    colLength: Int,
): ColumnWithTransform<String, String>(
    VarCharColumnType(colLength),
    StringTinkAeadEncryptionTransformer(encryptor)
)

/**
 * 문자열 컬럼의 저장/조회 경계에서 Tink AEAD 암복호화를 수행하는 transformer입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 문자열을 암호화 후 Base64 인코딩 문자열로 변환해 DB 저장 값으로 사용합니다.
 * - [wrap]은 DB에서 읽은 Base64 인코딩 암호문 문자열을 복호화해 평문 문자열로 변환합니다.
 * - AEAD는 비결정적(non-deterministic)이므로, 동일 평문에 대해 매번 다른 암호문이 생성됩니다.
 * - round-trip(`wrap(unwrap(x))`)은 원본 문자열과 동일합니다.
 *
 * ```kotlin
 * val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
 * val source = "tink-aead-source"
 * val restored = transformer.wrap(transformer.unwrap(source))
 * // restored == source
 * ```
 *
 * @param encryptor Tink AEAD 암/복호화 인스턴스입니다.
 */
class StringTinkAeadEncryptionTransformer(
    private val encryptor: TinkAead = TinkAeads.AES256_GCM,
): ColumnTransformer<String, String> {

    companion object: KLogging()

    /**
     * 평문 문자열을 Tink AEAD로 암호화 후 Base64 문자열로 변환합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 [encryptor]로 암호화하고 Base64 인코딩합니다.
     * - 비결정적 암호화이므로 동일 입력에 대해 매번 다른 결과가 반환됩니다.
     *
     * ```kotlin
     * val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
     * val encrypted = transformer.unwrap("tink-aead-source")
     * // encrypted != "tink-aead-source"
     * ```
     *
     * @param value 암호화할 평문 문자열입니다.
     */
    override fun unwrap(value: String): String {
        return encryptor.encrypt(value)
    }

    /**
     * Base64 암호문 문자열을 Tink AEAD로 복호화해 평문 문자열로 변환합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 Base64 디코딩 후 [encryptor]로 복호화합니다.
     * - 암호문 형식이 아니거나 키가 맞지 않으면 Tink 예외가 전파됩니다.
     *
     * ```kotlin
     * val transformer = StringTinkAeadEncryptionTransformer(TinkAeads.AES256_GCM)
     * val source = "tink-aead-source"
     * val restored = transformer.wrap(transformer.unwrap(source))
     * // restored == source
     * ```
     *
     * @param value 복호화할 Base64 인코딩 암호문 문자열입니다.
     */
    override fun wrap(value: String): String {
        return encryptor.decrypt(value)
    }
}
