package io.bluetape4k.exposed.postgresql.pgvector

import com.pgvector.PGvector
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requirePositiveNumber
import org.jetbrains.exposed.v1.core.ColumnType

/**
 * FloatArray를 PostgreSQL pgvector VECTOR 타입으로 저장하는 컬럼 타입.
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

    override fun valueFromDB(value: Any): FloatArray = when (value) {
        is PGvector -> value.toArray()
        is String   -> PGvector(value).toArray()
        else        -> error("Unsupported value type: ${value::class.java}")
    }
}
