package io.bluetape4k.exposed.mysql8.gis

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon

//
// ─── 관계 함수 ────────────────────────────────────────────────────────────────
//

/**
 * MySQL `ST_Contains` 함수를 사용하여 이 geometry가 other를 완전히 포함하는지 확인한다.
 *
 * MySQL dialect 전용.
 *
 * ```kotlin
 * val containsQuery = ZoneTable.selectAll()
 *     .where { ZoneTable.area.stContains(PointTable.location) }
 * // SQL: ... WHERE ST_Contains(zones.area, point_table.location)
 * ```
 *
 * @param other 포함 여부를 확인할 대상 geometry 컬럼
 * @return 포함 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stContains(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stContains는 MySQL dialect에서만 지원됩니다." }
    return StContainsOp(this, other)
}

/**
 * MySQL `ST_Within` 함수를 사용하여 이 geometry가 other 내부에 있는지 확인한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 포함 관계를 확인할 대상 geometry 컬럼
 * @return 포함 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stWithin(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stWithin는 MySQL dialect에서만 지원됩니다." }
    return StWithinOp(this, other)
}

/**
 * MySQL `ST_Intersects` 함수를 사용하여 두 geometry가 교차하는지 확인한다.
 *
 * 포함 관계 및 부분 겹침 모두 true를 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 교차 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stIntersects(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stIntersects는 MySQL dialect에서만 지원됩니다." }
    return StIntersectsOp(this, other)
}

/**
 * MySQL `ST_Disjoint` 함수를 사용하여 두 geometry가 완전히 분리되어 있는지 확인한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 분리 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stDisjoint(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stDisjoint는 MySQL dialect에서만 지원됩니다." }
    return StDisjointOp(this, other)
}

/**
 * MySQL `ST_Overlaps` 함수를 사용하여 두 geometry가 부분적으로 겹치는지 확인한다.
 *
 * 한쪽이 다른 쪽을 완전히 포함하는 경우 false를 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 겹침 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stOverlaps(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stOverlaps는 MySQL dialect에서만 지원됩니다." }
    return StOverlapsOp(this, other)
}

/**
 * MySQL `ST_Touches` 함수를 사용하여 두 geometry가 경계에서만 접촉하는지 확인한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 접촉 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stTouches(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stTouches는 MySQL dialect에서만 지원됩니다." }
    return StTouchesOp(this, other)
}

/**
 * MySQL `ST_Crosses` 함수를 사용하여 두 geometry가 교차하는지 확인한다.
 *
 * 서로 다른 차원의 geometry 간에 적용된다.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 교차 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stCrosses(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stCrosses는 MySQL dialect에서만 지원됩니다." }
    return StCrossesOp(this, other)
}

/**
 * MySQL `ST_Equals` 함수를 사용하여 두 geometry가 공간적으로 동일한지 확인한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 동일 여부 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stEquals(other: Column<out Geometry>): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stEquals는 MySQL dialect에서만 지원됩니다." }
    return StEqualsOp(this, other)
}

/**
 * MySQL `ST_Distance(left, right) <= distance` 조건을 사용하여
 * 두 geometry가 지정한 거리 이내인지 확인한다.
 *
 * MySQL dialect 전용.
 *
 * ```kotlin
 * val nearbyPlaces = PlaceTable.selectAll()
 *     .where { PlaceTable.location.stDWithin(myLocationColumn, 1000.0) }
 * // SQL: ... WHERE ST_Distance(places.location, my_location) <= 1000.0
 * ```
 *
 * @param other 비교 대상 geometry 컬럼
 * @param distance 최대 거리 (미터 단위, SRID 4326 기준)
 * @return 거리 조건 [Op]
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stDWithin(other: Column<out Geometry>, distance: Double): Op<Boolean> {
    check(currentDialect is MysqlDialect) { "stDWithin는 MySQL dialect에서만 지원됩니다." }
    return StDWithinOp(this, other, distance)
}

//
// ─── 측정 함수 ────────────────────────────────────────────────────────────────
//

/**
 * MySQL `ST_Distance` 함수를 사용하여 두 geometry 간의 거리를 미터 단위로 반환한다.
 *
 * geodetic(지구 타원체 기반) 거리 계산. SRID 4326 전용.
 *
 * MySQL dialect 전용.
 *
 * ```kotlin
 * val distanceExpr = PlaceTable.location.stDistance(OtherPlace.location)
 * val results = PlaceTable
 *     .select(PlaceTable.id, distanceExpr)
 *     .orderBy(distanceExpr)
 *     .limit(10)
 *     .toList()
 * ```
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 거리 표현식 (미터 단위)
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stDistance(other: Column<out Geometry>): Expression<Double> {
    check(currentDialect is MysqlDialect) { "stDistance는 MySQL dialect에서만 지원됩니다." }
    return StDistanceExpr(this, other)
}

/**
 * MySQL `ST_Distance_Sphere` 함수를 사용하여 두 geometry 간의 구면 거리를 미터 단위로 반환한다.
 *
 * spherical(구면 모델 기반) 거리 계산. SRID 4326 전용.
 *
 * MySQL dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 구면 거리 표현식 (미터 단위)
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stDistanceSphere(other: Column<out Geometry>): Expression<Double> {
    check(currentDialect is MysqlDialect) { "stDistanceSphere는 MySQL dialect에서만 지원됩니다." }
    return StDistanceSphereExpr(this, other)
}

/**
 * MySQL `ST_Length` 함수를 사용하여 LineString의 길이를 미터 단위로 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @return 길이 표현식 (미터 단위)
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@JvmName("stLengthLineString")
fun Column<out LineString>.stLength(): Expression<Double> {
    check(currentDialect is MysqlDialect) { "stLength는 MySQL dialect에서만 지원됩니다." }
    return StLengthExpr(this)
}

/**
 * MySQL `ST_Length` 함수를 사용하여 MultiLineString의 총 길이를 미터 단위로 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @return 총 길이 표현식 (미터 단위)
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@JvmName("stLengthMultiLineString")
fun Column<out MultiLineString>.stLength(): Expression<Double> {
    check(currentDialect is MysqlDialect) { "stLength는 MySQL dialect에서만 지원됩니다." }
    return StLengthExpr(this)
}

/**
 * MySQL `ST_Area` 함수를 사용하여 Polygon의 넓이를 제곱미터 단위로 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @return 넓이 표현식 (제곱미터 단위)
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@JvmName("stAreaPolygon")
fun Column<out Polygon>.stArea(): Expression<Double> {
    check(currentDialect is MysqlDialect) { "stArea는 MySQL dialect에서만 지원됩니다." }
    return StAreaExpr(this)
}

/**
 * MySQL `ST_Area` 함수를 사용하여 MultiPolygon의 총 넓이를 제곱미터 단위로 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @return 총 넓이 표현식 (제곱미터 단위)
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@JvmName("stAreaMultiPolygon")
fun Column<out MultiPolygon>.stArea(): Expression<Double> {
    check(currentDialect is MysqlDialect) { "stArea는 MySQL dialect에서만 지원됩니다." }
    return StAreaExpr(this)
}

//
// ─── 속성/변환 함수 ───────────────────────────────────────────────────────────
//

/**
 * MySQL `ST_AsText` 함수를 사용하여 geometry를 WKT 문자열로 반환한다.
 *
 * MySQL dialect 전용.
 *
 * ```kotlin
 * val wktExpr = PlaceTable.location.stAsText()
 * val results = PlaceTable.select(PlaceTable.id, wktExpr).toList()
 * // results[0][wktExpr] == "POINT(126.9779 37.5665)"
 * ```
 *
 * @return WKT 문자열 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stAsText(): ExpressionWithColumnType<String> {
    check(currentDialect is MysqlDialect) { "stAsText는 MySQL dialect에서만 지원됩니다." }
    return StAsTextExpr(this)
}

/**
 * MySQL `ST_SRID` 함수를 사용하여 geometry의 SRID를 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @return SRID 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stSrid(): ExpressionWithColumnType<Int> {
    check(currentDialect is MysqlDialect) { "stSrid는 MySQL dialect에서만 지원됩니다." }
    return StSridExpr(this)
}

/**
 * MySQL `ST_GeometryType` 함수를 사용하여 geometry의 타입 이름을 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @return geometry 타입 이름 표현식 (예: "ST_Point", "ST_Polygon")
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stGeometryType(): ExpressionWithColumnType<String> {
    check(currentDialect is MysqlDialect) { "stGeometryType는 MySQL dialect에서만 지원됩니다." }
    return StGeometryTypeExpr(this)
}

/**
 * MySQL `ST_Buffer` 함수를 사용하여 geometry 주변에 버퍼 영역을 생성한다.
 *
 * geographic SRS(SRID 4326)에서는 Point에만 안전하게 사용할 수 있다.
 * Polygon/LineString 등에서 사용하면 예상치 못한 결과가 발생할 수 있다.
 *
 * MySQL dialect 전용.
 *
 * @param distance 버퍼 거리 (미터 단위)
 * @return 버퍼 영역 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@Deprecated("ST_Buffer는 geographic SRS(4326)에서 Point에만 안전합니다. Polygon/LineString 사용 시 예상치 못한 결과가 발생할 수 있습니다.")
fun Column<out Geometry>.stBuffer(distance: Double): Expression<String> {
    check(currentDialect is MysqlDialect) { "stBuffer는 MySQL dialect에서만 지원됩니다." }
    return StBufferExpr(this, distance)
}

/**
 * MySQL `ST_Union` 함수를 사용하여 두 geometry의 합집합을 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 합집합 대상 geometry 컬럼
 * @return 합집합 geometry 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stUnion(other: Column<out Geometry>): Expression<String> {
    check(currentDialect is MysqlDialect) { "stUnion는 MySQL dialect에서만 지원됩니다." }
    return StUnionExpr(this, other)
}

/**
 * MySQL `ST_Difference` 함수를 사용하여 이 geometry에서 other를 뺀 차집합을 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 차집합 대상 geometry 컬럼
 * @return 차집합 geometry 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stDifference(other: Column<out Geometry>): Expression<String> {
    check(currentDialect is MysqlDialect) { "stDifference는 MySQL dialect에서만 지원됩니다." }
    return StDifferenceExpr(this, other)
}

/**
 * MySQL `ST_Intersection` 함수를 사용하여 두 geometry의 교집합을 반환한다.
 *
 * MySQL dialect 전용.
 *
 * @param other 교집합 대상 geometry 컬럼
 * @return 교집합 geometry 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
fun Column<out Geometry>.stIntersection(other: Column<out Geometry>): Expression<String> {
    check(currentDialect is MysqlDialect) { "stIntersection는 MySQL dialect에서만 지원됩니다." }
    return StIntersectionExpr(this, other)
}

/**
 * MySQL `ST_Centroid` 함수를 사용하여 geometry의 도심(centroid) 점을 반환한다.
 *
 * geographic SRS(SRID 4326)에서는 지원되지 않는다. Cartesian SRS(SRID 0) 전용.
 * SRID 4326 데이터에 사용하면 MySQL 오류가 발생한다.
 *
 * MySQL dialect 전용.
 *
 * @return 도심 geometry 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@Deprecated("ST_Centroid는 geographic SRS(4326)에서 지원되지 않습니다. Cartesian SRS 전용.")
fun Column<out Geometry>.stCentroid(): Expression<String> {
    check(currentDialect is MysqlDialect) { "stCentroid는 MySQL dialect에서만 지원됩니다." }
    return StCentroidExpr(this)
}

/**
 * MySQL `ST_Envelope` 함수를 사용하여 geometry의 최소 경계 사각형(MBR)을 반환한다.
 *
 * geographic SRS(SRID 4326)에서는 지원되지 않는다. Cartesian SRS(SRID 0) 전용.
 * SRID 4326 데이터에 사용하면 MySQL 오류가 발생한다.
 *
 * MySQL dialect 전용.
 *
 * @return MBR geometry 표현식
 * @throws IllegalStateException MySQL이 아닌 dialect에서 호출 시
 */
@Deprecated("ST_Envelope는 geographic SRS(4326)에서 지원되지 않습니다. Cartesian SRS 전용.")
fun Column<out Geometry>.stEnvelope(): Expression<String> {
    check(currentDialect is MysqlDialect) { "stEnvelope는 MySQL dialect에서만 지원됩니다." }
    return StEnvelopeExpr(this)
}
