package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold 의 [SimpleBigDecimalScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.simpleBigDecimalScore(
    name: String,
    length: Int = 255,
): Column<SimpleBigDecimalScore> = registerColumn(name, SimpleBigDecimalScoreColumnType(length))

class SimpleBigDecimalScoreColumnType(length: Int):
    ColumnWithTransform<String, SimpleBigDecimalScore>(
        VarCharColumnType(length),
        SimpleBigDecimalScoreTransformer()
    )

class SimpleBigDecimalScoreTransformer: ColumnTransformer<String, SimpleBigDecimalScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: SimpleBigDecimalScore): String = value.toString()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): SimpleBigDecimalScore = SimpleBigDecimalScore.parseScore(value)
}
