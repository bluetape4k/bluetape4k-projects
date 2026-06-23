package io.bluetape4k.science.exposed.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.exposed.AbstractPostgisTest
import io.bluetape4k.science.exposed.model.SpatialFeatureRecord
import io.bluetape4k.science.exposed.model.SpatialLayerRecord
import io.bluetape4k.science.exposed.schema.SpatialFeatureTable
import io.bluetape4k.science.exposed.schema.SpatialLayerTable
import io.bluetape4k.science.exposed.service.ShapefileImportService
import io.bluetape4k.science.projection.transform
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.geotools.api.data.Transaction as GeoToolsTransaction
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.referencing.CRS
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import java.io.File
import java.io.Serializable
import java.nio.file.Path
import kotlin.math.abs

/**
 * [SpatialLayerRepository] 및 [SpatialFeatureRepository] 통합 테스트.
 *
 * PostGIS 컨테이너(`postgis/postgis:16-3.4`)를 Testcontainers로 구동하여
 * DDL 생성, 레이어/피처 저장·조회, Shapefile 임포트를 검증합니다.
 */
class SpatialFeatureRepositoryTest: AbstractPostgisTest() {

    companion object: KLogging() {
        private const val SRID = 4326
        private val geometryFactory = GeometryFactory()

        /**
         * JTS Point 생성 헬퍼. 좌표 순서: x=경도(lng), y=위도(lat)
         */
        fun point(lng: Double, lat: Double): Point =
            geometryFactory.createPoint(Coordinate(lng, lat))
    }

    private val layerRepo = SpatialLayerRepository()
    private val featureRepo = SpatialFeatureRepository()

    @Test
    fun `DDL - SpatialLayerTable과 SpatialFeatureTable 생성 확인`() {
        // BeforeAll에서 이미 생성되었으므로, 테이블이 존재함을 확인
        transaction(db) {
            val layerCount = SpatialLayerTable.selectAll().count()
            log.debug { "SpatialLayerTable 레코드 수: $layerCount" }
            layerCount shouldBeEqualTo 0L
        }
    }

    @Test
    fun `레이어 저장 및 ID 조회`() {
        transaction(db) {
            val record = SpatialLayerRecord(
                name = "test-layer-${System.currentTimeMillis()}",
                description = "테스트 레이어",
                srid = SRID,
                geometryType = "POINT",
                bboxMinX = 124.0,
                bboxMinY = 33.0,
                bboxMaxX = 132.0,
                bboxMaxY = 43.0,
                recordCount = 0,
            )
            val saved = layerRepo.save(record)

            saved.id shouldBeGreaterThan 0L
            saved.name shouldBeEqualTo record.name

            val found = layerRepo.findByIdOrNull(saved.id)
            found.shouldNotBeNull()
            found.name shouldBeEqualTo record.name
            found.description shouldBeEqualTo record.description
            found.geometryType shouldBeEqualTo "POINT"

            log.debug { "저장된 레이어: $saved" }

            // cleanup
            layerRepo.deleteById(saved.id)
        }
    }

    @Test
    fun `레이어 이름으로 조회`() {
        val uniqueName = "named-layer-${System.currentTimeMillis()}"

        transaction(db) {
            val record = SpatialLayerRecord(
                name = uniqueName,
                description = "이름 조회 테스트",
                srid = SRID,
            )
            val saved = layerRepo.save(record)

            val found = layerRepo.findByName(uniqueName)
            found.shouldNotBeNull()
            found.name shouldBeEqualTo uniqueName

            val notFound = layerRepo.findByName("존재하지않는레이어")
            notFound.shouldBeNull()

            // cleanup
            layerRepo.deleteById(saved.id)
        }
    }

    @Test
    fun `피처 저장 및 조회`() {
        transaction(db) {
            val layerRecord = SpatialLayerRecord(
                name = "feature-layer-${System.currentTimeMillis()}",
                srid = SRID,
                geometryType = "POINT",
            )
            val savedLayer = layerRepo.save(layerRecord)

            val pgPoint = point(lng = 126.9780, lat = 37.5665)
            val featureRecord = SpatialFeatureRecord(
                layerId = savedLayer.id,
                featureType = "POINT",
                geom = pgPoint,
                properties = mapOf("name" to "서울", "code" to "11"),
                name = "서울특별시",
            )
            val savedFeature = featureRepo.save(featureRecord)

            savedFeature.id shouldBeGreaterThan 0L
            savedFeature.layerId shouldBeEqualTo savedLayer.id

            val found = featureRepo.findByIdOrNull(savedFeature.id)
            found.shouldNotBeNull()
            found.featureType shouldBeEqualTo "POINT"
            found.name shouldBeEqualTo "서울특별시"
            (found.geom as? Point)?.let { pt ->
                log.debug { "조회된 Point: x=${pt.x}, y=${pt.y}, geometryType=${pt.geometryType}" }
            }

            log.debug { "저장된 피처: $savedFeature" }

            // cleanup
            featureRepo.deleteById(savedFeature.id)
            layerRepo.deleteById(savedLayer.id)
        }
    }

