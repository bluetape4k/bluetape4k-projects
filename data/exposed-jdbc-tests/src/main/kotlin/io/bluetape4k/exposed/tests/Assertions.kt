package io.bluetape4k.exposed.tests

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

@Suppress("UnusedReceiverParameter")
private val Transaction.failedOn: String
    get() = currentTestDB?.name ?: currentDialectTest.name

/**
 * 현재 dialect 정보를 포함한 실패 메시지로 `true` 검증을 수행합니다.
 */
fun Transaction.assertTrue(actual: Boolean) = kotlin.test.assertTrue(actual, "Failed on $failedOn")

/**
 * 현재 dialect 정보를 포함한 실패 메시지로 `false` 검증을 수행합니다.
 */
fun Transaction.assertFalse(actual: Boolean) = kotlin.test.assertFalse(actual, "Failed on $failedOn")

/**
 * 현재 dialect 정보를 포함한 실패 메시지로 동등성 검증을 수행합니다.
 */
fun <T> Transaction.assertEquals(exp: T, act: T) = kotlin.test.assertEquals(exp, act, "Failed on $failedOn")

/**
 * 단일 원소 컬렉션의 값을 기대값과 비교합니다.
 */
fun <T> Transaction.assertEquals(exp: T, act: Collection<T>) =
    kotlin.test.assertEquals(exp, act.single(), "Failed on $failedOn")

/**
 * 블록 실행 실패를 검증한 뒤 트랜잭션을 롤백합니다.
 *
 * ## 동작/계약
 * - 먼저 `commit()`으로 현재 변경을 확정한 후 [block]을 실행합니다.
 * - [block]이 실패하지 않으면 assertion이 실패합니다.
 * - 검증 후에는 항상 `rollback()`을 호출합니다.
 *
 * ```kotlin
 * transaction {
 *     assertFailAndRollback("duplicate key") {
 *         // 중복 키를 유발하는 DML
 *     }
 * }
 * // 예외 검증 후 rollback 수행
 * ```
 */
fun JdbcTransaction.assertFailAndRollback(message: String, block: () -> Unit) {
    commit()
    assertFails("Failed on ${currentDialectTest.name}. $message") {
        block()
        commit()
    }
    rollback()
}

/**
 * 현재 dialect 정보를 포함한 실패 메시지로 비동등성 검증을 수행합니다.
 *
 * [exp]와 [act]가 같으면 assertion 실패로 처리됩니다.
 */
fun <T> Transaction.assertNotEquals(exp: T, act: T) =
    kotlin.test.assertNotEquals(exp, act, "Failed on $failedOn")

/**
 * 지정한 예외 타입 발생을 검증합니다.
 *
 * ## 동작/계약
 * - 예외 메시지에 현재 dialect 이름을 포함합니다.
 * - [body]가 예외를 던지지 않으면 assertion이 실패합니다.
 *
 * ```kotlin
 * expectException<IllegalArgumentException> {
 *     require(false)
 * }
 * // IllegalArgumentException 검증 성공
 * ```
 */
inline fun <reified T: Throwable> expectException(body: () -> Unit) {
    assertFailsWith<T>("Failed on ${currentDialectTest.name}") {
        body()
    }
}
