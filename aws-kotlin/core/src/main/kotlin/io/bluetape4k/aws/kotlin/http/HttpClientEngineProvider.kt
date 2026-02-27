package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine
import io.bluetape4k.utils.ShutdownQueue

object HttpClientEngineProvider {

    object Crt {
        @JvmStatic
        val httpEngine: CrtHttpEngine by lazy {
            crtHttpEngineOf().apply {
                ShutdownQueue.register(this)
            }
        }
    }

    object OkHttp {
        @JvmStatic
        val httpEngine: OkHttpEngine by lazy {
            okHttpEngineOf().apply {
                ShutdownQueue.register(this)
            }
        }
    }

    val defaultHttpEngine get() = Crt.httpEngine
}
