package io.bluetape4k.spring.virtualthread

import jakarta.annotation.PreDestroy
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * virtual-thread-per-task executor를 노출하는 base controller입니다.
 *
 * controller가 explicit
 * [ExecutorService] for virtual-thread task submission. Spring calls
 * [closeVirtualThreadExecutor] when the controller bean is destroyed, and the
 * shared executor is recreated on the next access if a later context needs it.
 *
 * ```kotlin
 * @RestController
 * class MyController: AbstractVirtualThreadController() {
 *     @GetMapping("/hello")
 *     fun hello(): String = "hello"
 * }
 * // MyController.virtualThreadExecutor != null
 * ```
 */
abstract class AbstractVirtualThreadController {

    @PreDestroy
    fun closeVirtualThreadExecutor() {
        shutdownVirtualThreadExecutor()
    }

    companion object {
        private val executorRef = AtomicReference(newVirtualThreadExecutor())

        /**
         * Virtual-thread-per-task executor.
         *
         * ## Contract
         * - Created by [Executors.newVirtualThreadPerTaskExecutor].
         * - Allocates a new virtual thread for each submitted task.
         * - If a Spring context shutdown closed the previous executor, the next
         *   access returns a fresh executor instead of a closed instance.
         *
         * ```kotlin
         * val future = AbstractVirtualThreadController.virtualThreadExecutor.submit { "done" }
         * // future.get() == "done"
         * ```
         */
        val virtualThreadExecutor: ExecutorService
            get() = getOrCreateVirtualThreadExecutor()

        private fun newVirtualThreadExecutor(): ExecutorService =
            Executors.newVirtualThreadPerTaskExecutor()

        private fun getOrCreateVirtualThreadExecutor(): ExecutorService {
            while (true) {
                val current = executorRef.get()
                if (!current.isShutdown && !current.isTerminated) {
                    return current
                }

                val replacement = newVirtualThreadExecutor()
                if (executorRef.compareAndSet(current, replacement)) {
                    return replacement
                }
                replacement.shutdown()
            }
        }

        internal fun shutdownVirtualThreadExecutor() {
            executorRef.get().shutdown()
        }
    }
}
