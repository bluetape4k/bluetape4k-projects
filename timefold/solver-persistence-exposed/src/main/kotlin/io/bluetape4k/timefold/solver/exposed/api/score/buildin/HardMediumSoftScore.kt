package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold 의 [HardMediumSoftScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.hardMediumSoftScore(
    name: String,
    length: Int = 255,
): Column<HardMediumSoftScore> = registerColumn(name, HardMediumSoftScoreColumnType(length))

class HardMediumSoftScoreColumnType(length: Int):
    ColumnWithTransform<String, HardMediumSoftScore>(VarCharColumnType(length), HardMediumSoftScoreTransformer())

class HardMediumSoftScoreTransformer: ColumnTransformer<String, HardMediumSoftScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: HardMediumSoftScore): String = value.toString()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): HardMediumSoftScore = HardMediumSoftScore.parseScore(value)
}
