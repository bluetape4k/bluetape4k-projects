package io.bluetape4k.exposed.sql.jasypt

import io.bluetape4k.crypto.encrypt.Encryptors
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
 */
fun Table.jasyptVarChar(
    name: String,
    cipherTextLength: Int,
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor = Encryptors.AES,
): Column<String> =
    registerColumn(name, JasyptVarCharColumnType(encryptor, cipherTextLength))


/**
 * 암호화된 `ByteArray`를 저장하기 위해 [name]의 `Binary` 컬럼을 생성합니다.
 *
 * `exposed-crypt` 모듈에서 제공하는 `encryptedBinary` 는 매번 암호화할 때 마다 다른 값으로 암호화가 되어
 * Indexing 할 수 없고, 암호화된 컬럼을 조건절에 이용할 수 없습니다.
 *
 * 대신 `jasyptBinary` 는 Jasypt 라이브러리를 이용하여, 암호화 결과가 항상 같습니다.
 * 그래서 Indexing 할 수 있고, 암호화된 컬럼을 조건절에 이용할 수 있습니다.
 */
fun Table.jasyptBinary(
    name: String,
    cipherByteLength: Int,
    encryptor: io.bluetape4k.crypto.encrypt.Encryptor = Encryptors.AES,
): Column<ByteArray> =
    registerColumn(name, JasyptBinaryColumnType(encryptor, cipherByteLength))
