package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold Solver의 [HardMediumSoftScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [HardMediumSoftScore]는 Hard, Medium, Soft 세 가지 제약 조건 레벨을 가지는 Score 유형입니다.
 * 데이터베이스에는 "999hard/999medium/999soft" 형태의 문자열로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = hardMediumSoftScore("score")
 * }
 *
 * // 사용 예시
 * val hardMediumSoftScore = HardMediumSoftScore.of(100, 50, -30)
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = hardMediumSoftScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @param length 문자열 최대 길이 (기본값: 255)
 * @return [HardMediumSoftScore] 타입의 컬럼
 *
 * @see HardMediumSoftScore
 */
fun Table.hardMediumSoftScore(
    name: String,
    length: Int = 255,
): Column<HardMediumSoftScore> = registerColumn(name, HardMediumSoftScoreColumnType(length))

/**
 * [HardMediumSoftScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin String 타입과 [HardMediumSoftScore] 간의 변환을 처리합니다.
 *
 * @property length 문자열 최대 길이
 */
class HardMediumSoftScoreColumnType(
    length: Int,
): ColumnWithTransform<String, HardMediumSoftScore>(VarCharColumnType(length), HardMediumSoftScoreTransformer())

/**
 * [HardMediumSoftScore]와 데이터베이스 String 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [HardMediumSoftScore]를 문자열로 변환하고,
 * [wrap] 메서드는 문자열을 파싱하여 [HardMediumSoftScore]로 변환합니다.
 */
class HardMediumSoftScoreTransformer: ColumnTransformer<String, HardMediumSoftScore> {
    /**
     * [HardMediumSoftScore]를 데이터베이스 String 값으로 변환합니다.
     *
     * @param value 변환할 [HardMediumSoftScore] 인스턴스
     * @return "hard/medium/soft" 형태의 문자열 (예: "100/50/-30")
     */
    override fun unwrap(value: HardMediumSoftScore): String = value.toString()

    /**
     * 데이터베이스 String 값을 [HardMediumSoftScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 문자열 (예: "100/50/-30")
     * @return 생성된 [HardMediumSoftScore] 인스턴스
     */
    override fun wrap(value: String): HardMediumSoftScore = HardMediumSoftScore.parseScore(value)
}
