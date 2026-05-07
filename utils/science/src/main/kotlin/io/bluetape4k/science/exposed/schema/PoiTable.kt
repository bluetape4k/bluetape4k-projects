package io.bluetape4k.science.exposed.schema

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.bluetape4k.science.exposed.support.geoPoint
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

private val poiMapper = jacksonObjectMapper()

/**
 * 관심 지점(Point of Interest)을 저장하는 Exposed 테이블입니다.
 *
 * 이름, 카테고리, PostGIS POINT 위치, JSONB 속성을 관리합니다.
 *
 * ```kotlin
 * // 테이블 생성 및 데이터 삽입
 * transaction {
 *     SchemaUtils.create(PoiTable)
 *     PoiTable.insertAndGetId {
 *         it[name] = "서울시청"
 *         it[category] = "GOVERNMENT"
 *         it[properties] = mapOf("address" to "서울특별시 중구")
 *     }
 * }
 * ```
 */
object PoiTable: LongIdTable("poi") {

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    /** POI 이름 */
    val name = varchar("name", 255)

    /** 카테고리 (선택) */
    val category = varchar("category", 100).nullable()

    /** 위치 (PostGIS POINT) */
    val location = geoPoint("location")

    /** 부가 속성 (JSONB) */
    val properties = jsonb<Map<String, Any?>>("properties",
        { poiMapper.writeValueAsString(it) },
        { poiMapper.readValue(it, object: TypeReference<Map<String, Any?>>() {}) }
    )
}
