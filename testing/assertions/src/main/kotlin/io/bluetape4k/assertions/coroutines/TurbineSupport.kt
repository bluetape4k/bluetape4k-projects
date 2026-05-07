package io.bluetape4k.assertions.coroutines

import app.cash.turbine.ReceiveTurbine
import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.shouldBeEqualTo

/**
 * 다음 항목을 기다렸다가 [expected]와 같은지 검증한다.
 *
 * Turbine 사용 필수: `testImplementation(libs.turbine)`.
 *
 * @receiver Turbine 인스턴스
 * @param expected 기대하는 값
 * @return catch한 항목
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemAndAssert(expected: T): T {
    val actual = awaitItem()
    actual shouldBeEqualTo expected
    return actual
}

/**
 * 다음 항목을 기다렸다가 [predicate]이 참인지 검증한다.
 *
 * Turbine 사용 필수: `testImplementation(libs.turbine)`.
 *
 * @receiver Turbine 인스턴스
 * @param predicate 항목이 만족해야 하는 조건
 * @return catch한 항목
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
    val actual = awaitItem()
    if (!predicate(actual)) {
        Failures.fail("Awaited item did not match predicate: $actual")
    }
    return actual
}

/**
 * 에러를 기다렸다가 [E] 타입인지 검증한다.
 *
 * Turbine 사용 필수: `testImplementation(libs.turbine)`.
 *
 * @receiver Turbine 인스턴스
 * @return [E] 타입의 예외
 */
suspend inline fun <reified E : Throwable> ReceiveTurbine<*>.awaitErrorOfType(): E {
    val error = awaitError()
    if (error !is E) {
        Failures.fail("Expected error of type ${E::class.simpleName}, but was ${error::class.simpleName}: ${error.message}")
    }
    return error as E
}
