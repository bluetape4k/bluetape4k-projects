package io.bluetape4k.exposed.postgresql.pgvector

import com.pgvector.PGvector
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.ColumnType

/**
 * FloatArray를 PostgreSQL pgvector VECTOR 타입으로 저장하는 컬럼 타입.
 *
 * ## 동작/계약
 * - [dimension] 양수여야 하며, 저장 시 벡터 차원이 일치하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - DB에서 읽을 때 [PGvector] 또는 문자열 형태를 [FloatArray]로 변환합니다.
 * - SQL 타입은 `VECTOR(dimension)` 형식으로 생성됩니다.
 *
 * ```kotlin
 * object EmbeddingTable: LongIdTable("embeddings") {
 *     val vector = vector("embedding", 1536)
 * }
 * val embedding = FloatArray(1536) { it.toFloat() / 1536 }
 * val id = EmbeddingTable.insertAndGetId { it[vector] = embedding }
 * val row = EmbeddingTable.selectAll().where { EmbeddingTable.id eq id }.single()
 * // row[EmbeddingTable.vector].size == 1536
 * ```
 *
 * @property dimension 벡터 차원 수 (양수여야 함)
 */
class VectorColumnType(val dimension: Int): ColumnType<FloatArray>() {

    companion object: KLogging()

    init {
        dimension.requirePositiveNumber("dimension")
    }

    override fun sqlType(): String = "VECTOR($dimension)"

    override fun notNullValueToDB(value: FloatArray): Any {
        require(value.size == dimension) {
            "벡터 차원 불일치: expected=$dimension, actual=${value.size}"
        }
        return PGvector(value)
    }

    /**
     * DB에서 읽은 값을 [FloatArray]로 변환한다.
     *
     * [PGvector] 또는 문자열(`"[1,2,3]"`) 형태를 지원한다.
     * 빈 문자열이거나 지원하지 않는 타입인 경우 명확한 오류 메시지로 빠르게 실패한다.
     *
     * @param value DB에서 읽은 값
     * @return 변환된 [FloatArray]
     * @throws IllegalArgumentException 빈 문자열인 경우
     * @throws IllegalStateException 지원하지 않는 값 타입인 경우
     */
    override fun valueFromDB(value: Any): FloatArray = when (value) {
        is PGvector -> value.toArray()
        is String   -> {
            require(value.isNotBlank()) {
                "VectorColumnType: DB 에서 읽은 벡터 문자열이 비어 있습니다."
            }
            PGvector(value).toArray()
        }
        else        -> error("VectorColumnType: 지원하지 않는 값 타입입니다: ${value::class.java}")
    }
}