    @Test
    fun `레이어별 피처 목록 조회`() {
        transaction(db) {
            val layerRecord = SpatialLayerRecord(
                name = "multi-feature-layer-${System.currentTimeMillis()}",
                srid = SRID,
                geometryType = "POINT",
            )
            val savedLayer = layerRepo.save(layerRecord)

            val cities = listOf(
                Triple("서울", 126.9780, 37.5665),
                Triple("부산", 129.0756, 35.1796),
                Triple("인천", 126.7052, 37.4563),
            )

            val savedIds = cities.map { (cityName, lng, lat) ->
                val feature = SpatialFeatureRecord(
                    layerId = savedLayer.id,
                    featureType = "POINT",
                    geom = point(lng, lat),
                    properties = mapOf("city" to cityName),
                    name = cityName,
                )
                featureRepo.save(feature).id
            }

            val all = featureRepo.findAll { SpatialFeatureTable.layerId eq savedLayer.id }
            all.size shouldBeEqualTo 3

            log.debug { "레이어 피처 목록: ${all.map { it.name }}" }

            // cleanup
            savedIds.forEach { featureRepo.deleteById(it) }
            layerRepo.deleteById(savedLayer.id)
        }
    }

    @Test
    fun `Shapefile 임포트 - 파일 존재 시 DB 적재`() {
        val shpFile = File(
            javaClass.classLoader.getResource("data/shp_v5/harbors/harbour_new.shp")?.file
                ?: return
        )

        if (!shpFile.exists()) {
            log.debug { "Shapefile 없음, 테스트 건너뜀: ${shpFile.absolutePath}" }
            return
        }

        val importService = ShapefileImportService(layerRepo, featureRepo)
        val layerName = "harbors-import-${System.currentTimeMillis()}"

        // Virtual Thread 트랜잭션으로 실행 — suspend 불필요
        val count = importService.importShapefile(shpFile, layerName)
        count shouldBeGreaterThan 0
        log.debug { "임포트된 피처 수: $count" }

        transaction(db) {
            val layer = layerRepo.findByName(layerName)
            layer.shouldNotBeNull()
            layer.geometryType.shouldNotBeNull()

            val features = featureRepo.findAll { SpatialFeatureTable.layerId eq layer.id }
            features.size shouldBeEqualTo count

            log.debug { "임포트된 레이어: $layer, 피처 수: ${features.size}" }

            // cleanup
            features.forEach { featureRepo.deleteById(it.id) }
            layerRepo.deleteById(layer.id)
        }
    }

    @Test
    fun `Shapefile import transforms projected CRS to WGS84`(@TempDir dir: Path) {
        val expectedLon = 126.9780
        val expectedLat = 37.5665
        val shpFile = createWebMercatorPointShapefile(dir, expectedLon, expectedLat)
        val importService = ShapefileImportService(layerRepo, featureRepo)
        val layerName = "web-mercator-import-${System.currentTimeMillis()}"

        val count = importService.importShapefile(shpFile, layerName)
        count shouldBeEqualTo 1

        transaction(db) {
            val layer = layerRepo.findByName(layerName)
            layer.shouldNotBeNull()
            layer.srid shouldBeEqualTo SRID

            val (srid, lon, lat) = queryStoredPoint(layer.id)
            srid shouldBeEqualTo SRID
            abs(lon - expectedLon) shouldBeLessThan 1e-5
            abs(lat - expectedLat) shouldBeLessThan 1e-5
            abs((layer.bboxMinX ?: Double.NaN) - expectedLon) shouldBeLessThan 1e-5
            abs((layer.bboxMinY ?: Double.NaN) - expectedLat) shouldBeLessThan 1e-5
            abs((layer.bboxMaxX ?: Double.NaN) - expectedLon) shouldBeLessThan 1e-5
            abs((layer.bboxMaxY ?: Double.NaN) - expectedLat) shouldBeLessThan 1e-5

            val features = featureRepo.findAll { SpatialFeatureTable.layerId eq layer.id }
            features.forEach { featureRepo.deleteById(it.id) }
            layerRepo.deleteById(layer.id)
        }
    }

    private fun createWebMercatorPointShapefile(dir: Path, lon: Double, lat: Double): File {
        val shpFile = dir.resolve("web-mercator-point.shp").toFile()
        val sourceCrs = CRS.decode("EPSG:3857", true)
        val (x, y) = transform("EPSG:4326", "EPSG:3857", lon, lat)

        val typeBuilder = SimpleFeatureTypeBuilder().apply {
            setName("web_mercator_points")
            setCRS(sourceCrs)
            add("the_geom", Point::class.java)
            add("NAME", String::class.java)
        }
        val featureType = typeBuilder.buildFeatureType()
        val params = mapOf<String, Serializable>("url" to shpFile.toURI().toURL())

        val dataStore = ShapefileDataStoreFactory().createNewDataStore(params) as ShapefileDataStore
        try {
            dataStore.createSchema(featureType)
            dataStore.forceSchemaCRS(sourceCrs)
            dataStore.getFeatureWriterAppend(GeoToolsTransaction.AUTO_COMMIT).use { writer ->
                val feature = writer.next()
                feature.setAttribute("the_geom", point(x, y))
                feature.setAttribute("NAME", "Seoul")
                writer.write()
            }
        } finally {
            dataStore.dispose()
        }
        return shpFile
    }

    private fun queryStoredPoint(layerId: Long): Triple<Int, Double, Double> {
        val sql = "SELECT ST_SRID(geom), ST_X(geom), ST_Y(geom) FROM spatial_features WHERE layer_id=?"
        val conn = org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager.current().connection.connection
            as java.sql.Connection
        return conn.prepareStatement(sql).use { ps ->
            ps.setLong(1, layerId)
            ps.executeQuery().use { rs ->
                rs.next()
                Triple(rs.getInt(1), rs.getDouble(2), rs.getDouble(3))
            }
        }
    }
}
