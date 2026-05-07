package io.bluetape4k.spring.data.exposed.jdbc.query

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.repository.query.ExposedQueryCreator
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class ExposedQueryCreatorTest {

    companion object : KLogging()

    @Test
    fun `escapeLikeWildcards escapes percent`() {
        val result = ExposedQueryCreator.escapeLikeWildcards("100%off")
        result shouldBeEqualTo "100\\%off"
    }

    @Test
    fun `escapeLikeWildcards escapes underscore`() {
        val result = ExposedQueryCreator.escapeLikeWildcards("hello_world")
        result shouldBeEqualTo "hello\\_world"
    }

    @Test
    fun `escapeLikeWildcards escapes backslash`() {
        val result = ExposedQueryCreator.escapeLikeWildcards("path\\file")
        result shouldBeEqualTo "path\\\\file"
    }

    @Test
    fun `escapeLikeWildcards leaves normal strings unchanged`() {
        val result = ExposedQueryCreator.escapeLikeWildcards("hello world")
        result shouldBeEqualTo "hello world"
    }

    @Test
    fun `escapeLikeWildcards handles empty string`() {
        val result = ExposedQueryCreator.escapeLikeWildcards("")
        result shouldBeEqualTo ""
    }

    @Test
    fun `escapeLikeWildcards escapes multiple wildcards`() {
        val result = ExposedQueryCreator.escapeLikeWildcards("%test_value%")
        result shouldBeEqualTo "\\%test\\_value\\%"
    }

    @Test
    fun `escapeLikeWildcards companion is not null`() {
        ExposedQueryCreator.shouldNotBeNull()
    }
}
