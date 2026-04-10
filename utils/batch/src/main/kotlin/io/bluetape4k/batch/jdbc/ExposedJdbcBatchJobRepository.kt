package io.bluetape4k.batch.jdbc

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.StepExecution
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.batch.internal.CheckpointJson
import io.bluetape4k.batch.jdbc.tables.BatchJobExecutionTable
import io.bluetape4k.batch.jdbc.tables.BatchStepExecutionTable
import io.bluetape4k.batch.jdbc.tables.toJobExecution
import io.bluetape4k.batch.jdbc.tables.toParamsHash
import io.bluetape4k.batch.jdbc.tables.toStepExecution
import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException
import java.time.Instant

/**
 * Exposed JDBC 기반 [BatchJobRepository] 구현.
 *
 * ## 동시성 안전
 * `(job_name, params_hash)` partial unique index가 동시 INSERT 경쟁을 방지한다.
 * UniqueConstraint 위반 시 catch 후 재조회(catch-and-retry)한다.
 *
 * ## Dispatchers.VT
 * 모든 DB 호출은 `withContext(Dispatchers.VT) { transaction(database) { } }`로 감싼다.
 * `runBlocking`이나 `newVirtualThreadJdbcTransaction`은 사용하지 않는다.
 *
 * ## 사용 예
 * ```kotlin
 * val repository = ExposedJdbcBatchJobRepository(database, CheckpointJson.jackson3())
 * val jobExecution = repository.findOrCreateJobExecution(
 *     jobName = "importOrders",
 *     params = mapOf("date" to "2026-04-10"),
 * )
 * ```
 *
 * @param database Exposed JDBC [Database]
 * @param checkpointJson Checkpoint 직렬화 전략 — 기본값 없음 (P1-B: toString() fallback 금지)
 */
