package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import org.junit.jupiter.api.Test

class NetCdfCoordinateSamplerTest {

    @Test
    fun `mutable sample is cleared and read-only copy is isolated`() {
        val target = MutableCoordinateSample()
        target.longitude = 120.0
        target.latitude = 35.0
        target.auxiliary["altitude"] = 100.0

        val copy = target.readOnlyCopy()
        target.clear()
        target.auxiliary["altitude"] = 200.0

        copy.longitude shouldBeEqualTo 120.0
        copy.latitude shouldBeEqualTo 35.0
        copy.auxiliary["altitude"] shouldBeEqualTo 100.0
        target.auxiliary["altitude"] shouldBeEqualTo 200.0
    }

    @Test
    fun `finite geographic coordinates satisfy WGS84 bounds`() {
        validateGeographicCoordinate(0.0, 0.0)
        validateGeographicCoordinate(180.0, 90.0)
        validateGeographicCoordinate(-180.0, -90.0)

        validateGeographicCoordinate(181.0, 0.0).shouldBeFalse()
        validateGeographicCoordinate(0.0, 91.0).shouldBeFalse()
    }
}
