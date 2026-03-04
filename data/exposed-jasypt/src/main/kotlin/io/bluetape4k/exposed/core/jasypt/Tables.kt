package io.bluetape4k.exposed.core.jasypt

import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table

/**
 * 암호화된 문자열을 저장하기 위해 [name]의 `VARCHAR` 컬럼을 생성합니다.
 *
 * `exposed-crypt` 모듈에서 제공하는 `encryptedVarChar` 는 매번 암호화할 때 마다 다른 값으로 암호화가 되어
 * Indexing 할 수 없고, 암호화된 컬럼을 조건절에 이용할 수 없습니다.
 *
 * 대신 `jasyptVarChar` 는 Jasypt 라이브러리를 이용하여, 암호화 결과가 항상 같습니다.
 * 그래서 Indexing 할 수 있고, 암호화된 컬럼을 조건절에 이용할 수 있습니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [JasyptVarCharColumnType]이며 DB에는 암호문이 저장되고 조회 시 평문으로 복원됩니다.
 * - `nullable()`/`index()` 조합을 사용할 수 있고, 테스트처럼 `where { col eq value }` 검색이 동작합니다.
 * - 수신 [Table] 메타데이터를 mutate하여 컬럼을 등록합니다.
 * - [name]이 blank면 `IllegalArgumentException`, [cipherTextLength]가 0 이하이면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * object T1: IntIdTable("string_table") {
 *     val name = jasyptVarChar("name", 255, Encryptors.AES).nullable().index()
 * }
 * val id = T1.insertAndGetId { it[name] = "debop" }
 * val count = T1.selectAll().where { T1.id eq id }.count()
 * // count == 1L
 * ```
 *
 * @param name 컬럼명입니다. blank 문자열은 허용되지 않습니다.
 * @param cipherTextLength 암호문을 저장할 컬럼 길이입니다. 0보다 커야 합니다.
 * @param encryptor 사용할 암/복호화기입니다.
 */
@Deprecated("use io.bluetape4k.exposed.core.tink.daeadVarChar in bluetape4k-exposed-tink.")
fun Table.jasyptVarChar(
    name: String,
    cipherTextLength: Int,
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor = Encryptors.AES,
): Column<String> =
    registerColumn(
        name.requireNotBlank("name"),
        JasyptVarCharColumnType(
            encryptor = encryptor,
            colLength = cipherTextLength.requirePositiveNumber("cipherTextLength")
        )
    )


/**
 * 암호화된 `ByteArray`를 저장하기 위해 [name]의 `Binary` 컬럼을 생성합니다.
 *
 * `exposed-crypt` 모듈에서 제공하는 `encryptedBinary` 는 매번 암호화할 때 마다 다른 값으로 암호화가 되어
 * Indexing 할 수 없고, 암호화된 컬럼을 조건절에 이용할 수 없습니다.
 *
 * 대신 `jasyptBinary` 는 Jasypt 라이브러리를 이용하여, 암호화 결과가 항상 같습니다.
 * 그래서 Indexing 할 수 있고, 암호화된 컬럼을 조건절에 이용할 수 있습니다.
 *
 * ## 동작/계약
 * - 등록되는 컬럼 타입은 [JasyptBinaryColumnType]이며 DB 저장 시 암호문 바이트, 조회 시 복호화된 바이트를 제공합니다.
 * - 테스트처럼 `selectAll().where { binary eq insertedBinary }` 조건으로 조회할 수 있습니다.
 * - 수신 [Table] 메타데이터를 mutate하여 컬럼을 등록합니다.
 * - [name]이 blank면 `IllegalArgumentException`, [cipherByteLength]가 0 이하이면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * object T1: IntIdTable("string_table") {
 *     val address = jasyptBinary("address", 255, Encryptors.TripleDES).nullable()
 * }
 * val id = T1.insertAndGetId { it[address] = "seoul".toUtf8Bytes() }
 * val restored = T1.selectAll().where { T1.id eq id }.single()[T1.address]!!
 * // restored.toUtf8String() == "seoul"
 * ```
 *
 * @param name 컬럼명입니다. blank 문자열은 허용되지 않습니다.
 * @param cipherByteLength 암호문 바이트를 저장할 컬럼 길이입니다. 0보다 커야 합니다.
 * @param encryptor 사용할 암/복호화기입니다.
 */
@Deprecated("use io.bluetape4k.exposed.core.tink.daeadBinary in bluetape4k-exposed-tink.")
fun Table.jasyptBinary(
    name: String,
    cipherByteLength: Int,
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor = Encryptors.AES,
): Column<ByteArray> =
    registerColumn(
        name.requireNotBlank("name"),
        JasyptBinaryColumnType(
            encryptor = encryptor,
            length = cipherByteLength.requirePositiveNumber("cipherByteLength")
        )
    )


@Deprecated("use io.bluetape4k.exposed.core.tink.daeadBlob in bluetape4k-exposed-tink.")
fun Table.jasyptBlob(
    name: String,
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor = Encryptors.AES,
): Column<ByteArray> =
    registerColumn(
        name.requireNotBlank("name"),
        JasyptBlobColumnType(encryptor)
    )
