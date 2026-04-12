package io.bluetape4k.batch.benchmark.support

/**
 * 엔드 투 엔드 배치 잡 벤치마크 공통 실행 로직입니다.
 */
internal object BatchJobBenchmarkSupport {
    fun runJdbc(environment: JdbcBenchmarkEnvironment, params: JobScenarioParams): Int =
        environment.runEndToEnd(
            dataSize = params.dataSize,
            poolSize = params.poolSize,
            parallelism = params.parallelism,
        )

    suspend fun runR2dbc(environment: R2dbcBenchmarkEnvironment, params: JobScenarioParams): Int =
        environment.runEndToEnd(
            dataSize = params.dataSize,
            poolSize = params.poolSize,
            parallelism = params.parallelism,
        )
}
