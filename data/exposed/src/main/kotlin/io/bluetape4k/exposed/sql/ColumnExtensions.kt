package io.bluetape4k.exposed.sql

import io.bluetape4k.idgenerators.ksuid.Ksuid
import io.bluetape4k.idgenerators.snowflake.Snowflakers
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table.Dual.clientDefault
import java.util.*

/**
 * 컬럼의 기본 값을 Timebased UUID 로 설정합니다.
 */
fun Column<UUID>.timebasedGenerated(): Column<UUID> =
    clientDefault { TimebasedUuid.Epoch.nextId() }

/**
 * 컬럼의 기본값을 Timebased UUID 를 Base62 로 인코딩한 문자열로 설정합니다.
 */
fun Column<String>.timebasedGenerated(): Column<String> =
    clientDefault { TimebasedUuid.Epoch.nextIdAsString() }

/**
 * 컬럼의 기본 값을 Snowflake ID 로 설정합니다.
 */
fun Column<Long>.snowflakeIdGenerated(): Column<Long> =
    clientDefault { Snowflakers.Global.nextId() }

/**
 * 컬럼의 기본 값을 Snowflake ID의 문자열로 설정합니다.
 */
fun Column<String>.snowflakeIdGenerated(): Column<String> =
    clientDefault { Snowflakers.Global.nextIdAsString() }


fun Column<String>.ksuidGenerated(): Column<String> =
    clientDefault { Ksuid.nextId() }
