package io.bluetape4k.assertions.internal

import org.opentest4j.AssertionFailedError
import org.opentest4j.MultipleFailuresError

/**
 * assertion 실패를 [AssertionFailedError]로 변환하는 내부 팩토리 함수 모음.
 *
 * IntelliJ의 diff viewer 동작을 위해 3-인자 생성자를 사용한다.
 *
 * @PublishedApi: public inline 함수에서 접근 가능하도록 공개 API로 노출한다.
 */
@PublishedApi
internal object Failures {

    /**
     * 단순 메시지만 있는 실패를 던진다.
     */
    @PublishedApi
    internal fun fail(message: String): Nothing =
        throw AssertionFailedError(message)

    /**
     * expected/actual 값을 포함한 비교 실패를 던진다.
     * IntelliJ diff viewer가 expected ↔ actual 차이를 시각화한다.
     */
    internal fun failComparison(message: String, expected: Any?, actual: Any?): Nothing =
        throw AssertionFailedError(message, expected, actual)

    /**
     * 원인 예외를 포함한 실패를 던진다.
     */
    internal fun failWithCause(message: String, cause: Throwable): Nothing =
        throw AssertionFailedError(message, cause)

    /**
     * 여러 실패를 한 번에 보고한다. [assertSoftly] 내부에서 사용.
     */
    internal fun failMultiple(heading: String, failures: List<Throwable>): Nothing =
        throw MultipleFailuresError(heading, failures)
}
