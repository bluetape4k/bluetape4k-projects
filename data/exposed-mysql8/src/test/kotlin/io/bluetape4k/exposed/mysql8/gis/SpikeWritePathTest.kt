package io.bluetape4k.exposed.mysql8.gis

/**
 * ## Spike 결과 (2026-03-28)
 *
 * **선택 경로**: A (ByteArray 직접 바인딩)
 *
 * - ByteArray 바인딩: 성공 — `[4byte SRID LE] + [JTS WKB]`를 `ps.setBytes()`로 직접 INSERT 가능
 * - axis-order 보존: 확인됨 — 단, 읽기 방식에 따라 축 순서가 다름
 *   - `SELECT geom` (raw getBytes): 원본 WKB 순서 유지 (x=lng, y=lat) -- Exposed columnType에 최적
 *   - `ST_AsWKB(geom)`: SRS 축 순서로 반환 (x=lat, y=lng) -- JTS 읽기 시 swap 필요
 *   - `ST_AsWKB(geom, 'axis-order=long-lat')`: JTS 호환 순서 반환 (x=lng, y=lat)
 *   - `ST_Longitude/ST_Latitude`: 항상 올바른 값 반환
 * - SRID mismatch: MySQL이 예외로 거부 (SRID 0 → SRID 4326 컬럼 INSERT 불가)
 * - WKT fallback: 동작 확인됨 (`ST_GeomFromText` + `axis-order=long-lat`)
 *
 * **Task 2/3 담당자에게**: 경로 A로 GeometryColumnType 구현할 것.
 * - write: `buildMysqlInternalFormat(srid, WKBWriter.write(geom))` → `ps.setBytes()`
 * - read: `rs.getBytes()` → 4바이트 SRID skip → `WKBReader.read()` — 축 swap 불필요
 */

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.ByteOrderValues
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKBWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.sql.DriverManager

class SpikeWritePathTest: AbstractMySqlGisTest() {
    companion object: KLogging() {
        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
    }

