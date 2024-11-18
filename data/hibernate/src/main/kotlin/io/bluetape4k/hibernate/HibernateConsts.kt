package io.bluetape4k.hibernate

import org.hibernate.cfg.AvailableSettings
import java.util.*

object HibernateConsts {

    /**
     * Hibernate 기본 설정입니다.
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
