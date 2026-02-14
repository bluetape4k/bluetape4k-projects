package io.bluetape4k.junit5.tester

/**
 * 스트레스 테스터의 공통 설정 계약입니다.
 *
 * @param SELF 구현체 자신의 타입(Fluent API 체이닝용)
 */
interface StressTester<SELF> {

    companion object {
        const val DEFAULT_ROUNDS_PER_WORKER: Int = 2
        const val MIN_ROUNDS_PER_WORKER: Int = 1
        const val MAX_ROUNDS_PER_WORKER: Int = 1_000_000
    }

    /**
     * 실행 라운드 수를 설정합니다.
     */
    fun rounds(value: Int): SELF
}

/**
 * worker 수를 설정할 수 있는 스트레스 테스터 계약입니다.
 *
 * @param SELF 구현체 자신의 타입(Fluent API 체이닝용)
 */
interface WorkerStressTester<SELF>: StressTester<SELF> {

    companion object {
        const val DEFAULT_WORKER_SIZE: Int = 16
        const val MIN_WORKER_SIZE: Int = 1
        const val MAX_WORKER_SIZE: Int = 2000
    }

    /**
     * worker(thread/job 실행자) 수를 설정합니다.
     */
    fun workers(value: Int): SELF
}
