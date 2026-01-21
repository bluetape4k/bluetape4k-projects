package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * Timefold 의 [SimpleLongScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.simpleLongScore(
    name: String,
): Column<SimpleLongScore> = registerColumn(name, SimpleLongScoreColumnType())

class SimpleLongScoreColumnType:
    ColumnWithTransform<Long, SimpleLongScore>(LongColumnType(), SimpleLongScoreTransformer())

class SimpleLongScoreTransformer: ColumnTransformer<Long, SimpleLongScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: SimpleLongScore): Long = value.score()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: Long): SimpleLongScore = SimpleLongScore.of(value)
}
