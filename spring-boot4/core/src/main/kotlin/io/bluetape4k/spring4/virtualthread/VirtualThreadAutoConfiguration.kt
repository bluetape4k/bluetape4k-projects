package io.bluetape4k.spring4.virtualthread

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
 */
@Configuration
class VirtualThreadAutoConfiguration {
    /**
     * Virtual Thread 기반 [AsyncTaskExecutor] Bean을 생성합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun virtualThreadTaskExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor().apply {
            setVirtualThreads(true)
        }
}
