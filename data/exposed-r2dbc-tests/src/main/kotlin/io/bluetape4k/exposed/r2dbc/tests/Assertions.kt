package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

/**
 * 현재 테스트 방언 정보를 포함한 실패 메시지로 `true` 검증을 수행합니다.
 */
fun Transaction.assertTrue(actual: Boolean) = kotlin.test.assertTrue(actual, "Failed on $failedOn")

/**
 * 현재 테스트 방언 정보를 포함한 실패 메시지로 `false` 검증을 수행합니다.
 */
fun Transaction.assertFalse(actual: Boolean) = kotlin.test.assertFalse(actual, "Failed on $failedOn")

/**
 * 현재 테스트 방언 정보를 포함한 실패 메시지로 동등성 검증을 수행합니다.
 */
fun <T> Transaction.assertEquals(exp: T, act: T) = kotlin.test.assertEquals(exp, act, "Failed on $failedOn")

/**
 * 단일 원소 컬렉션과 기대값을 비교합니다.
 */
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) =
    kotlin.test.assertEquals(exp, act.single(), "Failed on $failedOn")

/**
 * 현재 테스트 방언 정보를 포함한 실패 메시지로 비동등성 검증을 수행합니다.
 *
 * [exp]와 [act]가 같으면 assertion 실패로 처리됩니다.
 */
fun <T> Transaction.assertNotEquals(exp: T, act: T) =
    kotlin.test.assertNotEquals(exp, act, "Failed on $failedOn")

/**
 * [block]이 실패하는지 확인하고, 실행 후 현재 트랜잭션을 롤백합니다.
 *
 * ## 동작/계약
 * - 먼저 `commit()`으로 현재 상태를 확정한 뒤 [block] 실행 실패를 기대합니다.
 * - [block]이 실패하지 않으면 assertion 실패로 처리합니다.
 * - 검증 후에는 항상 `rollback()`을 호출합니다.
 *
 * ```kotlin
 * assertFailAndRollback("duplicate key") {
 *     error("boom")
 * }
 * // rollback 수행됨
 * ```
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

/**
 * 지정한 예외 타입 발생을 검증합니다.
 */
inline fun <reified T: Throwable> expectException(body: () -> Unit) {
    assertFailsWith<T>("Failed on ${currentDialectTest.name}") {
        body()
    }
}

/**
 * suspend 블록이 [T] 예외를 던지는지 검사합니다.
 */
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
