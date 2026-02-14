package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

fun Transaction.assertTrue(actual: Boolean) = kotlin.test.assertTrue(actual, "Failed on $failedOn")
fun Transaction.assertFalse(actual: Boolean) = kotlin.test.assertFalse(actual, "Failed on $failedOn")
fun <T> Transaction.assertEquals(exp: T, act: T) = kotlin.test.assertEquals(exp, act, "Failed on $failedOn")
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) =
    kotlin.test.assertEquals(exp, act.single(), "Failed on $failedOn")

suspend fun R2dbcTransaction.assertFailAndRollback(message: String, block: suspend () -> Unit) {
    commit()
    try {
        block()
        commit()
        fail("Failed on ${currentDialectTest.name}. $message")
    } catch (_: Throwable) {
        // expected
    } finally {
        rollback()
    }
}

inline fun <reified T: Throwable> expectException(body: () -> Unit) {
    assertFailsWith<T>("Failed on ${currentDialectTest.name}") {
        body()
    }
}

suspend inline fun <reified T: Throwable> expectExceptionSuspending(crossinline body: suspend () -> Unit) {
    try {
        body()
        fail("Failed on ${currentDialectTest.name}. Expected exception ${T::class.simpleName}.")
    } catch (ex: Throwable) {
        if (ex !is T) {
            throw AssertionError("Failed on ${currentDialectTest.name}. Unexpected exception type: ${ex::class}", ex)
        }
    }
}
