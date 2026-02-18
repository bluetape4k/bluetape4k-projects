package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType

/**
 * Timefold Solver의 [SimpleBigDecimalScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [SimpleBigDecimalScore]는 BigDecimal 타입의 단일 점수 값을 가지는 Score 유형입니다.
 * 데이터베이스에는 문자열 형태로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = simpleBigDecimalScore("score")
 * }
 *
 * // 사용 예시
 * val simpleBigDecimalScore = SimpleBigDecimalScore.of(BigDecimal("100.5"))
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = simpleBigDecimalScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @param length 문자열 최대 길이 (기본값: 255)
 * @return [SimpleBigDecimalScore] 타입의 컬럼
 *
 * @see SimpleBigDecimalScore
 */
fun Table.simpleBigDecimalScore(
    name: String,
    length: Int = 255,
): Column<SimpleBigDecimalScore> = registerColumn(name, SimpleBigDecimalScoreColumnType(length))

/**
 * [SimpleBigDecimalScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin String 타입과 [SimpleBigDecimalScore] 간의 변환을 처리합니다.
 *
 * @property length 문자열 최대 길이
 */
class SimpleBigDecimalScoreColumnType(
    length: Int,
): ColumnWithTransform<String, SimpleBigDecimalScore>(
        VarCharColumnType(length),
    SimpleBigDecimalScoreTransformer(),
    )

/**
 * [SimpleBigDecimalScore]와 데이터베이스 String 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [SimpleBigDecimalScore]를 문자열로 변환하고,
 * [wrap] 메서드는 문자열을 파싱하여 [SimpleBigDecimalScore]로 변환합니다.
 */
class SimpleBigDecimalScoreTransformer: ColumnTransformer<String, SimpleBigDecimalScore> {
    /**
     * [SimpleBigDecimalScore]를 데이터베이스 String 값으로 변환합니다.
     *
     * @param value 변환할 [SimpleBigDecimalScore] 인스턴스
     * @return BigDecimal 값의 문자열 표현
     */
    override fun unwrap(value: SimpleBigDecimalScore): String = value.toString()

    /**
     * 데이터베이스 String 값을 [SimpleBigDecimalScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 문자열
     * @return 생성된 [SimpleBigDecimalScore] 인스턴스
     */
    override fun wrap(value: String): SimpleBigDecimalScore = SimpleBigDecimalScore.parseScore(value)
}
