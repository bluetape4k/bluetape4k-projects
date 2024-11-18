package io.bluetape4k.hibernate

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requirePositiveNumber
import org.hibernate.Session

private val log by lazy { KotlinLogging.logger { } }

/**
 * [batchSize]를 설정하고 [block]을 실행합니다.
 *
 * ```
 * // 100개씩 엔티티 저장
 * session.withBatchSize(100) {
 *    // 엔티티 저장
 *    entities.forEach { entity ->
 *          session.save(entity)
 *    }
 * }
 *
 * @param batchSize Batch size
 * @param block 실행할 코드 블럭
 */
fun <T> Session.withBatchSize(batchSize: Int, block: Session.() -> T): T {
    batchSize.requirePositiveNumber("batchSize")

    val prevBatchSize = this.jdbcBatchSize

    return try {
        log.debug { "Batch size[$batchSize]를 적용하여 작업을 수행합니다 ..." }
        this.jdbcBatchSize = batchSize
        block(this)
    } finally {
        this.jdbcBatchSize = prevBatchSize
    }
}
