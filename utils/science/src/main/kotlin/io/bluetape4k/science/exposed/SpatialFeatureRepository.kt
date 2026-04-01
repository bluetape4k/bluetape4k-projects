package io.bluetape4k.science.exposed

import io.bluetape4k.exposed.jdbc.repository.LongJdbcRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.shapefile.loadShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import net.postgis.jdbc.PGgeometry
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKTWriter
import java.io.File

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
            org.locationtech.jts.io.WKTReader().read(wkt)
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

/**
 * Shapefile을 읽어 공간 레이어와 피처를 DB에 임포트하는 서비스입니다.
 *
 * JTS [org.locationtech.jts.geom.Geometry]를 PostGIS [Geometry]로 변환하여 저장합니다.
 *
 * @param layerRepo   공간 레이어 Repository
 * @param featureRepo 공간 피처 Repository
 */
class ShapefileImportService(
    private val layerRepo: SpatialLayerRepository,
    private val featureRepo: SpatialFeatureRepository,
) {
    companion object : KLogging()

    /**
     * Shapefile을 읽어 DB에 임포트합니다.
     *
     * 동일한 이름의 레이어가 이미 존재하면 [IllegalArgumentException]이 발생합니다.
     * 피처는 [batchSize] 단위로 배치 삽입됩니다.
     *
     * @param file      .shp 확장자 파일
     * @param layerName 레이어 이름
     * @param batchSize 배치 삽입 크기 (기본값: 1000)
     * @return 임포트된 피처 수
     * @throws IllegalArgumentException 동일 이름 레이어가 이미 존재할 때
     */
    suspend fun importShapefile(file: File, layerName: String, batchSize: Int = 1000): Int {
        val shape = withContext(Dispatchers.IO) { loadShape(file) }

        // 1. 레이어 메타데이터 insert — 별도 트랜잭션 (중복 검사 포함)
        val layerRecord = withContext(Dispatchers.IO) {
            suspendTransaction {
                require(layerRepo.findByName(layerName) == null) {
                    "동일한 이름의 레이어가 이미 존재합니다: $layerName"
                }
                val header = shape.header
                layerRepo.save(
                    SpatialLayerRecord(
                        name = layerName,
                        sourceFile = file.absolutePath,
                        srid = 4326,
                        geometryType = shape.records.firstOrNull()?.geometry?.geometryType,
                        bboxMinX = header.bbox.minLon,
                        bboxMinY = header.bbox.minLat,
                        bboxMaxX = header.bbox.maxLon,
                        bboxMaxY = header.bbox.maxLat,
                        recordCount = shape.size,
                    )
                )
            }
        }

        // 2. 피처 배치 insert — 배치마다 독립 트랜잭션 (부분 실패 시 해당 배치만 롤백)
        val wktWriter = WKTWriter()
        var totalInserted = 0

        for (batch in shape.records.chunked(batchSize)) {
            coroutineContext.ensureActive()

            withContext(Dispatchers.IO) {
                suspendTransaction {
                    SpatialFeatureTable.batchInsert(batch) { record ->
                        val wkt = wktWriter.write(record.geometry)
                        val pgGeom = PGgeometry(wkt).geometry

                        this[SpatialFeatureTable.layerId] = layerRecord.id
                        this[SpatialFeatureTable.featureType] = record.geometry.geometryType
                        this[SpatialFeatureTable.geom] = pgGeom
                        this[SpatialFeatureTable.properties] = record.attributes
                        this[SpatialFeatureTable.name] = record.attributes["NAME"]?.toString()
                    }
                }
            }
            totalInserted += batch.size
            log.debug { "배치 삽입 완료: $totalInserted / ${shape.size}" }
        }
        return totalInserted
    }
}
