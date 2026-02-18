package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * Timefold Solver의 [SimpleScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [SimpleScore]는 단일 점수 값을 가지는 가장 기본적인 Score 유형입니다.
 * 데이터베이스에는 Integer 형태로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = simpleScore("score")
 * }
 *
 * // 사용 예시
 * val simpleScore = SimpleScore.of(100)
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = simpleScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @return [SimpleScore] 타입의 컬럼
 *
 * @see SimpleScore
 */
fun Table.simpleScore(name: String): Column<SimpleScore> = registerColumn(name, SimpleScoreColumnType())

/**
 * [SimpleScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin Int 타입과 [SimpleScore] 간의 변환을 처리합니다.
 */
class SimpleScoreColumnType: ColumnWithTransform<Int, SimpleScore>(IntegerColumnType(), SimpleScoreTransformer())

/**
 * [SimpleScore]와 데이터베이스 Int 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [SimpleScore]를 Int로 변환하고,
 * [wrap] 메서드는 Int를 [SimpleScore]로 변환합니다.
 */
class SimpleScoreTransformer: ColumnTransformer<Int, SimpleScore> {
    /**
     * [SimpleScore]를 데이터베이스 Int 값으로 변환합니다.
     *
     * @param value 변환할 [SimpleScore] 인스턴스
     * @return 점수의 Int 값
     */
    override fun unwrap(value: SimpleScore): Int = value.score()

    /**
     * 데이터베이스 Int 값을 [SimpleScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 Int 값
     * @return 생성된 [SimpleScore] 인스턴스
     */
    override fun wrap(value: Int): SimpleScore = SimpleScore.of(value)
}