class ExposedJdbcBatchJobRepository(
    private val database: Database,
    private val checkpointJson: CheckpointJson,
): BatchJobRepository {

    companion object: KLoggingChannel()

    /**
     * jobName + params 조합의 재시작 대상 [JobExecution]을 조회하거나 신규 생성한다.
     *
     * 1. RUNNING/FAILED/STOPPED 상태의 기존 실행을 조회 → 존재하면 RUNNING으로 복원하여 반환
     * 2. 없으면 신규 INSERT → UniqueConstraint 충돌 시 catch 후 재조회로 복원
     */
    override suspend fun findOrCreateJobExecution(
        jobName: String,
        params: Map<String, Any>,
    ): JobExecution {
        jobName.requireNotBlank("jobName")
        val hash = params.toParamsHash()

        // 1. 재시작 대상 조회
        val existing = withContext(Dispatchers.VT) {
            transaction(database) {
                BatchJobExecutionTable.selectAll()
                    .where {
                        (BatchJobExecutionTable.jobName eq jobName) and
                                (BatchJobExecutionTable.paramsHash eq hash) and
                                (
                                    BatchJobExecutionTable.status inList listOf(
                                        BatchStatus.RUNNING,
                                        BatchStatus.FAILED,
                                        BatchStatus.STOPPED,
                                    )
                                )
                    }
                    .orderBy(BatchJobExecutionTable.id, SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()
                    ?.toJobExecution(checkpointJson)
            }
        }

        if (existing != null) {
            log.debug { "기존 JobExecution 재사용: jobName=$jobName, id=${existing.id}, status=${existing.status}" }
            if (existing.status != BatchStatus.RUNNING) {
                withContext(Dispatchers.VT) {
                    transaction(database) {
                        BatchJobExecutionTable.update({ BatchJobExecutionTable.id eq existing.id }) { row ->
                            row[BatchJobExecutionTable.status] = BatchStatus.RUNNING
                        }
                    }
                }
            }
            return existing.copy(status = BatchStatus.RUNNING)
        }

        // 2. 신규 생성 — UniqueViolation 시 재조회
        return try {
            withContext(Dispatchers.VT) {
                transaction(database) {
                    val now = Instant.now()
                    val newId = BatchJobExecutionTable.insertAndGetId { row ->
                        row[BatchJobExecutionTable.jobName] = jobName
                        row[BatchJobExecutionTable.paramsHash] = hash
                        row[BatchJobExecutionTable.status] = BatchStatus.RUNNING
                        row[BatchJobExecutionTable.params] =
                            if (params.isEmpty()) null else checkpointJson.write(params)
                        row[BatchJobExecutionTable.startTime] = now
                    }
                    log.debug { "신규 JobExecution 생성: jobName=$jobName, id=${newId.value}" }
                    JobExecution(
                        id = newId.value,
                        jobName = jobName,
                        params = params,
                        status = BatchStatus.RUNNING,
                        startTime = now,
                    )
                }
            }
        } catch (e: Exception) {
            val sqle = e.findSqlException()
            if (sqle == null || !sqle.isUniqueViolation()) throw e
            log.debug(e) { "동시 INSERT 감지 — job=$jobName, 재조회" }
            withContext(Dispatchers.VT) {
                transaction(database) {
                    BatchJobExecutionTable.selectAll()
                        .where {
                            (BatchJobExecutionTable.jobName eq jobName) and
                                    (BatchJobExecutionTable.paramsHash eq hash) and
                                    (
                                        BatchJobExecutionTable.status inList listOf(
                                            BatchStatus.RUNNING,
                                            BatchStatus.FAILED,
                                            BatchStatus.STOPPED,
                                        )
                                    )
                        }
                        .orderBy(BatchJobExecutionTable.id, SortOrder.DESC)
                        .limit(1)
                        .first()
                        .toJobExecution(checkpointJson)
                }
            }
        }
    }

    override suspend fun completeJobExecution(execution: JobExecution, status: BatchStatus) {
        withContext(Dispatchers.VT) {
            transaction(database) {
                BatchJobExecutionTable.update({ BatchJobExecutionTable.id eq execution.id }) { row ->
                    row[BatchJobExecutionTable.status] = status
                    row[BatchJobExecutionTable.endTime] = Instant.now()
                }
            }
        }
        log.debug { "JobExecution 완료: id=${execution.id}, status=$status" }
    }

    /**
     * jobExecution + stepName 의 [StepExecution]을 조회하거나 신규 생성한다.
     *
     * | 기존 status              | 동작                               |
     * |--------------------------|-----------------------------------|
     * | COMPLETED                | 변경 없이 그대로 반환 (runner skip) |
     * | COMPLETED_WITH_SKIPS     | 변경 없이 그대로 반환 (runner skip) |
     * | FAILED / STOPPED         | RUNNING으로 UPDATE 후 copy(status=RUNNING) 반환 |
     * | RUNNING                  | UPDATE 없이 copy(status=RUNNING) 반환 |
     * | 없음                     | 신규 INSERT (status=RUNNING)       |
     */
    override suspend fun findOrCreateStepExecution(
        jobExecution: JobExecution,
        stepName: String,
    ): StepExecution {
        stepName.requireNotBlank("stepName")

        val existing = withContext(Dispatchers.VT) {
            transaction(database) {
                BatchStepExecutionTable.selectAll()
                    .where {
                        (BatchStepExecutionTable.jobExecutionId eq jobExecution.id) and
                                (BatchStepExecutionTable.stepName eq stepName)
                    }
                    .limit(1)
                    .firstOrNull()
                    ?.toStepExecution(checkpointJson)
            }
        }

        if (existing != null) {
            return when (existing.status) {
                BatchStatus.COMPLETED,
                BatchStatus.COMPLETED_WITH_SKIPS,
                    -> {
                    log.debug { "StepExecution skip (이미 완료): stepName=$stepName, status=${existing.status}" }
                    existing
                }

                BatchStatus.FAILED,
                BatchStatus.STOPPED,
                    -> {
                    log.debug { "StepExecution 재시작: stepName=$stepName, 이전 status=${existing.status}" }
                    withContext(Dispatchers.VT) {
                        transaction(database) {
                            BatchStepExecutionTable.update({ BatchStepExecutionTable.id eq existing.id }) { row ->
                                row[BatchStepExecutionTable.status] = BatchStatus.RUNNING
                            }
                        }
                    }
                    existing.copy(status = BatchStatus.RUNNING)
                }

                BatchStatus.RUNNING -> existing

                else -> {
                    log.debug { "StepExecution 예상치 못한 상태: stepName=$stepName, status=${existing.status}" }
                    existing
                }
            }
        }

        return withContext(Dispatchers.VT) {
            transaction(database) {
                val now = Instant.now()
                val newId = BatchStepExecutionTable.insertAndGetId { row ->
                    row[BatchStepExecutionTable.jobExecutionId] = jobExecution.id
                    row[BatchStepExecutionTable.stepName] = stepName
                    row[BatchStepExecutionTable.status] = BatchStatus.RUNNING
                    row[BatchStepExecutionTable.startTime] = now
                }
                log.debug { "신규 StepExecution 생성: stepName=$stepName, id=${newId.value}" }
                StepExecution(
                    id = newId.value,
                    jobExecutionId = jobExecution.id,
                    stepName = stepName,
                    status = BatchStatus.RUNNING,
                    startTime = now,
                )
            }
        }
    }

    override suspend fun completeStepExecution(execution: StepExecution, report: StepReport) {
        withContext(Dispatchers.VT) {
            transaction(database) {
                BatchStepExecutionTable.update({ BatchStepExecutionTable.id eq execution.id }) { row ->
                    row[BatchStepExecutionTable.status] = report.status
                    row[BatchStepExecutionTable.readCount] = report.readCount
                    row[BatchStepExecutionTable.writeCount] = report.writeCount
                    row[BatchStepExecutionTable.skipCount] = report.skipCount
                    row[BatchStepExecutionTable.checkpoint] =
                        report.checkpoint?.let { checkpointJson.write(it) }
                    row[BatchStepExecutionTable.endTime] = Instant.now()
                }
            }
        }
        log.debug {
            "StepExecution 완료: id=${execution.id}, stepName=${execution.stepName}, " +
                    "status=${report.status}, read=${report.readCount}, write=${report.writeCount}, skip=${report.skipCount}"
        }
    }

    override suspend fun saveCheckpoint(stepExecutionId: Long, checkpoint: Any) {
        val json = checkpointJson.write(checkpoint)
        withContext(Dispatchers.VT) {
            transaction(database) {
                BatchStepExecutionTable.update({ BatchStepExecutionTable.id eq stepExecutionId }) { row ->
                    row[BatchStepExecutionTable.checkpoint] = json
                }
            }
        }
        log.debug { "체크포인트 저장: stepExecutionId=$stepExecutionId" }
    }

    override suspend fun loadCheckpoint(stepExecutionId: Long): Any? {
        val result = withContext(Dispatchers.VT) {
            transaction(database) {
                BatchStepExecutionTable.selectAll()
                    .where { BatchStepExecutionTable.id eq stepExecutionId }
                    .limit(1)
                    .firstOrNull()
                    ?.let { it[BatchStepExecutionTable.checkpoint] }
                    ?.let { checkpointJson.read(it) }
            }
        }
        log.debug { "체크포인트 조회: stepExecutionId=$stepExecutionId, exists=${result != null}" }
        return result
    }

    /**
     * 예외 체인을 따라 최초 [SQLException]을 찾는다.
     * Exposed는 발생한 [SQLException]을 자체 `ExposedSQLException`으로 래핑하므로
     * unique violation 판정은 원본 [SQLException]에서 수행해야 한다.
     */
    private fun Throwable.findSqlException(): SQLException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is SQLException) return current
            current = current.cause
        }
        return null
    }

    /**
     * 대표 DB의 unique constraint violation 판정.
     * - PostgreSQL: SQLState `23505`
     * - MySQL/MariaDB: errorCode `1062`
     * - H2/기타: message에 "unique" 포함
     */
    private fun SQLException.isUniqueViolation(): Boolean =
        sqlState == "23505" ||
                errorCode == 1062 ||
                message?.contains("unique", ignoreCase = true) == true
}
