package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.redis.lettuce.map.MapWriter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Resilience4j Retry를 내장한 추상 [MapWriter] 구현체.
 *
 * 서브클래스는 [writeEntities]와 [deleteEntities]를 구현한다.
 *
 * ```kotlin
 * class MyWriter : EntityMapWriter<Long, MyEntity>() {
 *     override fun writeEntities(map: Map<Long, MyEntity>) {
 *         map.forEach { (id, entity) -> MyTable.upsert { it[name] = entity.name } }
 *     }
 *     override fun deleteEntities(keys: Collection<Long>) {
 *         MyTable.deleteWhere { MyTable.id inList keys }
 *     }
 * }
 * ```
 *
 * @param ID 키 타입
 * @param E 엔티티(DTO) 타입
 * @param retryConfig Resilience4j [RetryConfig]
 */
abstract class EntityMapWriter<ID: Any, E: Any>(
    retryConfig: RetryConfig = RetryConfig.ofDefaults(),
): MapWriter<ID, E> {
    private val retry = Retry.of("exposed-lettuce-writer", retryConfig)

    override fun write(map: Map<ID, E>) = Retry.decorateRunnable(retry) { transaction { writeEntities(map) } }.run()

    override fun delete(keys: Collection<ID>) =
        Retry.decorateRunnable(retry) { transaction { deleteEntities(keys) } }.run()

    protected abstract fun writeEntities(map: Map<ID, E>)

    protected abstract fun deleteEntities(keys: Collection<ID>)
}
