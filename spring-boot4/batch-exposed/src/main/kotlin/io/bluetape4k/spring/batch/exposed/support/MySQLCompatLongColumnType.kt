package io.bluetape4k.spring.batch.exposed.support

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * MySQL에서는 `CAST(x AS BIGINT)`가 지원되지 않으므로 `SIGNED`를 사용하는 [IColumnType].
 *
 * - MySQL 계열: `CAST(x AS SIGNED)`
 * - H2, PostgreSQL 등: `CAST(x AS BIGINT)`
 *
 * [LongColumnType]에 모든 메서드를 위임하고 [sqlType]만 방언에 따라 분기합니다.
 */
internal class MySQLCompatLongColumnType(
    private val delegate: LongColumnType = LongColumnType(),
) : IColumnType<Long> by delegate {

    override fun sqlType(): String =
        if (currentDialect is MysqlDialect) "SIGNED" else "BIGINT"
}

/**
 * [EntityID<Long>] 컬럼을 MySQL 호환 방식으로 [Long]으로 캐스팅합니다.
 *
 * - MySQL: `CAST(id AS SIGNED)`
 * - H2, PostgreSQL: `CAST(id AS BIGINT)`
 */
internal fun Column<EntityID<Long>>.castToLong(): ExpressionWithColumnType<Long> =
    castTo<Long>(MySQLCompatLongColumnType())
