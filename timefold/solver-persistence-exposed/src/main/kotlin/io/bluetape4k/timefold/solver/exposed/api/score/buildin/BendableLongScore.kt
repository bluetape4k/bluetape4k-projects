package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold Solver의 [BendableLongScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [BendableLongScore]는 Long 타입의 가변적인 수의 Hard 레벨과 Soft 레벨을 가지는 Score 유형입니다.
 * 데이터베이스에는 "[999/999]hard/[999/999]soft" 형태의 문자열로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = bendableLongScore("score")
 * }
 *
 * // 사용 예시
 * val bendableLongScore = BendableLongScore.of(
 *     longArrayOf(100L, 50L),  // hard levels
 *     longArrayOf(-30L, -20L)  // soft levels
 * )
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = bendableLongScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @param length 문자열 최대 길이 (기본값: 255)
 * @return [BendableLongScore] 타입의 컬럼
 *
 * @see BendableLongScore
 */
fun Table.bendableLongScore(
    name: String,
    length: Int = 255,
): Column<BendableLongScore> = registerColumn(name, BendableLongScoreColumnType(length))

/**
 * [BendableLongScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin String 타입과 [BendableLongScore] 간의 변환을 처리합니다.
 *
 * @property limit 문자열 최대 길이
 */
class BendableLongScoreColumnType(
    limit: Int,
): ColumnWithTransform<String, BendableLongScore>(VarCharColumnType(limit), BendableLongScoreTransformer())

/**
 * [BendableLongScore]와 데이터베이스 String 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [BendableLongScore]를 문자열로 변환하고,
 * [wrap] 메서드는 문자열을 파싱하여 [BendableLongScore]로 변환합니다.
 */
class BendableLongScoreTransformer: ColumnTransformer<String, BendableLongScore> {
    /**
     * [BendableLongScore]를 데이터베이스 String 값으로 변환합니다.
     *
     * @param value 변환할 [BendableLongScore] 인스턴스
     * @return "[hard]/[soft]" 형태의 문자열
     */
    override fun unwrap(value: BendableLongScore): String = value.toString()

    /**
     * 데이터베이스 String 값을 [BendableLongScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 문자열
     * @return 생성된 [BendableLongScore] 인스턴스
     */
    override fun wrap(value: String): BendableLongScore = BendableLongScore.parseScore(value)
}
