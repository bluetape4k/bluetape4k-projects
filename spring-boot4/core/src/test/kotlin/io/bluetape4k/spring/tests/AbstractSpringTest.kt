package io.bluetape4k.spring.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.http.BluetapeHttpServer

abstract class AbstractSpringTest {
    companion object: KLogging() {
        @JvmStatic
        protected val httpbin by lazy { BluetapeHttpServer.Launcher.bluetapeHttpServer }

        @JvmStatic
        protected val baseUrl by lazy { httpbin.httpbinUrl }
    }
}
