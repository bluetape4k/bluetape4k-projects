package io.bluetape4k.exposed.core.tink

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.aead.TinkAeads
import io.bluetape4k.tink.daead.TinkDaeads
import io.bluetape4k.tink.daead.TinkDeterministicAead
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/**
 * Google Tink AEAD(비결정적 암호화)로 암호화된 문자열을 저장하기 위해 [name]의 `VARCHAR` 컬럼을 생성합니다.
 *
 * AEAD는 매번 다른 nonce를 사용하므로 같은 평문이라도 암호화 결과가 달라집니다.
 * 따라서 이 컬럼은 인덱스/조건 검색에 사용할 수 없습니다.
 * 인덱스/조건 검색이 필요한 경우에는 [tinkDaeadVarChar]를 사용하세요.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [TinkAeadVarCharColumnType]이며 DB에는 Base64 암호문이 저장되고 조회 시 평문으로 복원됩니다.
 * - `nullable()` 조합을 사용할 수 있습니다.
 * - [name]이 blank면 `IllegalArgumentException`, [cipherTextLength]가 0 이하이면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * object T1: IntIdTable("secret_table") {
 *     val secret = tinkAeadVarChar("secret", 512).nullable()
 * }
 * val id = T1.insertAndGetId { it[secret] = "민감한 데이터" }
 * val row = T1.selectAll().where { T1.id eq id }.single()
 * // row[T1.secret] == "민감한 데이터"
 * ```
 *
 * @param name 컬럼명입니다. blank 문자열은 허용되지 않습니다.
 * @param cipherTextLength 암호문(Base64 인코딩)을 저장할 컬럼 길이입니다. 0보다 커야 합니다.
 * @param encryptor 사용할 Tink AEAD 암/복호화 인스턴스입니다.
 */
fun Table.tinkAeadVarChar(
    name: String,
    cipherTextLength: Int = 255,
    encryptor: TinkAead = TinkAeads.AES256_GCM,
): Column<String> =
    registerColumn(
        name.requireNotBlank("name"),
        TinkAeadVarCharColumnType(
            encryptor = encryptor,
            colLength = cipherTextLength.requirePositiveNumber("cipherTextLength")
        )
    )

/**
 * Google Tink AEAD(비결정적 암호화)로 암호화된 바이트 배열을 저장하기 위해 [name]의 `Binary` 컬럼을 생성합니다.
 *
 * AEAD는 매번 다른 nonce를 사용하므로 같은 평문이라도 암호화 결과가 달라집니다.
 * 따라서 이 컬럼은 인덱스/조건 검색에 사용할 수 없습니다.
 * 인덱스/조건 검색이 필요한 경우에는 [tinkDaeadBinary]를 사용하세요.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [TinkAeadBinaryColumnType]이며 DB 저장 시 암호문 바이트, 조회 시 복호화된 바이트를 제공합니다.
 * - [name]이 blank면 `IllegalArgumentException`, [cipherByteLength]가 0 이하이면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * object T1: IntIdTable("binary_secret_table") {
 *     val data = tinkAeadBinary("data", 512).nullable()
 * }
 * val id = T1.insertAndGetId { it[data] = "민감한 데이터".toByteArray() }
 * val row = T1.selectAll().where { T1.id eq id }.single()
 * // row[T1.data]!!.toString(Charsets.UTF_8) == "민감한 데이터"
 * ```
 *
 * @param name 컬럼명입니다. blank 문자열은 허용되지 않습니다.
 * @param cipherByteLength 암호문 바이트를 저장할 컬럼 길이입니다. 0보다 커야 합니다.
 * @param encryptor 사용할 Tink AEAD 암/복호화 인스턴스입니다.
 */
fun Table.tinkAeadBinary(
    name: String,
    cipherByteLength: Int,
    encryptor: TinkAead = TinkAeads.AES256_GCM,
): Column<ByteArray> =
    registerColumn(
        name.requireNotBlank("name"),
        TinkAeadBinaryColumnType(
            encryptor = encryptor,
            length = cipherByteLength.requirePositiveNumber("cipherByteLength")
        )
    )

