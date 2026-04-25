package io.bluetape4k.science.exposed.repository

import io.bluetape4k.exposed.jdbc.repository.LongJdbcRepository
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
 * - [renewLease] : 슬라이스 commit 시 호출, `lastSliceIdx` + `leaseExpiresAt` 갱신
 * - [markCompleted] : 정상 완료, `status=COMPLETED` + `leaseExpiresAt=null` + `completedAt=now()`
 * - [markFailed] : 예외 발생, `status=FAILED` + `leaseExpiresAt=null` + `errorMessage` 기록
 *
 * @see io.bluetape4k.science.exposed.service.NetCdfCatalogService
 */
class NetCdfImportProgressRepository: LongJdbcRepository<NetCdfImportProgress> {

    companion object: KLogging() {
        /**
         * heartbeat lease 기본 TTL — 5분.
         *
         * 슬라이스 10개 또는 30초마다 [renewLease] 호출로 갱신 권장.
         */
        val DEFAULT_LEASE_TTL: Duration = Duration.ofMinutes(5)
    }

    override val table = NetCdfImportProgressTable

    override fun extractId(entity: NetCdfImportProgress): Long = entity.id

    override fun ResultRow.toEntity(): NetCdfImportProgress = NetCdfImportProgress(
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
        // Step 1: 기존 row 조회 (COMPLETED 분기)
        val existing = findByFileAndVariable(fileId, variableName)
        if (existing != null && existing.status == NetCdfImportStatus.COMPLETED) {
            log.info { "lease skipped — already completed: fileId=$fileId var=$variableName" }
            return existing
        }

        // Step 2: 조건부 UPSERT (PENDING / FAILED / 만료 IN_PROGRESS 만 허용)
        val now = Instant.now()
        val leaseExp = now.plus(leaseTtl)
        // started_at 은 최초 시작 시각을 보존 — 재개(FAILED→IN_PROGRESS) 시 COALESCE 로 기존 값 유지 (M3).
        // 컬럼은 NOT NULL 이라 EXCLUDED.started_at 분기는 dead — 의도된 방어적 안전망.
        val sql = """
            INSERT INTO netcdf_import_progress
                (file_id, variable_name, status, last_slice_idx, lease_expires_at, error_message,
                 started_at, completed_at, updated_at)
            VALUES (?, ?, ?, NULL, ?, NULL, ?, NULL, ?)
            ON CONFLICT (file_id, variable_name)
            DO UPDATE SET
                status = EXCLUDED.status,
                lease_expires_at = EXCLUDED.lease_expires_at,
                started_at = COALESCE(netcdf_import_progress.started_at, EXCLUDED.started_at),
                error_message = NULL,
                updated_at = EXCLUDED.updated_at
            WHERE
                netcdf_import_progress.status IN ('PENDING', 'FAILED')
                OR (netcdf_import_progress.status = 'IN_PROGRESS'
                    AND netcdf_import_progress.lease_expires_at < ?)
            RETURNING id, status, last_slice_idx, lease_expires_at, started_at, updated_at
        """.trimIndent()

        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        return conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, fileId)
            stmt.setString(2, variableName)
            stmt.setString(3, NetCdfImportStatus.IN_PROGRESS.name)
            stmt.setTimestamp(4, Timestamp.from(leaseExp))
            stmt.setTimestamp(5, Timestamp.from(now))
            stmt.setTimestamp(6, Timestamp.from(now))
            stmt.setTimestamp(7, Timestamp.from(now))

            stmt.executeQuery().use { rs ->
                if (!rs.next()) {
                    // 0 row 갱신 — 활성 lease 보유 중인 다른 프로세스 존재 또는 동시 호출 race
                    throw NetCdfException.ImportAlreadyRunning(fileId, variableName)
                }
                val newId = rs.getLong("id")
                val newStatus = NetCdfImportStatus.valueOf(rs.getString("status"))
                val newLastSliceIdx = rs.getLong("last_slice_idx").takeUnless { rs.wasNull() }
                val newLeaseExp = rs.getTimestamp("lease_expires_at")?.toInstant()
                val newStartedAt = rs.getTimestamp("started_at").toInstant()
                val newUpdatedAt = rs.getTimestamp("updated_at").toInstant()

                log.debug {
                    "lease acquired — fileId=$fileId var=$variableName progressId=$newId " +
                        "lastSliceIdx=$newLastSliceIdx leaseExpiresAt=$newLeaseExp"
                }

                NetCdfImportProgress(
                    id = newId,
                    fileId = fileId,
                    variableName = variableName,
                    status = newStatus,
                    lastSliceIdx = newLastSliceIdx,
                    leaseExpiresAt = newLeaseExp,
                    errorMessage = null,
                    startedAt = newStartedAt,
                    completedAt = null,
                    updatedAt = newUpdatedAt,
                )
            }
        }
    }

    /**
     * heartbeat — `lastSliceIdx` 와 `leaseExpiresAt` 을 갱신합니다.
     *
     * 슬라이스 commit tx 안에서 함께 호출하여 원자성 보장.
     */
    fun renewLease(progressId: Long, lastSliceIdx: Long, leaseTtl: Duration = DEFAULT_LEASE_TTL) {
        val now = Instant.now()
        val newExpires = now.plus(leaseTtl)
        val sql = """
            UPDATE netcdf_import_progress
            SET last_slice_idx = ?, lease_expires_at = ?, updated_at = ?
            WHERE id = ? AND status = 'IN_PROGRESS'
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, lastSliceIdx)
            stmt.setTimestamp(2, Timestamp.from(newExpires))
            stmt.setTimestamp(3, Timestamp.from(now))
            stmt.setLong(4, progressId)
            stmt.executeUpdate()
        }
    }

    /**
     * 정상 완료 처리 — `status=COMPLETED`, `completedAt=now()`, `leaseExpiresAt=null`.
     */
    fun markCompleted(progressId: Long) {
        val now = Instant.now()
        val sql = """
            UPDATE netcdf_import_progress
            SET status = 'COMPLETED', completed_at = ?, lease_expires_at = NULL,
                error_message = NULL, updated_at = ?
            WHERE id = ?
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setTimestamp(1, Timestamp.from(now))
            stmt.setTimestamp(2, Timestamp.from(now))
            stmt.setLong(3, progressId)
            stmt.executeUpdate()
        }
        log.info { "import marked COMPLETED — progressId=$progressId" }
    }

    /**
     * 실패 처리 — `status=FAILED`, `errorMessage` 기록, `leaseExpiresAt=null`.
     *
     * `lastSliceIdx` 는 그대로 유지하여 재호출 시 그 다음부터 재개 가능.
     */
    fun markFailed(progressId: Long, errorMessage: String) {
        val now = Instant.now()
        val sql = """
            UPDATE netcdf_import_progress
            SET status = 'FAILED', error_message = ?, lease_expires_at = NULL, updated_at = ?
            WHERE id = ?
        """.trimIndent()
        val conn = TransactionManager.current().connection.connection as java.sql.Connection
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, errorMessage.take(MAX_ERROR_MESSAGE_LENGTH))
            stmt.setTimestamp(2, Timestamp.from(now))
            stmt.setLong(3, progressId)
            stmt.executeUpdate()
        }
        log.warn { "import marked FAILED — progressId=$progressId msg=${errorMessage.take(200)}" }
    }
}

private const val MAX_ERROR_MESSAGE_LENGTH = 4_000
