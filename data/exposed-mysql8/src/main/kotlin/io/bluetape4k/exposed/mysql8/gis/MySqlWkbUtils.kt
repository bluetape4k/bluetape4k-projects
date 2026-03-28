package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.logging.KLogging
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.ByteOrderValues
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKBWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** WGS 84 SRID 상수 */
const val SRID_WGS84: Int = 4326

/**
 * MySQL Internal Geometry Format 변환 유틸리티.
 *
 * MySQL 8.0은 공간 데이터를 `[4바이트 SRID (LE)] + [표준 WKB]` 형식으로 저장한다.
 */
object MySqlWkbUtils: KLogging() {

    /**
     * MySQL Internal Geometry Format ByteArray -> JTS Geometry 변환.
     *
     * @param bytes MySQL Internal Format (4바이트 LE SRID + WKB). 최소 5바이트 이상이어야 한다.
     * @return JTS Geometry (SRID 설정됨)
     */
    fun parseMySqlInternalGeometry(bytes: ByteArray): Geometry {
        require(bytes.size >= 5) {
            "MySQL internal geometry format requires at least 5 bytes (4 SRID + 1 WKB), got ${bytes.size}"
        }
        val srid = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val wkb = bytes.copyOfRange(4, bytes.size)
        val geometry = WKBReader().read(wkb)
        geometry.srid = srid
        return geometry
    }

    /**
     * JTS Geometry -> MySQL Internal Geometry Format ByteArray 변환.
     *
     * @param geometry JTS Geometry
     * @param srid SRID (기본값: 4326)
     * @return MySQL Internal Format (4바이트 LE SRID + Little-Endian WKB)
     */
    fun buildMySqlInternalGeometry(geometry: Geometry, srid: Int = SRID_WGS84): ByteArray {
        val wkb = WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN).write(geometry)
        val buf = ByteBuffer.allocate(4 + wkb.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(srid)
        buf.put(wkb)
        return buf.array()
    }
}
