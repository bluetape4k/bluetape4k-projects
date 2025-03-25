package io.bluetape4k.exposed.sql.fastjson2

import io.bluetape4k.fastjson2.FastjsonSerializer
import io.bluetape4k.fastjson2.deserialize
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.jetbrains.exposed.sql.vendors.currentDialect


class FastjsonBColumnType<T: Any>(
    serialize: (T) -> String,
    deserialize: (String) -> T,
): FastjsonColumnType<T>(serialize, deserialize) {
    override val usesBinaryFormat: Boolean = true

    override fun sqlType(): String = when (val dialect = currentDialect) {
        is H2Dialect -> dialect.originalDataTypeProvider.jsonBType()
        else -> dialect.dataTypeProvider.jsonBType()
    }
}


fun <T: Any> Table.fastjsonb(
    name: String,
    serialize: (T) -> String,
    deserialize: (String) -> T,
): Column<T> =
    registerColumn(name, FastjsonBColumnType(serialize, deserialize))

inline fun <reified T: Any> Table.fastjsonb(
    name: String,
    serializer: FastjsonSerializer = FastjsonSerializer.Default,
): Column<T> =
    fastjsonb(
        name,
        serialize = { serializer.serializeAsString(it) },
        deserialize = { serializer.deserialize<T>(it)!! }
    )