fun Table.tinkAeadBlob(
    name: String,
    encryptor: TinkAead = TinkAeads.AES256_GCM,
): Column<ByteArray> =
    registerColumn(
        name.requireNotBlank("name"),
        TinkAeadBlobColumnType(encryptor = encryptor)
    )

/**
 * Google Tink Deterministic AEAD(결정적 암호화)로 암호화된 문자열을 저장하기 위해 [name]의 `VARCHAR` 컬럼을 생성합니다.
 *
 * Deterministic AEAD(AES256-SIV)는 동일한 평문에 대해 항상 동일한 암호문을 생성하므로
 * 인덱스 및 조건절(`WHERE col = value`)로 검색이 가능합니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [TinkDaeadVarCharColumnType]이며 DB에는 Base64 암호문이 저장되고 조회 시 평문으로 복원됩니다.
 * - `nullable()`/`index()` 조합을 사용할 수 있고, `where { col eq value }` 검색이 동작합니다.
 * - [name]이 blank면 `IllegalArgumentException`, [cipherTextLength]가 0 이하이면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * object T1: IntIdTable("searchable_table") {
 *     val email = tinkDaeadVarChar("email", 512).index()
 * }
 * val id = T1.insertAndGetId { it[email] = "user@example.com" }
 * val count = T1.selectAll().where { T1.email eq "user@example.com" }.count()
 * // count == 1L
 * ```
 *
 * @param name 컬럼명입니다. blank 문자열은 허용되지 않습니다.
 * @param cipherTextLength 암호문(Base64 인코딩)을 저장할 컬럼 길이입니다. 0보다 커야 합니다.
 * @param encryptor 사용할 Tink Deterministic AEAD 암/복호화 인스턴스입니다.
 */
fun Table.tinkDaeadVarChar(
    name: String,
    cipherTextLength: Int = 255,
    encryptor: TinkDeterministicAead = TinkDaeads.AES256_SIV,
): Column<String> =
    registerColumn(
        name.requireNotBlank("name"),
        TinkDaeadVarCharColumnType(
            encryptor = encryptor,
            colLength = cipherTextLength.requirePositiveNumber("cipherTextLength")
        )
    )

/**
 * Google Tink Deterministic AEAD(결정적 암호화)로 암호화된 바이트 배열을 저장하기 위해 [name]의 `Binary` 컬럼을 생성합니다.
 *
 * Deterministic AEAD(AES256-SIV)는 동일한 평문에 대해 항상 동일한 암호문을 생성하므로
 * 인덱스 및 조건절(`WHERE col = value`)로 검색이 가능합니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [TinkDaeadBinaryColumnType]이며 DB 저장 시 암호문 바이트, 조회 시 복호화된 바이트를 제공합니다.
 * - `where { col eq insertedBinary }` 조건으로 조회할 수 있습니다.
 * - [name]이 blank면 `IllegalArgumentException`, [cipherByteLength]가 0 이하이면 `IllegalArgumentException`이 발생합니다.
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
 * @param name 컬럼명입니다. blank 문자열은 허용되지 않습니다.
 * @param cipherByteLength 암호문 바이트를 저장할 컬럼 길이입니다. 0보다 커야 합니다.
 * @param encryptor 사용할 Tink Deterministic AEAD 암/복호화 인스턴스입니다.
 */
fun Table.tinkDaeadBinary(
    name: String,
    cipherByteLength: Int,
    encryptor: TinkDeterministicAead = TinkDaeads.AES256_SIV,
): Column<ByteArray> =
    registerColumn(
        name.requireNotBlank("name"),
        TinkDaeadBinaryColumnType(
            encryptor = encryptor,
            length = cipherByteLength.requirePositiveNumber("cipherByteLength")
        )
    )

fun Table.tinkDaeadBlob(
    name: String,
    encryptor: TinkDeterministicAead = TinkDaeads.AES256_SIV,
): Column<ByteArray> =
    registerColumn(
        name.requireNotBlank("name"),
        TinkDaeadBlobColumnType(encryptor = encryptor),
    )
