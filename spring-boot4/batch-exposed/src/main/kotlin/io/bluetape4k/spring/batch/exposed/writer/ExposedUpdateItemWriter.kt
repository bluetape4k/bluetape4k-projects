package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

/**
 * Exposed `update` 기반 [ItemWriter].
 *
 * 기존 레코드를 업데이트할 때 사용. 각 아이템에 대해 개별 UPDATE 실행.
 * SpringTransactionManager 환경에서 청크 트랜잭션에 자동 참여.
 *
 * 사용 예시:
 * ```kotlin
 * val writer = ExposedUpdateItemWriter(
 *     table = TargetTable,
 *     keyColumn = TargetTable.id,
 *     keyExtractor = { it.id },
 * ) {
 *     this[TargetTable.value] = it.value
 * }
 * ```
 *
 * @param T 입력 타입
 * @param table 대상 Exposed [Table]
 * @param keyColumn WHERE 조건에 사용할 키 컬럼. `Column<Long>` 또는 `castTo<Long>()` 결과 모두 허용
 * @param keyExtractor T에서 키 값을 추출하는 함수
 * @param updateBody UPDATE SET 람다 (`UpdateStatement` 수신자, `T` 인자)
 */
class ExposedUpdateItemWriter<T : Any>(
    private val table: Table,
    private val keyColumn: ExpressionWithColumnType<Long>,
    private val keyExtractor: (T) -> Long,
    private val updateBody: UpdateStatement.(T) -> Unit,
) : ItemWriter<T> {

    companion object : KLogging()

    override fun write(chunk: Chunk<out T>) {
        if (chunk.isEmpty) return

        chunk.items.forEach { item ->
            table.update({ keyColumn eq keyExtractor(item) }) { stmt ->
                stmt.updateBody(item)
            }
        }

        log.debug { "${chunk.items.size}건 update 완료 (table=${table.tableName})" }
    }
}
