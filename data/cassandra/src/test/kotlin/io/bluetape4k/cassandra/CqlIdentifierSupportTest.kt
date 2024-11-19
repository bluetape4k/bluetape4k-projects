package io.bluetape4k.cassandra

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CqlIdentifierSupportTest {

    companion object: KLogging()

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
}
