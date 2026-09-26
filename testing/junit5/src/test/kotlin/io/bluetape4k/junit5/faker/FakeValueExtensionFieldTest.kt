package io.bluetape4k.junit5.faker

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestInstance

@FakeValueTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FakeValueExtensionFieldTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    @FakeValue(provider = FakeValueProvider.Name.Title)     // name.title
    private lateinit var title: String

    @FakeValue(provider = FakeValueProvider.Name.Username) // name.username
    private lateinit var username: String

    @RepeatedTest(REPEAT_SIZE)
    fun `inject from name provider`() {
        log.debug { "title=$title, username=$username" }
        title.shouldNotBeEmpty()
        username.shouldNotBeEmpty()
    }

    @FakeValue(provider = FakeValueProvider.Name.Username, type = String::class, size = 20)
    private lateinit var usernames: List<String>

    @RepeatedTest(REPEAT_SIZE)
    fun `inject string list`() {
        usernames shouldHaveSize 20
        usernames.all { it.isNotBlank() }.shouldBeTrue()
    }
}
