package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.core.type.DataTypes
import io.bluetape4k.cassandra.cql.userDefinedType
import io.bluetape4k.cassandra.toCqlIdentifier
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertFailsWith

class TermSupportTest {

    companion object: KLogging()

    @Test
    fun `generate arithmetic terms`() {
        ("a".raw() + "b".raw()).asCql() shouldBeEqualTo "a+b"
        (("a".raw() + "b".raw()) + ("c".raw() + "d".raw())).asCql() shouldBeEqualTo "a+b+c+d"
        (("a".raw() + "b".raw()) - ("c".raw() + "d".raw())).asCql() shouldBeEqualTo "a+b-(c+d)"
        (("a".raw() + "b".raw()) - ("c".raw() - "d".raw())).asCql() shouldBeEqualTo "a+b-(c-d)"

        // negate
        ("a".raw() + "b".raw()).negate().asCql() shouldBeEqualTo "-(a+b)"
        ("a".raw() - "b".raw()).negate().asCql() shouldBeEqualTo "-(a-b)"

        (("a".raw() + "b".raw()) * ("c".raw() + "d".raw())).asCql() shouldBeEqualTo "(a+b)*(c+d)"
        (("a".raw() * "b".raw()) / ("c".raw() * "d".raw())).asCql() shouldBeEqualTo "a*b/(c*d)"

        (("a".raw() * "b".raw()) remainder ("c".raw() / "d".raw())).asCql() shouldBeEqualTo "a*b%(c/d)"
    }

    @Test
    fun `generate function terms`() {
        functionTerm("f").asCql() shouldBeEqualTo "f()"
        functionTerm("f", "a".raw(), "b".raw()).asCql() shouldBeEqualTo "f(a,b)"

        functionTerm("ks", "f", "a".raw(), "b".raw()).asCql() shouldBeEqualTo "ks.f(a,b)"

        nowTerm().asCql() shouldBeEqualTo "now()"
        currentTimestampTerm().asCql() shouldBeEqualTo "currenttimestamp()"
        currentDateTerm().asCql() shouldBeEqualTo "currentdate()"
        currentTimeTerm().asCql() shouldBeEqualTo "currenttime()"
        currentTimeUuidTerm().asCql() shouldBeEqualTo "currenttimeuuid()"
        "a".raw().minTimeUuid().asCql() shouldBeEqualTo "mintimeuuid(a)"
        "a".raw().maxTimeUuid().asCql() shouldBeEqualTo "maxtimeuuid(a)"
        "a".raw().toDate().asCql() shouldBeEqualTo "todate(a)"
        "a".raw().toTimestamp().asCql() shouldBeEqualTo "totimestamp(a)"
        "a".raw().toUnixTimestamp().asCql() shouldBeEqualTo "tounixtimestamp(a)"
    }

    @Test
    fun `generate type hint terms`() {
        "1".raw().typeHint(DataTypes.BIGINT).asCql() shouldBeEqualTo "(bigint)1"
    }

    @Test
    fun `generate literal terms`() {
        1.literal().asCql() shouldBeEqualTo "1"
        "foo".literal().asCql() shouldBeEqualTo "'foo'"
        listOf(1, 2, 3).literal().asCql() shouldBeEqualTo "[1,2,3]"
        setOf(1, 2, 3).literal().asCql() shouldBeEqualTo "{1,2,3}"
        mapOf(1 to "one", 2 to "two").literal().asCql() shouldBeEqualTo "{1:'one',2:'two'}"

        val tupleType = DataTypes.tupleOf(DataTypes.INT, DataTypes.TEXT)
        val tupleValue = tupleType.newValue(1, "foo")
        tupleValue.literal().asCql() shouldBeEqualTo "(1,'foo')"

        val udtType = userDefinedType("ks".toCqlIdentifier(), "user".toCqlIdentifier()) {
            withField("first_name", DataTypes.TEXT)
            withField("last_name", DataTypes.TEXT)
        }
        val udtValue = udtType.newValue().setString("first_name", "Jane").setString("last_name", "Doe")
        udtValue.literal().asCql() shouldBeEqualTo "{first_name:'Jane',last_name:'Doe'}"
        null.literal().asCql() shouldBeEqualTo "NULL"
    }

    @Test
    fun `fail when no codec for literal`() {
        assertFailsWith<IllegalArgumentException> {
            Date(1234).literal()
        }
    }
}
