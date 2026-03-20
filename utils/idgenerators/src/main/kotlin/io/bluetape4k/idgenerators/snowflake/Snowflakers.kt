package io.bluetape4k.idgenerators.snowflake

import io.bluetape4k.idgenerators.getMachineId

/**
 * Snowflake ID 생성기 싱글턴 및 팩토리를 제공하는 진입점.
 *
 * ## 사용 예
 * ```kotlin
 * // 싱글턴 사용
 * val id: Long = Snowflakers.Default.nextId()
 * val id2: Long = Snowflakers.Global.nextId()
 *
 * // 팩토리 함수로 새 인스턴스 생성
 * val snowflake = Snowflakers.default(machineId = 42)
 * val globalSnowflake = Snowflakers.global()
 * ```
 */
object Snowflakers {
    /**
     * 기본 머신 ID를 사용하는 [DefaultSnowflake] 싱글턴 인스턴스.
     *
     * 머신 ID는 네트워크 인터페이스 기반으로 자동 결정됩니다.
     */
    val Default: Snowflake by lazy { DefaultSnowflake() }

    /**
     * [GlobalSnowflake] 싱글턴 인스턴스.
     *
     * 1 msec 당 `4096 * 1024`개의 ID를 생성할 수 있는 고처리량 생성기입니다.
     */
    val Global: Snowflake by lazy { GlobalSnowflake() }

    /**
     * 지정한 [machineId]로 새 [DefaultSnowflake] 인스턴스를 생성합니다.
     *
     * @param machineId 머신 식별자. 기본값은 네트워크 인터페이스 기반 자동 결정
     *
     * ```kotlin
     * val snowflake = Snowflakers.default(machineId = 1)
     * val id: Long = snowflake.nextId()
     * ```
     */
    fun default(machineId: Int = getMachineId(MAX_MACHINE_ID)): Snowflake = DefaultSnowflake(machineId)

    /**
     * 새 [GlobalSnowflake] 인스턴스를 생성합니다.
     *
     * ```kotlin
     * val snowflake = Snowflakers.global()
     * val id: Long = snowflake.nextId()
     * ```
     */
    fun global(): Snowflake = GlobalSnowflake()
}
