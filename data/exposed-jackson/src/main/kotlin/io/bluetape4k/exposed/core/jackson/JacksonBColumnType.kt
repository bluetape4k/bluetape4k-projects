package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.jackson.JacksonSerializer
import io.bluetape4k.jackson.deserializeFromString
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

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
