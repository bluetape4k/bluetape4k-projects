package io.bluetape4k.spring.batch.exposed.writer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchUpsertStatement
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

/**
 * Exposed `batchUpsert` 기반 [ItemWriter].
 *
 * 동일 키가 존재하면 UPDATE, 없으면 INSERT를 수행.
 * SpringTransactionManager 환경에서 청크 트랜잭션에 자동 참여.
 *
 * 사용 예시:
 * ```kotlin
 * val writer = ExposedUpsertItemWriter(table = TargetTable) {
 *     this[TargetTable.sourceName] = it.sourceName
 *     this[TargetTable.value] = it.value
 * }
 * ```
 *
 * @param T 입력 타입
 * @param table 대상 Exposed [Table]
 * @param upsertBody `batchUpsert` 람다
 */
class ExposedUpsertItemWriter<T : Any>(
    private val table: Table,
    private val upsertBody: BatchUpsertStatement.(T) -> Unit,
) : ItemWriter<T> {

    companion object : KLogging()

    override fun write(chunk: Chunk<out T>) {
        if (chunk.isEmpty) return

        val items = chunk.items

        table.batchUpsert(items, shouldReturnGeneratedValues = false) { item ->
            upsertBody(item)
        }

        log.debug { "${items.size}건 batchUpsert 완료 (table=${table.tableName})" }
    }
}
