@file:Suppress("UnusedReceiverParameter")

package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ExecutorService

/**
 * Kotlin Coroutines에서 Virtual Thread를 사용하기 위한 공유 [CoroutineDispatcher]입니다.
 *
 * 내부적으로 단일 [ExecutorService] 인스턴스를 재사용합니다.
 * 다중 코루틴에서 공유할 때 적합하며 매번 새 Executor를 생성하지 않아도 됩니다.
 *
 * ```kotlin
 * runBlocking {
 *     val result = withContext(Dispatchers.VT) {
 *         // Virtual Thread 위에서 실행
 *         Thread.currentThread().isVirtual  // true
 *         42
 *     }
 *     println(result) // 42
 * }
 * ```
 */
val Dispatchers.VT: CoroutineDispatcher
    get() = VirtualThreadExecutor.asCoroutineDispatcher()

/**
 * Kotlin Coroutines에서 Virtual Thread를 사용하기 위한 새 [CoroutineDispatcher]를 생성합니다.
 *
 * 매 호출마다 새로운 Virtual Thread 기반 [ExecutorService]를 생성하여 Dispatcher로 반환합니다.
 * 생성된 Dispatcher는 JVM 종료 시 [ShutdownQueue]를 통해 자동으로 정리됩니다.
 *
 * 여러 독립적인 실행 컨텍스트가 필요할 때 사용하세요. 재사용하려면 반환값을 변수에 저장하세요.
 *
 * ```kotlin
 * val dispatcher = Dispatchers.newVT()
 *
 * runBlocking {
 *     val result = withContext(dispatcher) {
 *         // 새로 생성된 Virtual Thread Executor 위에서 실행
 *         Thread.currentThread().isVirtual  // true
 *         "hello"
 *     }
 *     println(result) // "hello"
 * }
 * ```
 *
 * 매 호출마다 새로운 Virtual Thread 기반 Dispatcher를 생성합니다. 재사용하려면 변수에 저장하세요.
 */
fun Dispatchers.newVT(): CoroutineDispatcher =
    VirtualThreads.executorService().asCoroutineDispatcher().apply { ShutdownQueue.register(this) }
