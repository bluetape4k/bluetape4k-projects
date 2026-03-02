package io.bluetape4k.exposed.r2dbc.tests

import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.vendors.DatabaseDialectMetadata
import java.util.*

/** 현재 트랜잭션의 identifier 규칙으로 문자열 식별자 표기를 보정합니다. */
fun String.inProperCase(): String =
    TransactionManager.currentOrNull()?.db?.identifierManager?.inProperCase(this) ?: this

/** 현재 테스트 트랜잭션의 dialect를 반환합니다. */
val currentDialectTest: DatabaseDialect
    get() = TransactionManager.current().db.dialect

/** 현재 테스트 트랜잭션의 dialect metadata를 반환합니다. */
val currentDialectMetadataTest: DatabaseDialectMetadata
    get() = TransactionManager.current().db.dialectMetadata

/** 현재 트랜잭션이 있으면 dialect를, 없으면 `null`을 반환합니다. */
val currentDialectIfAvailableTest: DatabaseDialect?
    get() = TransactionManager.currentOrNull()?.db?.dialect

/** 전달한 enum 값들로 [EnumSet]을 생성합니다. */
inline fun <reified E: Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> =
    elements.toCollection(EnumSet.noneOf(E::class.java))

/** SQL Server에서 기본값 제약명 조각을 반환하고, 그 외 DB에서는 빈 문자열을 반환합니다. */
fun <T> Column<T>.constraintNamePart() = (currentDialectTest as? SQLServerDialect)?.let {
    " CONSTRAINT DF_${this.table.tableName}_${this.name}"
}.orEmpty()

/**
 * 기본 insert 후 커밋하고 [duration] 밀리초 동안 suspend 대기합니다.
 *
 * ## 동작/계약
 * - 인자 검증은 하지 않으며 음수 duration은 [delay]에서 예외가 발생할 수 있습니다.
 * - 테스트 기준으로 insert 후 상태를 관찰할 때 지연을 주는 용도로 사용합니다.
 *
 * ```kotlin
 * withDb(TestDB.H2) {
 *     UtilityTable.insertAndSuspending(10)
 * }
 * // insert + commit 후 10ms suspend 대기
 * ```
 */
suspend fun Table.insertAndSuspending(duration: Long) {
    this.insert { }
    TransactionManager.current().commit()
    delay(duration)
}
