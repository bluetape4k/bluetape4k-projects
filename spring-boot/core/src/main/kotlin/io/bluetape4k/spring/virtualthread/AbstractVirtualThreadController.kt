package io.bluetape4k.spring.virtualthread

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Virtual Thread Executor를 제공하는 컨트롤러 베이스 클래스입니다.
 *
 * Spring Boot 4에서는 기본으로 VT가 활성화되므로,
 * 명시적 VT Executor가 필요한 경우에만 이 클래스를 상속합니다.
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
    companion object {
        /**
         * Virtual Thread Per Task Executor.
         *
         * ## 동작/계약
         * - `Executors.newVirtualThreadPerTaskExecutor()`로 생성됩니다.
         * - 각 작업마다 새 가상 스레드를 할당합니다.
         *
         * ```kotlin
         * val future = AbstractVirtualThreadController.virtualThreadExecutor.submit { "done" }
         * // future.get() == "done"
         * ```
         */
        val virtualThreadExecutor: ExecutorService =
            Executors.newVirtualThreadPerTaskExecutor()
    }
}
