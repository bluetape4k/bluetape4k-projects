package io.bluetape4k.exposed.core.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.daead.TinkDaeads
import io.bluetape4k.tink.daead.TinkDeterministicAead
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * 문자열 값을 Google Tink Deterministic AEAD로 암호화해서 `VARCHAR` 컬럼에 저장하는 변환 컬럼 타입입니다.
 *
 * ## 동작/계약
 * - DB 저장 시 [StringTinkDaeadEncryptionTransformer.unwrap]로 결정적 암호화(Base64 인코딩)하고, 조회 시 `wrap`으로 복호화합니다.
 * - Deterministic AEAD(AES256-SIV)는 동일한 평문에 대해 항상 동일한 암호문을 생성하므로
 *   인덱스/조건 검색에 사용할 수 있습니다.
 * - 인덱스 검색이 불필요하고 보안이 더 중요한 경우에는 [TinkAeadVarCharColumnType]을 사용하세요.
 * - 컬럼 길이 검증은 생성 전에 호출되는 `tinkDaeadVarChar(...).requirePositiveNumber(...)` 경로에서 수행됩니다.
 *
 * ```kotlin
 * object T1: IntIdTable("searchable_table") {
 *     val email = tinkDaeadVarChar("email", 512).index()
 * }
 * val id = T1.insertAndGetId { it[email] = "user@example.com" }
 * // WHERE 절로 검색 가능
 * val count = T1.selectAll().where { T1.email eq "user@example.com" }.count()
 * // count == 1L
 * ```
 *
 * @param encryptor Tink Deterministic AEAD 암/복호화를 수행할 인스턴스입니다.
 * @param colLength 암호문(Base64 인코딩)을 저장할 컬럼 길이입니다.
 */
class TinkDaeadVarCharColumnType(
    colLength: Int,
    private val encryptor: TinkDeterministicAead,
): ColumnWithTransform<String, String>(
    VarCharColumnType(colLength),
    StringTinkDaeadEncryptionTransformer(encryptor)
)

/**
 * 문자열 컬럼의 저장/조회 경계에서 Tink Deterministic AEAD 암복호화를 수행하는 transformer입니다.
 *
 * ## 동작/계약
 * - [unwrap]은 평문 문자열을 결정적으로 암호화 후 Base64 인코딩 문자열로 변환해 DB 저장 값으로 사용합니다.
 * - [wrap]은 DB에서 읽은 Base64 암호문 문자열을 복호화해 평문 문자열로 변환합니다.
 * - 결정적(deterministic) 암호화이므로 동일 평문에 대해 항상 동일한 암호문이 생성됩니다.
 *   이 특성 덕분에 WHERE 조건절 및 인덱스 검색이 가능합니다.
 * - round-trip(`wrap(unwrap(x))`)은 원본 문자열과 동일합니다.
 *
 * ```kotlin
 * val transformer = StringTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
 * val source = "tink-daead-source"
 * val encrypted1 = transformer.unwrap(source)
 * val encrypted2 = transformer.unwrap(source)
 * // encrypted1 == encrypted2  (결정적 암호화)
 * val restored = transformer.wrap(encrypted1)
 * // restored == source
 * ```
 *
 * @param encryptor Tink Deterministic AEAD 암/복호화 인스턴스입니다.
 */
class StringTinkDaeadEncryptionTransformer(
    private val encryptor: TinkDeterministicAead = TinkDaeads.AES256_SIV,
): ColumnTransformer<String, String> {

    companion object: KLogging()

    /**
     * 평문 문자열을 Tink Deterministic AEAD로 결정적으로 암호화 후 Base64 문자열로 변환합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 [encryptor]로 결정적 암호화하고 Base64 인코딩합니다.
     * - 동일 입력에 대해 항상 동일한 암호문이 반환됩니다 (인덱스 검색 가능).
     *
     * ```kotlin
     * val transformer = StringTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
     * val e1 = transformer.unwrap("deterministic-source")
     * val e2 = transformer.unwrap("deterministic-source")
     * // e1 == e2
     * ```
     *
     * @param value 암호화할 평문 문자열입니다.
     */
    override fun unwrap(value: String): String {
        return encryptor.encryptDeterministically(value)
    }

    /**
     * Base64 암호문 문자열을 Tink Deterministic AEAD로 복호화해 평문 문자열로 변환합니다.
     *
     * ## 동작/계약
     * - 입력 [value]를 Base64 디코딩 후 [encryptor]로 복호화합니다.
     * - 암호문 형식이 아니거나 키가 맞지 않으면 Tink 예외가 전파됩니다.
     *
     * ```kotlin
     * val transformer = StringTinkDaeadEncryptionTransformer(TinkDaeads.AES256_SIV)
     * val source = "tink-daead-source"
     * val restored = transformer.wrap(transformer.unwrap(source))
     * // restored == source
     * ```
     *
     * @param value 복호화할 Base64 인코딩 암호문 문자열입니다.
     */
    override fun wrap(value: String): String {
        return encryptor.decryptDeterministically(value)
    }
}
