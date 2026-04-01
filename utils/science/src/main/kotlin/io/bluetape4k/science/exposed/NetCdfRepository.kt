package io.bluetape4k.science.exposed

import io.bluetape4k.exposed.jdbc.repository.LongJdbcRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insertAndGetId

/**
 * NetCDF 파일 메타데이터 레코드를 담는 데이터 클래스입니다.
 *
 * @param id          기본키 (자동 생성)
 * @param filename    파일 이름
 * @param filePath    파일 전체 경로
 * @param fileSize    파일 크기 (바이트)
 * @param variables   변수 목록
 * @param dimensions  차원 이름-크기 매핑
 * @param globalAttrs 전역 속성
 */
data class NetCdfFileRecord(
    val id: Long = 0L,
    val filename: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val variables: List<NetCdfVariableInfo> = emptyList(),
    val dimensions: Map<String, Int> = emptyMap(),
    val globalAttrs: Map<String, String> = emptyMap(),
)

/**
 * [NetCdfFileTable] 기반 JDBC Repository 입니다.
 *
 * NetCDF 파일 메타데이터를 조회/저장합니다.
 */
class NetCdfFileRepository : LongJdbcRepository<NetCdfFileRecord> {

    override val table = NetCdfFileTable

    override fun extractId(entity: NetCdfFileRecord): Long = entity.id

    override fun ResultRow.toEntity(): NetCdfFileRecord = NetCdfFileRecord(
        id = this[NetCdfFileTable.id].value,
        filename = this[NetCdfFileTable.filename],
        filePath = this[NetCdfFileTable.filePath],
        fileSize = this[NetCdfFileTable.fileSize],
        variables = this[NetCdfFileTable.variables],
        dimensions = this[NetCdfFileTable.dimensions],
        globalAttrs = this[NetCdfFileTable.globalAttrs],
    )

    /**
     * NetCDF 파일 레코드를 저장하고, 생성된 ID가 포함된 레코드를 반환합니다.
     *
     * @param record 저장할 파일 레코드
     * @return 생성된 ID가 설정된 [NetCdfFileRecord]
     */
    fun save(record: NetCdfFileRecord): NetCdfFileRecord {
        val id = NetCdfFileTable.insertAndGetId {
            it[filename] = record.filename
            it[filePath] = record.filePath
            it[fileSize] = record.fileSize
            it[variables] = record.variables
            it[dimensions] = record.dimensions
            it[globalAttrs] = record.globalAttrs
        }
        return record.copy(id = id.value)
    }
}

/**
 * NetCDF 파일 등록 및 격자 값 임포트를 담당하는 서비스입니다.
 *
 * Phase 4 (UCAR netcdfAll) 완료 후 실제 파일 파싱 로직을 구현합니다.
 *
 * @param fileRepo NetCDF 파일 Repository
 */
class NetCdfCatalogService(
    private val fileRepo: NetCdfFileRepository,
) {
    /**
     * NetCDF 파일을 읽어 메타데이터를 DB에 등록합니다.
     *
     * @param filePath 파일 경로
     * @return 등록된 파일 레코드 ID
     */
    suspend fun registerFile(filePath: String): Long {
        TODO("Phase 4 (UCAR netcdfAll) 완료 후 구현 예정")
    }

    /**
     * NetCDF 파일의 격자 값을 DB에 임포트합니다.
     *
     * @param fileId       파일 레코드 ID
     * @param variableName 변수 이름
     */
    suspend fun importGridValues(fileId: Long, variableName: String) {
        TODO("Phase 4 (UCAR netcdfAll) 완료 후 구현 예정")
    }
}
