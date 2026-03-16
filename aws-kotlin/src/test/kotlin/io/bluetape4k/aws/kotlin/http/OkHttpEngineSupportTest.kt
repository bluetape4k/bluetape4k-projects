package io.bluetape4k.aws.kotlin.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class OkHttpEngineSupportTest {

    companion object: KLogging()

    @Test
    fun `okHttpEngineOf는 OkHttpEngine 인스턴스를 생성한다`() {
        okHttpEngineOf().use { engine ->
            log.debug { "OkHttpEngine: $engine" }
            engine.shouldNotBeNull()
        }
    }
}
