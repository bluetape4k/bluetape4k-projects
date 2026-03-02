package io.bluetape4k.junit5.tester

/**
 * 스트레스 테스터의 공통 설정 계약입니다.
 *
 * ## 동작/계약
 * - 구현체는 [rounds]를 통해 실행 반복 횟수를 구성합니다.
 * - 반환 타입 [SELF]는 fluent 체이닝을 위한 자기 타입을 의미합니다.
 *
 * ```kotlin
 * val tester = MultithreadingTester().rounds(3)
 * // rounds 설정 후 동일 인스턴스로 체이닝 가능
 * ```
 *
 * @param SELF 구현체 자신의 타입(Fluent API 체이닝용)
 */
interface StressTester<SELF> {

    companion object {
        /** worker당 기본 반복 횟수입니다. */
        const val DEFAULT_ROUNDS_PER_WORKER: Int = 2
        /** 허용하는 최소 worker당 반복 횟수입니다. */
        const val MIN_ROUNDS_PER_WORKER: Int = 1
        /** 허용하는 최대 worker당 반복 횟수입니다. */
        const val MAX_ROUNDS_PER_WORKER: Int = 1_000_000
    }

    /**
     * 실행 라운드 수를 설정합니다.
     *
     * ## 동작/계약
     * - 유효 범위 검증 규칙은 각 구현체가 결정합니다.
     * - 일반적으로 자기 자신을 반환해 설정 체이닝에 사용합니다.
     */
    fun rounds(value: Int): SELF
}

/**
 * worker 수를 설정할 수 있는 스트레스 테스터 계약입니다.
 *
 * ## 동작/계약
 * - [workers]는 실행 동시성을 제어하는 공통 설정 포인트입니다.
 * - 구체적인 최소/최대 범위 검증은 구현체가 담당합니다.
 *
 * ```kotlin
 * val tester = SuspendedJobTester().workers(4).rounds(2)
 * // worker 4개 기준으로 실행
 * ```
 *
 * @param SELF 구현체 자신의 타입(Fluent API 체이닝용)
 */
interface WorkerStressTester<SELF>: StressTester<SELF> {

    companion object {
        /** 기본 worker 수입니다. */
        const val DEFAULT_WORKER_SIZE: Int = 16
        /** 허용하는 최소 worker 수입니다. */
        const val MIN_WORKER_SIZE: Int = 1
        /** 허용하는 최대 worker 수입니다. */
        const val MAX_WORKER_SIZE: Int = 2000
    }

    /**
     * worker(thread/job 실행자) 수를 설정합니다.
     *
     * ## 동작/계약
     * - 유효 범위 검증 규칙은 각 구현체가 정의합니다.
     * - 구현체는 일반적으로 fluent 체이닝을 위해 자기 자신을 반환합니다.
     */
    fun workers(value: Int): SELF
}
