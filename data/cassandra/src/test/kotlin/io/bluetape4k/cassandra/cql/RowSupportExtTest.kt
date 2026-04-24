package io.bluetape4k.cassandra.cql

import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.toCqlIdentifier
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * [io.bluetape4k.cassandra.cql.RowSupport]의 getStringOrEmpty, map, mapWithName,
 * toCqlIdentifierMap, mapWithCqlIdentifier 확장 함수를 검증합니다.
 */
class RowSupportExtTest : AbstractCassandraTest() {

    companion object : KLoggingChannel()

    @BeforeAll
    fun setup() {
        runSuspendIO {
            session.executeSuspending("DROP TABLE IF EXISTS row_ext_table")
            session.executeSuspending(
                "CREATE TABLE IF NOT EXISTS row_ext_table (id text PRIMARY KEY, name text, num int);"
            )
            session.executeSuspending("TRUNCATE row_ext_table")
            val ps = session.prepareSuspending("INSERT INTO row_ext_table(id, name, num) VALUES(?, ?, ?)")
            session.executeSuspending(ps.bind("1", "Alice", 42))
            // name 이 null 인 행 추가
            session.executeSuspending("INSERT INTO row_ext_table(id, num) VALUES('2', 99)")
        }
    }

    @Test
    fun `getStringOrEmpty 는 값이 있는 경우 문자열을 반환한다`() = runSuspendIO {
        val row = session.executeSuspending("SELECT * FROM row_ext_table WHERE id=?", "1").one()
            ?: error("행이 없습니다")

        row.getStringOrEmpty(0).shouldNotBeEmpty()
        row.getStringOrEmpty("id").shouldNotBeEmpty()
        row.getStringOrEmpty("id".toCqlIdentifier()).shouldNotBeEmpty()
    }

    @Test
    fun `getStringOrEmpty 는 null 컬럼에 대해 빈 문자열을 반환한다`() = runSuspendIO {
        val row = session.executeSuspending("SELECT id, name FROM row_ext_table WHERE id=?", "2").one()
            ?: error("행이 없습니다")

        // name 컬럼은 null
        row.getStringOrEmpty("name").shouldBeEmpty()
        row.getStringOrEmpty("name".toCqlIdentifier()).shouldBeEmpty()
    }

    @Test
    fun `map 은 인덱스를 키로 변환 결과를 반환한다`() = runSuspendIO {
        val row = session.executeSuspending("SELECT id, name, num FROM row_ext_table WHERE id=?", "1").one()
            ?: error("행이 없습니다")

        val mapped = row.map { it?.toString() ?: "" }
        mapped[0] shouldBeEqualTo "1"
        mapped[1] shouldBeEqualTo "Alice"
        mapped[2] shouldBeEqualTo "42"
    }

    @Test
    fun `mapWithName 은 컬럼명을 키로 변환 결과를 반환한다`() = runSuspendIO {
        val row = session.executeSuspending("SELECT id, name, num FROM row_ext_table WHERE id=?", "1").one()
            ?: error("행이 없습니다")

        val mapped = row.mapWithName { it?.toString() ?: "" }
        mapped["id"] shouldBeEqualTo "1"
        mapped["name"] shouldBeEqualTo "Alice"
        mapped["num"] shouldBeEqualTo "42"
    }

    @Test
    fun `toCqlIdentifierMap 은 CqlIdentifier 를 키로 반환한다`() = runSuspendIO {
        val row = session.executeSuspending("SELECT id, name, num FROM row_ext_table WHERE id=?", "1").one()
            ?: error("행이 없습니다")

        val idMap = row.toCqlIdentifierMap()
        idMap["id".toCqlIdentifier()] shouldBeEqualTo "1"
        idMap["name".toCqlIdentifier()] shouldBeEqualTo "Alice"
        idMap["num".toCqlIdentifier()] shouldBeEqualTo 42
    }

    @Test
    fun `mapWithCqlIdentifier 는 CqlIdentifier 를 키로 변환 결과를 반환한다`() = runSuspendIO {
        val row = session.executeSuspending("SELECT id, name, num FROM row_ext_table WHERE id=?", "1").one()
            ?: error("행이 없습니다")

        val mapped = row.mapWithCqlIdentifier { it?.toString() ?: "" }
        mapped["id".toCqlIdentifier()] shouldBeEqualTo "1"
        mapped["name".toCqlIdentifier()] shouldBeEqualTo "Alice"
        mapped["num".toCqlIdentifier()] shouldBeEqualTo "42"
    }
}
