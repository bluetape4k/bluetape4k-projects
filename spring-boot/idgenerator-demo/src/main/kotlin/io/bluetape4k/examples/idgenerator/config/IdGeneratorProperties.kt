package io.bluetape4k.examples.idgenerator.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * ID batch 발급 예제에서 사용하는 설정 속성입니다.
 *
 * ## 동작/계약
 * - `defaultBatchSize`는 size query parameter가 없을 때 사용합니다.
 * - `maxBatchSize`는 예제 API가 한 번에 발급하는 ID 개수를 제한합니다.
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
)
