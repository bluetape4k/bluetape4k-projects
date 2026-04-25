package io.bluetape4k.science.exposed.schema

import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.jackson3.jacksonb
import io.bluetape4k.exposed.postgresql.postgis.geoPoint
import io.bluetape4k.exposed.postgresql.postgis.geoPolygon
import io.bluetape4k.science.exposed.model.NetCdfImportStatus
import io.bluetape4k.science.exposed.model.NetCdfVariableInfo
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * NetCDF 파일 메타데이터를 저장하는 Exposed 테이블입니다.
 *
 * 파일 경로, 크기, 변수/차원 정보, 전역 속성, 시공간 범위 등을 관리합니다.
 *
 * ```kotlin
 * // 테이블 생성
 * SchemaUtils.create(NetCdfFileTable)
 *
 * // 레코드 삽입
 * transaction {
 *     NetCdfFileTable.insertAndGetId {
 *         it[filename] = "era5_2023.nc"
 *         it[filePath] = "/data/era5_2023.nc"
 *         it[fileSize] = 104_857_600L
 *         it[variables] = emptyList()
 *         it[dimensions] = mapOf("time" to 24, "lat" to 90, "lon" to 180)
 *         it[globalAttrs] = mapOf("source" to "ERA5")
 *     }
 * }
 * ```
 */
object NetCdfFileTable: AuditableLongIdTable("netcdf_files") {

    /** 파일 이름 */
    val filename = varchar("filename", 255)

    /** 파일 전체 경로 */
    val filePath = varchar("file_path", 1024)

    /** 파일 크기 (바이트) */
    val fileSize = long("file_size").default(0L)

    /** NetCDF 변수 목록 (JSONB) */
    val variables = jacksonb<List<NetCdfVariableInfo>>("variables")

    /** 차원 이름-크기 매핑 (JSONB) */
    val dimensions = jacksonb<Map<String, Int>>("dimensions")

    /** 전역 속성 (JSONB) */
    val globalAttrs = jacksonb<Map<String, String>>("global_attrs")

    /** 공간 경계 폴리곤 (PostGIS POLYGON, 선택) */
    val bbox = geoPolygon("bbox").nullable()

    /** 시간 범위 시작 (선택) */
    val timeStart = timestamp("time_start").nullable()

    /** 시간 범위 종료 (선택) */
    val timeEnd = timestamp("time_end").nullable()
}

/**
 * NetCDF 격자 값을 저장하는 Exposed 테이블입니다.
 *
 * 각 행은 특정 파일의 특정 변수에 대한 하나의 격자 셀 값을 나타냅니다.
 * 감사(Auditable) 컬럼은 필요하지 않으므로 일반 [LongIdTable]을 사용합니다.
 *
 * ```kotlin
 * // 격자 값 삽입
 * transaction {
 *     NetCdfGridValueTable.insert {
 *         it[fileId] = fileRecord.id
 *         it[variableName] = "temperature"
 *         it[timeIdx] = 0
 *         it[levelIdx] = 0
 *         it[value] = 293.15  // 20°C in Kelvin
 *     }
 * }
 * ```
 */
object NetCdfGridValueTable: LongIdTable("netcdf_grid_values") {

    /** 소속 NetCDF 파일 외래키 */
    val fileId = reference("file_id", NetCdfFileTable)

    /** 변수 이름 */
    val variableName = varchar("variable_name", 255)

    /**
     * 격자 위치 (PostGIS POINT, EPSG:4326).
     *
     * `nullable()` — 1D(시계열) 변수처럼 좌표축이 없는 케이스에서 null 로 저장됩니다.
     * 좌표가 있는 셀은 `Point(lon, lat)` 직접 생성하여 저장 (lon-first 순서 — R4).
     */
    val location = geoPoint("location").nullable()

    /** 시간 차원 인덱스 */
    val timeIdx = integer("time_idx").default(0)

    /** 레벨(고도) 차원 인덱스 */
    val levelIdx = integer("level_idx").default(0)

    /** 측정값 */
    val value = double("value")

    /** 부가 속성 (JSONB, 선택) */
    val attrs = jacksonb<Map<String, Any?>>("attrs").nullable()
}

