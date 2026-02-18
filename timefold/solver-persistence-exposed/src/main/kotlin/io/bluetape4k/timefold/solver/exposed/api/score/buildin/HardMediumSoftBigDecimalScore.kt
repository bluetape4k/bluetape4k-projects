package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold Solver의 [HardMediumSoftBigDecimalScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [HardMediumSoftBigDecimalScore]는 BigDecimal 타입의 Hard, Medium, Soft 세 가지 제약 조건 레벨을 가지는 Score 유형입니다.
 * 데이터베이스에는 "999.99hard/999.99medium/999.99soft" 형태의 문자열로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = hardMediumSoftBigDecimalScore("score")
 * }
 *
 * // 사용 예시
 * val hardMediumSoftBigDecimalScore = HardMediumSoftBigDecimalScore.of(
 *     BigDecimal("100.5"),
 *     BigDecimal("50.3"),
 *     BigDecimal("-30.1")
 * )
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = hardMediumSoftBigDecimalScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @param length 문자열 최대 길이 (기본값: 255)
 * @return [HardMediumSoftBigDecimalScore] 타입의 컬럼
 *
 * @see HardMediumSoftBigDecimalScore
 */
fun Table.hardMediumSoftBigDecimalScore(
    name: String,
    length: Int = 255,
): Column<HardMediumSoftBigDecimalScore> = registerColumn(name, HardMediumSoftBigDecimalScoreColumnType(length))

/**
 * [HardMediumSoftBigDecimalScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin String 타입과 [HardMediumSoftBigDecimalScore] 간의 변환을 처리합니다.
 *
 * @property length 문자열 최대 길이
 */
class HardMediumSoftBigDecimalScoreColumnType(
    length: Int,
): ColumnWithTransform<String, HardMediumSoftBigDecimalScore>(
        VarCharColumnType(length),
    HardMediumSoftBigDecimalScoreTransformer(),
    )

/**
 * [HardMediumSoftBigDecimalScore]와 데이터베이스 String 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [HardMediumSoftBigDecimalScore]를 문자열로 변환하고,
 * [wrap] 메서드는 문자열을 파싱하여 [HardMediumSoftBigDecimalScore]로 변환합니다.
 */
class HardMediumSoftBigDecimalScoreTransformer: ColumnTransformer<String, HardMediumSoftBigDecimalScore> {
    /**
     * [HardMediumSoftBigDecimalScore]를 데이터베이스 String 값으로 변환합니다.
     *
     * @param value 변환할 [HardMediumSoftBigDecimalScore] 인스턴스
     * @return "hard/medium/soft" 형태의 문자열 (예: "100.5/50.3/-30.1")
     */
    override fun unwrap(value: HardMediumSoftBigDecimalScore): String = value.toString()

    /**
     * 데이터베이스 String 값을 [HardMediumSoftBigDecimalScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 문자열 (예: "100.5/50.3/-30.1")
     * @return 생성된 [HardMediumSoftBigDecimalScore] 인스턴스
     */
    override fun wrap(value: String): HardMediumSoftBigDecimalScore = HardMediumSoftBigDecimalScore.parseScore(value)
}
