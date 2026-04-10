package io.bluetape4k.spring.batch.exposed.integration

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.batch.exposed.AbstractExposedBatchJobTest
import io.bluetape4k.spring.batch.exposed.SourceRecord
import io.bluetape4k.spring.batch.exposed.SourceTable
import io.bluetape4k.spring.batch.exposed.TargetRecord
import io.bluetape4k.spring.batch.exposed.TargetTable
import io.bluetape4k.spring.batch.exposed.dsl.partitionedBatchJob
import io.bluetape4k.spring.batch.exposed.insertTestData
import io.bluetape4k.spring.batch.exposed.partition.ExposedRangePartitioner
import io.bluetape4k.spring.batch.exposed.reader.ExposedKeysetItemReader
import io.bluetape4k.spring.batch.exposed.support.virtualThreadPartitionTaskExecutor
import io.bluetape4k.spring.batch.exposed.writer.ExposedItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.test.JobLauncherTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskExecutor
import org.springframework.transaction.PlatformTransactionManager

/**
 * [io.bluetape4k.spring.batch.exposed.config.ExposedBatchAutoConfiguration] 기반 통합 검증.
 *
 * - `@EnableBatchProcessing` 없이 Spring Boot 4.x Auto-Configuration으로 동작 확인
 * - `ExposedBatchAutoConfiguration`이 제공하는 `batchPartitionTaskExecutor` 빈 존재 확인
 * - 사용자 Job 빈과 AutoConfiguration 빈이 올바르게 조합되어 Job 실행 성공
 */
class EndToEndJobTest : AbstractExposedBatchJobTest() {

    companion object : KLogging()

    @TestConfiguration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean(name = ["e2eMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("e2e-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["e2ePartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("e2e-migration-manager", jobRepository)
            .partitioner("e2e-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["e2eRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 4,
            database = database,
        )

        @Bean(name = ["e2ePartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 4))
            gridSize = 4
        }

        @Bean(name = ["e2eWorkerStep"])
        fun workerStep(): Step = StepBuilder("e2e-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(500, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                TargetRecord(
                    sourceName = source.name.uppercase(),
                    transformedValue = source.value * 2,
                )
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["e2eKeysetReader"])
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 500,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        @Bean(name = ["e2eItemWriter"])
        fun itemWriter(): ExposedItemWriter<TargetRecord> = ExposedItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["e2eJobLauncherTestUtils"])
        fun jobLauncherTestUtils(
            @Qualifier("e2eMigrationJob") job: Job,
            jobLauncher: JobLauncher,
        ): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
            this.jobLauncher = jobLauncher
        }
    }

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    @Qualifier("e2eJobLauncherTestUtils")
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `AutoConfiguration batchPartitionTaskExecutor 빈 존재 확인`() {
        // ExposedBatchAutoConfiguration 이 등록한 기본 TaskExecutor 빈 검증
        val taskExecutor = applicationContext.getBean("batchPartitionTaskExecutor", TaskExecutor::class.java)
        taskExecutor.shouldNotBeNull()
    }

    @Test
    fun `AutoConfiguration으로 Job 실행 성공 검증`() {
        insertTestData(500)

        val params = JobParametersBuilder()
            .addLong("run.id", System.currentTimeMillis())
            .toJobParameters()

        val execution = jobLauncherTestUtils.launchJob(params)
        execution.status shouldBeEqualTo BatchStatus.COMPLETED

        val targetCount = transaction(database) { TargetTable.selectAll().count() }
        targetCount shouldBeEqualTo 500L
    }
}
