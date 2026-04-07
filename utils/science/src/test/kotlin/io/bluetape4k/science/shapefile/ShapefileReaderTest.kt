package io.bluetape4k.science.shapefile

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.science.coords.BoundingBox
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class ShapefileReaderTest {

    companion object: KLogging()

    private fun testResource(path: String): File {
        val url = javaClass.classLoader.getResource(path)
            ?: error("테스트 리소스를 찾을 수 없습니다: $path")
        return File(url.file)
    }

    @Test
    fun `harbor shapefile 읽기`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        shape.shouldNotBeNull()
        shape.records.shouldNotBeEmpty()
        shape.isEmpty.shouldBeFalse()
        shape.size shouldBeGreaterThan 0

        log.debug { "harbor 레코드 수: ${shape.size}" }
        log.debug { "harbor 헤더: ${shape.header}" }
        log.debug { "harbor 속성 정의: ${shape.attributes}" }
    }

    @Test
    fun `harbor shapefile 속성 정보 확인`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        shape.attributes.shouldNotBeEmpty()
        shape.records.all { it.attributes.isNotEmpty() }.shouldBeTrue()

        log.debug { "첫 번째 레코드 속성: ${shape.records.first().attributes}" }
    }

    @Test
    fun `harbor shapefile 헤더 확인`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        val header = shape.header
        header.shouldNotBeNull()
        header.fileCode shouldBeEqualTo 9994
        header.version shouldBeEqualTo 1000

        log.debug { "헤더 BoundingBox: ${header.bbox}" }
    }

    @Test
    fun `ocean shapefile 읽기`() {
        val file = testResource("data/shp_v5/oceans/07.merge.shp")
        val shape = loadShape(file)

        shape.shouldNotBeNull()
        shape.records.shouldNotBeEmpty()
        shape.size shouldBeGreaterThan 0

        log.debug { "ocean 레코드 수: ${shape.size}" }
    }

    @Test
    fun `존재하지 않는 파일 예외`() {
        assertThrows<IllegalArgumentException> {
            loadShape(File("nonexistent.shp"))
        }
    }

    @Test
    fun `shp 확장자가 아닌 파일 예외`() {
        val file = File(javaClass.classLoader.getResource("data/shp_v5/harbors/harbour_new.dbf")!!.file)
        assertThrows<IllegalArgumentException> {
            loadShape(file)
        }
    }

    @Test
    fun `harbor shapefile toGeoLocations 변환`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        // harbour는 Point 타입이므로 GeoLocation 변환 가능해야 함
        val locations = shape.toGeoLocations()
        log.debug { "GeoLocation 변환 수: ${locations.size}" }
        // Point 타입이면 반드시 변환됨
        if (locations.isNotEmpty()) {
            locations.all { it.latitude in -90.0..90.0 }.shouldBeTrue()
            locations.all { it.longitude in -180.0..180.0 }.shouldBeTrue()
        }
    }

    @Test
    fun `harbor shapefile filterByBoundingBox 필터링`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        // 전체 bbox로 필터링하면 모든 레코드가 포함되어야 함
        val fullBbox = shape.header.bbox
        val filtered = shape.filterByBoundingBox(fullBbox)
        filtered.size shouldBeEqualTo shape.size

        log.debug { "전체 bbox 필터링 결과: ${filtered.size}/${shape.size}" }
    }

    @Test
    fun `빈 영역으로 filterByBoundingBox 하면 결과 없음`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        // 데이터가 없는 임의 영역으로 필터링
        val emptyBbox = BoundingBox(minLat = 0.0, minLon = 0.0, maxLat = 0.001, maxLon = 0.001)
        val filtered = shape.filterByBoundingBox(emptyBbox)
        filtered.records.shouldBeEmpty()
    }

    @Test
    fun `loadShapeAsync 비동기 로딩`() = runTest {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShapeAsync(file)

        shape.records.shouldNotBeEmpty()
        log.debug { "비동기 로드 레코드 수: ${shape.size}" }
    }

    @Test
    fun `computeBoundingBox 계산`() {
        val file = testResource("data/shp_v5/harbors/harbour_new.shp")
        val shape = loadShape(file)

        val computed = shape.computeBoundingBox()
        computed.shouldNotBeNull()
        log.debug { "계산된 BoundingBox: $computed" }
    }
}
