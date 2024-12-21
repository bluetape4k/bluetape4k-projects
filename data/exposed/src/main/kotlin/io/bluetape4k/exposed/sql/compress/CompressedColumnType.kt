package io.bluetape4k.exposed.sql.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

class CompressedColumnType(
    private val compressor: Compressor,
): ColumnType<String>() {

    companion object: KLogging()

    override fun sqlType(): String = currentDialect.dataTypeProvider.blobType()

    override fun valueFromDB(value: Any): String? {
        log.debug { "valueFromDB: $value" }

        return when (value) {
            is String    -> value
            is ByteArray -> compressor.decompress(value as? ByteArray).toUtf8String()
            else         -> null
        }
    }

    override fun notNullValueToDB(value: String): Any {
        return compressor.compress(value.toUtf8Bytes())
    }

    override fun nonNullValueToString(value: String): String {
        return compressor.compress(value)
    }
}

/**
 * 주어진 이름의 BLOB 컬럼을 생성합니다.
 */
fun Table.compressedBlob(name: String, compressor: Compressor = Compressors.LZ4): Column<String> =
    registerColumn(name, CompressedColumnType(compressor))
