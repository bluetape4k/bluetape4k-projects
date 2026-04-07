package io.bluetape4k.science.exposed.repository

import io.bluetape4k.exposed.jdbc.repository.LongJdbcRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.exposed.model.SpatialFeatureRecord
import io.bluetape4k.science.exposed.model.SpatialLayerRecord
import io.bluetape4k.science.exposed.schema.SpatialFeatureTable
import io.bluetape4k.science.exposed.schema.SpatialLayerTable
import net.postgis.jdbc.PGgeometry
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter

/**
 * [SpatialLayerTable] 기반 JDBC Repository 입니다.
 *
 * 공간 레이어 메타데이터를 조회/저장합니다.
 *
 * ```kotlin
 * val repo = SpatialLayerRepository()
 * transaction {
 *     val layer = SpatialLayerRecord(name = "korea_regions", srid = 4326, recordCount = 17)
 *     val saved = repo.save(layer)
 *     println(saved.id) // 예: 1 (자동 생성)
 *
 *     val found = repo.findByName("korea_regions")
 *     println(found?.name) // "korea_regions"
 * }
 * ```
 */
class SpatialLayerRepository: LongJdbcRepository<SpatialLayerRecord> {

    override val table = SpatialLayerTable

    override fun extractId(entity: SpatialLayerRecord): Long = entity.id

    override fun ResultRow.toEntity(): SpatialLayerRecord = SpatialLayerRecord(
        id = this[SpatialLayerTable.id].value,
        name = this[SpatialLayerTable.name],
        description = this[SpatialLayerTable.description],
        sourceFile = this[SpatialLayerTable.sourceFile],
        srid = this[SpatialLayerTable.srid],
        geometryType = this[SpatialLayerTable.geometryType],
        bboxMinX = this[SpatialLayerTable.bboxMinX],
        bboxMinY = this[SpatialLayerTable.bboxMinY],
        bboxMaxX = this[SpatialLayerTable.bboxMaxX],
        bboxMaxY = this[SpatialLayerTable.bboxMaxY],
        recordCount = this[SpatialLayerTable.recordCount],
    )

    /**
     * 공간 레이어 레코드를 저장하고, 생성된 ID가 포함된 레코드를 반환합니다.
     *
     * ```kotlin
     * val repo = SpatialLayerRepository()
     * transaction {
     *     val layer = SpatialLayerRecord(name = "seoul_districts", srid = 4326)
     *     val saved = repo.save(layer)
     *     println(saved.id)   // 예: 1 (자동 생성)
     *     println(saved.name) // "seoul_districts"
     * }
     * ```
     *
     * @param record 저장할 레이어 레코드
     * @return 생성된 ID가 설정된 [SpatialLayerRecord]
     */
    fun save(record: SpatialLayerRecord): SpatialLayerRecord {
        val id = SpatialLayerTable.insertAndGetId {
            it[name] = record.name
            it[description] = record.description
            it[sourceFile] = record.sourceFile
            it[srid] = record.srid
            it[geometryType] = record.geometryType
            it[bboxMinX] = record.bboxMinX
            it[bboxMinY] = record.bboxMinY
            it[bboxMaxX] = record.bboxMaxX
            it[bboxMaxY] = record.bboxMaxY
            it[recordCount] = record.recordCount
        }
        return record.copy(id = id.value)
    }

    /**
     * 레이어 이름으로 조회합니다.
     *
     * ```kotlin
     * val repo = SpatialLayerRepository()
     * transaction {
     *     val found = repo.findByName("korea_regions")
     *     println(found?.name)        // "korea_regions"
     *     println(found?.recordCount) // 예: 17
     *     println(repo.findByName("nonexistent")) // null
     * }
     * ```
     *
     * @param layerName 레이어 이름
     * @return 레이어 레코드 또는 null
     */
    fun findByName(layerName: String): SpatialLayerRecord? {
        return SpatialLayerTable
            .selectAll()
            .where { SpatialLayerTable.name eq layerName }
            .map { it.toEntity() }
            .firstOrNull()
    }
}

