package io.bluetape4k.exposed.sql

import io.bluetape4k.idgenerators.ksuid.Ksuid
import io.bluetape4k.idgenerators.ksuid.KsuidMillis
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.v1.core.ArrayColumnType
import org.jetbrains.exposed.v1.core.AutoIncColumnType
import org.jetbrains.exposed.v1.core.BasicBinaryColumnType
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.CharacterColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.DecimalColumnType
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.EntityIDColumnType
import org.jetbrains.exposed.v1.core.EnumerationColumnType
import org.jetbrains.exposed.v1.core.EnumerationNameColumnType
import org.jetbrains.exposed.v1.core.FloatColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.ShortColumnType
import org.jetbrains.exposed.v1.core.StringColumnType
import org.jetbrains.exposed.v1.core.Table.Dual.clientDefault
import org.jetbrains.exposed.v1.core.UIntegerColumnType
import org.jetbrains.exposed.v1.core.ULongColumnType
import org.jetbrains.exposed.v1.core.UShortColumnType
import org.jetbrains.exposed.v1.core.UUIDColumnType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.datetime.KotlinDurationColumnType
import org.jetbrains.exposed.v1.datetime.KotlinLocalDateColumnType
import org.jetbrains.exposed.v1.datetime.KotlinLocalDateTimeColumnType
import org.jetbrains.exposed.v1.datetime.KotlinLocalTimeColumnType
import org.jetbrains.exposed.v1.datetime.KotlinOffsetDateTimeColumnType
import org.jetbrains.exposed.v1.javatime.JavaDurationColumnType
import org.jetbrains.exposed.v1.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.v1.javatime.JavaLocalTimeColumnType
import org.jetbrains.exposed.v1.javatime.JavaOffsetDateTimeColumnType
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass

private val log = KotlinLogging.logger { }

/**
 * Column 값을 [TimebasedUuid.Reordered]이 생성한 UUID 값으로 설정합니다.
 *
 * @see TimebasedUuid.Reordered
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
@JvmName("timebasedGeneratedUUID")
fun Column<UUID>.timebasedGenerated(): Column<UUID> =
    clientDefault { TimebasedUuid.Reordered.nextId() }

/**
 * Column 값을 [TimebasedUuid.Reordered] 이 생성한 Timebased UUID의 Base62 인코딩한 문자열로 설정합니다.
 *
 * @see TimebasedUuid.Reordered
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
@JvmName("timebasedGeneratedString")
fun Column<String>.timebasedGenerated(): Column<String> =
    clientDefault { TimebasedUuid.Reordered.nextIdAsString() }

/**
 * 컬럼의 기본 값을 Snowflake ID 로 설정합니다.
 */
fun Column<Long>.snowflakeIdGenerated(): Column<Long> =
    clientDefault { Snowflakers.Global.nextId() }

/**
 * Column 값을 [io.bluetape4k.idgenerators.snowflake.Snowflake] 값으로 설정합니다.
 *
 * @see [io.bluetape4k.idgenerators.snowflake.Snowflake]
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
@JvmName("snowflakeGeneratedLong")
fun Column<Long>.snowflakeGenerated(): Column<Long> =
    clientDefault { Snowflakers.Global.nextId() }

/**
 * Column 값을 [io.bluetape4k.idgenerators.snowflake.Snowflake] ID의 문자열로 설정합니다.
 *
 * @see io.bluetape4k.idgenerators.snowflake.Snowflake
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
@JvmName("snowflakeGeneratedString")
fun Column<String>.snowflakeGenerated(): Column<String> =
    clientDefault { Snowflakers.Global.nextIdAsString() }

/**
 * Column 값을 [Ksuid]의 생성 값으로 설정합니다.
 *
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
fun Column<String>.ksuidGenerated(): Column<String> =
    clientDefault { Ksuid.nextId() }

/**
 * Column 값을 [KsuidMillis]의 생성 값으로 설정합니다.
 *
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
fun Column<String>.ksuidMillisGenerated(): Column<String> =
    clientDefault { KsuidMillis.nextId() }


fun IColumnType<*>.getLanguageType(): KClass<*>? {
    log.debug { "get language type for ${this.javaClass.simpleName}" }

    return when (this) {
        is BooleanColumnType -> Boolean::class
        is CharacterColumnType -> Char::class
        is ShortColumnType -> Short::class
        is UShortColumnType -> UShort::class
        is IntegerColumnType -> Int::class
        is UIntegerColumnType -> UInt::class
        is LongColumnType -> Long::class
        is ULongColumnType -> ULong::class
        is FloatColumnType -> Float::class
        is DoubleColumnType -> Double::class
        is DecimalColumnType -> BigDecimal::class
        is StringColumnType -> String::class
        is UUIDColumnType -> UUID::class
        is EnumerationColumnType<*> -> Enum::class
        is EnumerationNameColumnType<*> -> Enum::class
        is BasicBinaryColumnType -> ByteArray::class
        is BlobColumnType -> ExposedBlob::class
        is ArrayColumnType<*, *> -> Array::class

        is JavaLocalDateColumnType -> java.time.LocalDate::class
        is JavaLocalTimeColumnType -> java.time.LocalTime::class
        is JavaLocalDateTimeColumnType -> java.time.LocalDateTime::class
        is JavaOffsetDateTimeColumnType -> java.time.OffsetDateTime::class
        is JavaDurationColumnType -> java.time.Duration::class

        // exposed-kotlin-datetime 모듈을 추가해야 함
        is KotlinLocalDateColumnType -> kotlinx.datetime.LocalDate::class
        is KotlinLocalTimeColumnType -> kotlinx.datetime.LocalTime::class
        is KotlinLocalDateTimeColumnType -> kotlinx.datetime.LocalDateTime::class
        is KotlinOffsetDateTimeColumnType -> java.time.OffsetDateTime::class
        is KotlinDurationColumnType -> java.time.Duration::class

        // exposed-json 모듈을 추가해야 함 
        // is JsonColumnType<*> -> Any::class

        is EntityIDColumnType<*> -> this.idColumn.columnType.getLanguageType()
        is AutoIncColumnType<*> -> this.delegate.getLanguageType()
        else -> {
            log.warn { "알 수 없는 타입: ${this.javaClass.simpleName}" }
            null
        }
    }
}

@JvmName("getColumnLanguageType")
fun Column<*>.getLanguageType(): KClass<*>? = this.columnType.getLanguageType()

@JvmName("getEntityColumnLanguageType")
fun <ID: Any> Column<EntityID<ID>>.getLanguageType(): KClass<*>? =
    this.columnType.getLanguageType()
