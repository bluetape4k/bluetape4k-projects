package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.internal.Messages
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import org.opentest4j.AssertionFailedError

/**
 * 구조적 동등성(==)으로 [expected]와 같은지 검증한다.
 *
 * `shouldBe`와 다르게 이 함수는 `==` 연산자를 사용한다.
 * 참조 동일성(===)이 아니라 값 동등성을 검증한다.
 *
 * @receiver 검증할 값
 * @param expected 기대하는 값
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldBeEqualTo(expected: T?): T {
    if (this != expected) {
        Failures.failComparison(
            Messages.expectedToBe("equal to", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 구조적 동등성(==)으로 [expected]와 다른지 검증한다.
 *
 * @receiver 검증할 값
 * @param expected 기대하지 않는 값
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldNotBeEqualTo(expected: T?): T {
    if (this == expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("equal to", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 참조 동일성(===)으로 [expected]와 같은 객체인지 검증한다.
 *
 * ⚠️ 주의: `shouldBe`는 `===` (참조 동일성)을 사용한다.
 * 값 동등성(==)을 검증하려면 [shouldBeEqualTo]를 사용하라.
 *
 * @receiver 검증할 값
 * @param expected 기대하는 참조
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldBe(expected: T?): T {
    if (this !== expected) {
        Failures.failComparison(
            Messages.expectedToBe("be the same instance as", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 참조 동일성(===)으로 [expected]와 다른 객체인지 검증한다.
 *
 * ⚠️ 주의: `shouldNotBe`는 `!==` (참조 동일성)을 사용한다.
 * 값 비동등성(!=)을 검증하려면 [shouldNotBeEqualTo]를 사용하라.
 *
 * @receiver 검증할 값
 * @param expected 기대하지 않는 참조
 * @return receiver (체이닝 지원)
 */
infix fun <T> T.shouldNotBe(expected: T?): T {
    if (this === expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("be the same instance as", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 값이 null인지 검증한다.
 *
 * @receiver 검증할 값
 * @return receiver (체이닝 지원)
 */
fun <T : Any> T?.shouldBeNull(): T? {
    if (this != null) {
        Failures.failComparison(
            Messages.expectedToBe("be", null, this),
            null,
            this
        )
    }
    return this
}

/**
 * 값이 null이 아닌지 검증한다.
 *
 * kotlin.contracts를 통해 smart-cast를 지원한다.
 * 이 함수 호출 이후 receiver를 non-null 타입으로 사용할 수 있다.
 *
 * @receiver 검증할 값
 * @return non-null receiver (체이닝 지원)
 */
@OptIn(ExperimentalContracts::class)
fun <T : Any> T?.shouldNotBeNull(): T {
    contract {
        returns() implies (this@shouldNotBeNull != null)
    }
    if (this == null) {
        Failures.fail("Expected value to not be null, but was null.")
    }
    return this
}

/**
 * Boolean 값이 true인지 검증한다.
 *
 * @receiver 검증할 Boolean 값 (nullable 허용)
 * @return non-null Boolean receiver (체이닝 지원)
 */
fun Boolean?.shouldBeTrue(): Boolean {
    if (this != true) {
        Failures.failComparison(
            Messages.expectedToBe("be", true, this),
            true,
            this
        )
    }
    return this!!
}

/**
 * Boolean 값이 false인지 검증한다.
 *
 * @receiver 검증할 Boolean 값 (nullable 허용)
 * @return non-null Boolean receiver (체이닝 지원)
 */
fun Boolean?.shouldBeFalse(): Boolean {
    if (this != false) {
        Failures.failComparison(
            Messages.expectedToBe("be", false, this),
            false,
            this
        )
    }
    return this!!
}

/**
 * Boolean 값이 true가 아닌지 검증한다 (false 또는 null).
 *
 * @receiver 검증할 Boolean 값 (nullable 허용)
 * @return receiver (체이닝 지원)
 */
fun Boolean?.shouldNotBeTrue(): Boolean? {
    if (this == true) {
        Failures.failComparison(
            Messages.expectedNotToBe("be", true, this),
            true,
            this
        )
    }
    return this
}

/**
 * Boolean 값이 false가 아닌지 검증한다 (true 또는 null).
 *
 * @receiver 검증할 Boolean 값 (nullable 허용)
 * @return receiver (체이닝 지원)
 */
fun Boolean?.shouldNotBeFalse(): Boolean? {
    if (this == false) {
        Failures.failComparison(
            Messages.expectedNotToBe("be", false, this),
            false,
            this
        )
    }
    return this
}

/**
 * 커스텀 predicate로 값을 검증한다.
 *
 * @receiver 검증할 값
 * @param message predicate 실패 시 표시할 메시지
 * @param predicate 검증 조건
 * @return receiver (체이닝 지원)
 */
fun <T> T.should(message: String, predicate: (T) -> Boolean): T {
    if (!predicate(this)) {
        Failures.fail(message)
    }
    return this
}

/**
 * 즉시 테스트를 실패시킨다.
 *
 * @param message 실패 메시지 (기본값: null)
 * @param cause 원인 예외 (기본값: null)
 */
fun fail(message: String? = null, cause: Throwable? = null): Nothing {
    val msg = message ?: "Test failed."
    if (cause != null) {
        Failures.failWithCause(msg, cause)
    } else {
        Failures.fail(msg)
    }
}

/**
 * block의 반환값이 [expected]와 같은지 검증한다.
 *
 * @param expected 기대하는 값
 * @param block 검증할 블록
 */
fun <T> expectThat(expected: T, block: () -> T) {
    val actual = block()
    if (actual != expected) {
        Failures.failComparison(
            Messages.comparison(expected, actual),
            expected,
            actual
        )
    }
}

/**
 * block의 반환값이 [expected]와 같은지 검증한다.
 *
 * @param expected 기대하는 값
 * @param message 실패 시 표시할 메시지
 * @param block 검증할 블록
 */
fun <T> expectThat(expected: T, message: String, block: () -> T) {
    val actual = block()
    if (actual != expected) {
        Failures.failComparison(message, expected, actual)
    }
}

/**
 * 참조 동일성(===)으로 [expected]와 같은 객체인지 검증한다.
 *
 * [shouldBe]의 명시적 이름 버전.
 *
 * @receiver 검증할 값
 * @param expected 기대하는 참조
 * @return receiver (체이닝 지원)
 */
infix fun <T> T?.shouldBeSameInstanceAs(expected: T?): T? {
    if (this !== expected) {
        Failures.failComparison(
            Messages.expectedToBe("be the same instance as", expected, this),
            expected,
            this
        )
    }
    return this
}

/**
 * 참조 동일성(===)으로 [expected]와 다른 객체인지 검증한다.
 *
 * [shouldNotBe]의 명시적 이름 버전.
 *
 * @receiver 검증할 값
 * @param expected 기대하지 않는 참조
 * @return receiver (체이닝 지원)
 */
infix fun <T> T?.shouldNotBeSameInstanceAs(expected: T?): T? {
    if (this === expected) {
        Failures.failComparison(
            Messages.expectedNotToBe("be the same instance as", expected, this),
            expected,
            this
        )
    }
    return this
}
