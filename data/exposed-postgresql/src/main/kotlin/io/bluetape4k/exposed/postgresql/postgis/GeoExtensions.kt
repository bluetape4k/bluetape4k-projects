package io.bluetape4k.exposed.postgresql.postgis

import net.postgis.jdbc.geometry.Point
import net.postgis.jdbc.geometry.Polygon
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect

/**
 * PostGIS POINT 컬럼을 테이블에 등록한다.
 *
 * @param name 컬럼 이름
 * @return [Point] 타입의 [Column]
 */
fun Table.geoPoint(name: String): Column<Point> =
    registerColumn(name, GeoPointColumnType())

/**
 * PostGIS POLYGON 컬럼을 테이블에 등록한다.
 *
 * @param name 컬럼 이름
 * @return [Polygon] 타입의 [Column]
 */
fun Table.geoPolygon(name: String): Column<Polygon> =
    registerColumn(name, GeoPolygonColumnType())

/**
 * PostGIS `ST_Distance` 함수를 사용하여 두 geometry 간 거리를 계산한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @return 거리 표현식
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Point>.stDistance(other: Column<Point>): Expression<Double> {
    check(currentDialect is PostgreSQLDialect) {
        "stDistance 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StDistanceExpr(this, other)
}

/**
 * PostGIS `ST_DWithin` 함수를 사용하여 두 geometry가 지정 거리 이내인지 확인한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param other 비교 대상 geometry 컬럼
 * @param distance 최대 거리 (도 단위, SRID 4326 기준)
 * @return 거리 조건 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Point>.stDWithin(other: Column<Point>, distance: Double): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stDWithin 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StDWithinOp(this, other, distance)
}

/**
 * PostGIS `ST_Within` 함수를 사용하여 point가 polygon 내부에 있는지 확인한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param polygon 폴리곤 컬럼
 * @return 포함 여부 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Point>.stWithin(polygon: Column<Polygon>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stWithin 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StWithinOp(this, polygon)
}

/**
 * PostGIS `ST_Contains` 함수를 사용하여 polygon이 다른 polygon을 완전히 포함하는지 확인한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param other 포함 여부를 확인할 대상 polygon 컬럼
 * @return 포함 여부 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Polygon>.stContains(other: Column<Polygon>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stContains 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StContainsPolygonOp(this, other)
}

/**
 * PostGIS `ST_Contains` 함수를 사용하여 polygon이 point를 완전히 포함하는지 확인한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param point 포함 여부를 확인할 point 컬럼
 * @return 포함 여부 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Polygon>.stContainsPoint(point: Column<Point>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stContainsPoint 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StContainsPointOp(this, point)
}

/**
 * PostGIS `ST_Overlaps` 함수를 사용하여 두 polygon이 부분적으로 겹치는지 확인한다.
 *
 * 한쪽이 다른 쪽을 완전히 포함하는 경우에는 false를 반환한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param other 비교 대상 polygon 컬럼
 * @return 겹침 여부 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Polygon>.stOverlaps(other: Column<Polygon>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stOverlaps 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StOverlapsOp(this, other)
}

/**
 * PostGIS `ST_Intersects` 함수를 사용하여 두 polygon이 교차(공유 영역 존재)하는지 확인한다.
 *
 * 포함 관계 및 부분 겹침 모두 true를 반환한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param other 비교 대상 polygon 컬럼
 * @return 교차 여부 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Polygon>.stIntersects(other: Column<Polygon>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stIntersects 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StIntersectsOp(this, other)
}

/**
 * PostGIS `ST_Disjoint` 함수를 사용하여 두 polygon이 완전히 분리(공유 영역 없음)되어 있는지 확인한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @param other 비교 대상 polygon 컬럼
 * @return 분리 여부 [Op]
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Polygon>.stDisjoint(other: Column<Polygon>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) {
        "stDisjoint 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StDisjointOp(this, other)
}

/**
 * PostGIS `ST_Area` 함수를 사용하여 polygon의 넓이(degree² 단위)를 반환한다.
 *
 * PostgreSQL(PostGIS) dialect 전용.
 *
 * @return 넓이 표현식
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<Polygon>.stArea(): Expression<Double> {
    check(currentDialect is PostgreSQLDialect) {
        "stArea 는 PostgreSQL(PostGIS) dialect 에서만 지원됩니다."
    }
    return StAreaExpr(this)
}

/**
 * PostGIS `ST_Distance(left, right)` SQL 표현식.
 */
class StDistanceExpr(
    private val left: Expression<Point>,
    private val right: Expression<Point>,
): ExpressionWithColumnType<Double>() {
    override val columnType = DoubleColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Distance(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_DWithin(left, right, distance)` SQL 표현식.
 */
class StDWithinOp(
    private val left: Expression<Point>,
    private val right: Expression<Point>,
    private val distance: Double,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_DWithin(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(", $distance)")
    }
}

/**
 * PostGIS `ST_Within(point, polygon)` SQL 표현식.
 */
class StWithinOp(
    private val point: Expression<Point>,
    private val polygon: Expression<Polygon>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Within(")
        queryBuilder.append(point)
        queryBuilder.append(", ")
        queryBuilder.append(polygon)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_Contains(polygon, other_polygon)` SQL 표현식.
 */
class StContainsPolygonOp(
    private val polygon: Expression<Polygon>,
    private val other: Expression<Polygon>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Contains(")
        queryBuilder.append(polygon)
        queryBuilder.append(", ")
        queryBuilder.append(other)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_Contains(polygon, point)` SQL 표현식.
 */
class StContainsPointOp(
    private val polygon: Expression<Polygon>,
    private val point: Expression<Point>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Contains(")
        queryBuilder.append(polygon)
        queryBuilder.append(", ")
        queryBuilder.append(point)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_Overlaps(left, right)` SQL 표현식.
 */
class StOverlapsOp(
    private val left: Expression<Polygon>,
    private val right: Expression<Polygon>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Overlaps(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_Intersects(left, right)` SQL 표현식.
 */
class StIntersectsOp(
    private val left: Expression<Polygon>,
    private val right: Expression<Polygon>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Intersects(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_Disjoint(left, right)` SQL 표현식.
 */
class StDisjointOp(
    private val left: Expression<Polygon>,
    private val right: Expression<Polygon>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Disjoint(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * PostGIS `ST_Area(polygon)` SQL 표현식.
 */
class StAreaExpr(
    private val polygon: Expression<Polygon>,
): ExpressionWithColumnType<Double>() {
    override val columnType = DoubleColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Area(")
        queryBuilder.append(polygon)
        queryBuilder.append(")")
    }
}
