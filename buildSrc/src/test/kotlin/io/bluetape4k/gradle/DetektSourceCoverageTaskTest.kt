package io.bluetape4k.gradle

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class DetektSourceCoverageTaskTest {

    @Test
    fun `report preserves coverage totals and explicit exclusions`() {
        val report = DetektSourceCoverageReportRenderer.render(
            rows = listOf(
                DetektSourceCoverageRow(":alpha", mainCount = 2, testCount = 1),
                DetektSourceCoverageRow(":beta", mainCount = 0, testCount = 3),
            ),
            exclusions = mapOf(":sample" to "Example sources are excluded."),
        )

        assertContains(report, "- Included modules: 2")
        assertContains(report, "- Kotlin source files: 6 (main: 2, test: 4)")
        assertContains(report, "| `:alpha` | 2 | 1 | 3 |")
        assertContains(report, "- `:sample` — Example sources are excluded.")
        assertEquals(1, report.lineSequence().count { it == "- Empty included modules: 0" })
    }
}
