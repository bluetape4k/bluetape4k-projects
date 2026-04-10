package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

/**
 * Exposed `batchInsert` 기반 [ItemWriter].
 *
 * SpringTransactionManager 환경에서 Spring Batch 청크 트랜잭션에 자동 참여.
 * 별도 `transaction { }` 블록 불필요.
 *
 * 사용 예시:
 * ```kotlin
 * val writer = ExposedItemWriter(table = TargetTable) {
 *     this[TargetTable.name] = it.name
 *     this[TargetTable.value] = it.value
 * }
 * ```
 *
 * @param T 입력 타입
 * @param table 대상 Exposed [Table]
 * @param insertBody `batchInsert` 람다
 */
class ExposedItemWriter<T : Any>(
    private val table: Table,
    private val insertBody: BatchInsertStatement.(T) -> Unit,
) : ItemWriter<T> {

    companion object : KLogging()

    override fun write(chunk: Chunk<out T>) {
        if (chunk.isEmpty) return

        val items = chunk.items

        table.batchInsert(items, shouldReturnGeneratedValues = false) { item ->
            insertBody(item)
        }

        log.debug { "${items.size}건 batchInsert 완료 (table=${table.tableName})" }
    }
}
