package io.bluetape4k.hibernate

import org.hibernate.cfg.AvailableSettings
import java.util.*

/**
 * Hibernate 관련 상수 및 기본 설정을 제공하는 object 입니다.
 */
object HibernateConsts {
    /**
     * Hibernate 기본 JPA 설정입니다.
     *
     * - `HBM2DDL_AUTO`: `none` (스키마 자동 생성 비활성화)
     * - `POOL_SIZE`: `30` (커넥션 풀 크기)
     * - `SHOW_SQL`: `true` (SQL 출력)
     * - `FORMAT_SQL`: `true` (SQL 포매팅)
     */
    val DefaultJpaProperties: Properties by lazy {
        Properties().apply {
            put(AvailableSettings.HBM2DDL_AUTO, "none")

            put(AvailableSettings.POOL_SIZE, 30)
            put(AvailableSettings.SHOW_SQL, true)
            put(AvailableSettings.FORMAT_SQL, true)
        }
    }
}
