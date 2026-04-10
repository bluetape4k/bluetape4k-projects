package io.bluetape4k.batch.jdbc.tables

import io.bluetape4k.batch.api.BatchStatus
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.security.MessageDigest

/**
 * Job 실행 이력 테이블.
 *
 * ## 재시작 단일성
 * `(job_name, params_hash)` 조합으로 재시작 대상을 식별한다.
 * 권장 인덱스:
 * ```sql
 * CREATE UNIQUE INDEX batch_job_exec_active_uidx
 *   ON batch_job_execution(job_name, params_hash)
 *   WHERE status IN ('RUNNING', 'FAILED', 'STOPPED');
 * ```
 * 동시 INSERT 경쟁은 `UNIQUE constraint violation` catch 후 재조회로 처리한다.
 * `SELECT ... FOR UPDATE`는 사용하지 않는다 (빈 결과 시 무의미).
 */
object BatchJobExecutionTable : LongIdTable("batch_job_execution") {
    /** Job 이름 — 인덱스 포함 */
    val jobName = varchar("job_name", 100).index()

    /** Job 파라미터의 SHA-256 해시 (재시작 식별 키) */
    val paramsHash = varchar("params_hash", 64).nullable()

    /** 현재 실행 상태 */
    val status = enumerationByName<BatchStatus>("status", 20)

    /** Job 파라미터 JSON 문자열 */
    val params = text("params").nullable()

    /** 실행 시작 시각 (UTC) */
    val startTime = timestamp("start_time")

    /** 실행 종료 시각 (UTC), 실행 중이면 null */
    val endTime = timestamp("end_time").nullable()
}

/**
 * Job 파라미터 Map을 SHA-256 해시 문자열로 변환한다.
 *
 * 정렬된 `key=value` 문자열의 SHA-256 hex를 반환한다.
 * 빈 Map이면 빈 문자열을 반환한다.
 * stdlib만 사용하며 jackson3 의존성이 없다.
 *
 * ```kotlin
 * val hash = mapOf("date" to "2026-04-10", "region" to "KR").toParamsHash()
 * // → "date=2026-04-10&region=KR" 의 SHA-256 hex
 * ```
 */
internal fun Map<String, Any>.toParamsHash(): String {
    if (isEmpty()) return ""
    val sorted = entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }
    val digest = MessageDigest.getInstance("SHA-256").digest(sorted.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
