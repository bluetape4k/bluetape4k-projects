package io.bluetape4k.science.exposed.repository

import io.bluetape4k.exposed.jdbc.repository.LongJdbcRepository
import io.bluetape4k.science.exposed.model.NetCdfFileRecord
import io.bluetape4k.science.exposed.schema.NetCdfFileTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insertAndGetId

/**
 * [NetCdfFileTable] 기반 JDBC Repository 입니다.
 *
 * NetCDF 파일 메타데이터를 조회/저장합니다.
 *
 * ```kotlin
 * val repo = NetCdfFileRepository()
 * transaction {
 *     val record = NetCdfFileRecord(
 *         filename = "era5_2023.nc",
 *         filePath = "/data/era5_2023.nc",
 *         fileSize = 104_857_600L
 *     )
 *     val saved = repo.save(record)
 *     println(saved.id) // 자동 생성된 ID (예: 1)
 * }
 * ```
 */
class NetCdfFileRepository: LongJdbcRepository<NetCdfFileRecord> {

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
     * ```kotlin
     * val repo = NetCdfFileRepository()
     * transaction {
     *     val record = NetCdfFileRecord(filename = "era5.nc", filePath = "/data/era5.nc")
     *     val saved = repo.save(record)
     *     println(saved.id) // 예: 1 (자동 생성)
     * }
     * ```
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