/**
 * [NetCdfGridValueTable] 의 partial unique index DDL 모음.
 *
 * Exposed `object` 내부에는 `companion object` 를 둘 수 없으므로 별도 namespace 로 분리.
 *
 * 테스트 `@BeforeAll` 또는 프로덕션 마이그레이션에서 `SchemaUtils.create` 후 두 DDL 을 실행한다.
 */
object NetCdfGridValueIndexes {

    /**
     * `(file_id, variable_name, time_idx, level_idx, MD5(ST_AsBinary(location)))` partial unique index DDL.
     *
     * PostGIS `geometry` 컬럼은 b-tree operator class 가 없으므로 `MD5(ST_AsBinary(location))` 해시 사용 (M2).
     * `WHERE location IS NOT NULL` partial 인덱스 — location 이 있는 셀의 중복 방지.
     */
    const val DDL_UNIQUE_FULL: String =
        "CREATE UNIQUE INDEX IF NOT EXISTS uk_netcdf_grid_values_full " +
            "ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx, MD5(ST_AsBinary(location))) " +
            "WHERE location IS NOT NULL"

    /**
     * `(file_id, variable_name, time_idx, level_idx)` partial unique index DDL.
     *
     * `WHERE location IS NULL` — 1D 시계열처럼 location 이 없는 셀의 중복 방지.
     */
    const val DDL_UNIQUE_NULLOC: String =
        "CREATE UNIQUE INDEX IF NOT EXISTS uk_netcdf_grid_values_nulloc " +
            "ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx) " +
            "WHERE location IS NULL"
}

/**
 * NetCDF 변수 단위 import 진행 상황을 추적하는 Exposed 테이블입니다.
 *
 * heartbeat lease 기반 동시성 제어 및 sliceIdx 기반 4D 재개를 지원합니다.
 *
 * 시스템 import 상태 추적이므로 user context 가 불필요하여 일반 [LongIdTable] 사용 (M1).
 *
 * ```kotlin
 * SchemaUtils.create(NetCdfImportProgressTable)
 *
 * // 진입 시 lease 획득 — Repository.acquireLease() 사용 (raw SQL ON CONFLICT DO UPDATE)
 * // 슬라이스 commit 후 renewLease(progressId, lastSliceIdx, newExpiresAt)
 * // 완료 시 markCompleted(progressId)
 * // 실패 시 markFailed(progressId, errorMessage)
 * ```
 */
object NetCdfImportProgressTable: LongIdTable("netcdf_import_progress") {

    /** 소속 NetCDF 파일 외래키 */
    val fileId = reference("file_id", NetCdfFileTable)

    /** 임포트 대상 변수 이름 */
    val variableName = varchar("variable_name", 255)

    /** import 상태 (PENDING / IN_PROGRESS / COMPLETED / FAILED) */
    val status = enumerationByName("status", 20, NetCdfImportStatus::class)

    /**
     * 마지막 commit 된 슬라이스의 선형 인덱스.
     *
     * - 1D: 0
     * - 2D: 0 (단일 슬라이스)
     * - 3D: timeIdx
     * - 4D: timeIdx × levelN + levelIdx
     *
     * 재개 시 `(lastSliceIdx ?: -1) + 1` 부터 시작.
     */
    val lastSliceIdx = long("last_slice_idx").nullable()

    /**
     * heartbeat lease 만료 시각.
     *
     * `IN_PROGRESS` 상태에서 `now() < leaseExpiresAt` 면 다른 프로세스의 활성 lease 로 간주.
     * 만료 시 stale lease → 다른 호출자가 재획득 가능.
     *
     * COMPLETED / FAILED 시 null 로 초기화.
     */
    val leaseExpiresAt = timestamp("lease_expires_at").nullable()

    /** 마지막 실패 메시지 (FAILED 상태일 때) */
    val errorMessage = text("error_message").nullable()

    /** 임포트 시작 시각 */
    val startedAt = timestamp("started_at")

    /** 완료 시각 (COMPLETED 일 때만 설정) */
    val completedAt = timestamp("completed_at").nullable()

    /** 마지막 갱신 시각 (heartbeat / 상태 변경 시 자동 갱신) */
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("ux_netcdf_import_progress_file_var", fileId, variableName)
    }
}
