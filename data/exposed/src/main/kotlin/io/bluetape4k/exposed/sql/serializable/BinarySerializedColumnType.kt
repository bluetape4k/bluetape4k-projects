package io.bluetape4k.exposed.sql.serializable

import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.currentDialect

class BinarySerializedColumnType<T: Any>(private val serializer: BinarySerializer): ColumnType<T>() {

    companion object: KLogging()


    override fun sqlType(): String = currentDialect.dataTypeProvider.blobType()

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T? {
        log.debug { "valueFromDB: $value" }

        return when (value) {
            is ByteArray -> serializer.deserialize(value)
            else         -> value as? T
        }
    }

    override fun notNullValueToDB(value: T): Any {
        return serializer.serialize(value)
    }

    override fun nonNullValueToString(value: T): String {
        return serializer.serialize(value).encodeBase64String()
    }
}

fun <T: Any> Table.binarySerialized(
    name: String,
    serializer: BinarySerializer = BinarySerializers.LZ4Fury,
): Column<T> =
    registerColumn(name, BinarySerializedColumnType(serializer))
