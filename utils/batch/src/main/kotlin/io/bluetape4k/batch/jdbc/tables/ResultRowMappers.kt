package io.bluetape4k.batch.jdbc.tables

import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.StepExecution
import io.bluetape4k.batch.internal.CheckpointJson
import org.jetbrains.exposed.v1.core.ResultRow

/**
 * Exposed [ResultRow]를 [JobExecution]으로 변환한다.
 *
 * JDBC와 R2DBC 모두 동일한 `org.jetbrains.exposed.v1.core.ResultRow`를 사용하므로
 * 이 파일 하나로 양쪽에서 공유한다.
 *
 * ```kotlin
 * val jobExecution = transaction {
 *     BatchJobExecutionTable
 *         .selectAll()
 *         .where { BatchJobExecutionTable.id eq executionId }
 *         .single()
 *         .toJobExecution(checkpointJson)
 * }
 * ```
 *
 * @param checkpointJson params 역직렬화에 사용 (params 컬럼이 JSON 문자열인 경우)
 */
@Suppress("UNCHECKED_CAST")
fun ResultRow.toJobExecution(checkpointJson: CheckpointJson): JobExecution = JobExecution(
    id = this[BatchJobExecutionTable.id].value,
    jobName = this[BatchJobExecutionTable.jobName],
    params = this[BatchJobExecutionTable.params]
        ?.let { checkpointJson.read(it) as? Map<String, Any> }
        ?: emptyMap(),
    status = this[BatchJobExecutionTable.status],
    startTime = this[BatchJobExecutionTable.startTime],
    endTime = this[BatchJobExecutionTable.endTime],
)

/**
 * Exposed [ResultRow]를 [StepExecution]으로 변환한다.
 *
 * checkpoint 컬럼(text)을 [CheckpointJson.read]로 역직렬화한다.
 *
 * ```kotlin
 * val stepExecution = transaction {
 *     BatchStepExecutionTable
 *         .selectAll()
 *         .where { BatchStepExecutionTable.id eq stepId }
 *         .single()
 *         .toStepExecution(checkpointJson)
 * }
 * ```
 *
 * @param checkpointJson checkpoint 역직렬화에 사용
 */
fun ResultRow.toStepExecution(checkpointJson: CheckpointJson): StepExecution = StepExecution(
    id = this[BatchStepExecutionTable.id].value,
    jobExecutionId = this[BatchStepExecutionTable.jobExecutionId].value,
    stepName = this[BatchStepExecutionTable.stepName],
    status = this[BatchStepExecutionTable.status],
    readCount = this[BatchStepExecutionTable.readCount],
    writeCount = this[BatchStepExecutionTable.writeCount],
    skipCount = this[BatchStepExecutionTable.skipCount],
    checkpoint = this[BatchStepExecutionTable.checkpoint]?.let { checkpointJson.read(it) },
    startTime = this[BatchStepExecutionTable.startTime],
    endTime = this[BatchStepExecutionTable.endTime],
)
