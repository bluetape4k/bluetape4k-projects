package io.bluetape4k.exposed.sql

import io.bluetape4k.idgenerators.ksuid.Ksuid
import io.bluetape4k.idgenerators.ksuid.KsuidMillis
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.ArrayColumnType
import org.jetbrains.exposed.sql.AutoIncColumnType
import org.jetbrains.exposed.sql.BasicBinaryColumnType
import org.jetbrains.exposed.sql.BlobColumnType
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.CharacterColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.DecimalColumnType
import org.jetbrains.exposed.sql.DoubleColumnType
import org.jetbrains.exposed.sql.EntityIDColumnType
import org.jetbrains.exposed.sql.EnumerationColumnType
import org.jetbrains.exposed.sql.EnumerationNameColumnType
import org.jetbrains.exposed.sql.FloatColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.ShortColumnType
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table.Dual.clientDefault
import org.jetbrains.exposed.sql.UIntegerColumnType
import org.jetbrains.exposed.sql.ULongColumnType
import org.jetbrains.exposed.sql.UShortColumnType
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.javatime.JavaDurationColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalTimeColumnType
import org.jetbrains.exposed.sql.javatime.JavaOffsetDateTimeColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinDurationColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinLocalDateColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinLocalDateTimeColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinLocalTimeColumnType
import org.jetbrains.exposed.sql.kotlin.datetime.KotlinOffsetDateTimeColumnType
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass

private val log = KotlinLogging.logger { }

/**
 * Column 값을 [TimebasedUuid.Epoch]이 생성한 UUID 값으로 설정합니다.
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
