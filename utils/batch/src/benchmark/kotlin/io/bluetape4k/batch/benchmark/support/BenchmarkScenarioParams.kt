package io.bluetape4k.batch.benchmark.support

import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

/**
 * 시드 적재 벤치마크에서 사용하는 공통 파라미터입니다.
 */
@State(Scope.Benchmark)
open class SeedScenarioParams {
    /** 적재할 소스 데이터 건수입니다. */
    @Param("1000", "10000", "100000")
    var dataSize: Int = 1000

    /** 커넥션 풀 크기입니다. */
    @Param("10", "30", "60")
    var poolSize: Int = 10
}

/**
 * 엔드 투 엔드 배치 잡 벤치마크에서 사용하는 공통 파라미터입니다.
 */
@State(Scope.Benchmark)
open class JobScenarioParams {
    /** 처리할 소스 데이터 건수입니다. */
    @Param("1000", "10000", "100000")
    var dataSize: Int = 1000

    /** 커넥션 풀 크기입니다. */
    @Param("10", "30", "60")
    var poolSize: Int = 10

    /** 병렬 파티션 수입니다. */
    @Param("1", "4", "8")
    var parallelism: Int = 1
}
