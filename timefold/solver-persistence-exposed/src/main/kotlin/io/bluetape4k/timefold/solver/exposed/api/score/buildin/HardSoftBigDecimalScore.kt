package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold Solver의 [HardSoftBigDecimalScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [HardSoftBigDecimalScore]는 BigDecimal 타입의 Hard 제약 조건과 Soft 제약 조건을 각각 점수로 가지는 Score 유형입니다.
 * 데이터베이스에는 "999.99hard/999.99soft" 형태의 문자열로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = hardSoftBigDecimalScore("score")
 * }
 *
 * // 사용 예시
 * val hardSoftBigDecimalScore = HardSoftBigDecimalScore.of(BigDecimal("100.5"), BigDecimal("-50.3"))
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = hardSoftBigDecimalScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @param length 문자열 최대 길이 (기본값: 255)
 * @return [HardSoftBigDecimalScore] 타입의 컬럼
 *
 * @see HardSoftBigDecimalScore
 */
fun Table.hardSoftBigDecimalScore(
    name: String,
    length: Int = 255,
): Column<HardSoftBigDecimalScore> = registerColumn(name, HardSoftBigDecimalScoreColumnType(length))

/**
 * [HardSoftBigDecimalScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin String 타입과 [HardSoftBigDecimalScore] 간의 변환을 처리합니다.
 *
 * @property length 문자열 최대 길이
 */
class HardSoftBigDecimalScoreColumnType(
    length: Int,
): ColumnWithTransform<String, HardSoftBigDecimalScore>(
        VarCharColumnType(length),
    HardSoftBigDecimalScoreTransformer(),
    )

/**
 * [HardSoftBigDecimalScore]와 데이터베이스 String 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [HardSoftBigDecimalScore]를 문자열로 변환하고,
 * [wrap] 메서드는 문자열을 파싱하여 [HardSoftBigDecimalScore]로 변환합니다.
 */
class HardSoftBigDecimalScoreTransformer: ColumnTransformer<String, HardSoftBigDecimalScore> {
    /**
     * [HardSoftBigDecimalScore]를 데이터베이스 String 값으로 변환합니다.
     *
     * @param value 변환할 [HardSoftBigDecimalScore] 인스턴스
     * @return "hard/soft" 형태의 문자열 (예: "100.5/-50.3")
     */
    override fun unwrap(value: HardSoftBigDecimalScore): String = value.toString()

    /**
     * 데이터베이스 String 값을 [HardSoftBigDecimalScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 문자열 (예: "100.5/-50.3")
     * @return 생성된 [HardSoftBigDecimalScore] 인스턴스
     */
    override fun wrap(value: String): HardSoftBigDecimalScore = HardSoftBigDecimalScore.parseScore(value)
}
