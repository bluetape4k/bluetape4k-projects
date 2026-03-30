package io.bluetape4k.exposed.core

import io.bluetape4k.idgenerators.ksuid.Ksuid
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.idgenerators.ulid.ULID
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.logging.KotlinLogging
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
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.java.UUIDColumnType
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

private val log by lazy { KotlinLogging.logger { } }
private val statefulMonotonicUlid by lazy { ULID.statefulMonotonic() }

/**
 * UUID 컬럼의 기본값을 `Uuid.V7.nextId()`로 설정합니다.
 *
 * ## 동작/계약
 * - DB 기본값이 아니라 Exposed `clientDefault`를 사용해 INSERT 시점에 값이 생성됩니다.
 * - 컬럼 정의를 mutate 하여 같은 [Column] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val id = uuid("id").timebasedGenerated()
 * // id.defaultValueFun != null
 * ```
 */
@JvmName("timebasedGeneratedUUID")
fun Column<UUID>.timebasedGenerated(): Column<UUID> = clientDefault { Uuid.V7.nextId() }

/**
 * 문자열 컬럼의 기본값을 UUIDv7 Base62 문자열(`Uuid.V7.nextIdAsString()`)로 설정합니다.
 *
 * ## 동작/계약
 * - Exposed `clientDefault`를 사용하므로 INSERT 시 클라이언트에서 값이 계산됩니다.
 * - MySQL 계열은 case-sensitive collation(`utf8mb4_bin`)을 사용하지 않으면 중복 위험이 있습니다.
 *
 * ```kotlin
 * val id = varchar("id", 24).timebasedGenerated()
 * // id.defaultValueFun != null
 * ```
 */
@JvmName("timebasedGeneratedString")
fun Column<String>.timebasedGenerated(): Column<String> = clientDefault { Uuid.V7.nextIdAsString() }

/**
 * `snowflakeGenerated()`의 이전 이름입니다.
 */
@Deprecated(
    "Use snowflakeGenerated() instead for consistency",
    ReplaceWith("snowflakeGenerated()")
)
fun Column<Long>.snowflakeIdGenerated(): Column<Long> = snowflakeGenerated()

/**
 * Long 컬럼의 기본값을 Snowflake ID(`Snowflakers.Global.nextId()`)로 설정합니다.
 */
@JvmName("snowflakeGeneratedLong")
fun Column<Long>.snowflakeGenerated(): Column<Long> = clientDefault { Snowflakers.Global.nextId() }

/**
 * 문자열 컬럼의 기본값을 Snowflake ID 문자열(`nextIdAsString`)로 설정합니다.
 */
@JvmName("snowflakeGeneratedString")
fun Column<String>.snowflakeGenerated(): Column<String> = clientDefault { Snowflakers.Global.nextIdAsString() }

/**
 * 문자열 컬럼의 기본값을 KSUID(`Ksuid.Seconds.nextId()`)로 설정합니다.
 */
fun Column<String>.ksuidGenerated(): Column<String> = clientDefault { Ksuid.Seconds.nextId() }

/**
 * 문자열 컬럼의 기본값을 밀리초 기반 KSUID(`Ksuid.Millis.nextId()`)로 설정합니다.
 */
fun Column<String>.ksuidMillisGenerated(): Column<String> = clientDefault { Ksuid.Millis.nextId() }

/**
 * 문자열 컬럼의 기본값을 상태 기반 단조 증가 ULID로 설정합니다.
 *
 * ## 동작/계약
 * - Exposed `clientDefault`를 사용하므로 INSERT 시 클라이언트에서 값이 계산됩니다.
 * - 모듈 전역 [ULID.StatefulMonotonic] 인스턴스를 공유하여 동일 밀리초 내에서도 단조 증가를 보장합니다.
 *
 * ```kotlin
 * val id = varchar("id", 26).ulidGenerated()
 * // id.defaultValueFun != null
 * ```
 */
