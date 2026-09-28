@file:OptIn(ExperimentalTypeInference::class)

package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference
import kotlin.time.Duration

/**
 * suspend 환경에서 지연 초기화 값을 제공하는 계약입니다.
 *
 * ## 동작/계약
 * - 값 계산 시점(첫 호출/지연 시작)과 동시성 정책은 구현체에 따라 달라집니다.
 * - `invoke()`를 호출하면 계산된 값을 반환하거나 계산 중/계산 후 예외를 그대로 전파합니다.
 * - 이 인터페이스 자체는 입력 사전조건이나 null/blank/empty 규칙을 정의하지 않습니다.
 *
 * ```kotlin
 * val lazyValue: SuspendLazy<Int> = suspendBlockingLazy { 42 }
 * val result = lazyValue()
 * // result == 42
 * ```
 */
interface SuspendLazy<out T> {
    /**
     * 지연 값을 계산하거나 캐시된 값을 반환합니다.
     *
     * ## 동작/계약
     * - 첫 호출 시 계산을 시작할 수 있으며, 이후 동작(재계산/캐시)은 구현체 정책을 따릅니다.
     * - 계산 중 예외가 발생하면 해당 예외를 호출자에게 전파합니다.
     * - 이 함수는 인자를 받지 않으므로 별도 사전조건이 없습니다.
     *
     * ```kotlin
     * val lazyValue: SuspendLazy<String> = suspendBlockingLazy { "ok" }
     * val result = lazyValue()
     * // result == "ok"
     * ```
     */
    suspend operator fun invoke(): T

    /**
     * 제한 시간 안에 지연 값을 얻습니다.
     *
     * 제한 시간이 지나면 호출 waiter에 `TimeoutCancellationException`을 전달합니다. 값 계산은
     * 수신 구현체의 소유 scope에서 계속될 수 있으며, 이 함수는 계산 작업을 직접 취소하지 않습니다.
     *
     * @param timeout 값 계산을 기다릴 최대 시간입니다.
     * @return 계산된 값입니다.
     */
    suspend fun getUntil(timeout: Duration): T = withTimeout(timeout) { invoke() }

    /**
     * 제한 시간 안에 지연 값을 얻고, 제한 시간을 넘기면 `null`을 반환합니다.
     *
     * 실제 값이 `null`일 수 있으므로 반환된 `null`만으로 timeout 여부를 구분할 수 없습니다.
     * timeout은 호출 waiter에만 적용되며 값 계산 작업을 직접 취소하지 않습니다.
     *
     * @param timeout 값 계산을 기다릴 최대 시간입니다.
     * @return 계산된 값 또는 timeout 시 `null`입니다.
     */
    suspend fun getUntilOrNull(timeout: Duration): T? = withTimeoutOrNull(timeout) { invoke() }
}

/**
 * 블로킹 초기화 함수를 suspend 환경에서 지연 평가하는 `SuspendLazy`를 생성합니다.
 *
 * ## 동작/계약
 * - 첫 `invoke()`에서 값이 초기화되지 않았다면 `coroutineContext`로 전환해 `initializer`를 실행합니다.
 * - 값이 성공적으로 계산되면 이후 호출은 같은 값을 반환합니다.
 * - `initializer`가 예외를 던지면 예외가 전파되며, 값은 초기화되지 않아 다음 호출에서 다시 시도될 수 있습니다.
 * - timeout getter는 독립 대기 작업으로 초기화를 시작하므로, timeout 이후에도 동기 initializer가 계속 실행될 수 있습니다.
 *
 * ```kotlin
 * val lazyValue = suspendBlockingLazy(Dispatchers.Default) { 10 }
 * val result = lazyValue()
 * // result == 10
 * ```
 * @param coroutineContext 초기 계산 시 `initializer`를 실행할 코루틴 컨텍스트입니다.
 * @param mode 내부 `lazy` 캐시의 thread-safety 모드입니다.
 * @param initializer 실제 값을 계산하는 블로킹 초기화 함수입니다.
 */
inline fun <T> suspendBlockingLazy(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    crossinline initializer: () -> T,
): SuspendLazy<T> =
    SuspendBlockingLazyImpl(coroutineContext, mode) { initializer() }


