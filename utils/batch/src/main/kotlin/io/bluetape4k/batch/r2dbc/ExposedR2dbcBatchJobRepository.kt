package io.bluetape4k.batch.r2dbc

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
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import java.time.Instant

/**
 * Exposed R2DBC 기반 [BatchJobRepository] 구현 — 네이티브 suspend.
 *
 * 모든 DB 접근은 `suspendTransaction(db = database) { ... }`로 감싼다.
 * `Dispatchers.VT` / `withContext`가 필요 없다 (R2DBC는 네이티브 suspend).
 *
 * ## 재시작 시나리오
 * ```
 * findOrCreateJobExecution("importOrders", params)
 *   → RUNNING/FAILED/STOPPED 기존 실행 재사용, or 신규 INSERT
 *   → 동시 INSERT 충돌 시 UniqueViolation catch → 재조회
 * ```
 *
 * @param database Exposed R2DBC Database
 * @param checkpointJson Checkpoint 직렬화 전략 (CheckpointJson.jackson3() 등)
 */
class ExposedR2dbcBatchJobRepository(
    private val database: R2dbcDatabase,
    private val checkpointJson: CheckpointJson,
) : BatchJobRepository {

    companion object : KLoggingChannel()

    override suspend fun findOrCreateJobExecution(
        jobName: String,
        params: Map<String, Any>,
    ): JobExecution {
        jobName.requireNotBlank("jobName")
        val hash = params.toParamsHash()

        val existing = suspendTransaction(db = database) {
            BatchJobExecutionTable.selectAll()
                .where {
                    (BatchJobExecutionTable.jobName eq jobName) and
                        (BatchJobExecutionTable.paramsHash eq hash) and
                        (BatchJobExecutionTable.status inList listOf(
                            BatchStatus.RUNNING, BatchStatus.FAILED, BatchStatus.STOPPED,
                        ))
                }
                .orderBy(BatchJobExecutionTable.id, SortOrder.DESC)
                .limit(1)
                .map { it.toJobExecution(checkpointJson) }
                .firstOrNull()
        }

        if (existing != null) {
            if (existing.status != BatchStatus.RUNNING) {
                suspendTransaction(db = database) {
                    BatchJobExecutionTable.update({ BatchJobExecutionTable.id eq existing.id }) { row ->
                        row[BatchJobExecutionTable.status] = BatchStatus.RUNNING
                    }
                }
            }
            return existing.copy(status = BatchStatus.RUNNING)
        }

        return try {
            suspendTransaction(db = database) {
                val now = Instant.now()
                val newId = BatchJobExecutionTable.insertAndGetId { row ->
                    row[BatchJobExecutionTable.jobName] = jobName
                    row[BatchJobExecutionTable.paramsHash] = hash  // "" for empty params (consistent with SELECT eq hash)
                    row[BatchJobExecutionTable.status] = BatchStatus.RUNNING
                    row[BatchJobExecutionTable.params] = if (params.isEmpty()) null
                    else checkpointJson.write(params)
                    row[BatchJobExecutionTable.startTime] = now
                }
                JobExecution(
                    id = newId.value,
                    jobName = jobName,
                    params = params,
                    status = BatchStatus.RUNNING,
                    startTime = now,
                )
            }
        } catch (e: Throwable) {
            if (!e.isUniqueViolation()) throw e
            log.debug(e) { "동시 INSERT 감지 — job=$jobName, 재조회" }
            suspendTransaction(db = database) {
                BatchJobExecutionTable.selectAll()
                    .where {
                        (BatchJobExecutionTable.jobName eq jobName) and
                            (BatchJobExecutionTable.paramsHash eq hash) and
                            (BatchJobExecutionTable.status inList listOf(
                                BatchStatus.RUNNING, BatchStatus.FAILED, BatchStatus.STOPPED,
                            ))
                    }
                    .orderBy(BatchJobExecutionTable.id, SortOrder.DESC)
                    .limit(1)
                    .map { it.toJobExecution(checkpointJson) }
                    .firstOrNull()!!
            }
        }
    }

    override suspend fun completeJobExecution(execution: JobExecution, status: BatchStatus) {
        suspendTransaction(db = database) {
            BatchJobExecutionTable.update({ BatchJobExecutionTable.id eq execution.id }) { row ->
                row[BatchJobExecutionTable.status] = status
                row[BatchJobExecutionTable.endTime] = Instant.now()
            }
        }
    }

    override suspend fun findOrCreateStepExecution(
        jobExecution: JobExecution,
        stepName: String,
    ): StepExecution {
        stepName.requireNotBlank("stepName")

        val existing = suspendTransaction(db = database) {
            BatchStepExecutionTable.selectAll()
                .where {
                    (BatchStepExecutionTable.jobExecutionId eq jobExecution.id) and
                        (BatchStepExecutionTable.stepName eq stepName)
                }
                .limit(1)
                .map { it.toStepExecution(checkpointJson) }
                .firstOrNull()
        }

        if (existing != null) {
            return when (existing.status) {
                BatchStatus.COMPLETED, BatchStatus.COMPLETED_WITH_SKIPS -> existing
                BatchStatus.FAILED, BatchStatus.STOPPED, BatchStatus.RUNNING -> {
                    if (existing.status != BatchStatus.RUNNING) {
                        suspendTransaction(db = database) {
                            BatchStepExecutionTable.update({ BatchStepExecutionTable.id eq existing.id }) { row ->
                                row[BatchStepExecutionTable.status] = BatchStatus.RUNNING
                            }
                        }
                    }
                    existing.copy(status = BatchStatus.RUNNING)
                }

                else -> existing
            }
        }

        return suspendTransaction(db = database) {
            val now = Instant.now()
            val newId = BatchStepExecutionTable.insertAndGetId { row ->
                row[BatchStepExecutionTable.jobExecutionId] = jobExecution.id
                row[BatchStepExecutionTable.stepName] = stepName
                row[BatchStepExecutionTable.status] = BatchStatus.RUNNING
                row[BatchStepExecutionTable.startTime] = now
            }
            StepExecution(
                id = newId.value,
                jobExecutionId = jobExecution.id,
                stepName = stepName,
                status = BatchStatus.RUNNING,
                startTime = now,
            )
        }
    }

    override suspend fun completeStepExecution(execution: StepExecution, report: StepReport) {
        suspendTransaction(db = database) {
            BatchStepExecutionTable.update({ BatchStepExecutionTable.id eq execution.id }) { row ->
                row[BatchStepExecutionTable.status] = report.status
                row[BatchStepExecutionTable.readCount] = report.readCount
                row[BatchStepExecutionTable.writeCount] = report.writeCount
                row[BatchStepExecutionTable.skipCount] = report.skipCount
                row[BatchStepExecutionTable.checkpoint] = report.checkpoint?.let { checkpointJson.write(it) }
                row[BatchStepExecutionTable.endTime] = Instant.now()
            }
        }
    }

    override suspend fun saveCheckpoint(stepExecutionId: Long, checkpoint: Any) {
        suspendTransaction(db = database) {
            BatchStepExecutionTable.update({ BatchStepExecutionTable.id eq stepExecutionId }) { row ->
                row[BatchStepExecutionTable.checkpoint] = checkpointJson.write(checkpoint)
            }
        }
    }

    override suspend fun loadCheckpoint(stepExecutionId: Long): Any? {
        return suspendTransaction(db = database) {
            BatchStepExecutionTable.selectAll()
                .where { BatchStepExecutionTable.id eq stepExecutionId }
                .limit(1)
                .map { it[BatchStepExecutionTable.checkpoint] }
                .firstOrNull()
                ?.let { checkpointJson.read(it) }
        }
    }
}

/**
 * UniqueViolation 예외 여부를 판별한다.
 *
 * R2DBC 드라이버마다 예외 타입이 다르므로, 메시지 문자열로 판별한다.
 * - PostgreSQL: SQLSTATE 23505
 * - MySQL/MariaDB: SQLSTATE 23000, error code 1062
 * - H2: "unique" 포함 메시지
 */
private fun Throwable.isUniqueViolation(): Boolean {
    val msg = message ?: cause?.message ?: ""
    return msg.contains("unique", ignoreCase = true) ||
        msg.contains("23505") ||
        msg.contains("1062")
}
