package io.bluetape4k.exposed.tests

import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.*

/**
 * 현재 트랜잭션의 identifier 규칙으로 문자열 식별자 표기를 보정합니다.
 */
fun String.inProperCase(): String =
    TransactionManager.currentOrNull()?.db?.identifierManager?.inProperCase(this) ?: this

/** 현재 테스트 트랜잭션의 dialect를 반환합니다. */
val currentDialectTest: DatabaseDialect
    get() = TransactionManager.current().db.dialect

/** 현재 트랜잭션이 있으면 dialect를, 없으면 `null`을 반환합니다. */
val currentDialectIfAvailableTest: DatabaseDialect?
    get() = TransactionManager.currentOrNull()?.db?.dialect

/** 전달된 enum 원소들로 [EnumSet]을 생성합니다. */
inline fun <reified E: Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> =
    elements.toCollection(EnumSet.noneOf(E::class.java))

/** SQL Server에서 기본값 제약명 조각을 생성하고, 그 외 DB에서는 빈 문자열을 반환합니다. */
fun <T> Column<T>.constraintNamePart() = (currentDialectTest as? SQLServerDialect)?.let {
    " CONSTRAINT DF_${table.tableName}_$name"
}.orEmpty()

/**
 * 기본 값으로 정보를 레코드를 생성하고, [duration]만큼 대기합니다.
 *
 * ## 동작/계약
 * - `insert` 후 `commit()`을 수행한 다음 현재 스레드를 [duration] 밀리초 동안 블로킹 대기합니다.
 * - [duration] 검증은 수행하지 않으며 음수면 `Thread.sleep`에서 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * withDb(testDB) {
 *     UtilityTable.insertAndWait(10)
 * }
 * // insert + commit 이후 10ms 대기
 * ```
 */
inline fun Table.insertAndWait(
    duration: Long,
    crossinline body: Table.(InsertStatement<Number>) -> Unit = {},
) {
    this.insert { body(it) }
    // this.insert(body)
    TransactionManager.current().commit()
    Thread.sleep(duration)
}

/**
 * 기본 값으로 정보를 레코드를 생성하고, [duration]만큼 대기합니다.
 *
 * ## 동작/계약
 * - `insert` 후 `commit()`을 수행하고 [delay]로 [duration] 밀리초 비동기 대기합니다.
 * - 호출 스레드를 블로킹하지 않으며, 코루틴 컨텍스트 취소 시 대기가 취소됩니다.
 *
 * ```kotlin
 * withDbSuspending(testDB) {
 *     UtilityTable.insertAndSuspending(10)
 * }
 * // insert + commit 이후 10ms suspend 대기
 * ```
 */
suspend inline fun Table.insertAndSuspending(
    duration: Long,
    crossinline body: Table.(InsertStatement<Number>) -> Unit = {},
) {
    this.insert { body(it) }
    // this.insert(body)
    TransactionManager.current().commit()
    delay(duration)
}