/**
 * `Dispatchers.IO`에서 블로킹 초기화를 수행하는 `SuspendLazy`를 생성합니다.
 *
 * ## 동작/계약
 * - 첫 `invoke()` 시 `Dispatchers.IO`에서 `initializer`를 실행합니다.
 * - 값 계산 성공 후에는 동일 값을 재사용합니다.
 * - `initializer` 예외는 호출자에게 전파되며, 초기화 재시도 여부는 내부 `lazy` 동작을 따릅니다.
 *
 * ```kotlin
 * val lazyValue = suspendBlockingLazyIO { "io" }
 * val result = lazyValue()
 * // result == "io"
 * ```
 * @param mode 내부 `lazy` 캐시의 thread-safety 모드입니다.
 * @param initializer 실제 값을 계산하는 블로킹 초기화 함수입니다.
 */
inline fun <T> suspendBlockingLazyIO(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    crossinline initializer: () -> T,
): SuspendLazy<T> =
    SuspendBlockingLazyImpl(Dispatchers.IO, mode) { initializer() }


@PublishedApi
internal class SuspendBlockingLazyImpl<out T>(
    private val dispatcher: CoroutineContext = EmptyCoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    initializer: () -> T,
): SuspendLazy<T> {

    private val lazyValue: Lazy<T> = lazy(mode, initializer)
    private val timeoutInitializationLock = Any()
    private var timeoutInitializer: Deferred<T>? = null
    private var timeoutScope: CoroutineScope? = null

    override suspend fun invoke(): T = with(lazyValue) {
        if (isInitialized()) value
        else if (dispatcher === EmptyCoroutineContext) value
        else withContext(dispatcher) { value }
    }

    override suspend fun getUntil(timeout: Duration): T {
        if (lazyValue.isInitialized()) {
            currentCoroutineContext().ensureActive()
            return lazyValue.value
        }
        return withTimeout(timeout) { timeoutInitializer(currentCoroutineContext()).await() }
    }

    override suspend fun getUntilOrNull(timeout: Duration): T? {
        if (lazyValue.isInitialized()) {
            currentCoroutineContext().ensureActive()
            return lazyValue.value
        }
        return withTimeoutOrNull(timeout) { timeoutInitializer(currentCoroutineContext()).await() }
    }

    private fun timeoutInitializer(
        callerContext: CoroutineContext,
    ): Deferred<T> = synchronized(timeoutInitializationLock) {
        timeoutInitializer?.takeUnless { it.isCancelled }
            ?: timeoutScope(callerContext).async(start = CoroutineStart.LAZY) { invoke() }.also { deferred ->
                timeoutInitializer = deferred
                deferred.invokeOnCompletion { cause ->
                    if (cause != null) {
                        synchronized(timeoutInitializationLock) {
                            if (timeoutInitializer === deferred) timeoutInitializer = null
                        }
                    }
                }
            }
    }

    private fun timeoutScope(callerContext: CoroutineContext): CoroutineScope {
        return timeoutScope ?: run {
            val context = callerContext.minusKey(Job) + dispatcher.minusKey(Job)
            val ownerJob = dispatcher[Job] ?: SupervisorJob()
            CoroutineScope(context + ownerJob).also { timeoutScope = it }
        }
    }
}

/**
 * `CoroutineScope` 기반 suspend 초기화 함수를 지연 실행하는 `SuspendLazy`를 생성합니다.
 *
 * ## 동작/계약
 * - 첫 `invoke()`에서 `async(start = LAZY)`로 생성된 작업이 시작되고 결과를 `await()`합니다.
 * - 한 번 시작된 `Deferred`를 재사용하므로 성공 시 같은 값을 반환합니다.
 * - 실패/취소 시 예외 또는 취소가 이후 호출에도 동일하게 전파됩니다.
 *
 * ```kotlin
 * val scope = CoroutineScope(Dispatchers.Default)
 * val lazyValue = scope.suspendLazy { 21 * 2 }
 * val result = lazyValue()
 * // result == 42
 * ```
 * @param context 지연 시작되는 `async`에 전달할 코루틴 컨텍스트입니다.
 * @param mode `Deferred` 생성을 감싸는 내부 `lazy`의 thread-safety 모드입니다.
 * @param initializer `CoroutineScope` 수신 객체를 사용하는 suspend 초기화 함수입니다.
 */
inline fun <T> CoroutineScope.suspendLazy(
    context: CoroutineContext = EmptyCoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    crossinline initializer: suspend CoroutineScope.() -> T,
): SuspendLazy<T> {
    return SuspendLazyImpl(this, context, mode) { initializer() }
}

@PublishedApi
internal class SuspendLazyImpl<out T>(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    initializer: suspend CoroutineScope.() -> T,
): SuspendLazy<T> {

    private val deferredValue by lazy(mode) {
        coroutineScope.async(coroutineContext, start = CoroutineStart.LAZY, block = initializer)
    }

    override suspend fun invoke(): T = deferredValue.await()
}
