package io.bluetape4k.examples.idgenerator.config

import io.bluetape4k.support.requirePositiveNumber
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * ID batch example 설정 property입니다.
 *
 * ## Behavior
 * - `defaultBatchSize` is used when the `size` query parameter is omitted.
 * - `maxBatchSize` limits how many IDs the example API can issue at once.
 * - Both values must be positive, and `defaultBatchSize` must not exceed `maxBatchSize`.
 *
 * ```yaml
 * bluetape4k:
 *   id-generator:
 *     default-batch-size: 10
 *     max-batch-size: 100
 * ```
 */
@ConfigurationProperties(prefix = "bluetape4k.id-generator")
data class IdGeneratorProperties(
    val defaultBatchSize: Int = 10,
    val maxBatchSize: Int = 100,
) {
    init {
        defaultBatchSize.requirePositiveNumber("defaultBatchSize")
        maxBatchSize.requirePositiveNumber("maxBatchSize")
        require(defaultBatchSize <= maxBatchSize) {
            "defaultBatchSize must be less than or equal to maxBatchSize: " +
                    "defaultBatchSize=$defaultBatchSize, maxBatchSize=$maxBatchSize"
        }
    }
}
