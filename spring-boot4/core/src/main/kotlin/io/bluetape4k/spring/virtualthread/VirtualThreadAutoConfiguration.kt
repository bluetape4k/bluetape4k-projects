package io.bluetape4k.spring.virtualthread

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor

/**
 * Virtual Thread 기반 [AsyncTaskExecutor]를 자동 구성합니다.
 *
 * Spring Boot 4에서는 기본으로 Virtual Thread가 활성화되나,
 * 명시적 VT Executor Bean이 필요한 경우에 사용합니다.
 *
 * ```kotlin
 * @SpringBootApplication
 * @Import(VirtualThreadAutoConfiguration::class)
 * class MyApp
 * // virtualThreadTaskExecutor 빈이 자동 등록된다.
 * ```
 */
@Configuration
class VirtualThreadAutoConfiguration {
    /**
     * Virtual Thread 기반 [AsyncTaskExecutor] Bean을 생성합니다.
     *
     * ## 동작/계약
     * - [AsyncTaskExecutor] 빈이 없을 때만 등록됩니다.
     * - `SimpleAsyncTaskExecutor.setVirtualThreads(true)`를 적용해 가상 스레드를 사용합니다.
     *
     * ```kotlin
     * val executor = SimpleAsyncTaskExecutor().apply { setVirtualThreads(true) }
     * // executor.isVirtualThreads == true
     * ```
     */
    @Bean
    @ConditionalOnMissingBean
    fun virtualThreadTaskExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor().apply {
            setVirtualThreads(true)
        }
}
