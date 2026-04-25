package io.bluetape4k.exposed.clickhouse.engine

import io.bluetape4k.exposed.clickhouse.sanitizeForClickHouse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.Test

class MergeTreeDslTest {

    @Test
    fun `Memory engine toClause`() {
        Memory.toClause() shouldBeEqualTo "ENGINE = Memory()"
    }

    @Test
    fun `Log engine toClause`() {
        Log.toClause() shouldBeEqualTo "ENGINE = Log()"
    }

    @Test
    fun `MergeTree basic with orderBy`() {
        val engine = mergeTree { orderBy("a", "b") }
        val clause = engine.toClause()
        clause shouldContain "ENGINE = MergeTree()"
        clause shouldContain "ORDER BY (a, b)"
    }

    @Test
    fun `MergeTree with partitionBy`() {
        val engine = mergeTree {
            orderBy("a", "b")
            partitionBy("toYYYYMM(c)")
        }
        val clause = engine.toClause()
        clause shouldContain "PARTITION BY toYYYYMM(c)"
    }

    @Test
    fun `MergeTree with settings`() {
        val engine = mergeTree {
            orderBy("a")
            settings("index_granularity" to "8192")
        }
        val clause = engine.toClause()
        clause shouldContain "SETTINGS index_granularity = 8192"
    }

    @Test
    fun `MergeTree requires at least one orderBy`() {
        try {
            mergeTree { /* orderBy 없음 */ }
            error("Should have thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `ReplacingMergeTree with versionColumn`() {
        val engine = replacingMergeTree {
            orderBy("id")
            versionColumn("version")
        }
        val clause = engine.toClause()
        clause shouldContain "ENGINE = ReplacingMergeTree(version)"
        clause shouldContain "ORDER BY (id)"
    }

    @Test
    fun `SummingMergeTree with sumColumns`() {
        val engine = summingMergeTree {
            orderBy("id")
            sumColumns("amount", "count")
        }
        val clause = engine.toClause()
        clause shouldContain "ENGINE = SummingMergeTree(amount, count)"
    }

    @Test
    fun `AggregatingMergeTree basic`() {
        val engine = aggregatingMergeTree {
            orderBy("id")
            partitionBy("toYYYYMM(date)")
        }
        val clause = engine.toClause()
        clause shouldContain "ENGINE = AggregatingMergeTree()"
        clause shouldContain "ORDER BY (id)"
    }

    @Test
    fun `sanitizeForClickHouse removes PRIMARY KEY`() {
        val sql = "CREATE TABLE t (id BIGINT PRIMARY KEY, name VARCHAR(255) NOT NULL)"
        val sanitized = sanitizeForClickHouse(sql)
        sanitized shouldNotContain "PRIMARY KEY"
        sanitized shouldNotContain "NOT NULL"
    }

    @Test
    fun `sanitizeForClickHouse removes CONSTRAINT PRIMARY KEY`() {
        val sql = "CREATE TABLE t (id BIGINT, CONSTRAINT pk PRIMARY KEY (id))"
        val sanitized = sanitizeForClickHouse(sql)
        sanitized shouldNotContain "CONSTRAINT"
        sanitized shouldNotContain "PRIMARY KEY"
    }

    @Test
    fun `sanitizeForClickHouse removes REFERENCES`() {
        val sql = "CREATE TABLE t (user_id BIGINT REFERENCES users(id))"
        val sanitized = sanitizeForClickHouse(sql)
        sanitized shouldNotContain "REFERENCES"
    }

    @Test
    fun `sanitizeForClickHouse removes NULL and NOT NULL`() {
        val sql = "CREATE TABLE t (a INT NOT NULL, b INT NULL, c INT)"
        val sanitized = sanitizeForClickHouse(sql)
        sanitized shouldNotContain "NOT NULL"
        sanitized shouldNotContain " NULL"
    }
}
