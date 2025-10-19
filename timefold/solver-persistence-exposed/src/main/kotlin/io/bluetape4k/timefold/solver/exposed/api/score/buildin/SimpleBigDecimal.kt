package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.DecimalColumnType
import org.jetbrains.exposed.v1.core.Table
import java.math.BigDecimal
import java.math.MathContext

/**
 * Timefold 의 [SimpleBigDecimalScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.simpleBigDecimalScore(
    name: String,
    precision: Int = MathContext.DECIMAL64.precision,
    scale: Int = 20,
): Column<SimpleBigDecimalScore> = registerColumn(name, SimpleBigDecimalScoreColumnType(precision, scale))

class SimpleBigDecimalScoreColumnType(
    precision: Int = MathContext.DECIMAL64.precision,
    scale: Int = 20,
): ColumnWithTransform<BigDecimal, SimpleBigDecimalScore>(
    DecimalColumnType(precision, scale),
    SimpleBigDecimalScoreTransformer()
)

class SimpleBigDecimalScoreTransformer(): ColumnTransformer<BigDecimal, SimpleBigDecimalScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: SimpleBigDecimalScore): BigDecimal = value.score()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: BigDecimal): SimpleBigDecimalScore = SimpleBigDecimalScore.of(value)
}
