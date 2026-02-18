package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold Solver의 [HardSoftScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [HardSoftScore]는 Hard 제약 조건과 Soft 제약 조건을 각각 점수로 가지는 Score 유형입니다.
 * 데이터베이스에는 "999hard/999soft" 형태의 문자열로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = hardSoftScore("score")
 * }
 *
 * // 사용 예시
 * val hardSoftScore = HardSoftScore.of(100, -50)
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = hardSoftScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @param length 문자열 최대 길이 (기본값: 255)
 * @return [HardSoftScore] 타입의 컬럼
 *
 * @see HardSoftScore
 */
fun Table.hardSoftScore(
    name: String,
    length: Int = 255,
): Column<HardSoftScore> = registerColumn(name, HardSoftScoreColumnType(length))

/**
 * [HardSoftScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin String 타입과 [HardSoftScore] 간의 변환을 처리합니다.
 *
 * @property length 문자열 최대 길이
 */
class HardSoftScoreColumnType(
    length: Int,
): ColumnWithTransform<String, HardSoftScore>(VarCharColumnType(length), HardSoftScoreTransformer())

/**
 * [HardSoftScore]와 데이터베이스 String 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [HardSoftScore]를 "hard/soft" 형태의 문자열로 변환하고,
 * [wrap] 메서드는 문자열을 파싱하여 [HardSoftScore]로 변환합니다.
 */
class HardSoftScoreTransformer: ColumnTransformer<String, HardSoftScore> {
    /**
     * [HardSoftScore]를 데이터베이스 String 값으로 변환합니다.
     *
     * @param value 변환할 [HardSoftScore] 인스턴스
     * @return "hard/soft" 형태의 문자열 (예: "100/-50")
     */
    override fun unwrap(value: HardSoftScore): String = value.toString()

    /**
     * 데이터베이스 String 값을 [HardSoftScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 문자열 (예: "100/-50")
     * @return 생성된 [HardSoftScore] 인스턴스
     */
    override fun wrap(value: String): HardSoftScore = HardSoftScore.parseScore(value)
}
