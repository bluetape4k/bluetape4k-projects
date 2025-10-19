package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType


/**
 * Timefold 의 [BendableScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.bendableScore(
    name: String,
    length: Int,
): Column<BendableScore> = registerColumn(name, BendableScoreColumnType(length))

class BendableScoreColumnType(length: Int):
    ColumnWithTransform<String, BendableScore>(VarCharColumnType(length), BendableScoreTransformer())

class BendableScoreTransformer(): ColumnTransformer<String, BendableScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: BendableScore): String = value.toString()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): BendableScore = BendableScore.parseScore(value)
}
