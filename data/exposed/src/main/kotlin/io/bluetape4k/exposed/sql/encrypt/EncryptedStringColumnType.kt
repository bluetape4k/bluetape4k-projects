package io.bluetape4k.exposed.sql.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

class EncryptedStringColumnType(private val encryptor: Encryptor): ColumnType<String>() {

    companion object: KLogging()

    override fun sqlType(): String =
        currentDialect.dataTypeProvider.binaryType()

    override fun valueFromDB(value: Any): String? {
        log.debug { "valueFromDB: $value" }
        return when (value) {
            is ByteArray -> encryptor.decrypt(value).toUtf8String()
            is String    -> value
            else         -> null
        }
    }

    override fun notNullValueToDB(value: String): Any =
        encryptor.encrypt(value.toUtf8Bytes())

    override fun nonNullValueToString(value: String): String =
        encryptor.encrypt(value)
}

fun Table.encryptedString(
    name: String,
    encryptor: Encryptor = Encryptors.AES,
): Column<String> =
    registerColumn(name, EncryptedStringColumnType(encryptor))
