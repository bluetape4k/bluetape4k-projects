package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold 의 [BendableLongScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.bendableLongScore(
    name: String,
    length: Int = 255,
): Column<BendableLongScore> = registerColumn(name, BendableLongScoreColumnType(length))

class BendableLongScoreColumnType(limit: Int):
    ColumnWithTransform<String, BendableLongScore>(VarCharColumnType(limit), BendableLongScoreTransformer())

class BendableLongScoreTransformer: ColumnTransformer<String, BendableLongScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: BendableLongScore): String = value.toString()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): BendableLongScore = BendableLongScore.parseScore(value)
}
