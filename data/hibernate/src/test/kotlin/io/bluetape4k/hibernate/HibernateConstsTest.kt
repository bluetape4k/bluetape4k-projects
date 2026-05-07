package io.bluetape4k.hibernate

import io.bluetape4k.assertions.shouldNotBeNull
import org.hibernate.cfg.AvailableSettings
import org.junit.jupiter.api.Test

class HibernateConstsTest {

    @Test
    fun `DefaultJpaProperties는 기본 Hibernate 설정을 포함한다`() {
        val props = HibernateConsts.DefaultJpaProperties
        props.shouldNotBeNull()
        props.containsKey(AvailableSettings.HBM2DDL_AUTO)
        props.containsKey(AvailableSettings.POOL_SIZE)
        props.containsKey(AvailableSettings.SHOW_SQL)
        props.containsKey(AvailableSettings.FORMAT_SQL)
    }

    @Test
    fun `DefaultJpaProperties는 lazy 초기화 되어 항상 동일 인스턴스를 반환한다`() {
        val props1 = HibernateConsts.DefaultJpaProperties
        val props2 = HibernateConsts.DefaultJpaProperties
        props1 === props2
    }
}
