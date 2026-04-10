package io.bluetape4k.spring.batch.exposed.config

import io.bluetape4k.logging.KLogging
import org.springframework.batch.core.job.Job
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor

/**
 * Spring Boot Auto-Configuration for Exposed Batch 컴포넌트.
 *
 * 주의: `@EnableBatchProcessing` 절대 사용 금지 -- Spring Boot 4.x auto-config 무력화됨.
 *
 * 권장 설정:
 * ```yaml
 * spring:
 *   batch:
 *     job:
 *       enabled: false  # 자동 실행 비활성화, 명시적 JobLauncher 사용 권장
 * ```
 */
@AutoConfiguration(after = [BatchAutoConfiguration::class])
@ConditionalOnClass(Job::class)
class ExposedBatchAutoConfiguration {

    companion object : KLogging()

    /**
     * 배치 파티션 실행용 VirtualThread TaskExecutor.
     * 사용자가 직접 빈을 등록하면 이 기본 빈은 생성되지 않음.
     */
    @Bean
    @ConditionalOnMissingBean(name = ["batchPartitionTaskExecutor"])
    fun batchPartitionTaskExecutor(): TaskExecutor =
        SimpleAsyncTaskExecutor("batch-partition-").apply {
            setVirtualThreads(true)
            setConcurrencyLimit(Runtime.getRuntime().availableProcessors() * 2)
        }
}
