package io.bluetape4k.batch.jdbc.tables

import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * [ResultRowMappers.kt]에 정의된 [ResultRow.toJobExecution] 및 [ResultRow.toStepExecution] 확장 함수를
 * H2 in-memory DB 기반 통합 테스트로 검증한다.
 *
 * 실제 테이블에 INSERT 후 [ResultRow]를 조회하여 매퍼를 호출하고, 변환 결과가 올바른지 확인한다.
 */
class ResultRowMappersTest : AbstractExposedTest() {

    companion object : KLogging()

    private val batchTables = arrayOf(BatchJobExecutionTable, BatchStepExecutionTable)
    private val checkpointJson = CheckpointJson.jackson3()

    /**
     * [ResultRow.toJobExecution] 매퍼가 nullable 컬럼(endTime, params)을 올바르게 null로 처리하는지 검증한다.
     *
     * - `params`를 null로 삽입하면 `emptyMap()`으로 반환되어야 한다.
     * - `endTime`을 삽입하지 않으면 null로 반환되어야 한다.
     */
    @Test
    fun `BatchJobExecution 매퍼는 nullable column 을 정상 처리`() {
        withTables(TestDB.H2, *batchTables) {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            // params=null, endTime=null(기본값) 로 삽입
            val insertedId = BatchJobExecutionTable.insertAndGetId {
                it[jobName] = "nullableJob"
                it[paramsHash] = null
                it[status] = BatchStatus.RUNNING
                it[params] = null
                it[startTime] = now
                // endTime 은 nullable — 명시적으로 설정하지 않으면 null
            }

            val row = BatchJobExecutionTable
                .selectAll()
                .where { BatchJobExecutionTable.id eq insertedId }
                .single()

            val jobExecution = row.toJobExecution(checkpointJson)

            jobExecution.shouldNotBeNull()
            jobExecution.id shouldBeEqualTo insertedId.value
            jobExecution.jobName shouldBeEqualTo "nullableJob"
            jobExecution.status shouldBeEqualTo BatchStatus.RUNNING
            jobExecution.startTime shouldBeEqualTo now
            jobExecution.endTime.shouldBeNull()
            // params null → emptyMap() 반환
            jobExecution.params shouldBeEqualTo emptyMap()
        }
    }

    /**
     * [ResultRow.toStepExecution] 매퍼가 모든 필드를 정확히 매핑하는지 검증한다.
     *
     * 모든 필드(readCount, writeCount, skipCount, checkpoint, endTime 포함)에 값을 설정하고
     * 조회 후 매퍼 결과가 삽입한 값과 일치하는지 확인한다.
     */
    @Test
    fun `BatchStepExecution 매퍼는 모든 필드를 정확히 매핑`() {
        withTables(TestDB.H2, *batchTables) {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            val stepEndTime = now.plusSeconds(60)

            // Job 먼저 삽입 (FK 참조)
            val jobId = BatchJobExecutionTable.insertAndGetId {
                it[jobName] = "allFieldsJob"
                it[paramsHash] = null
                it[status] = BatchStatus.RUNNING
                it[params] = null
                it[startTime] = now
            }

            // checkpoint 직렬화
            val checkpointValue = 999L
            val checkpointJsonStr = checkpointJson.write(checkpointValue)

            val stepId = BatchStepExecutionTable.insertAndGetId {
                it[jobExecutionId] = jobId
                it[stepName] = "allFieldsStep"
                it[status] = BatchStatus.COMPLETED
                it[readCount] = 200L
                it[writeCount] = 190L
                it[skipCount] = 10L
                it[checkpoint] = checkpointJsonStr
                it[startTime] = now
                it[endTime] = stepEndTime
            }

            val row = BatchStepExecutionTable
                .selectAll()
                .where { BatchStepExecutionTable.id eq stepId }
                .single()

            val stepExecution = row.toStepExecution(checkpointJson)

            stepExecution.shouldNotBeNull()
            stepExecution.id shouldBeEqualTo stepId.value
            stepExecution.jobExecutionId shouldBeEqualTo jobId.value
            stepExecution.stepName shouldBeEqualTo "allFieldsStep"
            stepExecution.status shouldBeEqualTo BatchStatus.COMPLETED
            stepExecution.readCount shouldBeEqualTo 200L
            stepExecution.writeCount shouldBeEqualTo 190L
            stepExecution.skipCount shouldBeEqualTo 10L
            stepExecution.startTime shouldBeEqualTo now
            stepExecution.endTime shouldBeEqualTo stepEndTime
            // checkpoint round-trip 검증
            stepExecution.checkpoint.shouldNotBeNull()
            (stepExecution.checkpoint as Long) shouldBeEqualTo checkpointValue
        }
    }

    /**
     * Job + Step 연결 상태에서 [ResultRow.toJobExecution]과 [ResultRow.toStepExecution] 매퍼를
     * 체인으로 검증한다.
     *
     * - params를 설정한 Job 실행 후 매퍼 적용 시 params가 정상 복원되어야 한다.
     * - Step의 checkpoint=null 시 null로 반환되어야 한다.
     * - Job과 Step의 ID 연결(jobExecutionId)이 올바른지 확인한다.
     */
    @Test
    fun `매퍼 함수는 실제 테이블 insert-select 라운드트립을 거친 데이터 정상 처리`() {
        withTables(TestDB.H2, *batchTables) {
            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            // params 설정하여 Job 삽입
            val paramsMap = mapOf<String, Any>("region" to "KR", "date" to "2026-04-26")
            val paramsJson = checkpointJson.write(paramsMap)

            val jobId = BatchJobExecutionTable.insertAndGetId {
                it[jobName] = "chainJob"
                it[paramsHash] = paramsMap.toParamsHash()
                it[status] = BatchStatus.RUNNING
                it[params] = paramsJson
                it[startTime] = now
            }

            // Step 삽입 (checkpoint=null)
            val stepId = BatchStepExecutionTable.insertAndGetId {
                it[jobExecutionId] = jobId
                it[stepName] = "chainStep"
                it[status] = BatchStatus.RUNNING
                it[readCount] = 0L
                it[writeCount] = 0L
                it[skipCount] = 0L
                it[startTime] = now
                // checkpoint, endTime 은 nullable — 설정하지 않으면 null
            }

            // Job 매퍼 검증
            val jobRow = BatchJobExecutionTable
                .selectAll()
                .where { BatchJobExecutionTable.id eq jobId }
                .single()

            val jobExecution = jobRow.toJobExecution(checkpointJson)
            jobExecution.id shouldBeEqualTo jobId.value
            jobExecution.jobName shouldBeEqualTo "chainJob"
            jobExecution.status shouldBeEqualTo BatchStatus.RUNNING
            // params 복원 검증 (키가 포함되어 있어야 함)
            jobExecution.params.shouldNotBeNull()
            jobExecution.params.containsKey("region") shouldBeEqualTo true
            jobExecution.params.containsKey("date") shouldBeEqualTo true

            // Step 매퍼 검증
            val stepRow = BatchStepExecutionTable
                .selectAll()
                .where { BatchStepExecutionTable.id eq stepId }
                .single()

            val stepExecution = stepRow.toStepExecution(checkpointJson)
            stepExecution.id shouldBeEqualTo stepId.value
            stepExecution.jobExecutionId shouldBeEqualTo jobId.value
            stepExecution.stepName shouldBeEqualTo "chainStep"
            stepExecution.status shouldBeEqualTo BatchStatus.RUNNING
            // checkpoint=null → null 반환
            stepExecution.checkpoint.shouldBeNull()
            stepExecution.endTime.shouldBeNull()
        }
    }
}
