package io.bluetape4k.assertions

import io.bluetape4k.assertions.internal.Failures
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking

/**
 * 동기 블록을 감싸 예외 검증용 DSL을 제공한다.
 *
 * `invoking { ... }` 으로 생성하여 [shouldThrow], [shouldNotThrow] 및
 * 메시지/원인/속성 검증을 체이닝할 수 있다.
 *
 * @property block 검증할 코드 블록
 */
class InvokingBlock(val block: () -> Any?) {

    /**
     * 블록이 [expectedType] 또는 그 하위 타입의 예외를 던지는지 검증한다.
     *
     * @param expectedType 기대하는 예외 타입의 [KClass]
     * @return catch한 예외 (체이닝 또는 추가 검증용)
     * @throws org.opentest4j.AssertionFailedError 예외가 발생하지 않거나 타입이 일치하지 않을 때
     */
    @Suppress("UNCHECKED_CAST")
    infix fun <T : Throwable> shouldThrow(expectedType: KClass<T>): T {
        try {
            block()
        } catch (e: Throwable) {
            if (!expectedType.isInstance(e)) {
                Failures.failComparison(
                    "Expected ${expectedType.simpleName} but got ${e::class.simpleName}",
                    expectedType.simpleName,
                    e::class.simpleName
                )
            }
            return e as T
        }
        Failures.fail("Expected ${expectedType.simpleName} but no exception was thrown")
    }

    /**
     * 블록이 예외를 던지지 않는지 검증한다.
     *
     * @return 블록의 반환값
     * @throws org.opentest4j.AssertionFailedError 예외가 발생하면 실패
     */
    fun shouldNotThrow(): Any? {
        try {
            return block()
        } catch (e: Throwable) {
            Failures.failWithCause("Expected no exception but got ${e::class.simpleName}: ${e.message}", e)
        }
    }

