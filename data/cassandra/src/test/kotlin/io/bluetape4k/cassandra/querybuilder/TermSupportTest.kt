package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cassandra.cql.userDefinedTypeOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.util.*

class TermSupportTest {

    companion object: KLoggingChannel()

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
    fun `function term overloads support iterable and identifiers`() {
        val args = listOf("a".raw(), "b".raw())
        val functionId = CqlIdentifier.fromCql("fn")
        val keyspaceId = CqlIdentifier.fromCql("ks")

        functionTerm("f", args).asCql() shouldBeEqualTo "f(a,b)"
        functionTerm("ks", "f", args).asCql() shouldBeEqualTo "ks.f(a,b)"
        functionTerm(functionId, args).asCql() shouldBeEqualTo "fn(a,b)"
        functionTerm(functionId, *args.toTypedArray()).asCql() shouldBeEqualTo "fn(a,b)"
        functionTerm(keyspaceId, functionId, args).asCql() shouldBeEqualTo "ks.fn(a,b)"
        functionTerm(keyspaceId, functionId, *args.toTypedArray()).asCql() shouldBeEqualTo "ks.fn(a,b)"
    }

    @Test
    fun `collection terms report idempotence`() {
        listOf(1.literal(), 2.literal()).tuple().isIdempotent shouldBeEqualTo true
        ListTerm(listOf(1.literal())).isIdempotent shouldBeEqualTo true
        SetTerm(listOf(1.literal())).isIdempotent shouldBeEqualTo true
        MapTerm(mapOf(1.literal() to 2.literal())).isIdempotent shouldBeEqualTo true
    }

    @Test
    fun `identifier query builder overloads create markers and UDT references`() {
        val identifier = CqlIdentifier.fromCql("address")

        identifier.bindMarker().asCql() shouldBeEqualTo ":address"
        "address".udt().name shouldBeEqualTo CqlIdentifier.fromCql("address")
        identifier.udt().name shouldBeEqualTo identifier
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

        val udtType = userDefinedTypeOf("ks", "user") {
            withField("first_name", DataTypes.TEXT)
            withField("last_name", DataTypes.TEXT)
        }
        val udtValue = udtType.newValue().setString("first_name", "Jane").setString("last_name", "Doe")
        udtValue.literal().asCql() shouldBeEqualTo "{first_name:'Jane',last_name:'Doe'}"
        null.literal().asCql() shouldBeEqualTo "NULL"
    }

    @Test
    fun `generate collection literals with registry and codec`() {
        "foo".literal(CodecRegistry.DEFAULT).asCql() shouldBeEqualTo "'foo'"
        listOf(1, 2).literal(CodecRegistry.DEFAULT).asCql() shouldBeEqualTo "[1,2]"
        setOf("a", "b").literal(CodecRegistry.DEFAULT).asCql() shouldBeEqualTo "{'a','b'}"
        mapOf("one" to 1).literal(CodecRegistry.DEFAULT).asCql() shouldBeEqualTo "{'one':1}"

        42.literal(TypeCodecs.INT).asCql() shouldBeEqualTo "42"
        listOf(1, 2).literal(TypeCodecs.INT).asCql() shouldBeEqualTo "[1,2]"
        setOf(1, 2).literal(TypeCodecs.INT).asCql() shouldBeEqualTo "{1,2}"
        mapOf(1 to 2).literal(TypeCodecs.INT).asCql() shouldBeEqualTo "{1:2}"
    }

    @Test
    fun `codec literals reject null collection members`() {
        assertFailsWith<IllegalArgumentException> {
            listOf(null).literal(TypeCodecs.INT)
        }
        assertFailsWith<IllegalArgumentException> {
            mapOf(1 to null).literal(TypeCodecs.INT)
        }
    }

    @Test
    fun `fail when no codec for literal`() {
        assertFailsWith<IllegalArgumentException> {
            Date(1234).literal()
        }
    }
}
