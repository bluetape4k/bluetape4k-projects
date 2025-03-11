package io.bluetape4k.exposed.sql.jackson

import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.jackson.deserializeFromString
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.jetbrains.exposed.sql.vendors.currentDialect

class JacksonBColumnType<T: Any>(
    serialize: (T) -> String,
    deserialize: (String) -> T,
): JacksonColumnType<T>(serialize, deserialize) {
    override val usesBinaryFormat: Boolean = true

    override fun sqlType(): String = when (val dialect = currentDialect) {
        is H2Dialect -> dialect.originalDataTypeProvider.jsonBType()
        else -> dialect.dataTypeProvider.jsonBType()
    }
}


fun <T: Any> Table.jacksonb(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, JacksonBColumnType(serialize, deserialize))

inline fun <reified T: Any> Table.jacksonb(
    name: String,
    serializer: JacksonSerializer = DefaultJacksonSerializer,
): Column<T> =
    jacksonb(
        name,
        serialize = { serializer.serializeAsString(it) },
        deserialize = { serializer.deserializeFromString<T>(it)!! }
    )
