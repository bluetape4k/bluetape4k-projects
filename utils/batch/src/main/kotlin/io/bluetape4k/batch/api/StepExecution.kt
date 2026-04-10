package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Instant

/**
 * Step 실행 컨텍스트. [BatchJobRepository]가 생성/관리한다.
 *
 * Job 내 개별 Step의 실행 이력을 나타낸다.
 * COMPLETED / COMPLETED_WITH_SKIPS 상태의 Step은 재시작 시 자동으로 skip된다.
 *
 * ```kotlin
 * val stepExecution = repository.findOrCreateStepExecution(jobExecution, "readAndWrite")
 * if (stepExecution.status == BatchStatus.COMPLETED) {
 *     // 이미 완료된 Step — skip
 *     return
 * }
 * ```
 *
 * @property id Step 실행 고유 ID
 * @property jobExecutionId 소속 Job 실행 ID
 * @property stepName Step 이름
 * @property status 현재 실행 상태
 * @property readCount 읽은 아이템 수
 * @property writeCount 저장한 아이템 수
 * @property skipCount skip된 아이템 수
 * @property checkpoint 마지막 커밋 체크포인트 (재시작 시 사용)
 * @property startTime 실행 시작 시각
 * @property endTime 실행 종료 시각 (실행 중이면 null)
 */
data class StepExecution(
    val id: Long,
    val jobExecutionId: Long,
    val stepName: String,
    val status: BatchStatus = BatchStatus.STARTING,
    val readCount: Long = 0L,
    val writeCount: Long = 0L,
    val skipCount: Long = 0L,
    val checkpoint: Any? = null,
    val startTime: Instant = Instant.now(),
    val endTime: Instant? = null,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