/**
 * [SpatialFeatureTable] 기반 JDBC Repository 입니다.
 *
 * 공간 피처(도형 + 속성)를 조회/저장합니다.
 *
 * ```kotlin
 * val layerRepo = SpatialLayerRepository()
 * val featureRepo = SpatialFeatureRepository()
 * transaction {
 *     val layer = layerRepo.save(SpatialLayerRecord(name = "poi", srid = 4326))
 *     val gf = GeometryFactory()
 *     val point = gf.createPoint(Coordinate(126.9780, 37.5665))
 *     val feature = SpatialFeatureRecord(
 *         layerId = layer.id, featureType = "Point",
 *         geom = point, name = "서울시청"
 *     )
 *     val saved = featureRepo.save(feature)
 *     println(saved.id) // 예: 1 (자동 생성)
 * }
 * ```
 */
class SpatialFeatureRepository: LongJdbcRepository<SpatialFeatureRecord> {

    companion object: KLogging()

    override val table = SpatialFeatureTable

    override fun extractId(entity: SpatialFeatureRecord): Long = entity.id

    override fun ResultRow.toEntity(): SpatialFeatureRecord {
        val pgGeom = this[SpatialFeatureTable.geom]
        val wktStr = pgGeom.toString()
        // PostGIS Geometry.toString()은 EWKT 형태를 반환합니다.
        // WKTReader가 SRID 포함 문자열을 처리할 수 있도록 SRID 접두어를 제거합니다.
        val jtsGeom = runCatching {
            val wkt = if (wktStr.startsWith("SRID=", ignoreCase = true)) wktStr.substringAfter(";") else wktStr
            WKTReader().read(wkt)
        }.getOrElse { wktEx ->
            log.debug(wktEx) { "WKT 파싱 실패, WKB 헥스 폴백 시도: $wktStr" }
            // WKB 헥스 폴백: PostGIS Geometry의 바이너리 WKB 변환 시도
            runCatching {
                WKBReader().read(WKBReader.hexToBytes(wktStr))
            }.getOrElse { wkbEx ->
                throw IllegalStateException(
                    "PostGIS 도형 변환 실패 (WKT, WKB 모두 실패): $wktStr",
                    wkbEx.also { it.addSuppressed(wktEx) }
                )
            }
        }
        return SpatialFeatureRecord(
            id = this[SpatialFeatureTable.id].value,
            layerId = this[SpatialFeatureTable.layerId].value,
            featureType = this[SpatialFeatureTable.featureType],
            geom = jtsGeom,
            properties = this[SpatialFeatureTable.properties],
            name = this[SpatialFeatureTable.name],
        )
    }

    /**
     * 공간 피처 레코드를 저장하고, 생성된 ID가 포함된 레코드를 반환합니다.
     *
     * ```kotlin
     * val repo = SpatialFeatureRepository()
     * transaction {
     *     val gf = GeometryFactory()
     *     val point = gf.createPoint(Coordinate(126.9780, 37.5665))
     *     val feature = SpatialFeatureRecord(
     *         layerId = 1L, featureType = "Point", geom = point
     *     )
     *     val saved = repo.save(feature)
     *     println(saved.id)          // 예: 1 (자동 생성)
     *     println(saved.featureType) // "Point"
     * }
     * ```
     *
     * @param record 저장할 피처 레코드
     * @return 생성된 ID가 설정된 [SpatialFeatureRecord]
     */
    fun save(record: SpatialFeatureRecord): SpatialFeatureRecord {
        val pgGeom = PGgeometry(WKTWriter().write(record.geom)).geometry
        val id = SpatialFeatureTable.insertAndGetId {
            it[layerId] = record.layerId
            it[featureType] = record.featureType
            it[geom] = pgGeom
            it[properties] = record.properties
            it[name] = record.name
        }
        return record.copy(id = id.value)
    }
}
