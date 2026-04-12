package io.bluetape4k.batch.benchmark.support

/**
 * 시드 데이터 적재 벤치마크 공통 실행 로직입니다.
 */
internal object SeedBenchmarkSupport {
    fun runJdbc(environment: JdbcBenchmarkEnvironment, params: SeedScenarioParams): Int {
        environment.resetSchema()
        environment.seedSourceRows(params.dataSize)
        return params.dataSize
    }

    suspend fun runR2dbc(environment: R2dbcBenchmarkEnvironment, params: SeedScenarioParams): Int {
        environment.resetSchema()
        environment.seedSourceRows(params.dataSize)
        return params.dataSize
    }
}
