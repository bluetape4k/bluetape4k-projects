package io.bluetape4k.science.exposed.schema

import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.jackson3.jacksonb
import io.bluetape4k.exposed.postgresql.postgis.geoPoint
import io.bluetape4k.exposed.postgresql.postgis.geoPolygon
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
object NetCdfFileTable : AuditableLongIdTable("netcdf_files") {

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
object NetCdfGridValueTable : LongIdTable("netcdf_grid_values") {

    /** 소속 NetCDF 파일 외래키 */
    val fileId = reference("file_id", NetCdfFileTable)

    /** 변수 이름 */
    val variableName = varchar("variable_name", 255)

    /** 격자 위치 (PostGIS POINT) */
    val location = geoPoint("location")

    /** 시간 차원 인덱스 */
    val timeIdx = integer("time_idx").default(0)

    /** 레벨(고도) 차원 인덱스 */
    val levelIdx = integer("level_idx").default(0)

    /** 측정값 */
    val value = double("value")

    /** 부가 속성 (JSONB, 선택) */
    val attrs = jacksonb<Map<String, Any?>>("attrs").nullable()
}
