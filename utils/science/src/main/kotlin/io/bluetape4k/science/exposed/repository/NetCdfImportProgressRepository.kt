package io.bluetape4k.science.exposed.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.bluetape4k.science.exposed.NetCdfException
import io.bluetape4k.science.exposed.model.NetCdfImportProgress
import io.bluetape4k.science.exposed.model.NetCdfImportStatus
import io.bluetape4k.science.exposed.schema.NetCdfImportProgressTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

/**
 * [NetCdfImportProgressTable] 기반 JDBC Repository.
 *
 * heartbeat lease 패턴으로 NetCDF 변수 단위 import 동시성 제어 및 재개를 지원합니다.
 *
 * ## 핵심 메서드
 *
 * - [acquireLease] : 진입 시 raw SQL `INSERT ... ON CONFLICT DO UPDATE WHERE` 로 lease 획득.
 *   - 기존 row 가 `COMPLETED` 면 그대로 반환 (호출자가 no-op 분기)
 *   - `IN_PROGRESS` 이고 lease 유효 → [NetCdfException.ImportAlreadyRunning] throw
 *   - `PENDING` / `FAILED` / 만료된 `IN_PROGRESS` → lease 획득 후 갱신된 row 반환
 * - [renewLease] : 슬라이스 commit 시 호출, owner token 검증 후 `lastSliceIdx` + `leaseExpiresAt` 갱신
 * - [markCompleted] : owner token 검증 후 `status=COMPLETED` + `leaseExpiresAt=null` + `completedAt=now()`
 * - [markFailed] : owner token 검증 후 `status=FAILED` + `leaseExpiresAt=null` + `errorMessage` 기록
 *
 * @see io.bluetape4k.science.exposed.service.NetCdfCatalogService
 */
class NetCdfImportProgressRepository {

    companion object: KLogging() {
        /**
         * heartbeat lease 기본 TTL — 5분.
         *
         * 슬라이스 10개 또는 30초마다 [renewLease] 호출로 갱신 권장.
         */
        val DEFAULT_LEASE_TTL: Duration = Duration.ofMinutes(5)
    }

    val table = NetCdfImportProgressTable

    fun ResultRow.toEntity(): NetCdfImportProgress = NetCdfImportProgress(
        id = this[NetCdfImportProgressTable.id].value,
        fileId = this[NetCdfImportProgressTable.fileId].value,
        variableName = this[NetCdfImportProgressTable.variableName],
        status = this[NetCdfImportProgressTable.status],
        lastSliceIdx = this[NetCdfImportProgressTable.lastSliceIdx],
        leaseExpiresAt = this[NetCdfImportProgressTable.leaseExpiresAt],
        errorMessage = this[NetCdfImportProgressTable.errorMessage],
        startedAt = this[NetCdfImportProgressTable.startedAt],
        completedAt = this[NetCdfImportProgressTable.completedAt],
        updatedAt = this[NetCdfImportProgressTable.updatedAt],
    )

    /**
     * `(fileId, variableName)` 으로 진행 상태를 조회합니다.
     *
     * @return row 가 없으면 null
     */
    fun findByFileAndVariable(fileId: Long, variableName: String): NetCdfImportProgress? =
        NetCdfImportProgressTable
            .selectAll()
            .where {
                (NetCdfImportProgressTable.fileId eq fileId) and
                    (NetCdfImportProgressTable.variableName eq variableName)
            }
            .firstOrNull()
            ?.run { toEntity() }

