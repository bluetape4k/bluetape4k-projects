package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import kotlin.time.Duration

/**
 * Step 실행 결과 보고서.
 *
 * Step 완료 후 실행 통계와 결과 상태를 담는다.
 * [BatchReport]의 [BatchReport.stepReports]에 포함된다.
 *
 * ```kotlin
 * val report = StepReport(
 *     stepName = "importStep",
 *     status = BatchStatus.COMPLETED_WITH_SKIPS,
 *     readCount = 1000L,
 *     writeCount = 990L,
 *     skipCount = 10L,
 *     duration = 5.seconds,
 * )
 * ```
 *
 * @property stepName Step 이름
 * @property status 최종 실행 상태
 * @property readCount 읽은 아이템 수
 * @property writeCount 저장한 아이템 수
 * @property skipCount skip된 아이템 수
 * @property duration Step 실행 소요 시간
 * @property checkpoint 마지막 성공 체크포인트
 * @property error Step 실패 원인 예외 (성공 시 null)
 */
data class StepReport(
    val stepName: String,
    val status: BatchStatus,
    val readCount: Long = 0L,
    val writeCount: Long = 0L,
    val skipCount: Long = 0L,
    val duration: Duration = Duration.ZERO,
    val checkpoint: Any? = null,
    val error: Throwable? = null,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
