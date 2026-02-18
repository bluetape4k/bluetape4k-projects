package io.bluetape4k.timefold.solver.exposed.api.score.buildin

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * Timefold Solver의 [SimpleLongScore]를 저장할 수 있는 Column을 생성합니다.
 *
 * [SimpleLongScore]는 Long 타입의 단일 점수 값을 가지는 Score 유형입니다.
 * 데이터베이스에는 BigInt 형태로 저장됩니다.
 *
 * ```kotlin
 * object PlanningTables : IntIdTable("planning_solution") {
 *     val name = varchar("name", 255)
 *     val score = simpleLongScore("score")
 * }
 *
 * // 사용 예시
 * val simpleLongScore = SimpleLongScore.of(100L)
 * PlanningTables.insert {
 *     it[name] = "Test Solution"
 *     it[score] = simpleLongScore
 * }
 * ```
 *
 * @param name 컬럼 이름
 * @return [SimpleLongScore] 타입의 컬럼
 *
 * @see SimpleLongScore
 */
fun Table.simpleLongScore(name: String): Column<SimpleLongScore> = registerColumn(name, SimpleLongScoreColumnType())

/**
 * [SimpleLongScore]를 위한 Exposed ColumnType 구현체입니다.
 *
 * 날춤 Kotlin Long 타입과 [SimpleLongScore] 간의 변환을 처리합니다.
 */
class SimpleLongScoreColumnType:
    ColumnWithTransform<Long, SimpleLongScore>(LongColumnType(), SimpleLongScoreTransformer())

/**
 * [SimpleLongScore]와 데이터베이스 Long 값 간의 변환을 수행하는 Transformer 클래스입니다.
 *
 * [unwrap] 메서드는 [SimpleLongScore]를 Long으로 변환하고,
 * [wrap] 메서드는 Long을 [SimpleLongScore]로 변환합니다.
 */
class SimpleLongScoreTransformer: ColumnTransformer<Long, SimpleLongScore> {
    /**
     * [SimpleLongScore]를 데이터베이스 Long 값으로 변환합니다.
     *
     * @param value 변환할 [SimpleLongScore] 인스턴스
     * @return 점수의 Long 값
     */
    override fun unwrap(value: SimpleLongScore): Long = value.score()

    /**
     * 데이터베이스 Long 값을 [SimpleLongScore]로 변환합니다.
     *
     * @param value 데이터베이스에서 읽은 Long 값
     * @return 생성된 [SimpleLongScore] 인스턴스
     */
    override fun wrap(value: Long): SimpleLongScore = SimpleLongScore.of(value)
}
