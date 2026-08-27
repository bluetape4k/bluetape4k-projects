package io.bluetape4k.science.exposed

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class NetCdfExceptionApiCompatibilityTest {

    @Test
    fun `new typed failures retain structured fields`() {
        val axis = NetCdfException.UnsupportedCoordinateAxis("temperature", "lat", "rank")
        axis.variableName shouldBeEqualTo "temperature"
        axis.coordinateName shouldBeEqualTo "lat"
        axis.reason shouldBeEqualTo "rank"

        val duplicate = NetCdfException.DuplicateCoordinate(1L, "temperature", 2, 3, 120.0, 35.0)
        duplicate.fileId shouldBeEqualTo 1L
        duplicate.variableName shouldBeEqualTo "temperature"
        duplicate.timeIdx shouldBeEqualTo 2
        duplicate.levelIdx shouldBeEqualTo 3

        val resource = NetCdfException.ResourceLimitExceeded("tile", 10L, 11L)
        resource.resource shouldBeEqualTo "tile"
        resource.limit shouldBeEqualTo 10L
        resource.actual shouldBeEqualTo 11L

        val changed = NetCdfException.FileChanged(1L, "expected", "actual")
        changed.expectedFingerprint shouldBeEqualTo "expected"
        changed.actualFingerprint shouldBeEqualTo "actual"

        val corrupt = NetCdfException.CorruptProgress(3L, "checkpoint")
        corrupt.progressId shouldBeEqualTo 3L
        corrupt.detail shouldBeEqualTo "checkpoint"
    }

    @Test
    fun `base exception handling keeps an explicit default branch`() {
        val exception: NetCdfException = NetCdfException.UnsupportedCoordinateAxis("v", null, "test")
        val code = when (exception) {
            is NetCdfException.FileOpen -> "file-open"
            is NetCdfException.FileRecordNotFound -> "file-record"
            is NetCdfException.VariableNotFound -> "variable"
            is NetCdfException.UnsupportedVariable -> "unsupported-variable"
            is NetCdfException.MissingCoordinate -> "missing-coordinate"
            is NetCdfException.UnsupportedProjection -> "projection"
            is NetCdfException.ImportAlreadyRunning -> "already-running"
            is NetCdfException.ImportLeaseLost -> "lease-lost"
            is NetCdfException.UnsupportedCoordinateAxis -> "axis"
            is NetCdfException.DuplicateCoordinate -> "duplicate"
            is NetCdfException.ResourceLimitExceeded -> "resource"
            is NetCdfException.FileChanged -> "changed"
            is NetCdfException.CorruptProgress -> "corrupt"
            else -> "unknown"
        }
        code shouldBeEqualTo "axis"
    }
}
