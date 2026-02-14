package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

/** 현재 테스트 방언 정보를 포함한 assertTrue 래퍼 */
fun Transaction.assertTrue(actual: Boolean) = kotlin.test.assertTrue(actual, "Failed on $failedOn")

/** 현재 테스트 방언 정보를 포함한 assertFalse 래퍼 */
fun Transaction.assertFalse(actual: Boolean) = kotlin.test.assertFalse(actual, "Failed on $failedOn")

/** 현재 테스트 방언 정보를 포함한 assertEquals 래퍼 */
fun <T> Transaction.assertEquals(exp: T, act: T) = kotlin.test.assertEquals(exp, act, "Failed on $failedOn")

/** 단일 원소 컬렉션과 기대값 비교 */
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) =
    kotlin.test.assertEquals(exp, act.single(), "Failed on $failedOn")

/**
 * [block]이 실패하는지 확인하고, 실행 후 현재 트랜잭션을 롤백합니다.
 */
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

/** suspend 블록이 [T] 예외를 던지는지 검사합니다. */
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
