package io.bluetape4k.images.vips.java21

import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag

@Tag("vips-required")
abstract class AbstractJVipsTest {

    companion object : KLogging() {
        @JvmStatic
        @BeforeAll
        fun initRuntime() {
            // -Dvips.enabled=false 이면 명시적 opt-out
            if (System.getProperty("vips.enabled") == "false") {
                assumeTrue(false, "vips tests disabled via -Dvips.enabled=false")
            }
            // libvips 자동 감지: init() 실패 시 skip
            runCatching { JVipsRuntime.init() }.onFailure { e ->
                log.warn("JVipsRuntime.init() failed — skipping vips tests: ${e.message}")
                assumeTrue(false, "libvips not available: ${e.message}")
            }
        }
    }
}