fun Column<String>.ulidGenerated(): Column<String> = clientDefault { statefulMonotonicUlid.nextULID().toString() }

/**
 * Exposed 컬럼 타입을 대응되는 Kotlin 런타임 타입으로 매핑합니다.
 *
 * ## 동작/계약
 * - 내장 컬럼 타입과 주요 확장 타입(`datetime`, `EntityID`, `AutoInc`)을 처리합니다.
 * - 매핑할 수 없는 타입은 경고 로그를 남기고 `null`을 반환합니다.
 * - 새로운 객체를 생성하지 않고 타입 매핑만 수행합니다.
 *
 * ```kotlin
 * val type = column.columnType.getLanguageType()
 * // type != null
 * ```
 */
fun IColumnType<*>.getLanguageType(): KClass<*>? =
    when (this) {
        is BooleanColumnType -> {
            Boolean::class
        }
        is CharacterColumnType -> {
            Char::class
        }
        is ShortColumnType -> {
            Short::class
        }
        is UShortColumnType -> {
            UShort::class
        }
        is IntegerColumnType -> {
            Int::class
        }
        is UIntegerColumnType -> {
            UInt::class
        }
        is LongColumnType -> {
            Long::class
        }
        is ULongColumnType -> {
            ULong::class
        }
        is FloatColumnType -> {
            Float::class
        }
        is DoubleColumnType -> {
            Double::class
        }
        is DecimalColumnType -> {
            BigDecimal::class
        }
        is StringColumnType -> {
            String::class
        }
        is UUIDColumnType -> {
            UUID::class
        }
        is EnumerationColumnType<*> -> {
            Enum::class
        }
        is EnumerationNameColumnType<*> -> {
            Enum::class
        }
        is BasicBinaryColumnType -> {
            ByteArray::class
        }
        is BlobColumnType -> {
            ExposedBlob::class
        }
        is ArrayColumnType<*, *> -> {
            Array::class
        }
        is JavaLocalDateColumnType -> {
            java.time.LocalDate::class
        }
        is JavaLocalTimeColumnType -> {
            java.time.LocalTime::class
        }
        is JavaLocalDateTimeColumnType -> {
            java.time.LocalDateTime::class
        }
        is JavaOffsetDateTimeColumnType -> {
            java.time.OffsetDateTime::class
        }
        is JavaDurationColumnType -> {
            java.time.Duration::class
        }
        // exposed-kotlin-datetime 모듈을 추가해야 함
        is KotlinLocalDateColumnType -> {
            kotlinx.datetime.LocalDate::class
        }
        is KotlinLocalTimeColumnType -> {
            kotlinx.datetime.LocalTime::class
        }
        is KotlinLocalDateTimeColumnType -> {
            kotlinx.datetime.LocalDateTime::class
        }
        is KotlinOffsetDateTimeColumnType -> {
            java.time.OffsetDateTime::class
        }
        is KotlinDurationColumnType -> {
            java.time.Duration::class
        }
        // exposed-json 모듈을 추가해야 함
        // is JsonColumnType<*> -> Any::class

        is EntityIDColumnType<*> -> {
            this.idColumn.columnType.getLanguageType()
        }
        is AutoIncColumnType<*> -> {
            this.delegate.getLanguageType()
        }
        else -> {
            log.warn { "알 수 없는 타입: ${this.javaClass.simpleName}" }
            null
        }
    }

/**
 * 컬럼의 `columnType`을 기준으로 Kotlin 런타임 타입을 반환합니다.
 */
@JvmName("getColumnLanguageType")
fun Column<*>.getLanguageType(): KClass<*>? = this.columnType.getLanguageType()

/**
 * `EntityID` 컬럼의 내부 식별자 타입을 기준으로 Kotlin 런타임 타입을 반환합니다.
 */
@JvmName("getEntityColumnLanguageType")
fun <ID : Any> Column<EntityID<ID>>.getLanguageType(): KClass<*>? = this.columnType.getLanguageType()