    /**
     * 블록이 예외를 던지고, 메시지가 [message]와 정확히 일치하는지 검증한다.
     *
     * @param message 기대하는 예외 메시지 (정확 일치)
     * @return 자기 자신 (체이닝)
     */
    infix fun withMessage(message: String): InvokingBlock {
        val ex = captureException()
        if (ex.message != message) {
            Failures.failComparison(
                "Expected exception message to be \"$message\" but was \"${ex.message}\"",
                message,
                ex.message
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, 메시지가 [substring]을 포함하는지 검증한다.
     *
     * @param substring 메시지에 포함되어야 하는 부분 문자열
     * @return 자기 자신 (체이닝)
     */
    infix fun withMessageContaining(substring: String): InvokingBlock {
        val ex = captureException()
        val msg = ex.message
        if (msg == null || !msg.contains(substring)) {
            Failures.failComparison(
                "Expected exception message to contain \"$substring\" but was \"$msg\"",
                substring,
                msg
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, 메시지가 [regex]에 매칭되는지 검증한다.
     *
     * @param regex 메시지가 매칭되어야 하는 정규식
     * @return 자기 자신 (체이닝)
     */
    infix fun withMessageMatching(regex: Regex): InvokingBlock {
        val ex = captureException()
        val msg = ex.message
        if (msg == null || !regex.containsMatchIn(msg)) {
            Failures.failComparison(
                "Expected exception message to match $regex but was \"$msg\"",
                regex.pattern,
                msg
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, cause가 [causeType] 또는 그 하위 타입인지 검증한다.
     *
     * @param causeType 기대하는 cause 타입의 [KClass]
     * @return 자기 자신 (체이닝)
     */
    infix fun withCause(causeType: KClass<*>): InvokingBlock {
        val ex = captureException()
        val cause = ex.cause
        if (cause == null || !causeType.isInstance(cause)) {
            Failures.failComparison(
                "Expected cause to be ${causeType.simpleName} but was ${cause?.let { it::class.simpleName } ?: "<null>"}",
                causeType.simpleName,
                cause?.let { it::class.simpleName }
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, [block]을 통한 추가 검증을 수행한다.
     *
     * @param block catch한 예외에 대한 커스텀 검증 블록
     * @return 자기 자신 (체이닝)
     */
    infix fun with(block: Throwable.() -> Unit): InvokingBlock {
        val ex = captureException()
        ex.block()
        return this
    }

    /**
     * 내부적으로 블록을 실행하고 발생한 예외를 반환한다.
     * 예외가 발생하지 않으면 실패로 처리한다.
     */
    private fun captureException(): Throwable {
        try {
            block()
        } catch (e: Throwable) {
            return e
        }
        Failures.fail("Expected an exception but no exception was thrown")
    }
}

/**
 * 예외 검증용 DSL의 진입점.
 *
 * @param block 검증할 동기 코드 블록
 * @return [InvokingBlock] 인스턴스
 *
 * ```kotlin
 * invoking { throw IllegalStateException("oops") }
 *     .shouldThrow(IllegalStateException::class)
 *     .withMessage("oops")
 * ```
 */
fun invoking(block: () -> Any?): InvokingBlock = InvokingBlock(block)

/**
 * suspend 블록을 감싸 예외 검증용 DSL을 제공한다.
 *
 * `coInvoking { ... }` 으로 생성하여 [shouldThrow], [shouldNotThrow] 및
 * 메시지/원인/속성 검증을 체이닝할 수 있다.
 *
 * **중요 (CancellationException 처리):**
 * [shouldThrow]는 발생한 예외가 [CancellationException]이고 기대 타입이
 * [CancellationException]이 아닌 경우, catch하지 않고 즉시 rethrow하여
 * 코루틴 취소 협력을 보장한다.
 *
 * @property block 검증할 suspend 코드 블록
 */
class CoInvokingBlock(val block: suspend () -> Any?) {

    /**
     * 블록이 [expectedType] 또는 그 하위 타입의 예외를 던지는지 검증한다.
     *
     * [CancellationException]은 기대 타입이 아니면 즉시 rethrow한다.
     *
     * @param expectedType 기대하는 예외 타입의 [KClass]
     * @return catch한 예외
     */
    @Suppress("UNCHECKED_CAST")
    suspend infix fun <T : Throwable> shouldThrow(expectedType: KClass<T>): T {
        try {
            block()
        } catch (e: Throwable) {
            // CancellationException은 기대 타입이 아닌 경우 즉시 rethrow
            if (e is CancellationException && !expectedType.isInstance(e)) throw e
            if (!expectedType.isInstance(e)) {
                Failures.failComparison(
                    "Expected ${expectedType.simpleName} but got ${e::class.simpleName}",
                    expectedType.simpleName,
                    e::class.simpleName
                )
            }
            return e as T
        }
        Failures.fail("Expected ${expectedType.simpleName} but no exception was thrown")
    }

    /**
     * 블록이 예외를 던지지 않는지 검증한다.
     *
     * [CancellationException]은 catch하지 않고 즉시 rethrow한다.
     *
     * @return 블록의 반환값
     */
    suspend fun shouldNotThrow(): Any? {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Failures.failWithCause("Expected no exception but got ${e::class.simpleName}: ${e.message}", e)
        }
    }

    /**
     * 블록이 예외를 던지고, 메시지가 [message]와 정확히 일치하는지 검증한다.
     */
    suspend infix fun withMessage(message: String): CoInvokingBlock {
        val ex = captureException()
        if (ex.message != message) {
            Failures.failComparison(
                "Expected exception message to be \"$message\" but was \"${ex.message}\"",
                message,
                ex.message
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, 메시지가 [substring]을 포함하는지 검증한다.
     */
    suspend infix fun withMessageContaining(substring: String): CoInvokingBlock {
        val ex = captureException()
        val msg = ex.message
        if (msg == null || !msg.contains(substring)) {
            Failures.failComparison(
                "Expected exception message to contain \"$substring\" but was \"$msg\"",
                substring,
                msg
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, 메시지가 [regex]에 매칭되는지 검증한다.
     */
    suspend infix fun withMessageMatching(regex: Regex): CoInvokingBlock {
        val ex = captureException()
        val msg = ex.message
        if (msg == null || !regex.containsMatchIn(msg)) {
            Failures.failComparison(
                "Expected exception message to match $regex but was \"$msg\"",
                regex.pattern,
                msg
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, cause가 [causeType] 또는 그 하위 타입인지 검증한다.
     */
    suspend infix fun withCause(causeType: KClass<*>): CoInvokingBlock {
        val ex = captureException()
        val cause = ex.cause
        if (cause == null || !causeType.isInstance(cause)) {
            Failures.failComparison(
                "Expected cause to be ${causeType.simpleName} but was ${cause?.let { it::class.simpleName } ?: "<null>"}",
                causeType.simpleName,
                cause?.let { it::class.simpleName }
            )
        }
        return this
    }

    /**
     * 블록이 예외를 던지고, [block]을 통한 추가 검증을 수행한다.
     */
    suspend infix fun with(block: Throwable.() -> Unit): CoInvokingBlock {
        val ex = captureException()
        ex.block()
        return this
    }

    /**
     * 내부적으로 suspend 블록을 실행하고 발생한 예외를 반환한다.
     * [CancellationException]은 즉시 rethrow한다.
     */
    private suspend fun captureException(): Throwable {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return e
        }
        Failures.fail("Expected an exception but no exception was thrown")
    }
}

/**
 * 예외 검증용 DSL의 suspend 진입점.
 *
 * @param block 검증할 suspend 코드 블록
 * @return [CoInvokingBlock] 인스턴스
 *
 * ```kotlin
 * coInvoking { someSuspendCall() }
 *     .shouldThrow(IllegalStateException::class)
 *     .withMessage("oops")
 * ```
 */
fun coInvoking(block: suspend () -> Any?): CoInvokingBlock = CoInvokingBlock(block)

/**
 * 동기 또는 suspend 블록이 타입 [T]의 예외를 던지는지 검증하고, 예외를 반환한다.
 *
 * 단일 오버로드로 동기/suspend 코드 모두 처리한다. suspend 블록은 내부적으로 [kotlinx.coroutines.runBlocking]을
 * 통해 실행되므로 `runTest` 블록 안에서 사용할 경우 즉시 예외를 던지는 단순 케이스에만 적합하다.
 *
 * [CancellationException]은 기대 타입이 아닌 경우 즉시 rethrow하여 코루틴 취소 협력을 보장한다.
 *
 * @param message 검증 실패 시 출력할 추가 메시지 (null이면 기본 메시지 사용)
 * @param block 검증할 코드 블록 (동기 또는 suspend)
 * @return 발생한 예외 (타입 [T])
 */
inline fun <reified T : Throwable> assertFailsWith(
    message: String? = null,
    noinline block: suspend () -> Unit,
): T {
    val caught: Throwable? = try {
        runBlocking { block() }
        null
    } catch (e: CancellationException) {
        if (e is T) e else throw e
    } catch (e: Throwable) {
        e
    }
    val prefix = if (message != null) "$message — " else ""
    if (caught == null) {
        Failures.fail("${prefix}Expected ${T::class.simpleName} but no exception was thrown")
    }
    if (caught !is T) {
        Failures.failComparison(
            "${prefix}Expected ${T::class.simpleName} but got ${caught.javaClass.simpleName}",
            T::class.simpleName,
            caught.javaClass.simpleName
        )
    }
    return caught
}

/**
 * 동기 또는 suspend 블록이 어떤 예외라도 던지는지 검증한다.
 *
 * @param block 검증할 코드 블록 (동기 또는 suspend)
 * @return 발생한 예외
 */
fun assertFails(block: suspend () -> Unit): Throwable =
    assertFailsWith<Throwable>(block = block)

/**
 * 동기 또는 suspend 블록이 타입 [T]의 예외를 던지지 **않는지** 검증한다.
 *
 * 타입 [T]의 예외가 발생하면 검증 실패. 다른 타입의 예외나 예외 없음은 통과한다.
 * Kluent의 `internal assertNotFailsWith`에 해당하는 공개 API.
 *
 * @param block 검증할 코드 블록 (동기 또는 suspend)
 */
inline fun <reified T : Throwable> assertNotFailsWith(noinline block: suspend () -> Unit) {
    try {
        runBlocking { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        if (e is T) {
            Failures.fail("Expected block NOT to throw ${T::class.simpleName} but it did: ${e.message}")
        }
    }
}

/**
 * 동기 또는 suspend 블록이 어떤 예외도 던지지 않는지 검증한다.
 *
 * @param block 검증할 코드 블록 (동기 또는 suspend)
 */
fun assertNotFails(block: suspend () -> Unit) {
    try {
        runBlocking { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Failures.failWithCause("Expected no exception but got ${e::class.simpleName}: ${e.message}", e)
    }
}
