package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Table

// SimpleScoreColumnType

/**
 * Timefold 의 [SimpleScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.simpleScore(
    name: String,
): Column<SimpleScore> = registerColumn(name, SimpleScoreColumnType())

class SimpleScoreColumnType():
    ColumnWithTransform<Int, SimpleScore>(IntegerColumnType(), SimpleScoreTransformer())

class SimpleScoreTransformer(): ColumnTransformer<Int, SimpleScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: SimpleScore): Int = value.score()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: Int): SimpleScore = SimpleScore.of(value)
}
