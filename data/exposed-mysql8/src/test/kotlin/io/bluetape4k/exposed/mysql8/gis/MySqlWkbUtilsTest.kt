package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MySqlWkbUtilsTest {

    companion object: KLogging() {
        private val geometryFactory = GeometryFactory(PrecisionModel(), SRID_WGS84)
    }

    @Test
    fun `parseMySqlInternalGeometry - Point 라운드트립`() {
        val point = geometryFactory.createPoint(Coordinate(127.0, 37.5))
        point.srid = SRID_WGS84

        val bytes = MySqlWkbUtils.buildMySqlInternalGeometry(point)
        val parsed = MySqlWkbUtils.parseMySqlInternalGeometry(bytes)

        parsed.equalsExact(point, 1e-10).shouldBeTrue()
        parsed.srid.shouldBeEqualTo(SRID_WGS84)
    }

    @Test
    fun `parseMySqlInternalGeometry - Polygon 라운드트립`() {
        val coords = arrayOf(
            Coordinate(0.0, 0.0),
            Coordinate(10.0, 0.0),
            Coordinate(10.0, 10.0),
            Coordinate(0.0, 10.0),
            Coordinate(0.0, 0.0),
        )
        val polygon = geometryFactory.createPolygon(coords)
        polygon.srid = SRID_WGS84

        val bytes = MySqlWkbUtils.buildMySqlInternalGeometry(polygon)
        val parsed = MySqlWkbUtils.parseMySqlInternalGeometry(bytes)

        parsed.equalsExact(polygon, 1e-10).shouldBeTrue()
        parsed.srid.shouldBeEqualTo(SRID_WGS84)
    }

    @Test
    fun `parseMySqlInternalGeometry - SRID가 올바르게 설정됨`() {
        val point = geometryFactory.createPoint(Coordinate(1.0, 2.0))
        val customSrid = 3857

        val bytes = MySqlWkbUtils.buildMySqlInternalGeometry(point, srid = customSrid)
        val parsed = MySqlWkbUtils.parseMySqlInternalGeometry(bytes)

        parsed.srid.shouldBeEqualTo(customSrid)
    }

    @Test
    fun `parseMySqlInternalGeometry - 빈 배열은 예외`() {
        assertThrows<IllegalArgumentException> {
            MySqlWkbUtils.parseMySqlInternalGeometry(byteArrayOf())
        }
    }

    @Test
    fun `parseMySqlInternalGeometry - 4바이트 미만 입력은 예외`() {
        for (size in 1..3) {
            assertThrows<IllegalArgumentException> {
                MySqlWkbUtils.parseMySqlInternalGeometry(ByteArray(size))
            }
        }
        // 4바이트도 WKB 부분이 없으므로 예외
        assertThrows<IllegalArgumentException> {
            MySqlWkbUtils.parseMySqlInternalGeometry(ByteArray(4))
        }
    }

    @Test
    fun `buildMySqlInternalGeometry - 4바이트 SRID LE prefix 확인`() {
        val point = geometryFactory.createPoint(Coordinate(1.0, 2.0))
        val srid = 4326

        val bytes = MySqlWkbUtils.buildMySqlInternalGeometry(point, srid)

        // 첫 4바이트가 LE로 인코딩된 SRID
        val parsedSrid = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        parsedSrid.shouldBeEqualTo(srid)

        // 5번째 바이트부터 WKB 시작 (첫 바이트는 byte order: 0x01 = LE)
        bytes[4].shouldBeEqualTo(0x01.toByte())
    }

    @Test
    fun `buildMySqlInternalGeometry - 기본 SRID는 4326`() {
        val point = geometryFactory.createPoint(Coordinate(1.0, 2.0))

        val bytes = MySqlWkbUtils.buildMySqlInternalGeometry(point)

        val parsedSrid = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        parsedSrid.shouldBeEqualTo(SRID_WGS84)
    }
}
