package io.bluetape4k.exposed.postgresql.pgvector

import com.pgvector.PGvector
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.jdbc.statements.api.ExposedConnection

/**
 * VECTOR 컬럼을 등록하는 factory 확장 함수.
 *
 * ```kotlin
 * object EmbeddingTable: LongIdTable("embeddings") {
 *     val embedding = vector("embedding", 1536)
 * }
 * // EmbeddingTable.embedding.name == "embedding"
 * ```
 *
 * @param name 컬럼 이름
 * @param dimension 벡터 차원 수
 * @return [FloatArray] 타입의 [Column]
 */
fun Table.vector(name: String, dimension: Int): Column<FloatArray> =
    registerColumn(name, VectorColumnType(dimension))

/**
 * JDBC Connection에 pgvector 타입을 등록한다.
 * Database.connect() 직후 transaction {} 안에서 호출해야 한다.
 *
 * ```kotlin
 * transaction {
 *     connection.registerVectorType()
 * }
 * ```
 */
fun java.sql.Connection.registerVectorType() {
    PGvector.addVectorType(this)
}

/**
 * Exposed [ExposedConnection]에 pgvector 타입을 등록한다.
 * `transaction {}` 블록 내에서 `connection.registerVectorType()` 으로 호출한다.
 *
 * ```kotlin
 * transaction {
 *     connection.registerVectorType()
 * }
 * ```
 */
fun ExposedConnection<*>.registerVectorType() {
    PGvector.addVectorType(this.connection as java.sql.Connection)
}

/**
 * 코사인 거리 연산자 (`<=>`).
 * PostgreSQL dialect 전용.
 *
 * ```kotlin
 * val queryVector = floatArrayOf(0.1f, 0.2f, 0.3f)
 * val results = EmbeddingTable
 *     .selectAll()
 *     .orderBy(EmbeddingTable.embedding.cosineDistance(queryVector.literal()))
 *     .limit(10)
 *     .toList()
 * ```
 *
 * @param other 비교 대상 벡터 컬럼
 * @return 코사인 거리 표현식
 */
fun Column<FloatArray>.cosineDistance(other: Expression<FloatArray>): VectorDistanceOp {
    check(currentDialect is PostgreSQLDialect) { "cosineDistance (<=>) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return VectorDistanceOp(this, other, "<=>")
}

/**
 * L2 유클리드 거리 연산자 (`<->`).
 * PostgreSQL dialect 전용.
 *
 * ```kotlin
 * val queryVector = floatArrayOf(0.1f, 0.2f, 0.3f)
 * val results = EmbeddingTable
 *     .selectAll()
 *     .orderBy(EmbeddingTable.embedding.l2Distance(queryVector.literal()))
 *     .limit(10)
 *     .toList()
 * ```
 *
 * @param other 비교 대상 벡터 컬럼
 * @return L2 거리 표현식
 */
fun Column<FloatArray>.l2Distance(other: Expression<FloatArray>): VectorDistanceOp {
    check(currentDialect is PostgreSQLDialect) { "l2Distance (<->) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return VectorDistanceOp(this, other, "<->")
}

/**
 * 내적 연산자 (`<#>`).
 * PostgreSQL dialect 전용.
 *
 * ```kotlin
 * val queryVector = floatArrayOf(0.1f, 0.2f, 0.3f)
 * val results = EmbeddingTable
 *     .selectAll()
 *     .orderBy(EmbeddingTable.embedding.innerProduct(queryVector.literal()))
 *     .limit(10)
 *     .toList()
 * ```
 *
 * @param other 비교 대상 벡터 컬럼
 * @return 내적 거리 표현식
 */
fun Column<FloatArray>.innerProduct(other: Expression<FloatArray>): VectorDistanceOp {
    check(currentDialect is PostgreSQLDialect) { "innerProduct (<#>) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return VectorDistanceOp(this, other, "<#>")
}

/**
 * 벡터 거리 SQL 표현식.
 *
 * @property left 왼쪽 벡터 표현식
 * @property right 오른쪽 벡터 표현식
 * @property operator 거리 연산자 문자열 (`<=>`, `<->`, `<#>`)
 */
class VectorDistanceOp(
    private val left: Expression<FloatArray>,
    private val right: Expression<FloatArray>,
    private val operator: String,
) : ExpressionWithColumnType<Double>() {
    override val columnType = DoubleColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(left)
        queryBuilder.append(" $operator ")
        queryBuilder.append(right)
    }
}
