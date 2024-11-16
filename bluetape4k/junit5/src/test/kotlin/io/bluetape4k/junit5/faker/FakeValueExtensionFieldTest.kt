package io.bluetape4k.junit5.faker

import io.github.oshai.kotlinlogging.KotlinLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance

@FakeValueTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FakeValueExtensionFieldTest {

    companion object {
        private val log = KotlinLogging.logger { }
        private const val REPEAT_SIZE = 10
    }

    @FakeValue(provider = FakeValueProvider.Name.Title)     // name.title
    private lateinit var title: String

    @FakeValue(provider = FakeValueProvider.Name.Username) // name.username
    private lateinit var username: String

    @RepeatedTest(REPEAT_SIZE)
    fun `inject from name provider`() {
        title.shouldNotBeEmpty()
        username.shouldNotBeEmpty()

        log.trace { "title=$title, username=$username" }
    }

    @FakeValue(provider = FakeValueProvider.Name.Username, type = String::class, size = 20)
    private lateinit var usernames: List<String>

    @RepeatedTest(REPEAT_SIZE)
    fun `inject string list`() {
        usernames.size shouldBeEqualTo 20
        usernames.all { it.isNotBlank() }.shouldBeTrue()
    }
}
