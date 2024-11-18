package io.bluetape4k.hibernate.mapping.localized

import io.bluetape4k.hibernate.AbstractHibernateTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class TravelContentTest(
    @Autowired private val repository: TravelContentRepository,
): AbstractHibernateTest() {

    @Test
    fun `다국어를 지원하는 엔티티 활용`() {
        val travelContent = TravelContent(
            title = "방콕 여행",
            contents = "방콕 여행의 비밀을 알려드립니당",
            creator = "방콕 여행사"
        )

        travelContent.localeMap[Locale.ENGLISH] = LocalTravelContent("Bangkok", "Secret of Bangkok")
        travelContent.localeMap[Locale.forLanguageTag("th-TH")] =
            LocalTravelContent("กรุงเทพมหานคร", "Secret of Bangkok")

        repository.saveAndFlush(travelContent)
        clear()

        val loaded = repository.findByIdOrNull(travelContent.id)!!

        loaded shouldBeEqualTo travelContent
        loaded.getLocalizedValue(Locale.ENGLISH).title shouldBeEqualTo "Bangkok"
        loaded.getLocalizedValue(Locale.forLanguageTag("th-TH")).title shouldBeEqualTo "กรุงเทพมหานคร"
    }
}
