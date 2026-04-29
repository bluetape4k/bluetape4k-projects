package io.bluetape4k.images.vips.java25

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag

@Tag("vips-required")
abstract class AbstractFfmVipsTest {

    companion object : KLogging() {
        @JvmStatic
        @BeforeAll
        fun initRuntime() {
            val enabled = System.getProperty("vips.enabled", "false").toBoolean()
            if (!enabled) {
                assumeTrue(false, "libvips not available — set -Dvips.enabled=true to run these tests")
            }
            runCatching { FfmVipsRuntime.init() }.onFailure { e ->
                log.warn("FfmVipsRuntime.init() failed — skipping vips tests: ${e.message}")
                assumeTrue(false, "FfmVipsRuntime initialization failed: ${e.message}")
            }
        }
    }
}
