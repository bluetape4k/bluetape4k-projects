package io.bluetape4k.cassandra

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class CqlIdentifierSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `string to CqlIdentifier`() {
        "name".toCqlIdentifier().asCql(true) shouldBeEqualTo "name"
        "user id".toCqlIdentifier().asCql(true) shouldBeEqualTo "\"user id\""
        "user's id".toCqlIdentifier().asCql(true) shouldBeEqualTo "\"user's id\""
    }

    @Test
    fun `pretty cql string`() {
        "name".toCqlIdentifier().prettyCql() shouldBeEqualTo "name"
        "user id".toCqlIdentifier().prettyCql() shouldBeEqualTo "\"user id\""
        "user's id".toCqlIdentifier().prettyCql() shouldBeEqualTo "\"user's id\""
    }

    @Test
    fun `blank string to CqlIdentifier 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { "".toCqlIdentifier() }
        assertFailsWith<IllegalArgumentException> { " ".toCqlIdentifier() }
    }
}
