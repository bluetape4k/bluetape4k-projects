package io.bluetape4k.hibernate

import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.hibernate.cfg.AvailableSettings
import org.junit.jupiter.api.Test

class HibernateConstsTest {

    companion object: KLogging()

    @Test
    fun `DefaultJpaProperties는 기본 Hibernate 설정을 포함한다`() {
        val props = HibernateConsts.DefaultJpaProperties
        props.shouldNotBeNull()
        props.containsKey(AvailableSettings.HBM2DDL_AUTO).shouldBeTrue()
        props.containsKey(AvailableSettings.POOL_SIZE).shouldBeTrue()
        props.containsKey(AvailableSettings.SHOW_SQL).shouldBeTrue()
        props.containsKey(AvailableSettings.FORMAT_SQL).shouldBeTrue()
    }

    @Test
    fun `DefaultJpaProperties는 lazy 초기화 되어 항상 동일 인스턴스를 반환한다`() {
        val props1 = HibernateConsts.DefaultJpaProperties
        val props2 = HibernateConsts.DefaultJpaProperties
        props1 shouldBe props2
    }
}
