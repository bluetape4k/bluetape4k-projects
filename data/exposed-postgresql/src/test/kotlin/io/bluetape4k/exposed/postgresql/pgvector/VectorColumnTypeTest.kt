package io.bluetape4k.exposed.postgresql.pgvector

import com.pgvector.PGvector
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.PgvectorServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * pgvector 컬럼 타입 및 거리 연산 통합 테스트.
 *
 * pgvector 전용 컨테이너(`pgvector/pgvector:pg16`)를 사용한다.
 * `TestDB.POSTGRESQL`은 pgvector 확장이 없으므로 별도 컨테이너를 사용한다.
 */
class VectorColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging() {
        private const val DIMENSION = 3

        @JvmStatic
        val pgvectorContainer = PgvectorServer.Launcher.pgvector

        @JvmStatic
        val db: Database by lazy {
            Database.connect(
                url = pgvectorContainer.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = pgvectorContainer.username!!,
                password = pgvectorContainer.password!!,
            ).also { db ->
                transaction(db) {
                    PGvector.addVectorType(connection.connection as java.sql.Connection)
                }
            }
        }
    }

    object Embeddings: LongIdTable("embeddings") {
        val name = varchar("name", 255)
        val embedding = vector("embedding", DIMENSION)
    }

    object EmbeddingPairs: LongIdTable("embedding_pairs") {
        val name = varchar("name", 255)
        val embedding = vector("embedding", DIMENSION)
        val query = vector("query_embedding", DIMENSION)
    }

    /**
     * pgvector 전용 테이블 생성/삭제를 처리하는 헬퍼.
     *
     * [PGvector.addVectorType]을 매 트랜잭션마다 호출해야 한다.
     */
    private fun withVectorTables(vararg tables: Table, statement: JdbcTransaction.() -> Unit) {
        transaction(db) {
            runCatching { SchemaUtils.drop(*tables) }
            SchemaUtils.create(*tables)
        }
        try {
            transaction(db) {
                PGvector.addVectorType(connection.connection as java.sql.Connection)
                statement()
            }
        } finally {
            transaction(db) {
                runCatching { SchemaUtils.drop(*tables) }
            }
        }
    }

    @Test
    fun `벡터 저장 및 조회`() {
        val vector = floatArrayOf(1.0f, 2.0f, 3.0f)

        withVectorTables(Embeddings) {
            Embeddings.insert {
                it[name] = "test"
                it[embedding] = vector
            }

            val row = Embeddings.selectAll().single()
            row[Embeddings.name] shouldBeEqualTo "test"

            val result = row[Embeddings.embedding]
            result.shouldNotBeNull()
            result.size shouldBeEqualTo DIMENSION
            result[0].toDouble().shouldBeNear(1.0, 0.001)
            result[1].toDouble().shouldBeNear(2.0, 0.001)
            result[2].toDouble().shouldBeNear(3.0, 0.001)
        }
    }

    @Test
    fun `코사인 거리 기준 유사도 검색`() {
        withVectorTables(EmbeddingPairs) {
            EmbeddingPairs.insert {
                it[name] = "aligned"
                it[embedding] = floatArrayOf(1.0f, 0.0f, 0.0f)
                it[query] = floatArrayOf(1.0f, 0.0f, 0.0f)
            }
            EmbeddingPairs.insert {
                it[name] = "orthogonal"
                it[embedding] = floatArrayOf(0.0f, 1.0f, 0.0f)
                it[query] = floatArrayOf(1.0f, 0.0f, 0.0f)
            }
            EmbeddingPairs.insert {
                it[name] = "near"
                it[embedding] = floatArrayOf(0.9f, 0.1f, 0.0f)
                it[query] = floatArrayOf(1.0f, 0.0f, 0.0f)
            }

            val results = EmbeddingPairs
                .selectAll()
                .orderBy(EmbeddingPairs.embedding.cosineDistance(EmbeddingPairs.query) to SortOrder.ASC)
                .map { it[EmbeddingPairs.name] }

            results.size shouldBeEqualTo 3
            results.first() shouldBeEqualTo "aligned"
            results.last() shouldBeEqualTo "orthogonal"
        }
    }

    @Test
    fun `L2 거리 기준 유사도 검색`() {
        withVectorTables(EmbeddingPairs) {
            EmbeddingPairs.insert {
                it[name] = "origin"
                it[embedding] = floatArrayOf(0.0f, 0.0f, 0.0f)
                it[query] = floatArrayOf(0.0f, 0.0f, 0.0f)
            }
            EmbeddingPairs.insert {
                it[name] = "near"
                it[embedding] = floatArrayOf(1.0f, 0.0f, 0.0f)
                it[query] = floatArrayOf(0.0f, 0.0f, 0.0f)
            }
            EmbeddingPairs.insert {
                it[name] = "far"
                it[embedding] = floatArrayOf(10.0f, 10.0f, 10.0f)
                it[query] = floatArrayOf(0.0f, 0.0f, 0.0f)
            }

            val results = EmbeddingPairs
                .selectAll()
                .orderBy(EmbeddingPairs.embedding.l2Distance(EmbeddingPairs.query) to SortOrder.ASC)
                .map { it[EmbeddingPairs.name] }

            results.size shouldBeEqualTo 3
            results.first() shouldBeEqualTo "origin"
            results.last() shouldBeEqualTo "far"
        }
    }

    @Test
    fun `VectorColumnType dimension이 0이면 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            VectorColumnType(0)
        }
    }

    @Test
    fun `VectorColumnType dimension이 음수이면 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            VectorColumnType(-1)
        }
    }

    @Test
    fun `VectorColumnType sqlType 검증`() {
        val columnType = VectorColumnType(128)
        columnType.sqlType() shouldBeEqualTo "VECTOR(128)"
    }

    @Test
    fun `VectorColumnType 은 문자열과 PGvector 를 FloatArray 로 복원한다`() {
        val columnType = VectorColumnType(DIMENSION)

        columnType.valueFromDB("[1,2,3]").toList() shouldBeEqualTo listOf(1.0f, 2.0f, 3.0f)
        columnType.valueFromDB(PGvector(floatArrayOf(4.0f, 5.0f, 6.0f))).toList() shouldBeEqualTo listOf(4.0f, 5.0f, 6.0f)
    }

    @Test
    fun `VectorColumnType 은 차원이 맞지 않는 벡터를 거부한다`() {
        val columnType = VectorColumnType(DIMENSION)

        assertThrows<IllegalArgumentException> {
            columnType.notNullValueToDB(floatArrayOf(1.0f, 2.0f))
        }
    }

    @Test
    fun `VectorDistanceOp 는 select expr 로 거리값을 직접 조회할 수 있다`() {
        withVectorTables(EmbeddingPairs) {
            EmbeddingPairs.insert {
                it[name] = "distance-check"
                it[embedding] = floatArrayOf(1.0f, 0.0f, 0.0f)
                it[query] = floatArrayOf(0.0f, 1.0f, 0.0f)
            }

            val cosineExpr = EmbeddingPairs.embedding.cosineDistance(EmbeddingPairs.query)
            val l2Expr = EmbeddingPairs.embedding.l2Distance(EmbeddingPairs.query)
            val innerExpr = EmbeddingPairs.embedding.innerProduct(EmbeddingPairs.query)

            val row = EmbeddingPairs.select(cosineExpr, l2Expr, innerExpr).single()
            val cosine = row[cosineExpr]
            val l2 = row[l2Expr]
            val inner = row[innerExpr]

            cosine.shouldNotBeNull()
            l2.shouldNotBeNull()
            inner.shouldNotBeNull()
            cosine shouldBeGreaterThan 0.9
            l2 shouldBeGreaterThan 1.4
            inner shouldBeLessThan 0.1
        }
    }

    @Test
    fun `여러 벡터 저장 후 전체 조회`() {
        withVectorTables(Embeddings) {
            repeat(5) { i ->
                Embeddings.insert {
                    it[name] = "item-$i"
                    it[embedding] = floatArrayOf(i.toFloat(), (i * 2).toFloat(), (i * 3).toFloat())
                }
            }

            val results = Embeddings.selectAll().toList()
            results.size shouldBeEqualTo 5
        }
    }
}
