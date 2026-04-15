package io.bluetape4k.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.http.BluetapeHttpServer

abstract class AbstractRetrofitTest {

    companion object: KLogging() {
        @JvmStatic
        protected val httpbinServer by lazy { BluetapeHttpServer.Launcher.bluetapeHttpServer }

        @JvmStatic
        protected val httpbinBaseUrl: String by lazy { httpbinServer.httpbinUrl }
    }

    protected val testBaseUrl: String
        get() = httpbinBaseUrl
}
