package io.bluetape4k.aws.kotlin.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CrtHttpEngineSupportTest {

    companion object: KLogging()

    @Test
    fun `crtHttpEngineOf는 CrtHttpEngine 인스턴스를 생성한다`() {
        crtHttpEngineOf().use { engine ->
            log.debug { "CrtHttpEngine: $engine" }
            engine.shouldNotBeNull()
        }
    }

    @Test
    fun `HttpClientEngineProvider default는 Crt singleton을 재사용한다`() {
        val crtEngine = HttpClientEngineProvider.Crt.httpEngine
        val defaultEngine = HttpClientEngineProvider.defaultHttpEngine
        assertSame(crtEngine, defaultEngine)
    }
}
