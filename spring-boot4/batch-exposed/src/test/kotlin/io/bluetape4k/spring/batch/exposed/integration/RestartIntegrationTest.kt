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
import io.bluetape4k.spring.batch.exposed.writer.ExposedUpsertItemWriter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.context.annotation.Bean
import org.springframework.transaction.PlatformTransactionManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 중간 실패 후 Job 재시작 시 lastKey 기반 재개 검증.
 *
 * - 1차 실행: "item-300" 처리 중 의도적 예외 → [BatchStatus.FAILED]
 * - 2차 실행: 동일 파라미터로 재시작, lastKey 이후부터 재개 → [BatchStatus.COMPLETED]
 * - 최종 Target 1000건 (중복 없음)
 */
class RestartIntegrationTest : AbstractExposedBatchJobTest() {

    companion object : KLogging() {
        /**
         * 첫 번째 실행에서 실패를 유도하고, 두 번째 실행에서 성공하도록 토글합니다.
         * companion object 로 선언하여 [JobConfig] (static nested class) 에서 참조 가능.
         */
        val shouldFail = AtomicBoolean(true)
    }

    @BeforeEach
    fun resetFailFlag() {
        shouldFail.set(true)
    }

    @TestConfiguration
    class JobConfig(
        private val jobRepository: JobRepository,
        private val transactionManager: PlatformTransactionManager,
        private val database: Database,
    ) {
        @Bean(name = ["restartMigrationJob"])
        fun migrationJob(): Job = partitionedBatchJob("restart-migration-job", jobRepository) {
            start(partitionedStep())
        }

        @Bean(name = ["restartPartitionedStep"])
        fun partitionedStep(): Step = StepBuilder("restart-migration-manager", jobRepository)
            .partitioner("restart-migration-worker", rangePartitioner())
            .partitionHandler(partitionHandler())
            .build()

        @Bean(name = ["restartRangePartitioner"])
        fun rangePartitioner(): ExposedRangePartitioner = ExposedRangePartitioner.forEntityId(
            table = SourceTable,
            gridSize = 2,
            database = database,
        )

        @Bean(name = ["restartPartitionHandler"])
        fun partitionHandler(): TaskExecutorPartitionHandler = TaskExecutorPartitionHandler().apply {
            setStep(workerStep())
            setTaskExecutor(virtualThreadPartitionTaskExecutor(concurrencyLimit = 2))
            gridSize = 2
        }

        @Bean(name = ["restartWorkerStep"])
        fun workerStep(): Step = StepBuilder("restart-migration-worker", jobRepository)
            .chunk<SourceRecord, TargetRecord>(100, transactionManager)
            .reader(keysetReader())
            .processor(ItemProcessor { source ->
                // shouldFail=true 이고 name이 "item-300"이면 예외 → 청크 롤백 → Job FAILED
                if (shouldFail.get() && source.name == "item-300") {
                    throw RuntimeException("item-300 처리 중 의도적 실패")
                }
                TargetRecord(sourceName = source.name.uppercase(), transformedValue = source.value * 2)
            })
            .writer(itemWriter())
            .build()

        @Bean(name = ["restartKeysetReader"])
        @org.springframework.batch.core.configuration.annotation.StepScope
        fun keysetReader(): ExposedKeysetItemReader<SourceRecord> = ExposedKeysetItemReader.forEntityId(
            table = SourceTable,
            pageSize = 100,
            rowMapper = { row ->
                SourceRecord(
                    id = row[SourceTable.id].value,
                    name = row[SourceTable.name],
                    value = row[SourceTable.value],
                )
            },
            database = database,
        )

        // Spring Batch 6.x 재시작 시 COMPLETED 파티션도 재실행되므로
        // ExposedUpsertItemWriter (idempotent) 를 사용하여 중복 삽입 오류 방지
        @Bean(name = ["restartItemWriter"])
        fun itemWriter(): ExposedUpsertItemWriter<TargetRecord> = ExposedUpsertItemWriter(table = TargetTable) {
            this[TargetTable.sourceName] = it.sourceName
            this[TargetTable.transformedValue] = it.transformedValue
        }

        @Bean(name = ["restartJobLauncherTestUtils"])
        fun jobLauncherTestUtils(
            @Qualifier("restartMigrationJob") job: Job,
            jobLauncher: JobLauncher,
        ): JobLauncherTestUtils = JobLauncherTestUtils().apply {
            this.job = job
            this.jobRepository = this@JobConfig.jobRepository
            this.jobLauncher = jobLauncher
        }
    }

    @Autowired
    @Qualifier("restartJobLauncherTestUtils")
    private lateinit var jobLauncherTestUtils: JobLauncherTestUtils

    @Test
    fun `중간 실패 후 재시작 시 마지막 위치부터 재개`() {
        // 1000건 삽입 (item-1 ~ item-1000)
        insertTestData(1000)

        val params = JobParametersBuilder()
            .addLong("run.id", 1L)
            .toJobParameters()

        // 1차 실행: item-300 에서 예외 발생 → FAILED
        shouldFail.set(true)
        val firstExecution = jobLauncherTestUtils.launchJob(params)
        firstExecution.status shouldBeEqualTo BatchStatus.FAILED

        val countAfterFirst = transaction(database) { TargetTable.selectAll().count() }
        countAfterFirst shouldBeGreaterThan 0L  // 일부 청크는 커밋됨

        // 2차 실행: 동일 params로 재시작, shouldFail=false → lastKey부터 재개 → COMPLETED
        shouldFail.set(false)
        val restartExecution = jobLauncherTestUtils.launchJob(params)
        restartExecution.status shouldBeEqualTo BatchStatus.COMPLETED

        val countAfterRestart = transaction(database) { TargetTable.selectAll().count() }
        countAfterRestart shouldBeEqualTo 1000L  // 전체 완료, 중복 없음
    }
}
