package io.bluetape4k.batch.benchmark.support

import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * [BenchmarkSourceTable]의 소스 레코드를 표현합니다.
 */
data class BenchmarkSourceRecord(
    val id: Long,
    val name: String,
    val value: Int,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * [BenchmarkTargetTable]의 대상 레코드를 표현합니다.
 */
data class BenchmarkTargetRecord(
    val sourceName: String,
    val transformedValue: Int,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
