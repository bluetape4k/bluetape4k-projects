package io.bluetape4k.science.exposed.service

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.coords.BoundingBox
import io.bluetape4k.science.exposed.model.SpatialLayerRecord
import io.bluetape4k.science.exposed.repository.SpatialFeatureRepository
import io.bluetape4k.science.exposed.repository.SpatialLayerRepository
import io.bluetape4k.science.exposed.schema.SpatialFeatureTable
import io.bluetape4k.science.shapefile.ShapeRecord
import io.bluetape4k.science.shapefile.loadShape
import net.postgis.jdbc.PGgeometry
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.io.WKTWriter
import java.io.File

/**
 * Shapefile을 읽어 공간 레이어와 피처를 DB에 임포트하는 서비스입니다.
 *
 * JTS [org.locationtech.jts.geom.Geometry]를 PostGIS Geometry로 변환하여 저장합니다.
 * 모든 DB 작업은 Virtual Thread에서 실행되어 플랫폼 스레드를 차지하지 않습니다.
 *
 * ```kotlin
 * val layerRepo = SpatialLayerRepository()
 * val featureRepo = SpatialFeatureRepository()
 * val service = ShapefileImportService(layerRepo, featureRepo)
 * val count = service.importShapefile(
 *     file = File("korea_regions.shp"),
 *     layerName = "korea_regions",
 *     batchSize = 500
 * )
 * println(count) // 임포트된 피처 수 (예: 250)
 * ```
 *
 * @param layerRepo   공간 레이어 Repository
 * @param featureRepo 공간 피처 Repository
 */
class ShapefileImportService(
    private val layerRepo: SpatialLayerRepository,
    private val featureRepo: SpatialFeatureRepository,
) {
    companion object: KLogging() {
        private const val TARGET_SRID = 4326
        private const val TARGET_CRS_CODE = "EPSG:$TARGET_SRID"
    }

    /**
     * Shapefile을 읽어 DB에 임포트합니다.
     *
     * 동일한 이름의 레이어가 이미 존재하면 [IllegalArgumentException]이 발생합니다.
     * 피처는 [batchSize] 단위로 배치 삽입되며, 배치마다 독립 트랜잭션이 열립니다.
     * Virtual Thread에서 실행되므로 JDBC 블로킹 I/O가 플랫폼 스레드를 차지하지 않습니다.
     *
     * ```kotlin
     * val service = ShapefileImportService(layerRepo, featureRepo)
     * val count = service.importShapefile(
     *     file = File("korea_regions.shp"),
     *     layerName = "korea_regions"
     * )
     * println(count) // 임포트된 피처 수 (예: 250)
     * // service.importShapefile(file, "korea_regions") // throws IllegalArgumentException
     * ```
     *
     * @param file      .shp 확장자 파일
     * @param layerName 레이어 이름
     * @param batchSize 배치 삽입 크기 (기본값: 1000)
     * @return 임포트된 피처 수
     * @throws IllegalArgumentException 동일 이름 레이어가 이미 존재할 때
     */
    fun importShapefile(file: File, layerName: String, batchSize: Int = 1000): Int {
        val shape = loadShape(file)
        val records = shape.records.toWgs84Records(file)
        val bbox = records.toBoundingBox()

        // 1. 레이어 메타데이터 insert — Virtual Thread 트랜잭션 (중복 검사 포함)
        val layerRecord = transaction {
            require(layerRepo.findByName(layerName) == null) {
                "동일한 이름의 레이어가 이미 존재합니다: $layerName"
            }
            layerRepo.save(
                SpatialLayerRecord(
                    name = layerName,
                    sourceFile = file.absolutePath,
                    srid = TARGET_SRID,
                    geometryType = records.firstOrNull()?.geometry?.geometryType,
                    bboxMinX = bbox?.minLon,
                    bboxMinY = bbox?.minLat,
                    bboxMaxX = bbox?.maxLon,
                    bboxMaxY = bbox?.maxLat,
                    recordCount = records.size,
                )
            )
        }

        // 2. 피처 배치 insert — 배치마다 독립 Virtual Thread 트랜잭션 (부분 실패 시 해당 배치만 롤백)
        val wktWriter = WKTWriter()
        var totalInserted = 0

        for (batch in records.chunked(batchSize)) {
            if (Thread.currentThread().isInterrupted) break

            transaction {
                SpatialFeatureTable.batchInsert(batch) { record ->
                    val wkt = wktWriter.write(record.geometry)
                    val pgGeom = PGgeometry("SRID=$TARGET_SRID;$wkt")

                    this[SpatialFeatureTable.layerId] = layerRecord.id
                    this[SpatialFeatureTable.featureType] = record.geometry.geometryType
                    this[SpatialFeatureTable.geom] = pgGeom
                    this[SpatialFeatureTable.properties] = record.attributes
                    this[SpatialFeatureTable.name] = record.attributes["NAME"]?.toString()
                }
            }
            totalInserted += batch.size
            log.debug { "배치 삽입 완료: $totalInserted / ${records.size}" }
        }
        return totalInserted
    }

    private fun List<ShapeRecord>.toWgs84Records(file: File): List<ShapeRecord> {
        val sourceCrs = resolveSourceCrs(file)
        val targetCrs = CRS.decode(TARGET_CRS_CODE, true)
        val mathTransform = sourceCrs?.toTargetTransform(targetCrs)

        return map { record ->
            val geometry = if (mathTransform == null) {
                record.geometry.copy()
            } else {
                JTS.transform(record.geometry, mathTransform)
            }
            geometry.setSRID(TARGET_SRID)
            record.copy(
                geometry = geometry,
                bbox = geometry.envelopeInternal.toBoundingBox(),
            )
        }
    }

    private fun resolveSourceCrs(file: File): CoordinateReferenceSystem? {
        val dataStore = ShapefileDataStore(file.toURI().toURL())
        return try {
            dataStore.schema?.coordinateReferenceSystem
        } finally {
            dataStore.dispose()
        }
    }

    private fun CoordinateReferenceSystem.toTargetTransform(
        targetCrs: CoordinateReferenceSystem,
    ): MathTransform? =
        if (CRS.equalsIgnoreMetadata(this, targetCrs)) {
            null
        } else {
            CRS.findMathTransform(this, targetCrs, true)
        }

    private fun List<ShapeRecord>.toBoundingBox(): BoundingBox? {
        if (isEmpty()) return null

        val envelope = Envelope()
        forEach { envelope.expandToInclude(it.geometry.envelopeInternal) }
        return envelope.toBoundingBox()
    }

    private fun Envelope.toBoundingBox(): BoundingBox =
        BoundingBox(
            minLat = minY,
            minLon = minX,
            maxLat = maxY,
            maxLon = maxX,
        )
}
