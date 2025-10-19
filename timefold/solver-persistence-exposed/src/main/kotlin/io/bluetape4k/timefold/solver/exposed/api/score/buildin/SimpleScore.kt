package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

// SimpleScoreColumnType

/**
 * Timefold 의 [SimpleScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.simpleScore(
    name: String,
    length: Int = 255,
): Column<SimpleScore> = registerColumn(name, SimpleScoreColumnType(length))

class SimpleScoreColumnType(length: Int):
    ColumnWithTransform<String, SimpleScore>(VarCharColumnType(length), SimpleScoreTransformer())

class SimpleScoreTransformer(): ColumnTransformer<String, SimpleScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: SimpleScore): String = value.toString()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): SimpleScore = SimpleScore.parseScore(value)
}
