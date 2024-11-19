package io.bluetape4k.cassandra.querybuilder

import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.datastax.oss.driver.api.querybuilder.relation.Relation
import io.bluetape4k.cassandra.quote
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class RelationStatementExamples {

    companion object: KLogging()

    @Test
    fun `generate comparison relation`() {
        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=?"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq("value".bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=:value"
    }

    @Test
    fun `generate is not null`() {
        selectFrom("foo")
            .all()
            .where(Relation.column("k").isNotNull)
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k IS NOT NULL"
    }

    @Test
    fun `generate in relation`() {
        selectFrom("foo")
            .all()
            .where(Relation.column("k").inValues(QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k IN ?"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").inValues(QueryBuilder.bindMarker(), QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k IN (?,?)"
    }

    @Test
    fun `generate token relation`() {
        selectFrom("foo")
            .all()
            .where(Relation.token("k1", "k2").eq("token1".bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE token(k1,k2)=:token1"
    }

    @Test
    fun `generate column component relation`() {
        selectFrom("foo")
            .all()
            .where(
                Relation.column("id").eq(QueryBuilder.bindMarker()),
                Relation.mapValue("user", "name".literal()).eq(QueryBuilder.bindMarker())
            )
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE id=? AND user['name']=?"

        selectFrom("foo")
            .all()
            .where(
                Relation.column("id").eq(QueryBuilder.bindMarker()),
                Relation.mapValue("user", "name".quote().raw()).eq(QueryBuilder.bindMarker())
            )
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE id=? AND user['name']=?"
    }

    @Test
    fun `generate tuple relation`() {
        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.columns("c1", "c2", "c3").inValues(QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3) IN ?"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.columns("c1", "c2", "c3").inValues(QueryBuilder.bindMarker(), QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3) IN (?,?)"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.columns("c1", "c2", "c3").inValues(QueryBuilder.bindMarker(), "(4,5,6)".raw()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3) IN (?,(4,5,6))"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(
                Relation.columns("c1", "c2", "c3").inValues(
                    QueryBuilder.tuple(QueryBuilder.bindMarker(), QueryBuilder.bindMarker(), QueryBuilder.bindMarker()),
                    QueryBuilder.tuple(QueryBuilder.bindMarker(), QueryBuilder.bindMarker(), QueryBuilder.bindMarker())
                )
            )
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3) IN ((?,?,?),(?,?,?))"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.columns("c1", "c2", "c3").eq(QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3)=?"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.columns("c1", "c2", "c3").lt(QueryBuilder.bindMarker()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3)<?"

        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.columns("c1", "c2", "c3").gte("(1,2,3)".raw()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND (c1,c2,c3)>=(1,2,3)"
    }

    @Test
    fun `generate custom index relation`() {
        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(Relation.customIndex("my_index", "custom expression".literal()))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND expr(my_index,'custom expression')"
    }

    @Test
    fun `generate raw relation`() {
        selectFrom("foo")
            .all()
            .where(Relation.column("k").eq(QueryBuilder.bindMarker()))
            .where(QueryBuilder.raw("c = 'test'"))
            .asCql() shouldBeEqualTo "SELECT * FROM foo WHERE k=? AND c = 'test'"
    }
}