    private fun buildMysqlInternalFormat(srid: Int, wkb: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(4 + wkb.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(srid)
        buf.put(wkb)
        return buf.array()
    }

    private fun getConnection() = DriverManager.getConnection(
        mysqlContainer.jdbcUrl + "?allowPublicKeyRetrieval=true&useSSL=false",
        mysqlContainer.username,
        mysqlContainer.password
    )

    /**
     * ByteArray 직접 바인딩 (경로 A) 검증.
     *
     * JTS Coordinate(x=lng, y=lat) → WKB [lng, lat] 순서로 기록.
     * MySQL SRID 4326은 INSERT 시 WKB 바이트를 그대로 받아들이지만,
     * ST_AsWKB는 SRS 축 순서(lat, lng)로 반환한다 → JTS 읽기 시 x=lat, y=lng.
     *
     * 따라서 ST_AsWKB 결과를 JTS로 읽을 때 축 swap이 필요하다.
     */
    @Test
    fun `ByteArray 직접 바인딩 -- ST_AsWKB는 축 순서가 뒤집혀 반환됨`() {
        val lng = 126.9780
        val lat = 37.5665
        val point = geometryFactory.createPoint(Coordinate(lng, lat))
        val wkb = WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(point)
        val internal = buildMysqlInternalFormat(4326, wkb)

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS spike_point (id INT PRIMARY KEY, geom GEOMETRY SRID 4326)")
                stmt.execute("DELETE FROM spike_point")
            }

            conn.prepareStatement("INSERT INTO spike_point VALUES (1, ?)").use { ps ->
                ps.setBytes(1, internal)
                ps.executeUpdate()
            }

            // ST_AsWKB는 geographic SRS 축 순서(lat, lng)로 반환
            conn.prepareStatement("SELECT ST_AsWKB(geom) FROM spike_point WHERE id = 1").use { ps ->
                ps.executeQuery().use { rs ->
                    rs.next()
                    val resultWkb = rs.getBytes(1)
                    resultWkb.shouldNotBeNull()
                    val result = WKBReader().read(resultWkb)
                    result.shouldNotBeNull()
                    log.info { "ST_AsWKB 결과: x=${result.coordinate.x}, y=${result.coordinate.y} (lat, lng 순서)" }
                    // ST_AsWKB는 SRS 축 순서(lat, lng)로 반환 → x=lat, y=lng
                    result.coordinate.x shouldBeEqualTo lat
                    result.coordinate.y shouldBeEqualTo lng
                }
            }

            // ST_AsWKB + axis-order=long-lat 옵션으로 JTS 호환 순서 반환
            conn.prepareStatement(
                "SELECT ST_AsWKB(geom, 'axis-order=long-lat') FROM spike_point WHERE id = 1"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    rs.next()
                    val resultWkb = rs.getBytes(1)
                    resultWkb.shouldNotBeNull()
                    val result = WKBReader().read(resultWkb)
                    result.shouldNotBeNull()
                    log.info { "ST_AsWKB(axis-order=long-lat): x=${result.coordinate.x}, y=${result.coordinate.y}" }
                    result.coordinate.x shouldBeEqualTo lng
                    result.coordinate.y shouldBeEqualTo lat
                }
            }
        }
    }

    /**
     * ByteArray 직접 바인딩 후 ST_Longitude/ST_Latitude로 검증.
     * MySQL 8에서 geographic SRS(SRID 4326)의 올바른 좌표 접근 방법.
     */
    @Test
    fun `ByteArray 직접 바인딩 -- ST_Longitude, ST_Latitude 검증`() {
        val lng = 126.9780
        val lat = 37.5665
        val point = geometryFactory.createPoint(Coordinate(lng, lat))
        val wkb = WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(point)
        val internal = buildMysqlInternalFormat(4326, wkb)

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS spike_lonlat (id INT PRIMARY KEY, geom GEOMETRY SRID 4326)")
                stmt.execute("DELETE FROM spike_lonlat")
            }

            conn.prepareStatement("INSERT INTO spike_lonlat VALUES (1, ?)").use { ps ->
                ps.setBytes(1, internal)
                ps.executeUpdate()
            }

            conn.prepareStatement(
                "SELECT ST_Longitude(geom), ST_Latitude(geom) FROM spike_lonlat WHERE id = 1"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    rs.next()
                    val stLng = rs.getDouble(1)
                    val stLat = rs.getDouble(2)
                    log.info { "ST_Longitude=$stLng, ST_Latitude=$stLat" }
                    stLng shouldBeEqualTo lng
                    stLat shouldBeEqualTo lat
                }
            }
        }
    }

    /**
     * ByteArray 직접 바인딩 후 geom 바이너리 직접 읽기 (ST_AsWKB 대신 raw bytes).
     * MySQL에서 geom 컬럼을 getBytes()로 읽으면 Internal Format (4byte SRID + WKB) 반환.
     * 이 WKB도 SRS 축 순서(lat, lng)이므로 읽을 때 swap 필요.
     */
    @Test
    fun `ByteArray 직접 바인딩 -- raw Internal Format 라운드트립`() {
        val lng = 126.9780
        val lat = 37.5665
        val point = geometryFactory.createPoint(Coordinate(lng, lat))
        val wkb = WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(point)
        val internal = buildMysqlInternalFormat(4326, wkb)

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS spike_raw (id INT PRIMARY KEY, geom GEOMETRY SRID 4326)")
                stmt.execute("DELETE FROM spike_raw")
            }

            conn.prepareStatement("INSERT INTO spike_raw VALUES (1, ?)").use { ps ->
                ps.setBytes(1, internal)
                ps.executeUpdate()
            }

            // geom 컬럼을 직접 getBytes()로 읽기 → Internal Format
            conn.prepareStatement("SELECT geom FROM spike_raw WHERE id = 1").use { ps ->
                ps.executeQuery().use { rs ->
                    rs.next()
                    val rawBytes = rs.getBytes(1)
                    rawBytes.shouldNotBeNull()
                    // 첫 4바이트: SRID (LE)
                    val sridBuf = ByteBuffer.wrap(rawBytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN)
                    val srid = sridBuf.getInt()
                    srid shouldBeEqualTo 4326
                    // 나머지: WKB (SRS 축 순서 lat, lng)
                    val resultWkb = rawBytes.copyOfRange(4, rawBytes.size)
                    val result = WKBReader().read(resultWkb)
                    result.shouldNotBeNull()
                    log.info { "raw Internal Format: srid=$srid, x=${result.coordinate.x}, y=${result.coordinate.y}" }
                    // raw getBytes()는 원본 WKB 바이트 그대로 반환 (lng, lat 순서 유지)
                    result.coordinate.x shouldBeEqualTo lng
                    result.coordinate.y shouldBeEqualTo lat
                }
            }
        }
    }

    @Test
    fun `SRID mismatch 동작 확인`() {
        val point = geometryFactory.createPoint(Coordinate(126.9780, 37.5665))
        val wkb = WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(point)
        // SRID 0으로 삽입 시도 (컬럼은 SRID 4326)
        val internal = buildMysqlInternalFormat(0, wkb)

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS spike_srid (id INT PRIMARY KEY, geom GEOMETRY SRID 4326)")
                stmt.execute("DELETE FROM spike_srid")
            }

            try {
                conn.prepareStatement("INSERT INTO spike_srid VALUES (1, ?)").use { ps ->
                    ps.setBytes(1, internal)
                    ps.executeUpdate()
                }
                log.info { "SRID mismatch: 자동 수용됨 (예외 없음)" }
            } catch (e: Exception) {
                log.info { "SRID mismatch: 예외 발생 = ${e.message}" }
            }
        }
    }

    @Test
    fun `WKT fallback -- ST_GeomFromText 방식 동작 확인`() {
        val lng = 126.9780
        val lat = 37.5665

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS spike_wkt (id INT PRIMARY KEY, geom GEOMETRY SRID 4326)")
                stmt.execute("DELETE FROM spike_wkt")
            }

            conn.prepareStatement(
                "INSERT INTO spike_wkt VALUES (1, ST_GeomFromText(?, 4326, 'axis-order=long-lat'))"
            ).use { ps ->
                ps.setString(1, "POINT($lng $lat)")
                ps.executeUpdate()
            }

            conn.prepareStatement(
                "SELECT ST_Longitude(geom), ST_Latitude(geom) FROM spike_wkt WHERE id = 1"
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    rs.next()
                    val stLng = rs.getDouble(1)
                    val stLat = rs.getDouble(2)
                    log.info { "WKT fallback: ST_Longitude=$stLng, ST_Latitude=$stLat" }
                    stLng shouldBeEqualTo lng
                    stLat shouldBeEqualTo lat
                }
            }
        }
    }
}
