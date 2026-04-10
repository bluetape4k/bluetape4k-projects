package io.bluetape4k.batch.jdbc.tables

import io.bluetape4k.batch.api.BatchStatus
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Step 실행 이력 테이블.
 *
 * `(job_execution_id, step_name)` unique constraint로 동일 Job 내 Step 중복 실행을 방지한다.
 *
 * ```sql
 * CREATE TABLE batch_step_execution (
 *     id                BIGSERIAL PRIMARY KEY,
 *     job_execution_id  BIGINT       NOT NULL REFERENCES batch_job_execution(id),
 *     step_name         VARCHAR(100) NOT NULL,
 *     status            VARCHAR(20)  NOT NULL,
 *     read_count        BIGINT       NOT NULL DEFAULT 0,
 *     write_count       BIGINT       NOT NULL DEFAULT 0,
 *     skip_count        BIGINT       NOT NULL DEFAULT 0,
 *     checkpoint        TEXT,
 *     start_time        TIMESTAMP    NOT NULL,
 *     end_time          TIMESTAMP,
 *     CONSTRAINT batch_step_exec_uidx UNIQUE (job_execution_id, step_name)
 * );
 * ```
 */
object BatchStepExecutionTable : LongIdTable("batch_step_execution") {
    /** 소속 Job 실행 ID — [BatchJobExecutionTable] FK */
    val jobExecutionId = reference("job_execution_id", BatchJobExecutionTable, onDelete = ReferenceOption.CASCADE)
        .index()

    /** Step 이름 */
    val stepName = varchar("step_name", 100)

    /** 현재 실행 상태 */
    val status = enumerationByName<BatchStatus>("status", 20)

    /** 읽은 아이템 수 */
    val readCount = long("read_count").default(0L)

    /** 저장한 아이템 수 */
    val writeCount = long("write_count").default(0L)

    /** skip된 아이템 수 */
    val skipCount = long("skip_count").default(0L)

    /** 마지막 커밋 체크포인트 JSON 문자열 (재시작 시 사용) */
    val checkpoint = text("checkpoint").nullable()

    /** 실행 시작 시각 (UTC) */
    val startTime = timestamp("start_time")

    /** 실행 종료 시각 (UTC), 실행 중이면 null */
    val endTime = timestamp("end_time").nullable()

    init {
        uniqueIndex(jobExecutionId, stepName)
    }
}
