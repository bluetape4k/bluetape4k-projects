package io.bluetape4k.apache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.util.*

class ApacheLocaleUtilsTest {

    @Test
    fun `availableLocaleList 필터링`() {
        val locales = availableLocaleList { it.language == "en" }
        locales.shouldNotBeEmpty()
        locales.all { it.language == "en" } shouldBeEqualTo true
    }

    @Test
    fun `countriesByLanguage 와 languageByCountry`() {
        val countries = countriesByLanguage("en")
        countries.shouldNotBeEmpty()

        val languages = languageByCountry("US")
        languages.shouldContain(Locale("en", "US"))
    }

    @Test
    fun `localeLookupList 는 계층적으로 반환한다`() {
        val base = Locale("fr", "CA", "xxx")
        val list = base.localeLookupList()
        list.shouldContainSame(listOf(base, Locale("fr", "CA"), Locale("fr")))
    }
}
