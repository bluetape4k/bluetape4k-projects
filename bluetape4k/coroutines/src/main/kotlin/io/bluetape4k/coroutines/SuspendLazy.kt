@file:OptIn(ExperimentalTypeInference::class)

package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * SuspendLazy는 suspend 함수를 호출하는 람다를 나타냅니다.
 *
 * @param T suspend 함수의 반환값
 */
interface SuspendLazy<out T> {
    suspend operator fun invoke(): T
}

/**
 * [Lazy]와 같이 값을 지연해서 계산합니다.
 * 값을 조회할 때, Coroutine Scope 에서 수행해야 합니다.
 *
 * ```
 * val lazyValue: SuspendLazy<Int> = suspendBlockingLazy {
 *      Thread.sleep(100)
 *      42
 * }
 *
 * val value = runBlocking { lazyValue() }  // 42
 * ```
 *
 * @param coroutineContext CoroutineContext
 * @param mode        [LazyThreadSafetyMode]
 * @param initializer suspend 함수
 */
inline fun <T> suspendBlockingLazy(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    @BuilderInference crossinline initializer: () -> T,
): SuspendLazy<T> =
    SuspendLazyBlockImpl(coroutineContext, mode) { initializer() }


/**
 * [Lazy]와 같이 값 계산을 지연해서 수형하는데, 값 계산을 [Dispatchers.IO] 환경 하에서 Blocking 하게 수행합니다.
 * 값을 조회할 때, Coroutine Scope 에서 수행해야 한다.
 *
 * ```
 * val lazyValue: SuspendLazy<Int> = suspendBlockingLazyIO {
 *      Thread.sleep(100)
 *      42
 * }
 *
 * val value = runBlocking { lazyValue() }  // 42
 * ```
 *
 * @param T
 * @param mode        [LazyThreadSafetyMode]
 * @param initializer 지연된 계산을 수행하는 함수
 * @return
 */
inline fun <T> suspendBlockingLazyIO(
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    @BuilderInference crossinline initializer: () -> T,
): SuspendLazy<T> =
    SuspendLazyBlockImpl(Dispatchers.IO, mode) { initializer() }


@PublishedApi
internal class SuspendLazyBlockImpl<out T>(
    private val dispatcher: CoroutineContext = EmptyCoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    initializer: () -> T,
): SuspendLazy<T> {

    private val lazyValue: Lazy<T> = lazy(mode, initializer)

    override suspend fun invoke(): T = with(lazyValue) {
        if (isInitialized()) value
        else withContext(dispatcher) { value }
    }
}

/**
 * 지연된 값을 구할 때 suspend 함수를 이용하여 비동기 방식으로 구하고, 값을 조회할 때도 CoroutineScope 하에서 구합니다.
 *
 * ```
 * runBlocking {
 *      val lazyValue: SuspendLazy<Int> = suspendLazy {
 *          delay(100)
 *          42
 *      }
 *      val value:Int = lazyValue() // 42
 * }
 * ```
 *
 * @param T
 * @param context     값을 계산하는 블럭을 수행할 [CoroutineContext]
 * @param mode        [LazyThreadSafetyMode]
 * @param initializer 지연된 계산을 수행하는 함수
 * @return
 */
inline fun <T> CoroutineScope.suspendLazy(
    context: CoroutineContext = EmptyCoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    @BuilderInference crossinline initializer: suspend CoroutineScope.() -> T,
): SuspendLazy<T> {
    return SuspendLazySuspendingImpl(this, context, mode) { initializer() }
}

@PublishedApi
internal class SuspendLazySuspendingImpl<out T>(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    mode: LazyThreadSafetyMode = LazyThreadSafetyMode.SYNCHRONIZED,
    @BuilderInference initializer: suspend CoroutineScope.() -> T,
): SuspendLazy<T> {

    private val deferredValue by lazy(mode) {
        coroutineScope.async(coroutineContext, start = CoroutineStart.LAZY, block = initializer)
    }

    override suspend fun invoke(): T = deferredValue.await()
}
