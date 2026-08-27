package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class NetCdfTilePlannerTest {

    @Test
    fun `planner is deterministic and never exceeds tile cell cap`() {
        val first = NetCdfTilePlanner.plan(rows = 1_024, columns = 1_024)
        val second = NetCdfTilePlanner.plan(rows = 1_024, columns = 1_024)

        first shouldBeEqualTo second
        first.all { it.rowCount.toLong() * it.columnCount.toLong() <= MAX_TILE_CELLS }
            .shouldBeTrue()
        first.sumOf { it.rowCount.toLong() * it.columnCount.toLong() } shouldBeEqualTo 1_048_576L
    }

    @Test
    fun `planner preserves partial edge tiles`() {
        val tiles = NetCdfTilePlanner.plan(rows = 3, columns = 4)

        tiles.size shouldBeEqualTo 1
        tiles.single().rowOrigin shouldBeEqualTo 0
        tiles.single().columnOrigin shouldBeEqualTo 0
        tiles.single().rowCount shouldBeEqualTo 3
        tiles.single().columnCount shouldBeEqualTo 4
    }
}
