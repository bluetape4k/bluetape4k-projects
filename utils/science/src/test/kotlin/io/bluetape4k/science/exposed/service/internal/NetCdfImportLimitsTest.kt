package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.science.exposed.NetCdfException
import org.junit.jupiter.api.Test

class NetCdfImportLimitsTest {

    @Test
    fun `checked product rejects long overflow as typed resource failure`() {
        assertFailsWith<NetCdfException.ResourceLimitExceeded> {
            checkedProduct(Long.MAX_VALUE, 2L)
        }
    }

    @Test
    fun `memory budget accounts for owned working set`() {
        val budget = MemoryBudget(
            tileBufferBytes = 1L,
            coordinateBytes = 2L,
            serializerScratchBytes = 3L,
            duplicateSetBytes = 4L,
        )

        budget.ownedWorkingSetBytes shouldBeEqualTo 10L
    }

    @Test
    fun `tile and batch limits are explicit bounded contracts`() {
        MAX_TILE_CELLS shouldBeEqualTo 65_536L
        MAX_BATCH_ROWS shouldBeEqualTo 1_000L
        MAX_AUXILIARY_JSONB_BYTES shouldBeEqualTo 8_192L
    }
}
