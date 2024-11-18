package io.bluetape4k.hibernate.converter

import io.bluetape4k.hibernate.AbstractHibernateTest
import io.bluetape4k.hibernate.findAs
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class ConverterTest: AbstractHibernateTest() {

    companion object: KLogging()

    private fun buildEntity(): ConvertableEntity =
        ConvertableEntity(faker.internet().username()).apply {
            locale = Locale.of(faker.nation().isoLanguage())
            duration = Duration.ofDays(3)
            password = faker.passport().valid()
        }

    @Test
    fun `apply converter`() {
        val entity = buildEntity()

        val loaded = tem.persistFlushFind(entity)
        loaded.isSame(entity).shouldBeTrue()

        tem.remove(loaded)
        flushAndClear()

        em.findAs<ConvertableEntity>(entity.id!!).shouldBeNull()
    }

    @Test
    fun `비밀번호처럼 암호화된 엔티티를 비밀번호로 조회하기`() {
        val entity = buildEntity()

        tem.persistAndFlush(entity)
        tem.clear()

        val query = em.createQuery("select cv from convertable_entity cv where cv.password = :password")
        query.setParameter("password", entity.password)

        val loaded = query.singleResult as ConvertableEntity
        loaded.isSame(entity).shouldBeTrue()
    }

    private fun ConvertableEntity.isSame(other: ConvertableEntity): Boolean {
        return name == other.name &&
                locale == other.locale &&
                duration == other.duration &&
                password == other.password &&
                component == other.component &&
                component.largeText.contentEquals(other.component.largeText)
    }
}
