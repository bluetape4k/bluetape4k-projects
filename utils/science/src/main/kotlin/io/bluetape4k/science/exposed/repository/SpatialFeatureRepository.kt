package io.bluetape4k.science.exposed.repository

import io.bluetape4k.exposed.jdbc.repository.LongJdbcRepository
import io.bluetape4k.science.exposed.schema.SpatialFeatureTable
import io.bluetape4k.science.exposed.schema.SpatialLayerTable
import net.postgis.jdbc.PGgeometry
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter

/**
 * 공간 레이어 레코드를 담는 데이터 클래스입니다.
 *
 * @param id           기본키 (자동 생성)
 * @param name         레이어 이름
 * @param description  레이어 설명
 * @param sourceFile   원본 파일 경로
 * @param srid         좌표 참조 시스템 SRID
 * @param geometryType 도형 유형
 * @param bboxMinX     경계 사각형 최소 X
 * @param bboxMinY     경계 사각형 최소 Y
 * @param bboxMaxX     경계 사각형 최대 X
 * @param bboxMaxY     경계 사각형 최대 Y
 * @param recordCount  레코드 수
 */
data class SpatialLayerRecord(
    val id: Long = 0L,
    val name: String,
    val description: String? = null,
    val sourceFile: String? = null,
    val srid: Int = 4326,
    val geometryType: String? = null,
    val bboxMinX: Double? = null,
    val bboxMinY: Double? = null,
    val bboxMaxX: Double? = null,
    val bboxMaxY: Double? = null,
    val recordCount: Int = 0,
)

/**
 * 공간 피처 레코드를 담는 데이터 클래스입니다.
 *
 * @param id          기본키 (자동 생성)
 * @param layerId     소속 레이어 ID
 * @param featureType 도형 유형 문자열
 * @param geom        JTS [Geometry] 객체 (compileOnly 의존성인 PostGIS 타입 대신 JTS 타입 사용)
 * @param properties  속성 맵
 * @param name        피처 이름
 */
data class SpatialFeatureRecord(
    val id: Long = 0L,
    val layerId: Long,
    val featureType: String,
    val geom: Geometry,
    val properties: Map<String, Any?> = emptyMap(),
    val name: String? = null,
)

/**
 * [SpatialLayerTable] 기반 JDBC Repository 입니다.
 *
 * 공간 레이어 메타데이터를 조회/저장합니다.
 */
class SpatialLayerRepository : LongJdbcRepository<SpatialLayerRecord> {

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
 */
class SpatialFeatureRepository : LongJdbcRepository<SpatialFeatureRecord> {

    override val table = SpatialFeatureTable

    override fun extractId(entity: SpatialFeatureRecord): Long = entity.id

    override fun ResultRow.toEntity(): SpatialFeatureRecord {
        val pgGeom = this[SpatialFeatureTable.geom]
        // PostGIS Geometry.toString()은 EWKT 형태를 반환합니다.
        // WKTReader가 SRID 포함 문자열을 처리할 수 있도록 SRID 접두어를 제거합니다.
        val jtsGeom = runCatching {
            val wkt = pgGeom.toString().let { s ->
                // "SRID=4326;POINT(...)" → "POINT(...)"
                if (s.startsWith("SRID=", ignoreCase = true)) s.substringAfter(";") else s
            }
            WKTReader().read(wkt)
        }.getOrElse {
            // WKB 헥스 폴백: PostGIS Geometry의 바이너리 WKB 변환 시도
            WKBReader().read(WKBReader.hexToBytes(pgGeom.toString()))
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

