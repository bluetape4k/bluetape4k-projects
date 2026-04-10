package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Instant

/**
 * Job 실행 컨텍스트. [BatchJobRepository]가 생성/관리한다.
 *
 * Job의 전체 실행 이력을 나타내며, 재시작 시 동일한 인스턴스를 재사용한다.
 *
 * ```kotlin
 * val jobExecution = repository.findOrCreateJobExecution(
 *     jobName = "importOrders",
 *     params = mapOf("date" to "2026-04-10")
 * )
 * println("Job ID: ${jobExecution.id}, Status: ${jobExecution.status}")
 * ```
 *
 * @property id Job 실행 고유 ID
 * @property jobName Job 이름
 * @property params Job 실행 파라미터
 * @property status 현재 실행 상태
 * @property startTime 실행 시작 시각
 * @property endTime 실행 종료 시각 (실행 중이면 null)
 */
data class JobExecution(
    val id: Long,
    val jobName: String,
    val params: Map<String, Any> = emptyMap(),
    val status: BatchStatus = BatchStatus.STARTING,
    val startTime: Instant = Instant.now(),
    val endTime: Instant? = null,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
