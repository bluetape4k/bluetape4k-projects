package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.type.DataTypes
import io.bluetape4k.cassandra.toCqlIdentifier
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class UserDefinedTypeSupportTest {

    @Test
    fun `userDefinedTypeOf 는 CqlIdentifier 인자로 UDT를 생성한다`() {
        val udt = userDefinedTypeOf("ks1".toCqlIdentifier(), "person".toCqlIdentifier()) {
            withField("name", DataTypes.TEXT)
            withField("age", DataTypes.INT)
        }

        udt.keyspace shouldBeEqualTo "ks1".toCqlIdentifier()
        udt.name shouldBeEqualTo "person".toCqlIdentifier()
        udt.contains("name").shouldBeTrue()
        udt.contains("age").shouldBeTrue()
    }

    @Test
    fun `userDefinedTypeOf 는 문자열 인자를 CqlIdentifier로 변환해 UDT를 생성한다`() {
        val udt = userDefinedTypeOf("ks2", "address") {
            withField("city", DataTypes.TEXT)
        }

        udt.keyspace shouldBeEqualTo "ks2".toCqlIdentifier()
        udt.name shouldBeEqualTo "address".toCqlIdentifier()
        udt.contains("city").shouldBeTrue()
    }
}
