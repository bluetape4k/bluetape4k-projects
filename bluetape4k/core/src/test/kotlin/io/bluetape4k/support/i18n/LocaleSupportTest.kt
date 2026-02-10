package io.bluetape4k.support.i18n

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertFailsWith

class LocaleSupportTest {

    companion object: KLogging()

    @Test
    fun `시스템 기본 Locale 확인`() {
        Locale.getDefault().isDefault().shouldBeTrue()
        Locale.ENGLISH.isDefault().shouldBeFalse()
    }

    @Test
    fun `null Locale은 기본 Locale을 반환`() {
        val locale: Locale? = null
        locale.orDefault() shouldBeEqualTo Locale.getDefault()
    }

    @Test
    fun `non-null Locale은 자기 자신을 반환`() {
        Locale.ENGLISH.orDefault() shouldBeEqualTo Locale.ENGLISH
    }

    @Test
    fun `variant가 있는 Locale의 부모`() {
        val locale = localeOf("en", "US", "WIN")
        val parent = locale.getParentOrNull()
        log.debug { "parent: $parent" }
        parent shouldBeEqualTo localeOf("en", "US")
    }

    @Test
    fun `country가 있는 Locale의 부모`() {
        val locale = localeOf("en", "US")
        val parent = locale.getParentOrNull()
        log.debug { "parent: $parent" }
        parent shouldBeEqualTo localeOf("en")
    }

    @Test
    fun `language만 있는 Locale의 부모는 null`() {
        localeOf("en").getParentOrNull().shouldBeNull()
    }

    @Test
    fun `Locale 부모 리스트 - variant 포함`() {
        val locale = localeOf("en", "US", "WIN")
        val parents = locale.getParentList()
        log.debug { "parents: $parents" }

        parents shouldBeEqualTo listOf(
            localeOf("en", "US", "WIN"),
            localeOf("en", "US"),
            localeOf("en"),
        )
    }

    @Test
    fun `Locale 부모 리스트 - language만`() {
        val parents = localeOf("en").getParentList()
        parents shouldBeEqualTo listOf(localeOf("en"))
    }

    @Test
    fun `calculateFilenames - empty`() {
        assertFailsWith<IllegalArgumentException> {
            Locale.getDefault().calculateFilenames("")
        }
    }

    @Test
    fun `calculateFilenames - language와 country`() {
        val filenames = localeOf("en", "US").calculateFilenames("messages")
        log.debug { "filenames: $filenames" }

        filenames shouldBeEqualTo listOf("messages_en_US", "messages_en", "messages")
    }

    @Test
    fun `calculateFilenames - language만`() {
        val filenames = localeOf("en").calculateFilenames("messages")
        log.debug { "filenames: $filenames" }

        filenames shouldBeEqualTo listOf("messages_en", "messages")
    }

    @Test
    fun `calculateFilenames - variant 포함`() {
        val filenames = localeOf("en", "US", "WIN").calculateFilenames("messages")
        log.debug { "filenames: $filenames" }

        filenames shouldBeEqualTo listOf("messages_en_US_WIN", "messages_en_US", "messages_en", "messages")
    }
}
