package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold 의 [HardMediumSoftLongScore] 를 저장할 수 있는 Column 을 생성합니다.
 */
fun Table.hardMediumSoftLongScore(
    name: String,
    length: Int = 255,
): Column<HardMediumSoftLongScore> = registerColumn(name, HardMediumSoftLongScoreColumnType(length))

class HardMediumSoftLongScoreColumnType(length: Int):
    ColumnWithTransform<String, HardMediumSoftLongScore>(
        VarCharColumnType(length),
        HardMediumSoftLongScoreTransformer()
    )

class HardMediumSoftLongScoreTransformer(): ColumnTransformer<String, HardMediumSoftLongScore> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: HardMediumSoftLongScore): String = value.toString()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: String): HardMediumSoftLongScore =
        HardMediumSoftLongScore.parseScore(value)
}
