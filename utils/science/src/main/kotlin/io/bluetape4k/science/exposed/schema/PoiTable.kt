package io.bluetape4k.science.exposed.schema

import io.bluetape4k.exposed.core.auditable.AuditableLongIdTable
import io.bluetape4k.exposed.core.jackson3.jacksonb
import io.bluetape4k.exposed.postgresql.postgis.geoPoint

/**
 * 관심 지점(Point of Interest)을 저장하는 Exposed 테이블입니다.
 *
 * 이름, 카테고리, PostGIS POINT 위치, JSONB 속성을 관리합니다.
 */
object PoiTable : AuditableLongIdTable("poi") {

    /** POI 이름 */
    val name = varchar("name", 255)

    /** 카테고리 (선택) */
    val category = varchar("category", 100).nullable()

    /** 위치 (PostGIS POINT) */
    val location = geoPoint("location")

    /** 부가 속성 (JSONB) */
    val properties = jacksonb<Map<String, Any?>>("properties")
}
