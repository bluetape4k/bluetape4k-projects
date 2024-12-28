package io.bluetape4k.exposed.sql

import io.bluetape4k.idgenerators.ksuid.Ksuid
import io.bluetape4k.idgenerators.ksuid.KsuidMillis
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table.Dual.clientDefault
import java.util.*

/**
 * Column 값을 [TimebasedUuid.Epoch]이 생성한 UUID 값으로 설정합니다.
 *
 * @see TimebasedUuid.Epoch
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
@JvmName("timebasedGeneratedUUID")
fun Column<UUID>.timebasedGenerated(): Column<UUID> =
    clientDefault { TimebasedUuid.Epoch.nextId() }

/**
 * Column 값을 [TimebasedUuid.Epoch]이 생성한 UUID의 Base62 인코딩한 문자열로 설정합니다.
 *
 * @see TimebasedUuid.Epoch
 * @sample io.bluetape4k.exposed.sql.ColumnExtensionsTest
 */
@JvmName("timebasedGeneratedString")
fun Column<String>.timebasedGenerated(): Column<String> =
    clientDefault { TimebasedUuid.Epoch.nextIdAsString() }

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
