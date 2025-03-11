package io.bluetape4k.exposed.tests

import org.jetbrains.exposed.sql.Transaction
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

fun Transaction.assertTrue(actual: Boolean) = kotlin.test.assertTrue(actual, "Failed on $failedOn")
fun Transaction.assertFalse(actual: Boolean) = kotlin.test.assertFalse(!actual, "Failed on $failedOn")
fun <T> Transaction.assertEquals(exp: T, act: T) = kotlin.test.assertEquals(exp, act, "Failed on $failedOn")
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) =
    kotlin.test.assertEquals(exp, act.single(), "Failed on $failedOn")

fun Transaction.assertFailAndRollback(message: String, block: () -> Unit) {
    commit()
    assertFails("Failed on ${currentDialectTest.name}. $message") {
        block()
        commit()
    }
    rollback()
}

inline fun <reified T: Throwable> expectException(body: () -> Unit) {
    assertFailsWith<T>("Failed on ${currentDialectTest.name}") {
        body()
    }
}