    /**
     * heartbeat lease 를 획득합니다 (Spec §3.8).
     *
     * 2단계 처리:
     * 1. SELECT 로 기존 row 조회 → `COMPLETED` 면 그대로 반환 (호출자가 no-op 분기)
     * 2. raw `INSERT ... ON CONFLICT (file_id, variable_name) DO UPDATE SET ... WHERE ... RETURNING`
     *    - WHERE 조건: `status IN ('PENDING','FAILED') OR (status='IN_PROGRESS' AND lease_expires_at < :now)`
     *    - COMPLETED 는 WHERE 에서 제외 → 갱신 불가
     *    - 0 row 갱신 → 활성 lease 보유 중 → [NetCdfException.ImportAlreadyRunning] throw
     *
     * @param fileId       NetCDF 파일 ID
     * @param variableName 임포트 대상 변수 이름
     * @param leaseTtl     lease 만료 시각 = `now() + leaseTtl` (기본 5분)
     * @return lease 를 획득한 갱신된 row, 또는 기존 COMPLETED row
     * @throws NetCdfException.ImportAlreadyRunning 다른 프로세스가 활성 lease 보유 중일 때
     */
    fun acquireLease(
        fileId: Long,
        variableName: String,
        leaseTtl: Duration = DEFAULT_LEASE_TTL,
    ): NetCdfImportProgress {
        require(leaseTtl.seconds > 0L && leaseTtl.nano == 0) {
            "leaseTtl must be a positive whole-second duration: $leaseTtl"
        }
        val leaseSeconds = leaseTtl.seconds
        val sql = """
            INSERT INTO netcdf_import_progress
                (file_id, variable_name, status, last_slice_idx, lease_expires_at, error_message,
                 started_at, completed_at, updated_at)
            VALUES (?, ?, 'IN_PROGRESS', NULL,
                    clock_timestamp() + (? * INTERVAL '1 second'), NULL,
                    CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP)
            ON CONFLICT (file_id, variable_name)
            DO UPDATE SET
                status = 'IN_PROGRESS',
                lease_expires_at = clock_timestamp() + (? * INTERVAL '1 second'),
                error_message = NULL,
                completed_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE
                netcdf_import_progress.status IN ('PENDING', 'FAILED')
                OR (netcdf_import_progress.status = 'IN_PROGRESS'
                    AND netcdf_import_progress.lease_expires_at <= clock_timestamp())
            RETURNING id, file_id, variable_name, status, last_slice_idx, lease_expires_at,
                      error_message, started_at, completed_at, updated_at
        """.trimIndent()

        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        return conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, fileId)
            stmt.setString(2, variableName)
            stmt.setLong(3, leaseSeconds)
            stmt.setLong(4, leaseSeconds)

            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    val current = findByFileAndVariable(fileId, variableName)
                    if (current?.status == NetCdfImportStatus.COMPLETED) {
                        log.info { "lease skipped — already completed: fileId=$fileId var=$variableName" }
                        return@use current
                    }
                    if (current?.status == NetCdfImportStatus.IN_PROGRESS && current.leaseExpiresAt == null) {
                        repairMalformedLease(current.id)
                        return@use acquireLease(fileId, variableName, leaseTtl)
                    }
                    throw NetCdfException.ImportAlreadyRunning(fileId, variableName)
                }
                val newId = rs.getLong("id")
                val newFileId = rs.getLong("file_id")
                val newVariableName = rs.getString("variable_name")
                val newStatus = NetCdfImportStatus.valueOf(rs.getString("status"))
                val newLastSliceIdx = rs.getLong("last_slice_idx").takeUnless { rs.wasNull() }
                val newLeaseExp = rs.getTimestamp("lease_expires_at")?.toInstant()
                val newError = rs.getString("error_message")
                val newStartedAt = rs.getTimestamp("started_at").toInstant()
                val newCompletedAt = rs.getTimestamp("completed_at")?.toInstant()
                val newUpdatedAt = rs.getTimestamp("updated_at").toInstant()

                log.debug {
                    "lease acquired — fileId=$fileId var=$variableName progressId=$newId " +
                        "lastSliceIdx=$newLastSliceIdx leaseExpiresAt=$newLeaseExp"
                }

                NetCdfImportProgress(
                    id = newId,
                    fileId = newFileId,
                    variableName = newVariableName,
                    status = newStatus,
                    lastSliceIdx = newLastSliceIdx,
                    leaseExpiresAt = newLeaseExp,
                    errorMessage = newError,
                    startedAt = newStartedAt,
                    completedAt = newCompletedAt,
                    updatedAt = newUpdatedAt,
                )
            }
        }
    }

    /**
     * heartbeat — owner token 을 검증하고 `lastSliceIdx` 와 `leaseExpiresAt` 을 갱신합니다.
     *
     * 슬라이스 commit tx 안에서 함께 호출하여 원자성 보장.
     *
     * @return 갱신된 lease owner token
     * @throws NetCdfException.ImportLeaseLost 같은 progress row 를 다른 importer 가 재획득했을 때
     */
    fun renewLease(
        progressId: Long,
        expectedLeaseExpiresAt: Instant,
        lastSliceIdx: Long,
        leaseTtl: Duration = DEFAULT_LEASE_TTL,
    ): Instant {
        require(leaseTtl.seconds > 0L && leaseTtl.nano == 0) {
            "leaseTtl must be a positive whole-second duration: $leaseTtl"
        }
        val sql = """
            UPDATE netcdf_import_progress
            SET last_slice_idx = ?,
                lease_expires_at = clock_timestamp() + (? * INTERVAL '1 second'),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'IN_PROGRESS' AND lease_expires_at = ?
              AND lease_expires_at > clock_timestamp()
            RETURNING lease_expires_at
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, lastSliceIdx)
            stmt.setLong(2, leaseTtl.seconds)
            stmt.setLong(3, progressId)
            stmt.setTimestamp(4, Timestamp.from(expectedLeaseExpiresAt))
            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    throw NetCdfException.ImportLeaseLost(progressId)
                }
                return rs.getTimestamp("lease_expires_at").toInstant()
            }
        }
    }

    /** checkpoint를 전진시키지 않고 현재 owner lease만 연장합니다. */
    fun touchLease(
        progressId: Long,
        expectedLeaseExpiresAt: Instant,
        leaseTtl: Duration = DEFAULT_LEASE_TTL,
    ): Instant {
        require(leaseTtl.seconds > 0L && leaseTtl.nano == 0) {
            "leaseTtl must be a positive whole-second duration: $leaseTtl"
        }
        val sql = """
            UPDATE netcdf_import_progress
            SET lease_expires_at = clock_timestamp() + (? * INTERVAL '1 second'),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'IN_PROGRESS' AND lease_expires_at = ?
              AND lease_expires_at > clock_timestamp()
            RETURNING lease_expires_at
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, leaseTtl.seconds)
            stmt.setLong(2, progressId)
            stmt.setTimestamp(3, Timestamp.from(expectedLeaseExpiresAt))
            stmt.executeQuery().use { rs ->
                if (!rs.next()) throw NetCdfException.ImportLeaseLost(progressId)
                return rs.getTimestamp("lease_expires_at").toInstant()
            }
        }
    }

    /**
     * 정상 완료 처리 — owner token 을 검증하고 `status=COMPLETED`, `completedAt=now()`, `leaseExpiresAt=null`.
     *
     * @throws NetCdfException.ImportLeaseLost 같은 progress row 를 다른 importer 가 재획득했을 때
     */
    fun markCompleted(progressId: Long, expectedLeaseExpiresAt: Instant) {
        val sql = """
            UPDATE netcdf_import_progress
            SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP, lease_expires_at = NULL,
                error_message = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'IN_PROGRESS' AND lease_expires_at = ?
              AND lease_expires_at > clock_timestamp()
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, progressId)
            stmt.setTimestamp(2, Timestamp.from(expectedLeaseExpiresAt))
            if (stmt.executeUpdate() != 1) {
                throw NetCdfException.ImportLeaseLost(progressId)
            }
        }
        log.info { "import marked COMPLETED — progressId=$progressId" }
    }

    /**
     * 실패 처리 — owner token 을 검증하고 `status=FAILED`, `errorMessage` 기록, `leaseExpiresAt=null`.
     *
     * `lastSliceIdx` 는 그대로 유지하여 재호출 시 그 다음부터 재개 가능.
     *
     * @throws NetCdfException.ImportLeaseLost 같은 progress row 를 다른 importer 가 재획득했을 때
     */
    fun markFailed(progressId: Long, expectedLeaseExpiresAt: Instant, errorMessage: String) {
        val sql = """
            UPDATE netcdf_import_progress
            SET status = 'FAILED', error_message = ?, lease_expires_at = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'IN_PROGRESS' AND lease_expires_at = ?
              AND lease_expires_at > clock_timestamp()
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, errorMessage.take(MAX_ERROR_MESSAGE_LENGTH))
            stmt.setLong(2, progressId)
            stmt.setTimestamp(3, Timestamp.from(expectedLeaseExpiresAt))
            if (stmt.executeUpdate() != 1) {
                throw NetCdfException.ImportLeaseLost(progressId)
            }
        }
        log.warn { "import marked FAILED — progressId=$progressId msg=${errorMessage.take(200)}" }
    }

    /**
     * 관리자 확인이 필요한 손상 checkpoint를 stable marker로 격리합니다.
     *
     * COMPLETED 행은 이미 외부에 관측된 성공 결과이므로 절대로 변경하지 않습니다.
     * 호출자는 이 메서드가 반환된 뒤 [NetCdfException.CorruptProgress]를 던져야 합니다.
     */
    fun quarantineCorruptProgress(progressId: Long, detail: String) {
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(
            "SELECT status FROM netcdf_import_progress WHERE id = ? FOR UPDATE",
        ).use { select ->
            select.setLong(1, progressId)
            select.executeQuery().use { rs ->
                if (!rs.next() || rs.getString(1) == NetCdfImportStatus.COMPLETED.name) return
            }
        }
        conn.prepareStatement(
            """
            UPDATE netcdf_import_progress
            SET status = 'FAILED', error_message = ?, lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status <> 'COMPLETED'
            """.trimIndent(),
        ).use { update ->
            update.setString(1, "CORRUPT_PROGRESS:$progressId:${detail.take(512)}")
            update.setLong(2, progressId)
            update.executeUpdate()
        }
    }

    private fun repairMalformedLease(progressId: Long) {
        val sql = """
            UPDATE netcdf_import_progress
            SET status = 'FAILED', error_message = ?, lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'IN_PROGRESS' AND lease_expires_at IS NULL
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, "CORRUPT_PROGRESS:$progressId")
            stmt.setLong(2, progressId)
            stmt.executeUpdate()
        }
    }
}

private const val MAX_ERROR_MESSAGE_LENGTH = 4_000
