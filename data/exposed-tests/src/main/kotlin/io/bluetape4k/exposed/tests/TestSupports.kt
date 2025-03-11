package io.bluetape4k.exposed.tests

import kotlinx.coroutines.delay
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.vendors.DatabaseDialect
import org.jetbrains.exposed.sql.vendors.SQLServerDialect
import java.util.*

fun String.inProperCase(): String =
    TransactionManager.currentOrNull()?.db?.identifierManager?.inProperCase(this) ?: this

val currentDialectTest: DatabaseDialect
    get() = TransactionManager.current().db.dialect

val currentDialectIfAvailableTest: DatabaseDialect?
    get() =
        if (TransactionManager.isInitialized() && TransactionManager.currentOrNull() != null) {
            currentDialectTest
        } else {
            null
        }

inline fun <reified E: Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> =
    elements.toCollection(EnumSet.noneOf(E::class.java))

fun <T> Column<T>.constraintNamePart() = (currentDialectTest as? SQLServerDialect)?.let {
    " CONSTRAINT DF_${table.tableName}_$name"
} ?: ""

/**
 * 기본 값으로 정보를 레코드를 생성하고, [duration]만큼 대기합니다.
 */
inline fun Table.insertAndWait(
    duration: Long,
    crossinline body: Table.(InsertStatement<Number>) -> Unit = {},
) {
    this.insert(body)
    TransactionManager.current().commit()
    Thread.sleep(duration)
}

/**
 * 기본 값으로 정보를 레코드를 생성하고, [duration]만큼 대기합니다.
 */
suspend inline fun Table.insertAndCoawait(
    duration: Long,
    crossinline body: Table.(InsertStatement<Number>) -> Unit = {},
) {
    this.insert(body)
    TransactionManager.current().commit()
    delay(duration)
}
