package io.bluetape4k.exposed.mysql8.gis

import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.TextColumnType
import org.locationtech.jts.geom.Geometry

//
// ─── 관계 함수 Op<Boolean> 클래스 ────────────────────────────────────────────────
//

/**
 * MySQL `ST_Contains(left, right)` SQL 표현식.
 *
 * left geometry가 right geometry를 완전히 포함하는지 확인한다.
 */
class StContainsOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Contains(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Within(left, right)` SQL 표현식.
 *
 * left geometry가 right geometry 내부에 있는지 확인한다.
 */
class StWithinOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Within(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Intersects(left, right)` SQL 표현식.
 *
 * 두 geometry가 교차(공유 영역 존재)하는지 확인한다.
 */
class StIntersectsOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
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
 * MySQL `ST_Disjoint(left, right)` SQL 표현식.
 *
 * 두 geometry가 완전히 분리(공유 영역 없음)되어 있는지 확인한다.
 */
class StDisjointOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
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
 * MySQL `ST_Overlaps(left, right)` SQL 표현식.
 *
 * 두 geometry가 부분적으로 겹치는지 확인한다.
 * 한쪽이 다른 쪽을 완전히 포함하는 경우 false를 반환한다.
 */
class StOverlapsOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
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
 * MySQL `ST_Touches(left, right)` SQL 표현식.
 *
 * 두 geometry가 경계에서만 접촉(내부는 공유하지 않음)하는지 확인한다.
 */
class StTouchesOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Touches(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Crosses(left, right)` SQL 표현식.
 *
 * 두 geometry가 교차하는지 확인한다 (서로 다른 차원의 geometry 간에 적용).
 */
class StCrossesOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Crosses(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Equals(left, right)` SQL 표현식.
 *
 * 두 geometry가 공간적으로 동일한지 확인한다.
 */
class StEqualsOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Equals(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Distance(left, right) <= distance` SQL 표현식.
 *
 * 두 geometry 간의 거리가 지정한 거리 이내인지 확인한다.
 *
 * @param distance 최대 거리 (미터 단위, SRID 4326 기준)
 */
class StDWithinOp(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
    private val distance: Double,
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Distance(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(") <= $distance")
    }
}

//
// ─── 측정 함수 Expression<Double> 클래스 ────────────────────────────────────────
//

/**
 * MySQL `ST_Distance(left, right)` SQL 표현식.
 *
 * 두 geometry 간의 거리를 미터 단위로 반환한다 (geodetic).
 */
class StDistanceExpr(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : ExpressionWithColumnType<Double>() {
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
 * MySQL `ST_Distance_Sphere(left, right)` SQL 표현식.
 *
 * 두 geometry 간의 구면 거리를 미터 단위로 반환한다 (spherical).
 */
class StDistanceSphereExpr(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : ExpressionWithColumnType<Double>() {
    override val columnType = DoubleColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Distance_Sphere(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Length(expr)` SQL 표현식.
 *
 * LineString 또는 MultiLineString의 길이를 미터 단위로 반환한다.
 */
class StLengthExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<Double>() {
    override val columnType = DoubleColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Length(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Area(expr)` SQL 표현식.
 *
 * Polygon 또는 MultiPolygon의 넓이를 제곱미터 단위로 반환한다.
 */
class StAreaExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<Double>() {
    override val columnType = DoubleColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_Area(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

//
// ─── 속성/변환 Expression 클래스 ────────────────────────────────────────────────
//

/**
 * MySQL `ST_AsText(expr)` SQL 표현식.
 *
 * geometry를 WKT(Well-Known Text) 형식 문자열로 반환한다.
 */
class StAsTextExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_SRID(expr)` SQL 표현식.
 *
 * geometry의 SRID(공간 참조 식별자)를 반환한다.
 */
class StSridExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<Int>() {
    override val columnType = IntegerColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_SRID(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_GeometryType(expr)` SQL 표현식.
 *
 * geometry의 타입 이름 문자열을 반환한다 (예: "Point", "Polygon").
 */
class StGeometryTypeExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_GeometryType(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * MySQL `ST_Buffer(expr, distance)` SQL 표현식.
 *
 * geometry 주변에 지정한 거리만큼의 버퍼 영역을 생성한다.
 *
 * @param distance 버퍼 거리 (미터 단위)
 */
class StBufferExpr(
    private val expr: Expression<out Geometry>,
    private val distance: Double,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(ST_Buffer(")
        queryBuilder.append(expr)
        queryBuilder.append(", $distance))")
    }
}

/**
 * MySQL `ST_Union(left, right)` SQL 표현식.
 *
 * 두 geometry의 합집합을 ST_AsText로 감싸 WKT 문자열로 반환한다.
 */
class StUnionExpr(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(ST_Union(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append("))")
    }
}

/**
 * MySQL `ST_Difference(left, right)` SQL 표현식.
 *
 * left에서 right를 뺀 차집합 geometry를 ST_AsText로 감싸 WKT 문자열로 반환한다.
 */
class StDifferenceExpr(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(ST_Difference(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append("))")
    }
}

/**
 * MySQL `ST_Intersection(left, right)` SQL 표현식.
 *
 * 두 geometry의 교집합을 ST_AsText로 감싸 WKT 문자열로 반환한다.
 */
class StIntersectionExpr(
    private val left: Expression<out Geometry>,
    private val right: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(ST_Intersection(")
        queryBuilder.append(left)
        queryBuilder.append(", ")
        queryBuilder.append(right)
        queryBuilder.append("))")
    }
}

/**
 * MySQL `ST_Centroid(expr)` SQL 표현식.
 *
 * geometry의 도심(centroid) 점을 반환한다.
 * geographic SRS(SRID 4326)에서는 지원되지 않는다.
 */
class StCentroidExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(ST_Centroid(")
        queryBuilder.append(expr)
        queryBuilder.append("))")
    }
}

/**
 * MySQL `ST_Envelope(expr)` SQL 표현식.
 *
 * geometry의 최소 경계 사각형(MBR)을 반환한다.
 * geographic SRS(SRID 4326)에서는 지원되지 않는다.
 */
class StEnvelopeExpr(
    private val expr: Expression<out Geometry>,
) : ExpressionWithColumnType<String>() {
    override val columnType = TextColumnType()
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("ST_AsText(ST_Envelope(")
        queryBuilder.append(expr)
        queryBuilder.append("))")
    }
}
